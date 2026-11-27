# The manual flow, on any CI

For GitLab, TeamCity, Bamboo, CircleCI, Azure Pipelines, Buildkite, Bitbucket Pipelines, Travis CI,
Bitrise, GoCD and any other CI that runs shell commands, and for Jenkins Freestyle jobs and GitHub
Actions where the Action cannot run: restore before the build, store after it. How a provider defines
steps, conditions and secrets is looked up in its own documentation at run time; what follows is what
the steps must do.

## The script

Use the `artifact-cache-cli` sample's script rather than writing the CLI calls by hand:

```
https://raw.githubusercontent.com/gradle/develocity-build-config-samples/main/artifact-cache-cli/artifact-cache.sh
https://raw.githubusercontent.com/gradle/develocity-build-config-samples/main/artifact-cache-cli/README.md
```

Read the README for its current variables, then commit the script to the repository unchanged, as
the sample says. Per run, it:

1. exchanges the access key at the Edge for a short-lived token (`POST <edge>/api/auth/token`), so
   the key itself never reaches the CLI
2. downloads the CLI from the Edge and checks it against the SHA-256 pinned in the script; the Edge
   serves no checksum, so the pin is the only check
3. names the image `<project>~<job>~<branch>~<os>~<arch>`, and lets `restore` fall back to a base
   branch's image
4. runs the CLI against the Edge, passing any further arguments through

A cache problem warns and exits 0. Two things exit with an error: missing configuration, and a CLI
that fails its checksum, which includes an image with no checksum tool (below). The checksum failure
is meant to stop the job, as the GitHub Action's does. Missing configuration is not, so guard every
call (below).

The CLI version is the one pinned in the script, and the script names every image itself, in every
CLI version: `image-names.md` has nothing to add for it, and nothing to remove at 1.11.0.

If the sample cannot be fetched, write the equivalent in POSIX `sh`, with the same four steps, the
same exit behaviour, and the CLI version and SHA-256 from the Download the CLI page.

## Its configuration

| Variable | Value |
| --- | --- |
| `DEVELOCITY_EDGE_URL` | the Edge from step 3; the server URL for the built-in Edge. Edge discovery is not available here. |
| `ARTIFACT_CACHE_ACCESS_KEY` | the bare key, derived from the existing secret and passed on each call (below) |
| `ARTIFACT_CACHE_PROJECT` | the repository's slug or id. Required: without it, repositories on one Edge share images. |
| `ARTIFACT_CACHE_JOB` | the job name as the pipeline declares it; add the matrix values where the provider's job name lacks them |
| `ARTIFACT_CACHE_BRANCH` | the branch being built; on a pull request build, something unique to it (its source branch or number) |
| `ARTIFACT_CACHE_BASE_BRANCH` | for `restore`: the pull request's target, else the default branch |
| `ARTIFACT_CACHE_JAVA_HOME` | a Java 21 home, where the job's `JAVA_HOME` is older |

Take the values from the provider's predefined variables, looked up in its documentation. The sample's
`gitlab-ci-gradle.yml` and `gitlab-ci-maven.yml` show GitLab's.

## The key

The onboarding secret reaches the build as `DEVELOCITY_ACCESS_KEY=<server host>=<key>`. The script
takes the bare key. Derive it rather than adding a second secret, with `<server host>` replaced by
the real host, and never print it:

```sh
set +x # Jenkins runs shell steps with -x, which would print the derived key
ac_key=$(printf '%s' "${DEVELOCITY_ACCESS_KEY:-}" | tr ';' '\n' | sed -n 's/^<server host>=//p')
```

- **`set +x` first, always.** Jenkins runs *Execute shell* and Pipeline `sh` steps with `sh -xe`, so
  without it the trace prints the bare key. Credentials Binding masks only the whole `host=key`
  value, never the bare key inside it.
