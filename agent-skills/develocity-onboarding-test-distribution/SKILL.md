---
name: develocity-onboarding-test-distribution
description: Enables Develocity Test Distribution on a Gradle or Maven project that is already connected to Develocity. Picks the test tasks or goals worth distributing from a baseline Build Scan, enables Test Distribution on them iteratively, a few at a time, checks the time saved after each step, and eventually opens a pull request. Use when asked to enable or set up Test Distribution.
license: Apache-2.0
compatibility: Needs git, curl and network access to the Develocity server. The GitHub CLI (gh) is optional; it is only used for GitHub repositories.
---

# Develocity Test Distribution

Enable Test Distribution (TD) on a project and hand the result over as a pull request.

Manual: https://docs.develocity.ai/test-distribution/

Build tool specifics live in `references/<tool>.md` (`gradle.md`, `maven.md`). Read only the one the project needs.
Develocity API access lives in `references/develocity.md`: make every API call as shown in its "Call the API" section.
The snippets in `references/` are examples: never copy their comments into the project.

## Inputs

| Input                                | Default                                                           | Example                                                            |
| ------------------------------------ | ----------------------------------------------------------------- | ------------------------------------------------------------------ |
| Repository                           | the current directory                                             | a local path or a Git URL (cloned to a scratch directory)          |
| Verification task                    | `check` (Gradle), `verify` (Maven)                                | 'integrationTest'                                                  |
| Tasks                                | the `<ELIGIBLE_TASKS>` tasks of the baseline, else all test tasks | a given list of tasks or all test tasks                            |
| Skip verification                    | no                                                                | yes (enables TD without capturing a baseline or running any build) |
| Local executors                      | not limited                                                       | N (applies globally)                                               |
| Remote executors                     | not limited                                                       | N (applies globally)                                               |
| Enable condition                     | `true`                                                            | `only on CI` (applies globally)                                    |
| `localOnly` patterns                 | none                                                              | `*IntegrationTest` (applies globally)                              |
| Capability requirements              | none                                                              | `os=windows` (applies globally)                                    |
| `<ELIGIBLE_TASKS>`                   | serial test time > 60s, spread over at least 5 test classes       | serial test time > 5m, spread over at least 3 test classes         |
| `<TASKS_TO_INTEGRATE_PER_ITERATION>` | 5                                                                 | N                                                                  |
| `<MAX_ITERATIONS>`                   | 10                                                                | N                                                                  |

"Task" below means a Gradle test task or a Maven Surefire/Failsafe goal execution.
The verification build runs the verification task with the build cache disabled, as shown in `references/<tool>.md`.
The scope is the set of tasks considered for Test Distribution. Without a baseline, it falls back to the Tasks input, else all test tasks.

## 1. Check the project requirements

Inspect the code to verify the requirements, stop and say why if any of these is true:

