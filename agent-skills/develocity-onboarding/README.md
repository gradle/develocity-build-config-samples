# develocity-onboarding

An agent skill, in the [Agent Skills](https://agentskills.io) format, that opens a pull request to
onboard a repository to Develocity. It detects the build tool, adapts the official configuration
sample, wires the access key into CI, checks the build still loads, opens the pull request and
follows its checks.

Maven, Gradle (Groovy and Kotlin DSL), npm, sbt, Bazel.

## Install

Make the skill directory available to your agent by linking or copying it into the agent's skills
directory. With Claude Code, for example:

```bash
ln -s "$(git rev-parse --show-toplevel)/agent-skills/develocity-onboarding" \
  ~/.claude/skills/develocity-onboarding
```

Other agents read skills from their own location; see their documentation. A symlink follows the
checkout, so pulling updates the skill.

## Use

Ask your agent, from the repository you want to onboard:

```
Onboard this repository to Develocity using https://develocity.example.com
```

Or from anywhere, with a repository URL, which it clones into a scratch directory:

```
Onboard https://github.com/acme/widget to Develocity using https://develocity.example.com
```

Agents with slash commands can also call it by name, e.g. `/develocity-onboarding https://develocity.example.com` in Claude Code.

It never asks for an access key and never writes one anywhere.

| Input | Required | Default |
| --- | --- | --- |
| Develocity server URL | no | asks the user |
| Repository | no | the current directory; a Git URL clones to a scratch dir |
| Tracking reference | no | none; an issue or JIRA URL, for targets whose PR template demands one |
| `allowUntrusted` | no | left out |
| Local build cache | no | enabled |
| Remote build cache | no | enabled |
| CI provider | no | detected |

## What you get

| Tool | Files |
| --- | --- |
| Maven | `.mvn/develocity.xml`, `.mvn/extensions.xml`, `.gitignore` |
| Gradle | `settings.gradle(.kts)`, `gradle.properties` |
| sbt | `build.sbt`, `project/plugins.sbt` |
| npm | `develocity.config.js`, `package.json`, the lockfile |
| Bazel | `.bazelrc`, `.gitignore` |

plus the pipeline change that exposes `DEVELOCITY_ACCESS_KEY` to the build, looked up in the vendor's
own documentation at run time rather than from a list that goes stale.

The configuration comes from the
[official samples](https://github.com/gradle/develocity-build-config-samples), fetched when the skill
runs, with versions resolved from Maven Central, the Gradle plugin portal and npm. Two things are
added that the samples leave to you:

- **CI is detected by the provider's own environment variable**, not `CI`, for the reasons in the
  *Rules* of `SKILL.md`. `references/ci-providers.md` holds the mapping.
- **Writing to the remote cache needs CI *and* a non-empty `DEVELOCITY_ACCESS_KEY`**, so forks and
  local builds only read from it. Bazel fails on an unreachable cache, so there the cache is used
  only when a key is present.

It stops and asks rather than overwrite a build that already uses another remote cache or build
event backend, and migrates a Gradle Enterprise configuration instead of adding a second one.

## After the PR

Add `DEVELOCITY_ACCESS_KEY` as a repository secret. Create a key on your Develocity server under
*My settings → Access keys*; the secret's value depends on the tool:

- Gradle, Maven, sbt, npm: `<server host>=<key>`, e.g. `develocity.example.com=abc123…`
- Bazel: the key alone

Nothing publishes until it exists.

## Adapting it

Nothing is generated from code: the Markdown is the whole skill. `SKILL.md` holds the steps and the
rules; each build tool's changes, merge rules and checks are in `references/<tool>.md`, so a run
reads only the one it needs. `references/ci-providers.md` maps CI provider to identifying
environment variable, and `references/gradle-enterprise-migration.md` covers older configurations.
