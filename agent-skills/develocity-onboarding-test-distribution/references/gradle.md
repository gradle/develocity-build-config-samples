# Gradle

## Connected to Develocity

The `com.gradle.develocity` plugin is applied, with a `server` set: in `settings.gradle(.kts)`, the root `build.gradle(.kts)` on Gradle 5, a convention plugin, or an init script.
`com.gradle.enterprise` instead is not supported.

## Versions supported

Gradle 5.4+, Java 8+.

## Minimum plugin version

3.17, for the DSL below.

## Latest plugin version

```
curl -s https://plugins.gradle.org/m2/com/gradle/develocity/com.gradle.develocity.gradle.plugin/maven-metadata.xml
```

Take `<release>`.

## Compatibility with the Develocity server

https://docs.develocity.ai/2026.3/miscellaneous/compatibility/#gradle_plugin_develocity_compatibility

## Keys file

`$HOME/.gradle/develocity/keys.properties`

## Test tasks

Every task of type `Test` (subclasses included). The baseline scan lists the ones that ran.

## Verification build

```
./gradlew --no-build-cache --continue clean <verification task> -Dscan.tag.<tag>
```

## Test performance

```
GET /api/builds/{id}/gradle-test-performance
```

Call it as shown in `references/develocity.md`. The tasks are under `testTasks`.

## Load check

```
./gradlew <test task paths> --dry-run
```

`<test task paths>` are the test tasks with TD, e.g. `:app:test :service:integrationTest`. They must be in the command, so their TD configuration is evaluated. It must succeed. This catches configuration errors, but the enable condition is only read when the tests run, so its value is not shown.

## JUnit Platform

https://docs.gradle.org/current/userguide/java_testing.html#using_junit5

```kotlin
tasks.test {
    useJUnitPlatform()
}
```

JUnit 4 tests also need `testRuntimeOnly("org.junit.vintage:junit-vintage-engine")`.

## TD configuration

The `develocity.testDistribution` extension is available on every `Test` task once the `com.gradle.develocity` plugin is applied (settings file on Gradle 6+, root build file on Gradle 5). A convention plugin also needs `com.gradle:develocity-gradle-plugin` on its own classpath to compile against it.

```kotlin
tasks.named<Test>("test").configure {
    develocity.testDistribution {
        enabled.set(true)                                   // or providers.environmentVariable("CI").map { true }.orElse(false)
        maxLocalExecutors.set(2)                            // only when given
        maxRemoteExecutors.set(4)                           // only when given
        requirements.set(setOf("os=linux"))                 // only when given
        localOnly {
            includeClasses.addAll("com.example.FooTest")    // only when given or needed
            includeAnnotationClasses.addAll("io.quarkus.test.junit.QuarkusIntegrationTest") // Quarkus only
        }
    }
}
```

```groovy
tasks.named('test', Test).configure {
    develocity.testDistribution {
        enabled = true
        maxLocalExecutors = 2
        maxRemoteExecutors = 4
        requirements = ['os=linux']
        localOnly {
            includeClasses = ['com.example.FooTest']
            includeAnnotationClasses = ['io.quarkus.test.junit.QuarkusIntegrationTest']
        }
    }
}
```

When every test task has a similar configuration, move the common part to the convention plugin applied to every project, if there is one, with `tasks.withType<Test>().configureEach`. Otherwise in the root build file:

```kotlin
subprojects {
    tasks.withType<Test>().configureEach {
        develocity.testDistribution {
            enabled.set(providers.environmentVariable("CI").map { true }.orElse(false)) // or true
        }
    }
}
```

```groovy
subprojects {
    tasks.withType(Test).configureEach {
        develocity.testDistribution {
            enabled = providers.environmentVariable('CI').map { true }.orElse(false) // or true
        }
    }
}
```

Read environment variables through `providers`, and do not reference `project` inside the task configuration, so it stays compatible with the configuration cache. Use `allprojects` instead when the root project has tests too.

What differs between tasks, e.g. their own `localOnly` patterns or requirements, stays in the task's project build file:

```kotlin
tasks.named<Test>("test").configure {
    develocity.testDistribution {
        localOnly {
            includeClasses.addAll("com.example.FooTest")
        }
    }
}
```

```groovy
tasks.named('test', Test).configure {
    develocity.testDistribution {
        localOnly {
            includeClasses = ['com.example.FooTest']
        }
    }
}
```

## Quarkus

Gradle plugin 4.5.0+, Quarkus 3.31.1+: https://docs.develocity.ai/test-distribution/#gradle-2
