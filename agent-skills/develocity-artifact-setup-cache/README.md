# develocity-artifact-setup-cache

An agent skill, in the [Agent Skills](https://agentskills.io) format, that opens a pull request to
turn on the [Develocity Artifact Cache and Setup Cache](https://docs.develocity.ai/artifact-cache/)
in CI for a repository that is already onboarded to Develocity. It finds the CI jobs and the tools
they run, checks the prerequisites, wires the cache into each job, opens the pull request, follows its
checks, and confirms from CI output and Build Scans that the caches are populated and hit.

GitHub Actions (through the official Action), Jenkins Pipelines (through the official plugin), and
any other CI that runs shell commands. The Artifact Cache covers Gradle, Maven, npm, pip, Cypress,
Playwright, SonarScanner and Docker images; the Setup Cache covers Gradle.

A repository that does not publish Build Scans yet starts with
[`develocity-onboarding`](../develocity-onboarding).

## Install

Put the skill directory in your agent's skills directory. The examples use Claude Code's
(`~/.claude/skills`); other agents read skills from their own location, see their documentation.

From a clone of this repository, symlink it. The link follows the checkout, so pulling updates the
skill:

```bash
ln -s "$(git rev-parse --show-toplevel)/agent-skills/develocity-artifact-setup-cache" \
  ~/.claude/skills/develocity-artifact-setup-cache
```

Without cloning, download just the skill directory. Run it again to update:

```bash
mkdir -p ~/.claude/skills && rm -rf ~/.claude/skills/develocity-artifact-setup-cache && \
  curl -sL https://codeload.github.com/gradle/develocity-build-config-samples/tar.gz/main \
  | tar -xz -C ~/.claude/skills --strip-components=2 \
    develocity-build-config-samples-main/agent-skills/develocity-artifact-setup-cache
```

## Use

Ask your agent, from the repository's checkout:

```
Turn on the Develocity Artifact Cache and Setup Cache in CI for this repository
```

Or from anywhere, with a repository URL, which it clones into a scratch directory, and the Edge:

```
Enable the Develocity Artifact Cache for https://github.com/acme/widget, using the Edge at https://edge.example.com
```

Agents with slash commands can also call it by name, e.g. `/develocity-artifact-setup-cache` in
Claude Code.

The skill never asks for an access key: it routes the one CI already has from onboarding.

| Input | Default |
| --- | --- |
| Repository | the current directory; a Git URL clones to a scratch dir |
| Pull request target | the repository and its default branch |
| Edge | asks: a dedicated Edge, Develocity's built-in Edge, or Edge discovery (access key with the GitHub Action or Jenkins plugin only) |
| Artifact Cache | enabled |
| Setup Cache | enabled |
| Docker images | asks when a job uses Docker |
| Jobs | recommended from the CI configuration, confirmed by you |
| Tracking reference | none; an issue or JIRA URL, for targets whose PR template demands one |

## Before you start

These live on the Develocity side, and the skill cannot set them up. It asks about each, and the pull
request lists any that are not confirmed:

- Develocity 2026.3.0 or later, and a Develocity Edge 2.3.0 or later in the same network or
  availability zone as the CI runners
- the `Artifact Cache` and `Setup Cache` entitlements on the license
- a CI access key whose user may read and write Build Cache data and, with project-level access
  control, **Allow data without an associated project** enabled
- an Edge maximum artifact size of about 1 GiB when jobs use Playwright, Cypress, Docker or
  Gradle-provisioned JDKs
- on Jenkins, the Develocity Artifact Cache plugin, installed from its `.hpi` by an administrator

See the [System Requirements](https://docs.develocity.ai/artifact-cache/current/reference/requirements/)
for the full list.

## What you get

| CI | Change |
| --- | --- |
| GitHub Actions | a `gradle/develocity-artifact-cache-github-action` step in each chosen job |
| Jenkins Pipeline | the build steps wrapped in `withDevelocityArtifactCache` |
| Any other CI, Jenkins Freestyle | the `artifact-cache-cli` sample's `artifact-cache.sh`, run before the build to restore and after it to store |

plus the removal of the CI's own caching of the directories the CLI now covers (each named in the
pull request), the image names the CLI version needs, and any build-side change the cache values in
Build Scans depend on: the Common Custom User Data Maven extension for Maven builds.

The wiring follows the `artifact-cache-*` samples in this repository, fetched when the skill runs.
It reuses the credential CI already has for Develocity: the access key secret, or workload identity
with the audience the workflow already requests.

Until the Artifact Cache CLI 1.11.0, generated image names are wrong on GitHub Actions, GitLab and
Jenkins multibranch pipelines, so the skill writes explicit names there, marked to be removed once
the CLI is 1.11.0 or later. On GitHub Actions and GitLab they are written to match the names 1.11.0
is to generate, so removing them should keep the cache, and at worst starts it cold once.

On a repository where the cache is already wired, the skill reviews that wiring instead (image names,
a pinned CLI version, Docker layers above the Edge's size limit, conflicting caches) and offers to
fix what it finds, and to extend the cache to the jobs that lack it.

After the pull request opens, the skill watches its checks and, with your go-ahead, re-runs a job
to show the second run restoring what the first one stored: entries and size from CI, and the cache
values from the Build Scan. Pull requests from forks get no secrets, so for those it verifies on the
default branch after the merge.
