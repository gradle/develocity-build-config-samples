# The manual flow, on any CI

For GitLab, TeamCity, Bamboo, CircleCI, Azure Pipelines, Buildkite, Bitbucket Pipelines, Travis CI,
Bitrise, GoCD and any other CI that runs shell commands, and for Jenkins Freestyle jobs and GitHub
Actions where the Action cannot run: download the CLI, restore before the build, store after it.
How a provider defines steps, conditions and secrets is looked up in its own documentation at run
time; what follows is what the steps must do.

## The key

The onboarding secret already reaches the build as `DEVELOCITY_ACCESS_KEY=<server host>=<key>`.
Make sure the cache commands see it too: a provider that maps secrets per step needs the same mapping
on them. Two forms are derived from it in shell:

- the bare key, for the CLI download's `Authorization: Bearer` header
- `<Edge host>=<key>`, for the CLI with `--dv-edge`

Derive them rather than adding a second secret; add one only when the existing secret cannot reach
these commands. A bare-key secret (Bazel's) is the key itself: `AC_KEY=${DEVELOCITY_ACCESS_KEY:-}`
below. Never print either form, and keep these lines out of `set -x`, which prints them.

On GitLab, a variable marked *Protected* reaches only protected branches and tags: a merge request
from an unprotected branch runs without it, like a fork.

## The setup

POSIX `sh`, run in the job before the restore, in the same shell as the restore and the store.
Values in `<>` come from steps 3 and 5:

```sh
AC_CLI_VERSION=<CLI version>
AC_URL=<Edge URL, or the server URL for the built-in Edge>
AC_HOST=<host of AC_URL>
AC_JAR="$HOME/.develocity/artifact-cache-cli/develocity-artifact-cache-cli-$AC_CLI_VERSION.jar"
AC_KEY=$(printf '%s' "${DEVELOCITY_ACCESS_KEY:-}" | tr ';' '\n' | sed -n 's/^<server host>=//p')

artifact_cache() {
  if [ -z "$AC_KEY" ] || [ ! -s "$AC_JAR" ]; then
    echo "Artifact Cache: $1 skipped, no access key or no CLI"
    return 0
  fi
  DEVELOCITY_ACCESS_KEY="$AC_HOST=$AC_KEY" java -jar "$AC_JAR" "$@" --dv-edge="$AC_URL" \
    || echo "Artifact Cache: $1 failed, the build goes on without it"
}

if [ -n "$AC_KEY" ] && [ ! -s "$AC_JAR" ]; then
  mkdir -p "$(dirname "$AC_JAR")"
  curl --fail --silent --show-error --location \
    --connect-timeout 5 --max-time 120 \
    --retry 3 --retry-delay 3 --retry-max-time 180 \
    --header "Authorization: Bearer $AC_KEY" \
    --output "$AC_JAR" "$AC_URL/edge/assets/artifact-cache-cli/$AC_CLI_VERSION" \
    || { rm -f "$AC_JAR"; echo "Artifact Cache: CLI download failed, caching skipped"; }
fi
```

- **Version**: the latest release, from the Release History. Pin it here; it decides the image
  naming (`image-names.md`).
- **Download**: the Edge serves the CLI from Edge 2.3.0 with Develocity 2026.3.0; the built-in Edge
  is reached at the server URL. An older Edge, or a policy against it, takes
  `https://docs.develocity.ai/downloads/develocity-artifact-cache-cli/develocity-artifact-cache-cli-<version>.jar`
  instead (or an internal mirror), checked against the `.sha256` published next to it; a failed check
  skips caching like a failed download. A failed download never fails the job.
- **Discovery** (step 3): pass `--dv-server=<server URL>` instead of `--dv-edge`, and leave
  `DEVELOCITY_ACCESS_KEY` as it is; the download still goes to the server URL.
- **Java**: `java` must be Java 21 or later. Where the job's default is older, call a Java 21 binary
  by its path. The CLI runs where the job's script runs: in a job that runs inside a container image
  (GitLab's `image:`, a CircleCI Docker executor), that image needs Java 21 and `curl`. Without them
  the job still passes, uncached; say so rather than switch the image unasked.

## Restore, build, store

```sh
artifact_cache restore <image names> <flags> --cache-metrics-file="$HOME/ac-restore.json"
<the build, unchanged>
artifact_cache store <the primary image name> <flags> --cache-metrics-file="$HOME/ac-store.json"
```

- **Restore** after the checkout and after the tool homes are set, before anything downloads.
- **Store only when the build succeeded.** On most providers a later step or command is skipped after
  a failure by default: check the provider's documentation, and make it explicit where it is not.
  - **GitLab**: make the store the last line of `script:`. It runs only when every earlier line
    succeeded, in the same shell. If a shared template owns `script:` and the store has to go in
    `after_script`, that runs after failures too and in a new shell: guard it with
    `if [ "$CI_JOB_STATUS" = "success" ]`, run the setup again there, and mind `after_script`'s own
    short timeout (5 minutes by default).
- **Same flags on both commands**: `--disable-artifact-cache`, `--disable-setup-cache`,
  `--no-autodetect=DOCKER`, home overrides. `--docker-exclude` matters only to `store`.
- **Image names**: TeamCity and Bamboo generate them, so pass none. GitLab and Jenkins multibranch
  below CLI 1.11.0, and every other provider in every version, need explicit names: `image-names.md`.
- **Results**: keep the two metrics files and the CLI log (`~/.develocity/artifact-cache/` by
  default) as job artifacts, also when the job fails. They are written outside the checkout so a
  build that checks for a clean working tree is not affected; copy them in at the end of the job when
  the provider only collects artifacts from there. Step 8 reads them.

## GitLab

An illustration of the shape, with the image-name workaround for CLI versions before 1.11.0:

```yaml
.artifact-cache-setup: &artifact-cache-setup
  - |
    <the setup above>
    # Artifact Cache CLI before 1.11.0 names images after the pipeline, not the job; remove the --image-name options once it is 1.11.0 or later
    AC_IMAGE="${CI_PROJECT_PATH_SLUG}~${CI_JOB_NAME}~${CI_COMMIT_REF_NAME}~${CI_RUNNER_EXECUTABLE_ARCH}"
    AC_FALLBACK="${CI_PROJECT_PATH_SLUG}~${CI_JOB_NAME}~${CI_MERGE_REQUEST_TARGET_BRANCH_NAME:-$CI_DEFAULT_BRANCH}~${CI_RUNNER_EXECUTABLE_ARCH}"

build:
  script:
    - *artifact-cache-setup
    - artifact_cache restore --image-name="$AC_IMAGE" --image-name="$AC_FALLBACK" --cache-metrics-file="$HOME/ac-restore.json"
    - ./gradlew build
    - artifact_cache store --image-name="$AC_IMAGE" --cache-metrics-file="$HOME/ac-store.json"
  after_script:
    - mkdir -p artifact-cache && cp "$HOME"/ac-*.json "$HOME"/.develocity/artifact-cache/*.log artifact-cache/ 2>/dev/null || true
  artifacts:
    when: always
    paths:
      - artifact-cache/
```

- Remove `cache:` entries whose paths are a home the CLI covers (`.gradle/`, `.m2/repository/`,
  `.npm/`, `.cache/pip/`), and keep the variables that point the tools there
  (`GRADLE_USER_HOME: $CI_PROJECT_DIR/.gradle`). A Maven repository moved through `MAVEN_OPTS`
  (`-Dmaven.repo.local=…`) is invisible to the CLI: pass the same path as `--maven-repository`.
- A job that sets its own `before_script` or `script` through `extends` or `!reference` keeps that
  structure; add the lines where the job's commands actually run.

## Jenkins Freestyle, and Pipelines without the plugin

Use the setup above in an *Execute shell* step, with the key from a Secret text credential bound as
`DEVELOCITY_ACCESS_KEY`, and the store in a step that runs only on success. Generated names need the
branch: set `DEVELOCITY_ARTIFACT_CACHE_BRANCH_NAME` and `DEVELOCITY_ARTIFACT_CACHE_BASE_BRANCH_NAME`
(in a multibranch pipeline, from `BRANCH_NAME` and `CHANGE_TARGET`; elsewhere, the branch the job
builds). A multibranch pipeline below CLI 1.11.0 takes explicit names instead: `image-names.md`.
