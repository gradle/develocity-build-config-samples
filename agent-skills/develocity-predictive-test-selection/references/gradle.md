# Gradle

## Versions

Read Gradle from `gradle/wrapper/gradle-wrapper.properties` (`gradle --version` without a wrapper), and
the Develocity plugin from the settings file (the root `build.gradle(.kts)` on Gradle 5). Never upgrade
Gradle as part of this skill.

| Component | Floor | Below it |
| --- | --- | --- |
| Gradle | 5.4 | PTS does not support the build: report and stop |
| Gradle | 6.2 | no `providers.gradleProperty`: the enforce configuration reads the switch with `gradle.startParameter.projectProperties.containsKey("ptsOnCi")` |
| Gradle | 6.6 | no `--no-configuration-cache`: leave it out of the inventory command |
| Gradle | 7.6 | no per-task `--rerun`: use `--rerun-tasks` wherever this reference says `--rerun` |
| Gradle | 8.2 | a PTS-enabled `Test` task is never cached nor up-to-date: `enforce` stops and asks (SKILL.md step 7) |
| Gradle | 8.3 | no `--test-dry-run`: the server check runs the tests for real |
| Develocity plugin | 3.17.4 | the plugin ignores the JVM's proxy settings for PTS requests |

Bump the plugin to the latest release when it is below a floor:

```
curl -s https://plugins.gradle.org/m2/com/gradle/develocity/com.gradle.develocity.gradle.plugin/maven-metadata.xml
```

Take `<release>`, never `<latest>`.

## Inventory

```
./gradlew help -q --no-scan --no-configuration-cache -I <skill dir>/scripts/pts-readiness.init.gradle | grep '^PTS-READINESS'
```

One line for the build and one per `Test` task. It resolves each task's test runtime classpath, so the
first run downloads dependencies. `supported=no` comes with the reason; `ptsEnabled=true` means PTS is
already configured for that task (SKILL.md step 1). `fileFingerprints=default` means on.

## Fixing blockers

Change the build file that configures the task, and nothing else.

**JUnit 4 on `useJUnit()`.** Add the vintage engine and switch the task to JUnit Platform:

```kotlin
dependencies {
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:<version>")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")   // needed explicitly on Gradle 9
}
tasks.test { useJUnitPlatform() }
```

Take the version from the JUnit BOM or version catalog the build already uses; otherwise the latest
5.x release of `org.junit.vintage:junit-vintage-engine`. Report instead of changing when the task
uses `useJUnit { includeCategories / excludeCategories }`: on JUnit Platform those become tags, and
translating them is the team's call.

**TestNG on `useTestNG()`.** Same move with `org.junit.support:testng-engine` (it needs TestNG on the
classpath, which the build already has). Report instead of changing when `useTestNG { }` sets
suites, groups or listeners.

**Cucumber.** Apply `com.gradle.cucumber.companion` in each project that runs Cucumber through JUnit
Platform, at the latest release:

```
curl -s https://plugins.gradle.org/m2/com/gradle/cucumber/companion/com.gradle.cucumber.companion.gradle.plugin/maven-metadata.xml
```

**File fingerprints.** Remove `fileFingerprints = false` from the `develocity { buildScan { capture { } } }`
block, `-Dscan.capture-file-fingerprints=false` from CI commands and `GRADLE_OPTS`, and
`systemProp.scan.capture-file-fingerprints=false` from `gradle.properties`.

**After any engine change, compare test counts.** Adding Jupiter where vintage was needed discovers
zero tests and passes. Run the affected tasks before and after the change, with PTS off:

```
./gradlew <task paths> --rerun --no-scan -Dpts.enabled=false
find . -path '*/build/test-results/*' -name '*.xml' -exec grep -o '<testcase ' {} + | wc -l
```

A different count is a broken change: revert it and report.

## Server check (dry run)

```
./gradlew <task path> --rerun [<task path> --rerun ...] --test-dry-run --no-scan -Dpts.enabled=true --info
```

- `--rerun` per task: an up-to-date task asks the server nothing.
- `--test-dry-run` needs Gradle 8.3; below it, leave it out and the selected tests run.
- `--no-scan` keeps the run out of the history the Simulator and the model learn from.
- `-Dpts.enabled=true` turns PTS on for every `Test` task, so list only tasks the inventory marked
  `supported=yes`: on a task that is not on JUnit Platform the build fails, even with
  `fallbackToRegularExecutionOnMissingPrerequisites` (plugin 4.6.1).

