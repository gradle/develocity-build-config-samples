# Gradle: `settings.gradle(.kts)`, `gradle.properties`

```groovy
def isCI = System.getenv('<CI_VAR>') != null
def accessKey = System.getenv('DEVELOCITY_ACCESS_KEY')
// in remote(develocity.buildCache):
push = isCI && accessKey != null && !accessKey.isEmpty()
```

```kotlin
val isCI = System.getenv("<CI_VAR>") != null
val hasAccessKey = !System.getenv("DEVELOCITY_ACCESS_KEY").isNullOrEmpty()
// in remote(develocity.buildCache):
isPush = isCI && hasAccessKey
```

With several CI providers: `def isCI = ['A', 'B'].any { System.getenv(it) != null }`, or
`val isCI = listOf("A", "B").any { System.getenv(it) != null }`.

## Gradle version

Read the version from `gradle/wrapper/gradle-wrapper.properties`, or `gradle --version` without a
wrapper. Never upgrade Gradle as part of onboarding.

- **Gradle 6 and later**: the settings plugin, as above and below.
- **Gradle 5**: apply `com.gradle.develocity` in the root project's `build.gradle(.kts)` instead of
  the settings file, with the same `develocity {}` configuration
  ([docs](https://docs.develocity.ai/gradle/current/gradle-plugin/#gradle-5-x)). The Develocity build
  cache connector needs Gradle 6, so leave the remote cache out: Build Scans only, and the local
  cache when enabled. Say so in the pull request.
- **Gradle 4 and earlier**: the Develocity plugin does not support them. Report the version and
  stop.

## Merging

Merge into an existing settings file rather than replacing it. The order Gradle enforces is
`pluginManagement {}`, then `buildscript {}`, then `plugins {}`, so the `plugins {}` block goes after
whichever of the first two exist and before everything else. The existing `rootProject.name` stays.
If the build has no settings file, create one and add `rootProject.name` matching the directory.

An existing `buildCache { remote(...) }` is covered by step 2: settings take only one remote, so do
not replace it.

## `gradle.properties`

Add `org.gradle.caching=true` to `gradle.properties` if absent, or the `buildCache` block does
nothing. Add only that: the sample's `gradle.properties` also contains some project decisions that must not be touched.

## Pull request

When `org.gradle.caching=true` was added, the description says so: it turns on the build cache for
every build of the project, local ones included.
