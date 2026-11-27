---
name: develocity-predictive-test-selection
description: Turns on Develocity Predictive Test Selection (PTS) for a Gradle or Maven repository that is already onboarded to Develocity. In simulate mode it inventories the test tasks and goals, fixes what keeps PTS from running on them, asks the server whether it can predict yet, reads the PTS Simulator's results, and says plainly whether there is enough data to enable PTS. In enforce mode it opens a pull request that enables PTS for local builds and pull request builds only. Use when asked to set up, evaluate, simulate, check readiness for, or enable Predictive Test Selection or PTS.
license: Apache-2.0
compatibility: Needs git, bash, curl, network access, Java, and the repository's own Gradle or Maven build. jq turns the Simulator's numbers into verdicts; without it they are read by hand. The GitHub CLI (gh) is optional; it is only used for GitHub repositories.
---

# Develocity Predictive Test Selection

Get a repository that already publishes Build Scans ready for Predictive Test Selection, tell the user
from data when it is ready, and then turn it on for the builds where fast feedback matters.

PTS has no build-side simulation mode. The **Simulator** runs on the Develocity server: once the
license enables the Simulator or PTS, it replays every published Build Scan that has test results
and file fingerprints, and estimates what PTS would have skipped and which failures it would have
caught. So the skill works in two modes:

- **simulate** (default): make every test task or goal produce Build Scans the Simulator can use,
  fix what blocks PTS, ask the server whether it can predict yet, read the Simulator's numbers, and
  report. A pull request only when a blocker was fixed.
- **enforce**: once simulate reports *ready*, a pull request that enables PTS for local builds and
  pull request builds. Main-branch, nightly, release and deploy builds keep running every test.

Reference files (read only the ones the run needs):

- `references/gradle.md`: versions, inventory, blocker fixes, the dry run, the enforce configuration and its checks
- `references/maven.md`: the same for Maven
- `references/selection-reasons.md`: what each line PTS prints means, and what to do about it
- `references/simulator.md`: reading the Simulator's numbers from the API, the readiness criteria, the UI links
- `scripts/pts-readiness.init.gradle`: the Gradle inventory
- `scripts/pts-simulation.sh` and `scripts/pts-readiness.jq`: the Simulator's numbers and a verdict per task

## Inputs

| Input | Required | Default |
| --- | --- | --- |
| Repository | no | the current working directory |
| Mode | no | `simulate`; `enforce` when the user asks to enable or turn on PTS |
| Test tasks or goals in scope | no | every supported one found in step 2, confirmed by the user |
| Tracking reference | no | none; an issue, JIRA ticket or other URL the pull request must cite |

Notes
- The Develocity server URL is read from the existing configuration; do not ask for it.
- Never ask for, print, write or log an access key. The checks run with the user's own key, which
  the build tool and `scripts/pts-simulation.sh` read themselves and never show.
- If a repository URL was given instead of a local path, clone it into a scratch directory and work there.

## 1. Check the repository is onboarded

Detect the build tool: `settings.gradle(.kts)` or `build.gradle(.kts)` for Gradle, `pom.xml` for
Maven. For sbt, Bazel or npm, say PTS supports Gradle and Maven only, and stop.

Look for the Develocity configuration: the `com.gradle.develocity` plugin and its `develocity {}`
block, or `develocity-maven-extension` in `.mvn/extensions.xml` with `.mvn/develocity.xml`. Read the
server URL from it.

- **Nothing found**: stop, and point the user to the `develocity-onboarding` skill.
- **Gradle Enterprise only** (`com.gradle.enterprise`, `gradle-enterprise-maven-extension`): stop.
  Migrate first; `develocity-onboarding` does it.
- **PTS already configured** (a `predictiveTestSelection` block, a `<predictiveSelection>` block,
  `pts.enabled` in a CI command or `gradle.properties` / `.mvn/maven.config`): report where and for
  which builds, and ask whether to continue from there or stop.

Find the CI check the configuration already uses (`isCI` in the settings file, the
`backgroundBuildScanUpload` expression in `.mvn/develocity.xml`): it names the CI provider variables
(`<CI_VAR>` below). Without one, detect the providers as `develocity-onboarding` does.