Read the result with `references/selection-reasons.md`:

```
grep -E 'Predictive Test Selection|fewer than fourteen days|fewer than fifty executions'
```

## enforce: the configuration

Put it in the settings file, after the `develocity {}` block. When the build has a convention plugin
that already configures `Test` tasks, put it there instead, as the same
`tasks.withType<Test>().configureEach { }` body with `ptsOnCi` and `onAnyCI` defined next to it.

Kotlin DSL:

```kotlin
val ptsOnCi = providers.gradleProperty("ptsOnCi").isPresent
val onAnyCI = listOf("CI", "GITHUB_ACTIONS", "GITLAB_CI", "JENKINS_URL", "HUDSON_URL", "TEAMCITY_VERSION",
    "CIRCLE_BUILD_URL", "bamboo_resultsUrl", "TRAVIS_JOB_ID", "BITRISE_BUILD_URL", "GO_SERVER_URL",
    "TF_BUILD", "BUILDKITE", "BITBUCKET_BUILD_NUMBER").any { System.getenv(it) != null }
gradle.allprojects {
    tasks.withType<Test>().configureEach {
        val inScope = name == "test"
        extensions.getByType<com.gradle.develocity.agent.gradle.test.DevelocityTestConfiguration>().predictiveTestSelection {
            enabled.set(provider { inScope && options is JUnitPlatformOptions && (!onAnyCI || ptsOnCi) })
        }
    }
}
```

Groovy DSL:

```groovy
def ptsOnCi = providers.gradleProperty('ptsOnCi').present
def onAnyCI = ['CI', 'GITHUB_ACTIONS', 'GITLAB_CI', 'JENKINS_URL', 'HUDSON_URL', 'TEAMCITY_VERSION',
    'CIRCLE_BUILD_URL', 'bamboo_resultsUrl', 'TRAVIS_JOB_ID', 'BITRISE_BUILD_URL', 'GO_SERVER_URL',
    'TF_BUILD', 'BUILDKITE', 'BITBUCKET_BUILD_NUMBER'].any { System.getenv(it) != null }
gradle.allprojects {
    tasks.withType(Test).configureEach { task ->
        def inScope = task.name == 'test'
        task.develocity.predictiveTestSelection {
            enabled = provider { inScope && task.options instanceof JUnitPlatformOptions && (!onAnyCI || ptsOnCi) }
        }
    }
}
```

- `inScope` holds the scope the user confirmed. By name when the in-scope tasks share one
  (`name == "test"`, `name in setOf("test", "unitTest")`), by path when they do not
  (`path in setOf(":app:test", ":core:test")`), with the exclusions spelled out
  (`&& path != ":legacy:test"`).
- The `options is JUnitPlatformOptions` check is lazy, so it sees what the project's build script set
  later. It keeps PTS off a task that is not on JUnit Platform, which would otherwise fail the build.
- `gradle.allprojects`, not `gradle.lifecycle.beforeProject`: an isolated `beforeProject` action
  cannot reference the settings script's variables, and fails the build.
- `onAnyCI`, not onboarding's `isCI`: `isCI` names only the providers the repository shows, for
  cache push. PTS must stay off on every CI, including a main, nightly or release job defined on a CI
  server the repository does not mention, so `onAnyCI` lists `CI` and every variable in onboarding's
  `references/ci-providers.md`. A developer who exports `CI` gets no PTS locally, the safe side. Add
  the variable of any provider step 7 finds that the list lacks.
- Gradle 5: the same body in the root `build.gradle(.kts)`, as `allprojects { }`, after the
  `develocity {}` block. Below Gradle 6.2, read the switch as in *Versions*.

CI pull request jobs add `-PptsOnCi` to their Gradle command.

## enforce: checks

All with `--rerun --test-dry-run --no-scan` on one in-scope task (plain `--rerun` below Gradle 8.3):

| Run | Expect |
| --- | --- |
| no extra flags | a `Predictive Test Selection` line |
| `<CI_VAR>=true`, for each provider that builds the repository | no `Predictive Test Selection` line |
| `<CI_VAR>=true` and `-PptsOnCi` | a `Predictive Test Selection` line |
| an excluded task, with `-PptsOnCi` | no line for that task |
| `-Dpts.enabled=false` | no line (the kill switch) |

Then once with `--configuration-cache` when the build uses it, and on Gradle 8.2 or later run the
in-scope task twice without `--rerun`: the second run is `UP-TO-DATE` or `FROM-CACHE`.
