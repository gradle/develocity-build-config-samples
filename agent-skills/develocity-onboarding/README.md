# develocity-onboarding

An agent skill, in the [Agent Skills](https://agentskills.io) format, that opens a pull request to
onboard a repository to Develocity. It detects the build tool, adapts the official configuration
sample, wires the access key into CI, checks the build still loads, opens the pull request and
follows its checks.

Maven, Gradle (Groovy and Kotlin DSL), npm, sbt, Bazel.

## Install

Put the skill directory in your agent's skills directory. The examples use Claude Code's
(`~/.claude/skills`); other agents read skills from their own location, see their documentation.

From a clone of this repository, symlink it. The link follows the checkout, so pulling updates the
skill:

```bash
ln -s "$(git rev-parse --show-toplevel)/agent-skills/develocity-onboarding" \
  ~/.claude/skills/develocity-onboarding
```

Without cloning, download just the skill directory. Run it again to update:

```bash
mkdir -p ~/.claude/skills && rm -rf ~/.claude/skills/develocity-onboarding && \
  curl -sL https://codeload.github.com/gradle/develocity-build-config-samples/tar.gz/main \
  | tar -xz -C ~/.claude/skills --strip-components=2 \
    develocity-build-config-samples-main/agent-skills/develocity-onboarding
```

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

The skill never asks for an access key and never writes one anywhere.

You can give it the following; anything left out takes the default. The server URL is needed, but
the skill asks for it when it is missing.

| Input | Default |
| --- | --- |
| Develocity server URL | asks the user |
| Repository | the current directory; a Git URL clones to a scratch dir |
| Tracking reference | none; an issue or JIRA URL, for targets whose PR template demands one |
| `allowUntrusted` | left out |
| Local build cache | enabled |
| Remote build cache | enabled |
| CI provider | detected |

## What you get

| Tool | Files |
| --- | --- |
| Maven | `.mvn/develocity.xml`, `.mvn/extensions.xml`, `.gitignore` |
| Gradle | `settings.gradle(.kts)` (the root `build.gradle(.kts)` on Gradle 5), `gradle.properties` |
| sbt | `build.sbt`, `project/plugins.sbt` |
| npm | `develocity.config.js`, `package.json`, the lockfile |
| Bazel | `.bazelrc`, `.gitignore` |

plus, when the CI pipeline is defined in the repository being onboarded, the pipeline change that
exposes `DEVELOCITY_ACCESS_KEY` to the build, looked up in the vendor's own documentation at run
time. When the pipeline lives elsewhere (a job configured on the CI server, a shared pipeline
library), the pull request says what to change there instead.

The configuration comes from the
[official samples](https://github.com/gradle/develocity-build-config-samples), fetched when the skill
runs, with versions resolved from Maven Central, the Gradle plugin portal and the npm public registry. Two things are
added that the samples leave to you:

- **Builds are recognised as CI through the provider's own environment variable** (`GITHUB_ACTIONS`,
  `JENKINS_URL`, …) rather than the generic `CI`, which several providers do not set.
  `references/ci-providers.md` holds the mapping.
- **Writing to the remote cache needs CI *and* a non-empty `DEVELOCITY_ACCESS_KEY`**, so forks and
  local builds only read from it. Bazel fails on an unreachable cache, so there the cache is used
  only when a key is present.

The skill stops and asks rather than overwrite a build that already uses another remote cache or build
event backend, and migrates a Gradle Enterprise configuration instead of adding a second one.

## After the PR

Add `DEVELOCITY_ACCESS_KEY` as a secret of the CI. Create a key on your Develocity server under
*My settings → Access keys*, with an account allowed to publish Build Scans and write to the build
cache. The secret's value depends on the tool, see its authentication docs:

- [Gradle](https://docs.develocity.ai/gradle/current/gradle-plugin/#authenticating),
  [Maven](https://docs.develocity.ai/maven/current/maven-extension/#authenticating),
  [sbt](https://docs.develocity.ai/sbt/current/sbt-plugin/#authenticating),
  [npm](https://docs.develocity.ai/npm/current/npm-agent/#authenticating-with-develocity):
  `<server host>=<key>`, e.g. `develocity.example.com=abc123…`
- [Bazel](https://docs.develocity.ai/bazel/current/bazel-config/#authenticating): the key alone

Unless the server allows anonymous publishing, nothing publishes until it exists.