- **A bare-key secret** (Bazel's) is the key itself: `ac_key=${DEVELOCITY_ACCESS_KEY:-}`, no `sed`.
- **In every shell that calls the script**: the variable does not survive into another shell. Each
  Jenkins Freestyle *Execute shell* step is its own shell, and so is GitLab's `after_script`.
- **Mask it** where the provider can: `echo "::add-mask::$ac_key"` on GitHub Actions,
  `echo "##vso[task.setsecret]$ac_key"` on Azure Pipelines.
- **Do not export it.** Pass it to the script only (below), so the build never sees the bare key.

With a dedicated Edge, the key's user must be allowed on that Edge; the host in the secret does not
matter, since the script builds the Edge entry itself.

**Workload identity** has no key to derive. Request the CI's OIDC token with the audience the
existing Develocity step requests, exchange it at
`$DEVELOCITY_EDGE_URL/api/auth/token?expiresInHours=2` (`POST`, the OIDC token as
`Authorization: Bearer`), mask the result, and use it as `ac_key`. Ask for
two hours: the script exchanges the token again for one. The sample's verification workflow
(`.github/workflows/artifact-cache-cli-verification.yml`) does this on GitHub Actions. A failed
exchange leaves `ac_key` empty, so the guard below skips the cache. If it is not confirmed that the
Edge's workload identity rule accepts that CI's tokens, leave the job out and say so.

The script stops with an error when the key or the branch is empty. The key is empty in every build
without the secret: forks, and on GitLab a merge request from an unprotected branch when the
variable is *Protected*. The branch is empty when the provider's variable is unset for that event
(CircleCI's `CIRCLE_BRANCH` and Bitbucket's `BITBUCKET_BRANCH` on tag builds, Jenkins' `BRANCH_NAME`
outside multibranch). Give the Edge, project and job literals or variables that are always set, and
guard each call on the other two, so those builds skip the cache instead of failing:

```sh
if [ -n "$ac_key" ] && [ -n "${ARTIFACT_CACHE_BRANCH:-}" ]; then
  ARTIFACT_CACHE_ACCESS_KEY="$ac_key" sh artifact-cache.sh restore
else
  echo "Artifact Cache: no access key or branch, skipping restore"
fi
```

## Restore, build, store

```sh
<guarded> sh artifact-cache.sh restore <flags> --cache-metrics-file="$HOME/ac-restore.json"
<the build, unchanged>
<guarded> sh artifact-cache.sh store <flags> --cache-metrics-file="$HOME/ac-store.json"
```

- **Restore** after the checkout and after the tool homes are set, before anything downloads.
- **Store only when the build succeeded.** On most providers a later step or command is skipped after
  a failure by default: check the provider's documentation, and make it explicit where it is not.
  - **GitLab**: make the store the last line of `script:`, as the sample does. It runs only when
    every earlier line succeeded, in the same shell. `after_script` runs after failures too, in a new
    shell, with its own 5-minute timeout; use it only when a shared template owns `script:`, and then
    guard on `"$CI_JOB_STATUS" = "success"` and derive the key again there.
- **Same flags on both commands**: `--disable-artifact-cache`, `--disable-setup-cache`,
  `--no-autodetect=DOCKER`, home overrides. `--docker-exclude` matters only to `store`.
- **Java**: the CLI runs where the job's script runs. In a job inside a container image (GitLab's
  `image:`, a CircleCI Docker executor), that image needs Java 21, `curl`, and `sha256sum` or
  `shasum`. Without Java or `curl` the script skips the cache and the job passes. Without either
  checksum tool the job **fails**, with an error that the CLI does not match its SHA-256. Check the
  image before wiring the job. When a tool is missing, say so, and leave the job out rather than
  switch the image unasked.
- **Results**: keep the two metrics files and the CLI log (`~/.develocity/artifact-cache/` by
  default) as job artifacts, also when the job fails. They are written outside the checkout so a
  build that checks for a clean working tree is not affected. Where the provider only collects
  artifacts from the checkout, copy them in from a step that also runs after failures: on GitLab,
  `after_script`, with `artifacts: when: always`. Step 8 reads them.

## GitLab

Start from the sample's `gitlab-ci-gradle.yml` or `gitlab-ci-maven.yml`, and fit it into the job:

- Drop the sample's header comment that asks for a separate `ARTIFACT_CACHE_ACCESS_KEY` variable,
  and wrap its calls in the derivation and guard above.
- Remove `cache:` entries whose paths are, or are inside, a home the CLI covers (`.gradle/`,
  `.m2/repository/`, `.npm/`, `.cache/pip/`), and keep the variables that point the tools there
  (`GRADLE_USER_HOME: $CI_PROJECT_DIR/.gradle`).
- A Maven repository moved through `MAVEN_OPTS` (`-Dmaven.repo.local=…`) is invisible to the CLI:
  pass the same path as `--maven-repository` to both commands.
- The Develocity GitLab template for Maven reduces `DEVELOCITY_ACCESS_KEY` to the server's entry.
  The derivation above reads that entry, so it still works.
- A job that sets its own `before_script` or `script` through `extends` or `!reference` keeps that
  structure; add the lines where the job's commands actually run.

## Jenkins Freestyle, and Pipelines without the plugin

Run the script in *Execute shell* steps (Pipeline `sh` steps), with the key from a Secret text
credential bound as `DEVELOCITY_ACCESS_KEY`, and the store in a step that runs only on success. Each
step starts with `set +x` and derives the key again (*The key*). Project and job: `JOB_NAME`
(without `/${JOB_BASE_NAME}` in a multibranch pipeline); branch and base branch: `BRANCH_NAME` and
`CHANGE_TARGET` in a multibranch pipeline, elsewhere the branch the job builds, as a literal.

## Existing manual wiring

A pipeline that already calls the CLI directly keeps working; review it against the list in
`SKILL.md` step 5, and offer the script as a replacement only when the review finds something the
script fixes: no project segment in the names, no checksum check, the raw key passed to the CLI, a
store that also runs after failed builds.
