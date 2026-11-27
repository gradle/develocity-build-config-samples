# develocity-onboarding-test-distribution

An agent skill, in the [Agent Skills](https://agentskills.io) format, that enables
[Test Distribution](https://docs.develocity.ai/test-distribution/) on a Gradle or Maven project
already connected to Develocity, and opens a pull request.

## Install

```bash
ln -s "$(git rev-parse --show-toplevel)/agent-skills/develocity-onboarding-test-distribution" \
  ~/.claude/skills/develocity-onboarding-test-distribution
```

## Use

```
Enable Test Distribution on this project
```

With inputs:

```
Enable Test Distribution on https://github.com/acme/widget, only on CI, with at most 4 remote executors and os=linux as a requirement
```

## Inputs

All optional. The skill shows the values it will use and lets you change them before Test Distribution is enabled.

| Input                                | Default                                                           | Example                                                            |
| ------------------------------------ | ----------------------------------------------------------------- | ------------------------------------------------------------------ |
| Repository                           | the current directory                                             | a local path or a Git URL (cloned to a scratch directory)          |
| Verification task                    | `check` (Gradle), `verify` (Maven)                                | 'integrationTest'                                                  |
| Tasks                                | the `<ELIGIBLE_TASKS>` tasks of the baseline, else all test tasks | a given list of tasks or all test tasks                            |
| Skip verification                    | no                                                                | yes (enables TD without capturing a baseline or running any build) |
| Non-interactive                      | no                                                                | yes (never asks, see Non-interactive mode)                         |
| Local executors                      | not limited                                                       | N (applies globally)                                               |
| Remote executors                     | not limited                                                       | N (applies globally)                                               |
| Enable condition                     | `true`                                                            | `only on CI` (applies globally)                                    |
| `localOnly` patterns                 | none                                                              | `*IntegrationTest` (applies globally)                              |
| Capability requirements              | none                                                              | `os=windows` (applies globally)                                    |
| `<ELIGIBLE_TASKS>`                   | serial test time > 60s, spread over at least 5 test classes       | serial test time > 5m, spread over at least 3 test classes         |
| `<TASKS_TO_INTEGRATE_PER_ITERATION>` | 5                                                                 | N                                                                  |
| `<MAX_ITERATIONS>`                   | 10                                                                | N                                                                  |

## Process

1. Check the requirements and upgrade the Develocity plugin or extension
2. Check the Develocity API can be accessed, then run the verification build as a baseline. When tests fail, you fix them, or verification is skipped. Verification is also skipped when the API or the results cannot be read. Without a baseline, the tasks fall back to the Tasks input, else all test tasks
3. Show the inputs
4. Iterate on the tasks, `<TASKS_TO_INTEGRATE_PER_ITERATION>` at a time, up to `<MAX_ITERATIONS>`. Each iteration:
   - shows its tasks; you can change them or stop
   - enables Test Distribution on them
   - runs the verification build preferring agents for the tests (until the pull request), stopped when it runs over 3 times the baseline, fixes failing tests or makes them `localOnly`, and compares each task to the baseline. A task with no time saved is reverted and the iterations stop. If no agent runs the tests, verification is skipped from then on
   - shows the results so far: status, wall clock time and difference with the baseline

When nothing is left to change, e.g. the only task was reverted because no time was saved, the skill says so and prepares no pull request.

With verification skipped, no build runs: a single iteration enables Test Distribution on the Tasks input, else all test tasks.

## Output

A pull request to review, with:

- the Test Distribution configuration of the selected tasks
- the JUnit Platform setup, when missing
- the Develocity plugin or extension upgrade, when not the latest
- fixes or `localOnly` entries for test classes that would not run remotely

## Next steps

Listed in the pull request:

- when tests did not run on agents, or verification was skipped: create an access key for a user with the "Use Test Distribution" permission, and make sure an agent with the required capabilities is running
- always: run the build once with the enable condition met (e.g. on CI) and check in the Build Scan that the tests were distributed
- when tasks are left: run the skill again
