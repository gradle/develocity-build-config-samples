# Reading the selection output

The plugin and the extension print one line per test task or goal. None of these fail the build.

## The server predicted

```
Predictive Test Selection: 34 of 37 test classes selected with profile 'Standard' (saving 4.097s serial time)
```

The server is **predicting** for the task when it names fewer selected classes than it has. The numbers from a dry run are
not representative: the dry-run flag is itself an input PTS sees as a change. The Simulator gives the
savings.

`Predictive Test Selection: all test classes selected` (shown with `--info` / `-X`) means the server
answered and chose to run everything. Read the per-class reasons below to see why.

## Per-class reasons

Logged at `--info` (Gradle) or `-X` (Maven) as `<category>: <reason> (<n>x) - <classes>`. Two of
them mean the history is still too short:

| Reason | Means | Status |
| --- | --- | --- |
| `Test task/goal has fewer than fourteen days of test execution history` | the server has seen this task for less than 14 days | **not yet**: rerun once 14 days have passed since CI started publishing scans for it |
| `Test has fewer than fifty executions, which is insufficient for prediction` | that class ran fewer than 50 times (flaky runs do not count) | **not yet** for those classes; others may already be predicted |

The other reasons (`High probability test affected by recent changes`, `Test or test fixtures have
changed`, `Test has failed in the last ten executions`, `Test was flaky within the last seven days`,
`Test was selected in order to meet the required percentage of test executions`, ...) are normal
selection decisions, not readiness problems.

## The server did not predict

`Predictive Test Selection selected all test classes due to: <reason>`, and every test runs:

| Reason | Means | Action |
| --- | --- | --- |
| `Insufficient change history available for task/goal.` | the server has never seen this task with input data | make CI publish Build Scans for it, with file fingerprints on; then wait 14 days |
| `Client not authorized to use Predictive Test Selection.` | no key, an expired key, or a key without the "Use Predictive Test Selection" permission | the user provisions their own key (`./gradlew provisionDevelocityAccessKey`, `./mvnw com.gradle:develocity-maven-extension:provision-access-key`) or asks an admin for the permission; the skill never handles the key |
| `Predictive Test Selection extension disabled on Develocity license.` | neither PTS nor the Simulator is enabled on the server | your Develocity administrator or Solutions Engineer enables it |
| `Recent test executions have not been processed.` | the server is more than 30 minutes behind on this task | retry later; if it persists, the admin checks server memory and the background processor |
| `Test selection request took too long to process.` | the server did not answer in time | retry; if it persists, as above |
| `Build was unable to contact Develocity server.` | network or proxy | check the server URL and proxy; plugin below 3.17.4 or extension below 1.21.3 ignore proxy settings |
| `The build was running in offline mode.` | `--offline` / `-o` | run without it |
| `Test tasks from buildSrc and included builds are currently not supported.` | as it says | leave the task out of scope |
| `Test task/goal inputs could not be determined.` | the inputs could not be fingerprinted | report it with the task |
| `Build plugin/extension is no longer supported; please upgrade it to use Predictive Test Selection.` | too old | bump it |
| `Build plugin encountered an internal error.`, `Predictive Test Selection encountered an internal error.` | a bug | report it with the build output |

## Not a selection line

- `Predictive Test Selection only supports JUnit Platform, falling back to regular test execution`:
  the task is not on JUnit Platform. Maven then runs it normally; Gradle (plugin 4.6.1) fails it
  anyway, which is why PTS must never be turned on for such a task.
- `Not publishing Build Scans may reduce the effectiveness of Predictive Test Selection`: expected
  with `--no-scan` or `-Ddevelocity.scan.disabled=true`.