- The project is not connected to Develocity (see `references/<tool>.md`). Point to the `develocity-onboarding` skill.
- The project uses the Gradle Enterprise plugin or extension instead of Develocity. It is not supported.
- The build tool or Java version is below what [TD supports](https://docs.develocity.ai/test-distribution/#test-compatibility) (`references/<tool>.md`).

## 2. Plugin or extension version

If the Develocity plugin or extension is not the latest (`references/<tool>.md`), say an upgrade is recommended and ask for the Develocity server version.
Upgrade to the latest version compatible with it (`references/<tool>.md`).

If the user declines and the version is below the minimum in `references/<tool>.md`, stop.

## 3. Baseline

When verification is skipped, skip this step: the scope falls back to the Tasks input, else all test tasks.

The baseline is the project as it is before the skill, including any task that already has TD.

1. Verify the Develocity API can be accessed (`references/develocity.md`). 
   If not:
    - tell the user the status code, and that setting the key for this server in `DEVELOCITY_ACCESS_KEY` or the keys file, then running the skill again, brings verification back
    - do not capture a baseline, fall back to the Tasks input, else all test tasks, and skip verification from now on
2. Show the user the verification build command, tagged `td-baseline`, and run it once they agree.
3. If a test fails: 
   - tell the user which ones
   - ask the user to fix them and to confirm when it is done, then run the verification build command again. If they would rather not, fall back to the Tasks input, else all test tasks, and skip verification from now on.
4. Otherwise take the Build Scan ID from the output and read the test performance (`references/<tool>.md`).
   - If it can be read, the scope is the `<ELIGIBLE_TASKS>` tasks, unless a list or all test tasks were given. Leave out tasks that already have TD (`testAcceleration.testDistribution.usageStatus` is `enabled`). `serialExecutionTime` is in milliseconds; the class count is `tests.testClassesCount`. Sort them longest first. If the scope is empty, tell the user and ask for a list; without one, go to step 6 with the preparation only.
   - Else, fall back to the Tasks input, else all test tasks, and skip verification from now on.

## 4. Share the inputs

List every input with the value that will be used, given or default, and the scope with each task's baseline duration and test class count (`n/a` without a baseline). When the scope fell back to the Tasks input or all test tasks, say so and why (verification skipped, API not accessible, failing tests, test performance not readable).
Let the user change any of them before going on.

## 5. Iterations

An iteration takes the next `<TASKS_TO_INTEGRATE_PER_ITERATION>` tasks of the scope, longest first when there is a baseline, else in the order the user gave (or in alphabetical order when all test tasks are selected). Iterations are numbered from 1.
When verification is skipped, a single iteration takes the whole scope.

They end when the scope is done, after `<MAX_ITERATIONS>`, or when the user stops. The cap does not apply to the iteration that follows skipping verification (5.3). Stopping before the first one keeps only the preparation done so far (plugin or extension upgrade) and goes to step 6 without enabling TD on any task.

### 5.1 Share the scope

Show the iteration number and its tasks (numbered), and let the user edit them (eg. remove or reorder tasks by their number) or stop.

### 5.2 Enable TD

For each task of the iteration:

#### 5.2.1 Verify requirements

Inspect the code to verify the requirements on each task, skip the task if any of these is true:
  - The test plugin is below what [TD supports](https://docs.develocity.ai/test-distribution/#test-compatibility) (`references/<tool>.md`).
  - The test doesn't use a [compatible](https://docs.develocity.ai/test-distribution/#frameworks_and_languages) engine.

#### 5.2.2 Apply configuration

If TD is already enabled on the task, skip it (skipped).

Set up the JUnit Platform if not already done, as shown in `references/<tool>.md`.

Enable TD on the task with the following settings:
- `enabled` set to `true`
- local and remote executors, when given
- `localOnly` patterns when given
- requirements when given

Configure each task in its own project or module, never in a place shared with other tasks.

[Quarkus application tests](https://docs.develocity.ai/test-distribution/#quarkus_application_tests): when a task runs `@QuarkusTest` classes, add the Quarkus configuration, unless the `localOnly` patterns exclude them.
Classes annotated with `io.quarkus.test.junit.QuarkusIntegrationTest` are not distributed: add the annotation to `localOnly`.
Quarkus below 3.31.1 is not supported: make its `@QuarkusTest` classes `localOnly` too, and say so in the pull request.

### 5.3 Verify

Skip this when verification is skipped, or when TD was enabled on no task of this iteration (go to 5.4).
The checks below only cover the tasks TD was enabled on in this iteration, and in previous ones for the time comparison.

1. Run the verification build tagged `td-exp<n>`, with `n` the iteration number. 
2. Take the Build Scan ID from the output and read the test performance (`references/<tool>.md`).
   No agent ran the tests when `usageStatus` is `enabled` and `remoteTestClassesCount` is 0 or null on every task TD was enabled on in this iteration, or when the output shows a TD error (authentication, missing "Use Test Distribution" permission, WebSocket connection). Then:
   - Tell the user that no agent ran the tests and provide possible reasons (e.g., no connected agents, missing capabilities, TD errors).
   - Inspect the TD agent configuration (`references/develocity.md`) and share with the user the available agents and their capabilities.
   - Skip verification from now on; the scope stays as it is. TD stays on the tasks of this iteration, unverified; the rest of the scope is enabled in one more iteration.
3. Among those tasks, one with `usageStatus` `disabled` has a configuration problem, `unavailable` an old plugin or extension or an error in the output, and `enabled` with no remote test class a requirement no agent matches: fix it and run again. If it cannot be fixed, revert TD on the task (skipped).
4. If a test fails, fix it using the [troubleshooting guide](https://docs.develocity.ai/test-distribution/#dealing_with_common_problems), mainly [classes sharing static state](https://docs.develocity.ai/test-distribution/#classes-sharing-static-state) and [tests reading files from `src/test/resources`](https://docs.develocity.ai/test-distribution/#tests_reading_files_from_src_test_resources), or your best judgement. If it cannot be fixed, add the class to the task's `localOnly` patterns. If most of a task's classes end up `localOnly`, revert TD on it (skipped). Run again.
5. Read the test performance as in step 5.3.2. For each task the skill enabled TD on, those of previous iterations included, compare `wallClockExecutionTime` with the baseline: the difference in %. If no time is saved on a task, revert all changes made on it, tell the user, and stop the iterations.

### 5.4 Share the results

Show every task processed so far, across all iterations: status (TD enabled, not verified, skipped with the reason, reverted because no time was saved), wall clock time and difference with the baseline, `n/a` when there is no value.

## 6. Pull request

Replace `enabled` = `true` with the enable condition, when one was given. When TD ended up on every test task with a similar configuration, move the common part to the shared place shown in `references/<tool>.md`, and keep what differs in the project or module of the task. Then run the load check from `references/<tool>.md`.
Never commit on the default branch. Follow the target's pull request template and `CONTRIBUTING.md`.

The description has:

- a table of the tasks with TD enabled: name, baseline duration, test class count (only when a subset of the classes is distributed); `n/a` where there is no value
- the other changes: plugin or extension upgrade, JUnit Platform, test class fixes, `localOnly` entries
- the decisions taken: tasks reverted and why, the iteration size, Quarkus, verification skipped and why
- the number of iterations and why they stopped: scope done, `<MAX_ITERATIONS>` reached, no time saved on a task (reverted), or the user asked to stop
- the results of step 5.4, folded, with the Build Scan links:

  ```
  <details><summary>Iterations</summary>

  | Iteration | Task | Status | Wall clock time | Difference |
  | --- | --- | --- | --- | --- |

  </details>
  ```
- the result of the agents check, when it ran
- when TD was not enabled on any task, the reason and that TD is only prepared
- next steps:
  - when no agent ran the tests, or verification was skipped: create an access key for a user with the "Use Test Distribution" permission, and make sure an agent with the required capabilities is running
  - always: run the build once with the enable condition met (e.g. on CI) and check in the Build Scan that the tests were distributed, since the final configuration is only checked for loading
  - when tasks of the scope are left: run this skill again

Show the user the diff, branch, title and description, and create the pull request only once they agree.

## Rules

- When local executors is 0, a `localOnly` entry is an error for TD. Whenever one would be added (input, test fix, Quarkus), tell the user and ask what to change.
- Never print an access key or write it to a file.
- Nothing is pushed or created remotely without the user seeing it first.
