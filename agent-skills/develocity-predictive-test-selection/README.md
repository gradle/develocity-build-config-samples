# develocity-predictive-test-selection

An agent skill, in the [Agent Skills](https://agentskills.io) format, that gets a Gradle or Maven
repository already onboarded to Develocity ready for
[Predictive Test Selection](https://docs.develocity.ai/current/guides/predictive-test-selection/)
(PTS), tells you from the PTS Simulator's numbers when it is ready, and then opens a pull request
that turns PTS on for local builds and pull request builds.

## Install

Put the skill directory in your agent's skills directory. The examples use Claude Code's
(`~/.claude/skills`); other agents read skills from their own location, see their documentation.

From a clone of this repository, symlink it. The link follows the checkout, so pulling updates the
skill:

```bash
ln -s "$(git rev-parse --show-toplevel)/agent-skills/develocity-predictive-test-selection" \
  ~/.claude/skills/develocity-predictive-test-selection
```

Without cloning, download just the skill directory. Run it again to update:

```bash
mkdir -p ~/.claude/skills && rm -rf ~/.claude/skills/develocity-predictive-test-selection && \
  curl -sL https://codeload.github.com/gradle/develocity-build-config-samples/tar.gz/main \
  | tar -xz -C ~/.claude/skills --strip-components=2 \
    develocity-build-config-samples-main/agent-skills/develocity-predictive-test-selection
```

## Use

From the repository, ask your agent:

```
Is this repository ready for Predictive Test Selection?
```

That runs **simulate**, the default. Once it reports tasks as ready:

```
Turn on Predictive Test Selection for this repository
```

That runs **enforce**. Agents with slash commands can also call it by name, e.g.
`/develocity-predictive-test-selection` in Claude Code.

| Input | Default |
| --- | --- |
| Repository | the current directory; a Git URL clones to a scratch dir |
| Mode | `simulate`; `enforce` when you ask to enable PTS |
| Test tasks or goals in scope | every supported one, which you confirm |
| Tracking reference | none; an issue or JIRA URL, for targets whose PR template demands one |

The server URL comes from the repository's Develocity configuration.

## What you get

**simulate** changes nothing about which tests run. It:

- lists every test task or goal, its test framework and engines, and whether PTS can run on it
- fixes what blocks PTS, in a pull request: a test engine missing for JUnit 4 or TestNG, the Cucumber
  Companion, file fingerprint capture turned off, a plugin or extension too old
- asks the server, with a dry run that publishes nothing, whether it can predict each task yet
- reads the Simulator's numbers for the last 28 days and gives each task a verdict: **ready**,
  **review**, **not yet** (and when to check again) or **blocked** (and who has to act)

A task is ready when the Simulator has enough data for it (at most 10% of its test time without
enough history), it failed at least once in the window, PTS would have caught its failing runs at
the rate its duration calls for (99% for tasks under a minute, down to 90% for tasks over ten), and
the server confirms it can predict it.

**enforce** opens a pull request for the ready tasks:

| Builds | PTS |
| --- | --- |
| local | on |
| pull request jobs | on, through `-PptsOnCi` (Gradle) or `-DptsEnabled=true` (Maven) |
| main branch, merge queue, nightly, release, deploy | off: they run every test and fill the build cache |
| any build with `-Dpts.enabled=false` | off |

| Tool | Files |
| --- | --- |
| Gradle | `settings.gradle(.kts)`, or the convention plugin that configures tests; the CI pipeline |
| Maven | the POM that configures Surefire (and Failsafe); the CI pipeline |

## Access keys

The skill never asks for, prints or stores a key. The build tool and the Simulator script use your
own key for the server: `DEVELOCITY_ACCESS_KEY`, or the one in `~/.gradle/develocity/keys.properties`
or `~/.m2/.develocity/keys.properties`. Reading the Simulator needs the **View Build Scans**
permission; CI's key needs **Use Predictive Test Selection** once PTS is on, or pull request builds
run every test.

## Before you start

- The server's license must enable the PTS Simulator or PTS. Ask your Develocity administrator or
  your Solutions Engineer.
- CI must publish Build Scans for the test tasks, with file fingerprints on. The Simulator needs at
  least two weeks of them; eight gives a well-trained model.

Onboarding a repository to Develocity first is the job of
[`develocity-onboarding`](../develocity-onboarding).
