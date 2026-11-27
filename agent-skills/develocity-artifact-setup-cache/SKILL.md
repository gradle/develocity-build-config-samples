---
name: develocity-artifact-setup-cache
description: Turns on the Develocity Artifact Cache and Setup Cache in CI for a repository that is already onboarded to Develocity. Finds the CI jobs and the tools they run, checks the server, Edge and runner prerequisites, wires the cache into each job (the GitHub Action, the Jenkins plugin, or the CLI's restore and store on any other CI), opens a pull request, and confirms from CI output and Build Scans that the caches are populated and hit. Use when asked to enable, set up or roll out the Artifact Cache, the Setup Cache or dependency caching on CI for a project that already publishes Build Scans.
license: Apache-2.0
compatibility: Needs git and network access. The GitHub CLI (gh) is optional; it is only used for GitHub repositories. actionlint and shellcheck are used when installed.
---

# Develocity Artifact Cache and Setup Cache

Turn on the Develocity Artifact Cache and Setup Cache in CI for a repository that already publishes
Build Scans, hand the change over as a reviewable pull request, and show on its own CI runs that the
caches fill and hit.

One CLI, `develocity-artifact-cache-cli`, serves both caches. It restores before the build and
stores after it, on a Develocity Edge close to the runners. The **Artifact Cache** holds what the
job's tools download: dependencies, wrapper distributions, browsers, Docker images. The **Setup
Cache** holds Gradle's build initialization: compiled scripts, `buildSrc`, included builds. On any
cache error the CLI logs and exits 0, so it never fails a build.

The CI wiring comes from the `artifact-cache-*` samples in the same repository as this skill,
[develocity-build-config-samples](https://github.com/gradle/develocity-build-config-samples),
fetched from `main` at run time, so it is always their most up-to-date, verified version:

| Sample | Used for |
| --- | --- |
| `artifact-cache-github-actions-gradle`, `artifact-cache-github-actions-maven` | the Action's steps (`references/github-actions.md`) |
| `artifact-cache-cli` | `artifact-cache.sh` and the GitLab examples, for every other CI (`references/generic-ci.md`) |

```
https://raw.githubusercontent.com/gradle/develocity-build-config-samples/main/<sample>/<file>
```

If a sample cannot be fetched, the reference files hold what the wiring must do.

What drifts (CLI and Action versions, inputs, requirements) is read from the docs at run time, not
assumed. Every page is served as Markdown at
`https://docs.develocity.ai/artifact-cache/current/<page>/index.md` (`https://docs.develocity.ai/llms.txt`
indexes them). The pages this skill names:

| Page | Path |
| --- | --- |
| System Requirements | `reference/requirements` |
| CLI Commands | `reference/cli-commands` |
| Release History (the CLI) | `release-history` |
| Jenkins Plugin Release History | `jenkins-plugin/release-history` |
| Configuring GitHub Actions, Configuring Jenkins | `how-to/configure-github-actions`, `how-to/configure-jenkins` |
| Download the CLI | `download-the-cli` |
| Understanding Images, Docker Image Caching | `concepts/images`, `concepts/docker-images` |
| Measure the Effectiveness of Artifact Cache | `how-to/measure-cache-effectiveness` |

Reference files (read only the ones the run needs):

- `references/tools.md`: the tools the CLI caches, how to spot them in a job, minimum versions, home directories
- `references/github-actions.md`: wiring with the official GitHub Action
- `references/jenkins.md`: wiring with the Jenkins plugin
- `references/generic-ci.md`: the manual flow (download, restore, store) for every other CI, and where the Action or plugin cannot be used
- `references/image-names.md`: image names per provider, the workaround for CLI versions before 1.11.0, matrix jobs
- `references/verification.md`: reading the restore and store results from CI and Build Scans

## Inputs

| Input | Required | Default |
| --- | --- | --- |
| Repository | no | the current working directory |
| Pull request target | no | the repository and its default branch; asks when the checkout is a fork |
| Edge | no | asks the user (step 3) |
| Artifact Cache | no | enabled |
| Setup Cache | no | enabled |
| Docker images | no | asks when a chosen job uses Docker (step 5) |
| Jobs | no | recommended in step 2, confirmed by the user |
| Tracking reference | no | none; an issue, JIRA ticket or other URL the pull request must cite |

Notes
- Never ask for, read, or write an access key. Onboarding already put one in CI; this skill routes it to the cache.
- If a repository URL was given instead of a local path, clone it into a scratch directory and work there.
- The pull request target is a repository and a base branch, both passed explicitly when the pull request is opened (step 7).

## 1. Check the repository is onboarded

Look for the Develocity configuration the builds already use: a `com.gradle.develocity` plugin and
`develocity {}` block, `.mvn/develocity.xml`, `@gradle-tech/develocity-agent` in `package.json`,
`sbt-develocity`, a Develocity `.bazelrc`, or injection by CI (`develocity-injection-enabled` on
`gradle/actions/setup-gradle`, the Develocity GitLab templates, the Gradle Jenkins plugin). Read the
server URL from it.

- **Nothing found**: stop. This skill builds on an onboarded repository; point the user to the
  `develocity-onboarding` skill, or to the configuration samples in
  [develocity-build-config-samples](https://github.com/gradle/develocity-build-config-samples).
- **Gradle Enterprise only** (`com.gradle.enterprise`, `gradle-enterprise-maven-extension`): below
  every floor in step 4. Ask: migrate to Develocity first (`develocity-onboarding` does it), or go
  on with caches whose results will not show in Build Scans.
- **The cache is already wired** (`gradle/develocity-artifact-cache-github-action`,
  `withDevelocityArtifactCache`, `develocity-artifact-cache-cli` or `artifact-cache.sh` in a
  pipeline): report which jobs have it, review that wiring (*Existing wiring* in step 5), and ask
  whether to fix what the review finds, extend the cache to the other jobs, both, or stop. A review
  with nothing to fix and no job to add ends here, with the findings.

Then find how CI authenticates to Develocity, which the cache reuses (step 5):

- **An access key**: the secret, variable or credential that becomes `DEVELOCITY_ACCESS_KEY`, its
  value format (`<server host>=<key>`, or the bare key for Bazel), and which jobs see it.
- **Workload identity**: a step that exchanges the CI's OIDC token for Develocity (on GitHub, one
  that requests a token from `ACTIONS_ID_TOKEN_REQUEST_URL` with an `audience`). Note the audience
  it requests: the cache's token needs the same one. There is no key to route.
- **Neither** (a server that accepts anonymous Build Scans): the cache still needs a credential. The
  pull request asks for it, and step 8 waits for it.

## 2. Find the CI jobs

Unless given, infer the CI providers from the repository: `.github/workflows/`, `Jenkinsfile`,
`.gitlab-ci.yml`, `.teamcity/`, `.circleci/`, `azure-pipelines.yml`, `.buildkite/`,
`bitbucket-pipelines.yml`, `.travis.yml`, `bitrise.yml`, `bamboo-specs/`. Several can match. If the
pipeline is not in the repository (a job configured on the CI server, a shared library), ask which CI
runs the build and where its definition lives.

For every job that builds, record:

- the commands it runs, and the tools they use (`references/tools.md`), with their versions
- its triggers: pull requests, pushes to the default branch, tags and releases, schedules
- its runner: provider-hosted or self-hosted, ephemeral or persistent, operating system and architecture
- its matrix axes
- the caching it already does: `actions/cache`, a `cache:` input on a setup action, GitLab `cache:`, the Jenkins Job Cacher
- whether it uses Docker, and whether the build runs inside a container the job starts

And for the repository: whether it takes pull requests from forks (on GitHub,
`gh pr list --repo <owner>/<name> --state all --limit 50 --json isCrossRepository`). Those builds get
no credential, so the cache skips them (step 5).

Show this as a table and recommend jobs that run **both on pull requests and on the default branch**:
default-branch builds fill the image that pull requests fall back to, and the pull request's own run
proves the wiring (step 8). Leave out by default, and say why:

- release, publish and deploy jobs: what a release ships should not depend on cache content
- jobs on persistent agents: their local caches are already warm, and a restore only adds work

**Stop when no job runs a supported tool**, and say which tools were found. The CLI detects tools
per job, not per build tool: an sbt or Bazel repository whose jobs run npm, pip, SonarScanner or
Docker still qualifies. Otherwise ask the user to confirm the jobs.

## 3. Prerequisites and topology

### The gate

Most prerequisites live on the server and cannot be read from the repository. Read the current
requirements page, then put each item to the user: yes, no, or unknown. The page wins over this table
where they differ.

| Requirement | Without it |
| --- | --- |
| Develocity 2026.3.0 or later, and Edge 2.3.0 or later | the current CLI is rejected |
| License entitlements `Artifact Cache` and `Setup Cache` | that cache warns on every run |
| The CI key's user may "Read and write Build Cache data" | restores and stores are refused |
| With project-level access control: **Allow data without an associated project** enabled | the CLI stores and restores nothing |
| Edge maximum artifact size of about 1 GiB, when jobs use Playwright, Cypress, Docker or Gradle-provisioned JDKs. Ask for the actual value: step 5 needs it | the default 100 MiB rejects those entries without an error |
| Runners: 2 CPUs, 4 GiB memory, 3000 IOPS, Java 21 for the CLI | the CLI is slow or cannot start |
| Runners reach the server over HTTP/1 and the Edge over HTTP/2; proxies on the way support HTTP/2 | restores fail |

A **no** on the versions or both entitlements: stop and report what is missing. A **no** on one
entitlement: turn that cache off (step 5). An **unknown**: go on, list it under *Before merging* in
the pull request; step 8 shows whether it holds.

### The Edge

Ask which Edge serves the chosen runners:

- **a dedicated Edge**: its URL, passed as `--dv-edge`
- **Develocity's built-in Edge**: the server URL, passed as `--dv-edge`
- **Edge discovery**: the server URL, passed as `--dv-server`; the CLI asks the server for the
  Edge location set on the CI key's user. Only with an access key, and only through the GitHub
  Action or the Jenkins plugin: workload identity tokens and the `artifact-cache-cli` sample script
  need an Edge URL.

`--dv-edge` needs a credential for the Edge host. With the built-in Edge, that is the server host
the existing secret already covers. For a dedicated Edge on its own host, each provider reference
says how the credential gets there; never as a second copy of the key in a new secret.

### Topology

The docs require the Edge in the same availability zone or network as the runners. Classify each
chosen job's runner: provider-hosted (GitHub-hosted, GitLab.com hosted, CircleCI cloud,
Microsoft-hosted Azure agents, Bitbucket Cloud) or self-hosted. A custom runner label can be either:
ask. Provider-hosted runners that reach an Edge elsewhere go over the public internet, where a
restore can take longer than the downloads it replaces. **Warn, and ask whether to proceed or stop.**
Proceeding is legitimate: step 8 can still confirm hits, and whether they save time is not this
skill's claim.

## 4. Build-side floors

The caches work without any build-side change. These versions make them visible and complete; bump
only what is below its floor:

| Component | Floor | Needed for |
| --- | --- | --- |
| Develocity Gradle plugin | 4.4.1 | Setup Cache for `buildSrc` and included builds; 4.4.0 for measurement |
| Common Custom User Data Maven extension | 2.1.0 | any cache activity in Maven Build Scans (below) |
| Develocity Maven extension | 2.4.0 | measurement, and smart cleanup of the image |

Gradle Build Scans get the `ARTIFACT_CACHE` and `SETUP_CACHE` tags and the restore metrics with no
further setup. Maven's do not: the CLI leaves them as a Groovy script in
`~/.m2/.develocity/custom-user-data`, which only the Common Custom User Data Maven extension
evaluates. A Maven job without it shows no cache activity in its Build Scans at all, so add it to
`.mvn/extensions.xml`, as the `artifact-cache-github-actions-maven` sample does, and name it in the
pull request. It also adds CI and Git values to every Build Scan; if the user declines, step 8
relies on CI output alone.

Resolve the version to bump to:

```
curl -s https://plugins.gradle.org/m2/com/gradle/develocity/com.gradle.develocity.gradle.plugin/maven-metadata.xml
curl -s https://repo1.maven.org/maven2/com/gradle/develocity-maven-extension/maven-metadata.xml
curl -s https://repo1.maven.org/maven2/com/gradle/common-custom-user-data-maven-extension/maven-metadata.xml
```

Take `<release>` from each rather than `<latest>`, which can be a pre-release, and skip any `-rc` or
`-milestone` version.

Never upgrade Gradle or Maven here. A tool below its minimum in `references/tools.md` gets nothing
cached; say so for each job it affects.

## 5. Wire the cache into CI

Follow the provider's reference: `references/github-actions.md`, `references/jenkins.md` for
Pipeline jobs, `references/generic-ci.md` for everything else (Jenkins Freestyle and GitHub Actions
without the Action included). Then `references/image-names.md`, for every job. These hold for all of
them:

- **Order**: checkout, then whatever sets the tool homes (`setup-gradle`, a `GRADLE_USER_HOME`
  export), then restore, then the build, then store. The Setup Cache restores into the checkout, so
  the checkout comes first.
- **Store only after a successful build.** A failed build leaves partial caches behind.
- **A cache problem never fails a job.** A missing key (forks), a failed CLI download, an
  unreachable Edge: log it and carry on.
- **One cache per directory.** Remove the provider's own caching of the directories the CLI covers
  (each reference lists them) and name each removal in the pull request. Caches of anything else stay.
  Builds that get no credential (fork pull requests, GitLab merge requests without the protected
  variable) then have no cache at all: when the repository has them, ask whether to keep the
  provider's cache for those builds only (`references/github-actions.md`, *Forks*), and name the
  choice.
- **A cache turned off**, in the inputs or by a missing entitlement (step 3):
  `--disable-artifact-cache` or `--disable-setup-cache`, on both commands.
- **Docker.** Docker images are cached by default since CLI 1.9 whenever a Docker daemon answers,
  whatever pulled them. A job uses Docker when its pipeline runs `docker` or when its build pulls
  images itself: Testcontainers, Quarkus Dev Services and native container builds, image builds
  from Gradle or Maven (`references/tools.md`). GitHub-hosted Linux runners always have a daemon, so
  check the build, not only the pipeline. For each job that uses Docker:
  - **An image layer larger than the Edge's maximum artifact size** (step 3) is rejected without an
    error, and an image with a rejected layer is never restored: its layers are uploaded on every
    store for nothing. Native-image builder, full JDK and browser images can have single layers of
    several hundred MiB. When the limit is below them, or unknown and the job pulls such images, pass
    `--no-autodetect=DOCKER`, or `--docker-exclude` for just those images, with a comment naming the
    reason.
  - Otherwise ask: keep it, or pass `--no-autodetect=DOCKER`. Keeping it adds Edge storage,
    runner-to-Edge traffic and, with replication on, a copy of every layer in Develocity's object
    storage.
  - When kept, add `--docker-exclude` for images tagged per build (a `*-SNAPSHOT` image from
    `bootBuildImage`, `*:pr-*`); see the Docker Image Caching page.
- **Tool homes** are auto-detected. Override them only where the build moves them
  (`references/tools.md`).
- **CLI version.** Determine the version that will actually run (each reference says how) and the
  latest release (the first entry of the Release History; skip pre-releases). The version
  decides the image naming.
- **Credential.** Reuse what step 1 found: the access key, or workload identity with the same
  audience. Never add a second secret holding the same key.

### Existing wiring

When step 1 found the cache already wired, review each wired job against the same rules before
adding anything, and list the findings:

- **Image names** (`references/image-names.md`, *Reviewing existing names*): the workaround missing
  below CLI 1.11.0, names without a project segment, a matrix axis missing, no fallback.
- **CLI version**: a pinned version (`cli-version`, `cliVersion`, a version in a script) below the
  latest release. Propose removing the pin, or raising it, and say what the pinned version lacks.
- **Docker**: kept, while a pulled image has a layer above the Edge's maximum artifact size.
- **Conflicting caches** still on, and **tool homes** the CLI does not see.
- **Store** that also runs after failed builds; a **cache problem** that can fail the job.
- **Build-side floors** (step 4), the CCUD Maven extension on Maven jobs above all.

Fix only what the user picks; leave choices that hold (an existing `cache-read-only` expression, a
`github.repository` segment) as they are. Each fix goes in the pull request with the finding it
answers.

## 6. Check locally

The cache needs CI's key and CI's runners, so it is not run here: the pull request's own run is the
check (step 8). Check what can be checked:

- `actionlint` on every changed workflow, when installed
- `shellcheck` on any shell added, extracted to a scratch file, when installed
- `glab ci lint` for `.gitlab-ci.yml`, when `glab` is authenticated for the project's GitLab
- a Jenkinsfile has no offline linter: say it was not checked
- after a bump in step 4, that the build still loads: `./gradlew help --no-scan`, `./mvnw -B -ntp validate`

If a tool is not available, say so in the pull request instead of claiming it was checked.

**Never commit on the default or integration branch** (`main`, `master`, `develop`, or whatever the
repository merges into). If the checkout is on one, read the target's branch naming first (step 7)
and create a branch; if the user already checked out a feature branch, commit there. Commit only the
files this skill changed. Check `git status` before staging rather than after pushing. If step 7
settles a different name for a branch this skill created, rename it with `git branch -m` before
pushing.

## 7. Prepare and open the pull request

Read the target's own rules before writing anything: `.github/pull_request_template.md`,
`.github/PULL_REQUEST_TEMPLATE/`, `CONTRIBUTING.md` and `AI_POLICY.md`. `CONTRIBUTING.md` is usually
where the binding requirement lives: a tracker the template never mentions, a branch named after the
issue key, a commit trailer. Follow all of them. Use the repository's own branch naming. If there is
no push access, fork and open the pull request from the fork (see *Rules*: the fork is shown and
confirmed first).

**The title is often linted.** Look for a workflow that reads `github.event.pull_request.title`
(`pr-title-check.yml`, `lint-pr.yml`, or a step using `amannn/action-semantic-pull-request`) and take
the rule from its configuration rather than from the template's examples.

**If the project has an AI policy, follow it.** State plainly what was used and what it did. Where
the policy asks for something that cannot be produced here, write that it was not done instead of
ticking the box.

If the template or the contributing guide requires a linked issue or ticket, use the **tracking
reference**. If none was given, offer to file one: show the exact title and body, and offer to create
it as written, edit it first, or take an existing reference instead. Wait for the answer. For a GitHub
issue, write a closing keyword: `Fixes #123`.

Keep the description short:

```
Turns on the Develocity Artifact Cache <and Setup Cache> for <jobs>, served by <the Edge at <URL> | Develocity's built-in Edge | Edge discovery>.

- <files changed, with the Action, plugin or CLI version>
- The cache restores before the build and stores after a successful one; a cache problem never fails a job, and builds without the secret (forks) skip it
- <each cache removed, and the directory the CLI now covers instead>
- <image names: generated, or explicit with the reason, see image-names.md for the wording>
- <Docker images: cached, left out (--no-autodetect=DOCKER), or excluded patterns>
- <each build-side bump or addition, such as the CCUD Maven extension>

Checked with <commands>: <results>. <The caches are to be verified on this pull request's own CI runs. | The caches cannot be verified on this pull request (<a fork without secrets | the job does not run on pull requests>); they are verified on the default branch after the merge.>

Before merging:
- <each prerequisite from step 3 not confirmed, as the action someone has to take>
```

For a review of existing wiring, the first line says what was fixed instead, and each finding is
one bullet.

Add anything that was guessed, and nothing else.

**Then stop and confirm before anything leaves the machine.** Show the user the diff, the branch
name, the target repository and base branch, the title and the description, and wait. Push and open
the pull request only on their go-ahead. Pass the target explicitly: in a fork's clone, `gh pr
create` defaults to the parent repository.

```
gh pr create --repo <owner>/<name> --base <base branch> --head <fork owner>:<branch> --title "…" --body-file <file>
```

`--head` takes the fork owner only when the branch lives in a fork.

## 8. Follow the checks and verify the caches

The pull request is not finished when it is opened. Wait for its checks and read any failure:

```
gh pr checks <n> --repo <owner>/<name> --watch
gh run view <run id> --repo <owner>/<name> --log-failed
```

On another forge, use its CLI or API (`glab ci status`, the Bitbucket pipelines API,
`az pipelines runs show`). If the result cannot be read from here, say the checks were not seen.

Every failure falls into one of three buckets:

- **The change broke the build.** A pipeline edit that is wrong for that provider, a step out of
  order, a removed cache something else relied on. Fix it.
- **The change broke the build only because something is not there yet**: the secret, an
  entitlement, Java 21, the Jenkins plugin. Still the change's fault: nothing in this change may
  fail a build before the prerequisites exist (see *Rules*). Fix it so the missing piece degrades to
  a warning. The Jenkins plugin is the exception that cannot degrade: see `references/jenkins.md`.
- **The repository's own gate**, which the change cannot satisfy: a CLA or DCO sign-off, a required
  approval, a fork without secrets, a signing rule. Say in the pull request which gate is red and
  what unblocks it, and tell the user.

**A fix-up is confirmed like the pull request was.** Show each fix-up's diff and the failure it
answers, and push only on the user's go-ahead. Add commits rather than force-pushing.

Before calling any failure unrelated, pre-existing or flaky, **check the base branch**: look at its
own recent runs of the same job. A failure that is red there is not yours; one that is green there
is. Never assert this without having looked.

**Then verify the caches**, following `references/verification.md`: the first run misses and
stores, a re-run of the same job restores. Re-running is visible to the maintainers, so ask first.
Report per job: what was stored, what was restored (Artifact Cache and Setup Cache entries, size,
failed and missing artifacts), and the Build Scan links; or "not verified", with the reason. Offer to
add the results to the pull request description, and update it only on the user's go-ahead.

## Rules

- **Never read, ask for, write or log an access key**, and never create a secret or a credential.
  Derived key values are masked or kept out of logs.
- **Nothing is created in someone's tracker or CI without them seeing it first**: an issue, a fork,
  a pushed branch, a pull request, a re-run, an edit to the pull request. Show it, and wait.
- **A cache problem never fails a build.** The CLI exits 0 on cache errors; everything this skill
  adds around it (the download, the key derivation, the store) must degrade the same way. The one
  exception is a downloaded CLI that does not match its pinned SHA-256: that stops the job, as the
  GitHub Action's does, by design. Never weaken that check.
- **Store only after a successful build**, and only from the jobs the user chose.
- **Existing caching is removed only for the directories the CLI covers**, and each removal is named
  in the pull request.
- **Hits are reported as confirmed only with numbers.** Savings are not this skill's claim; the
  "Dependency Caching" dashboard measures them over time.
