# Reading the Simulator

The Simulator runs on the server, over every published Build Scan with test results and file
fingerprints, once the license enables the Simulator or PTS. It answers, per test task or goal: how
often would PTS have caught a failing run, and how much test time would it have saved.

## Fetch the numbers

```
<skill dir>/scripts/pts-simulation.sh <server url> <project> > <scratch>/simulation.json
jq -r -f <skill dir>/scripts/pts-readiness.jq <scratch>/simulation.json
```

- `<project>` is the Build Scan project name: the Gradle root project name, or `groupId:artifactId`
  of a Maven build's top-level POM. An empty `""` lists every project the key may see, which helps
  when the name is unclear.
- Optional third to fifth arguments: the window in days (28), the profile (`standard`; also
  `conservative`, `fast`), and a Build Scan tag to narrow to the builds PTS would run on.
- Targets are named as in Build Scans: `:app:test` for Gradle, `surefire:test@default-test` or
  `failsafe:integration-test@default` for Maven.
- Without jq, read the JSON and apply the criteria below by hand.

The script calls `<server>/predictive-test-selection/info/simulation-performance`, the endpoint behind
the Simulator page. It is not part of the documented API and may change: if the response does not
have `"type": "simulation-performance"` and a `content` list, fall back below.

**Authentication.** The script uses the user's own key for the server host, from
`DEVELOCITY_ACCESS_KEY` or the Gradle or Maven `keys.properties`, and never prints it. Without one it
asks anonymously. The key needs **View Build Scans** on current servers (older ones, before about
2025.1, ask for **Use Predictive Test Selection**), and results cover only projects the user may see.

| Status | Means | Next |
| --- | --- | --- |
| 200, empty `content` | the project has no simulated builds in the window: wrong name, no scans with tests, or the Simulator is new | check the name with `""`; otherwise **not yet** |
| 401, 403 | no key, or no permission | the user provisions a key (`./gradlew provisionDevelocityAccessKey`, `./mvnw com.gradle:develocity-maven-extension:provision-access-key`) and reruns; else fall back |
| 404, 503 | neither the Simulator nor PTS is enabled on the license, or the server has no such endpoint | **blocked**: the Develocity administrator or Solutions Engineer enables the Simulator; else fall back |

## The criteria

`pts-readiness.jq` applies them in this order and prints the first that fails:

| Check | Ready when | Why |
| --- | --- | --- |
| Simulated builds | more than 0 | nothing to judge |
| `targetExecutionsWithInputCaptureIssues` | 0 | runs without file fingerprints cannot be predicted: fix capture first |
| Insufficient data | at most 10% of the task's test time | PTS runs every test for a class with under 50 executions, and for a task the server has seen for under 14 days; a high share means the model has not learned the task yet |
| Task failures in the window | at least 1; none gives **review** | with no failing run, nothing shows whether PTS would have caught one |
| Task failures predicted | at least the band for the task's mean duration: under 1 min 99%, 1 to 5 min 98%, 5 to 10 min 95%, over 10 min 90% | a missed failure is a failing change that would have merged green |
| Savings | 10% or more for a plain **ready**; below that, **ready, little to gain** | PTS would pay off little |

A **review** verdict is not a no:

- Failures predicted below the band: open the Simulator, switch on *Only incorrect*, and look at the
  missed runs. Failures from infrastructure (a timeout, a dead agent, an out-of-memory) are not
  something PTS should predict. If 98 of 100 failures were caught and one miss was infrastructure,
  the rate is 98 of 99.
- No failures in the window: rerun the script with a 56-day window (third argument) and judge from
  that. Still none: the savings are known, but whether PTS catches failures is unproven.

Either way, let the user decide with those numbers in front of them. **Ready, little to gain** is
also the user's call: enforce it only if they want it.

The bands are the thresholds Gradle recommends for enabling PTS, at the low end of each recommended
range. The 14 days and 50 executions are the server's own limits. Rule of thumb from the field: two
weeks of scans is the minimum, eight weeks gives a well-trained model.

Tell the user, with the savings, that the Simulator replays past builds: live savings depend on the
changes still to come, and will differ.

## Not yet: when to rerun

- Insufficient data: two weeks later, as long as CI keeps publishing scans for the task.
- No simulated builds, on a project that publishes scans: a newly enabled Simulator backfills the last
  56 days, which takes hours to a few days. Rerun tomorrow.

## The Simulator page

Always give the user the page, so they can check the numbers themselves:

```
<server>/scans/test-selection?predictive-test-selection.view=simulator&search.rootProjectNames=<project>&search.relativeStartTime=P28D
```

Add `&predictive-test-selection.testTarget=<project>%20%E2%80%BA%20<task path>` to open one Gradle task
(`›` between the project and the task path). URL-encode the project name.

## Fallback: the public API

When the endpoint above is unavailable, the documented Builds API carries the same simulation per
build, for a key with **Access build data via the API**:

```
GET <server>/api/builds?query=project:<project>&fromInstant=<epoch ms>&models=gradle-test-performance
```

(`maven-test-performance` for Maven; page with `fromBuild=<last id>` until a page is empty.) For each
build, `models.gradleTestPerformance.model.testTasks[]` (or `mavenTestPerformance...testGoals[]`) has
the task's `serialExecutionTime`, `tests.failedTestClassesCount`, and
`testAcceleration.predictiveTestSelection.simulation.standard`. Sum over the window:

- savings: Σ `serialTimeSavingsPotential` / Σ `serialExecutionTime`
- insufficient data, by class count (an approximation: the criteria weigh by test time): Σ `testClassesCountSelectedDueToInsufficientData` /
  Σ (`avoidableTestClassesCount` + `unavoidableTestClassesCount` + `testClassesCountSelectedDueToInsufficientData`)
- task failures predicted: of the runs with `failedTestClassesCount` > 0, the share where
  `failedTestClassesPredictedCount` is lower than `failedTestClassesCount` (PTS would have run at
  least one failing class)

`failedTestClassesPredictedCount` counts the failed classes PTS would have **missed**, despite its name.
Doing this by hand takes many requests; when even this is not available, give the user the Simulator
page and the criteria table, and ask them to read the numbers off it.