## 2. Inventory the test tasks

Gradle: run the init script (`references/gradle.md`, *Inventory*). Maven: read the effective POM and
the test dependencies (`references/maven.md`, *Inventory*).

Show the user one row per task or goal: path, framework and engines, supported or not with the
reason, and whether it looks like an integration test (`integrationTest`, Failsafe, `*IT` classes,
Testcontainers on its classpath). PTS sees file inputs only: a test whose outcome depends on a
database, a service or a container image it does not see as a changed input can be skipped when it
should have run. Recommend unit test tasks; leave integration tests out unless the user wants them.
Ask the user to confirm the scope.

## 3. Versions and blockers

Check every row in scope against the floors in the tool's reference, then these:

| Check | Action |
| --- | --- |
| Not on JUnit Platform: JUnit 4 or TestNG run natively | fix |
| Cucumber without the Cucumber Companion | fix |
| File fingerprint capture turned off | fix |
| Plugin or extension below its floor; Surefire or Failsafe below 2.22.2 | fix |
| `rootProject.name` not set in a Gradle settings file | fix: set it to the name the Build Scans already show |
| Kotest below 5.6, Spek, FitNesse, other unsupported engines | report; leave the task out |
| CI test builds that do not publish Build Scans (`--no-scan`, `publishing.onlyIf`, `-Ddevelocity.scan.disabled`) | report: the Simulator gets no data from them |
| `ignoreFailures` (Gradle) or `testFailureIgnore` (Maven) | report: PTS learns from failures, and hidden ones make the Simulator's results meaningless |
| `--tests`, `-Dtest=` or `-Dit.test=` in CI commands | report: they make every matching test run |
| No test retry | report: a flaky class is always selected, which lowers the savings |
| A coverage gate on pull request builds (JaCoCo, Sonar) | note for enforce |

Fix what the table says to fix, as each reference describes, and nothing else: never change test
code, and report a framework change that takes more than adding an engine. After an engine change,
compare the test counts before and after; a different count means revert and report.

In enforce mode, every fix here must already be merged: if one is outstanding, stop and run simulate.

## 4. Ask the server

Run the dry run in the tool's reference on the in-scope tasks the inventory marked supported. It
uses the user's own credentials and publishes nothing. Classify each task with
`references/selection-reasons.md`:

| Status | When |
| --- | --- |
| **predicting** | the selection line names fewer selected classes than it has |
| **not yet** | `fewer than fourteen days` reasons, or `fewer than fifty executions` reasons for most of the task's classes |
| **never seen** | `Insufficient change history available for task/goal.`: CI does not publish scans for this task yet |
| **blocked** | not authorized, extension disabled, unreachable, or another reason in the second table |

A task that matches both **predicting** and **not yet** is **not yet**.

When the dry run cannot run here (no Java, no network to the server, the build does not compile
locally), say so and go on with step 5 alone.

## 5. Read the Simulator

Fetch the Simulator's numbers for the project over the last 28 days with `scripts/pts-simulation.sh`,
and judge each in-scope task with `scripts/pts-readiness.jq` (`references/simulator.md`). It prints,
per task, the simulated builds, the task failures PTS would have caught against the band for the
task's duration, the savings, the share of insufficient data, and a verdict: **ready**, **ready,
little to gain**, **review**, **not yet** or **blocked**. The reference says what to do when the
endpoint cannot be reached, and how to handle a **review**.

A task's verdict for the report is the worse of steps 4 and 5, from worst: **blocked**, **not yet**
(step 4's **never seen** counts as not yet), **review**, **ready, little to gain**, **ready**. Step 4's
**predicting** adds nothing to step 5. So a task the dry run calls **not yet** is not ready, whatever
the 28-day window says. When only one of the two could run, use it and say which one is missing.

## 6. Report

One table, one row per task in scope:

| Task | Engines | Server (step 4) | Simulator, 28 days (step 5) | Verdict |
| --- | --- | --- | --- | --- |
| `:app:test` | junit-jupiter | predicting | <builds> builds, <caught>/<failures> task failures caught (need <band>), <savings> saved, <insufficient> insufficient data | **ready** |

The verdict is **ready**, **ready, little to gain** (with the savings), **review** (with the missed
runs to look at, or the note that no task failure was seen), **not yet** (with what is missing and
the date to rerun simulate), **blocked** (with who has to do what), or **out of scope** (with the
reason). Under the table:

- the Simulator link for the project (`references/simulator.md`), for the user to look at themselves
- with any savings figure: the Simulator replays past builds, so live savings will differ
- what the server side needs that the skill cannot check: the Simulator or PTS entitlement; on a
  self-hosted server, enough memory for PTS (at least 4608 MiB of application heap, the background
  processor on busy instances)
- the reports from step 3
- the next step: rerun simulate on the date given, or run enforce for the tasks that are ready

When step 3 changed nothing, simulate ends here. Otherwise it continues at step 9 with a pull
request for the fixes.

## 7. enforce: choose the builds

Rerun steps 1 to 5 first; the numbers may have moved since the last report. Enforce the tasks whose
verdict is **ready**, and the **review** or **ready, little to gain** ones the user accepts after
seeing their numbers; show the user the ones left out. On Gradle below 8.2, stop and
explain that a PTS-enabled test task is never cached nor up-to-date there; continue only if the user
accepts that.

Read every CI job that builds, and its triggers:

| Builds | PTS |
| --- | --- |
| local | on |
| pull request jobs | on, through the switch below |
| pull request jobs with a coverage gate | off, unless the user moves or drops the gate: PTS skips tests, so the coverage drops |
| main or default branch, merge queue | off: they run the full suite, and they fill the build cache (test results from a run where PTS skipped tests are never stored) |
| nightly, scheduled | off |
| release, publish, deploy | off |

A job that runs on pull requests and on anything else (a push to the default branch, a merge queue,
a schedule) gets the switch for the pull request event only: on GitHub Actions
`${{ github.event_name == 'pull_request' && '-PptsOnCi' || '' }}` (Maven:
`-DptsEnabled=${{ github.event_name == 'pull_request' }}`), on GitLab only when
`$CI_PIPELINE_SOURCE == "merge_request_event"` and `$CI_MERGE_REQUEST_EVENT_TYPE != "merge_train"`.
Where the CI cannot tell the events apart, split the job.

Ask which CI systems run builds the repository does not define (main, nightly, release or deploy
jobs on a Jenkins or TeamCity server, for example). The configuration keeps PTS off on CI by its
environment variables; a system that sets none of them needs its variable added (the tool's
reference, *enforce: the configuration*).

Ask the user when a job's role is unclear, and show them this table for their jobs before changing
anything.

## 8. enforce: configure and wire CI

Add the configuration in the tool's reference (*enforce: the configuration*): PTS on for in-scope
tasks when the build is not CI, or when the switch is passed. Then add the switch to the pull request
jobs' build commands: `-PptsOnCi` for Gradle, `-DptsEnabled=true` for Maven. When a pipeline is not
defined in the repository, change nothing there and say in the pull request what has to change, and
where.

Keep it to that: STANDARD profile, no must-run tests, no mode change. The pull request mentions the
options instead (*step 10*).

## 9. Check locally

Simulate, after fixes: the build still loads (`./gradlew help --no-scan`, `./mvnw -B -ntp validate`),
the test counts match, the inventory now marks the fixed tasks supported.

Enforce: the checks table in the tool's reference, every row.

If a check cannot run here, say so in the pull request instead of claiming it was checked.

**Never commit on the default or integration branch** (`main`, `master`, `develop`, or whatever the
repository merges into). If the checkout is on one, read the target's branch naming first (step 10)
and create a branch; if the user already checked out a feature branch, commit there. Commit only the
files this skill changed. Check `git status` before staging rather than after pushing.

## 10. Prepare and open the pull request

Read the target's own rules before writing anything: `.github/pull_request_template.md`,
`.github/PULL_REQUEST_TEMPLATE/`, `CONTRIBUTING.md` and `AI_POLICY.md`. Follow all of them: a linked
tracker, a branch named after the issue key, a commit trailer. Use the repository's own branch naming.
If there is no push access, fork and open the pull request from the fork (see *Rules*).

**The title is often linted.** Look for a workflow that reads `github.event.pull_request.title` and
take the rule from its configuration rather than from the template's examples.

**If the project has an AI policy, follow it.** State plainly what was used and what it did. Where
the policy asks for something that cannot be produced here, write that it was not done.

If the template or the contributing guide requires a linked issue or ticket, use the **tracking
reference**. If none was given, offer to file one: show the exact title and body, and offer to create
it as written, edit it first, or take an existing reference instead. Wait for the answer.

Simulate, the description:

```
Gets <tasks> ready for Develocity Predictive Test Selection. Nothing changes in which tests run.

- <each fix: engine added, plugin or extension bumped, fingerprints turned back on>
- Test counts before and after: <n> and <n>

Checked with <commands>: <results>.
```

Enforce, the description:

```
Turns on Develocity Predictive Test Selection for <tasks> in local builds and pull request builds.
Main-branch, nightly and release builds still run every test.

- <files changed>
- Pull request jobs pass <-PptsOnCi | -DptsEnabled=true>: <jobs>
- Simulator, last 28 days: <per task: runs, task failures predicted, savings>
- -Dpts.enabled=false turns PTS off for any build

Checked with <commands>: <results>.

Before merging:
- The account behind CI's DEVELOCITY_ACCESS_KEY needs the "Use Predictive Test Selection" permission;
  without it, pull request builds log "Client not authorized" and run every test.
```

Add to the enforce description, each in one line, only what applies: pull requests from forks get no
key and run every test, by design; coverage on the jobs that keep it; test results from a pull request
build where PTS skipped tests are not stored in the build cache; the follow-ups the user may want later
(`REMAINING_TESTS` mode on a nightly build, must-run classes with `includeClasses` or `@MustRun`, the
FAST profile after a few weeks). Add anything that was guessed, and nothing else.

**Then stop and confirm before anything leaves the machine.** Show the user the diff, the branch
name, the target, the title and the description, and wait. Push and open the pull request only on
their go-ahead. Apply any edits they ask for and show the result again.

## 11. Follow the checks

The pull request is not finished when it is opened. Wait for its checks and read any failure:

```
gh pr checks <n> --watch
gh run view <run id> --log-failed
```

On another forge, use its CLI or API. If the result cannot be read from here, say the checks were not
seen. Every failure falls into one of three buckets:

- **The change broke the build.** Fix it.
- **The change broke the build only because something is not there yet**: the key's permission, the
  entitlement. Still the change's fault: PTS must degrade to running every test, never to a failure.
- **The repository's own gate**, which the change cannot satisfy: a CLA or DCO, a required approval,
  a fork without secrets. Say which gate is red and what unblocks it, and tell the user.

**A fix-up is confirmed like the pull request was**: show its diff and the failure it answers, and
push only on the user's go-ahead. Add commits rather than force-pushing. Before calling a failure
unrelated or flaky, check the base branch's recent runs of the same job.

For enforce, read the pull request job's log: it shows `Predictive Test Selection: N of M test
classes selected`, or the reason PTS did not run (`references/selection-reasons.md`). Report that line
per task. Then confirm that no job running on the default branch, a merge queue or a schedule passes
the switch.

## Rules

- **Never ask for, print, write or log an access key**, and never create one. Only the build tool
  and `scripts/pts-simulation.sh` touch the user's own key, and neither shows it. The user provisions
  a key themselves when the server asks for one.
- **Nothing is created in someone's tracker or CI without them seeing it first**: an issue, a fork, a
  pushed branch, a pull request. Show it, and wait.
- **PTS is never enabled on main-branch, nightly, release or deploy builds**, and never on a task that
  is not on JUnit Platform.
- **No enforce without a ready verdict** from the data in steps 4 and 5, or a **review** or **ready,
  little to gain** one the user accepted, for each task enforced.
- **Never change test code.** A framework change beyond adding an engine is reported, not done.
- **A PTS problem never fails a build.** Server errors already degrade to running every test; the
  configuration must keep PTS off where it cannot run.
