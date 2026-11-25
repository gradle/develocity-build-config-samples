# Maven

## Connected to Develocity

`.mvn/extensions.xml` declares `com.gradle:develocity-maven-extension` and `.mvn/develocity.xml` sets a server URL.
`com.gradle:gradle-enterprise-maven-extension` instead is not supported.

## Versions supported

Surefire or Failsafe 2.22.2+, Java 8+.

## Minimum extension version

1.21, for the configuration below.

## Latest extension version

```
curl -s https://repo1.maven.org/maven2/com/gradle/develocity-maven-extension/maven-metadata.xml
```

Take `<release>`.

## Compatibility with the Develocity server

https://docs.develocity.ai/2026.3/miscellaneous/compatibility/#maven_extension_develocity_compatibility

## Keys file

`$HOME/.m2/.develocity/keys.properties`

## Test goals

Each `surefire:test` and `failsafe:integration-test` execution, per module. The baseline scan lists the ones that ran.

## Verification build

```
./mvnw clean <verification task> --fail-at-end -Ddevelocity.cache.local.enabled=false -Ddevelocity.cache.remote.enabled=false -Dscan.tag.<tag>
```

## Test performance

```
GET /api/builds/{id}/maven-test-performance
```

Call it as shown in `references/develocity.md`. The tasks are under `testGoals`.

## Load check

```
./mvnw -B help:effective-pom -pl <module> | grep -A1 '<distribution>'
```

`<distribution><enabled>` must read `true`. With an enable condition, run it once with the condition met (e.g. the CI variable set) and once without: it must read `true`, then `false`.

## JUnit Platform

https://maven.apache.org/surefire/maven-surefire-plugin/examples/junit-platform.html#configuring-junit-platform

JUnit 4 tests also need `org.junit.vintage:junit-vintage-engine` as a test dependency.

## TD configuration

In the `configuration` of `maven-surefire-plugin` or `maven-failsafe-plugin` (same block), or of a single `execution` when only that one is selected.
If a `<properties>` block already exists, add `<distribution>` to it.

```xml
<plugin>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <properties>
      <distribution>
        <enabled>${td.enabled}</enabled>
        <maxLocalExecutors>2</maxLocalExecutors>      <!-- only when given -->
        <maxRemoteExecutors>4</maxRemoteExecutors>    <!-- only when given -->
        <requirements>                                <!-- only when given -->
          <requirement>os=linux</requirement>
        </requirements>
        <localOnly>                                   <!-- only when given or needed -->
          <includeClasses>
            <include>com.example.FooTest</include>
          </includeClasses>
          <includeAnnotationClasses>                  <!-- Quarkus only -->
            <include>io.quarkus.test.junit.QuarkusIntegrationTest</include>
          </includeAnnotationClasses>
        </localOnly>
      </distribution>
    </properties>
  </configuration>
</plugin>
```

Set `td.enabled` once, in the `<properties>` of the parent POM the modules inherit from: `true` with the default condition. With a filter, `false` there and `true` in a profile of that same POM, e.g. activated by `<property><name>env.CI</name></property>`.
Escape `<` and `>` in requirements (`memory&gt;=2Gi`).

Put it in the module's own POM. When every test goal has a similar configuration, move the common part to the parent POM's `pluginManagement`, once for each of the two plugins used.

What differs between modules, e.g. their own `localOnly` patterns or requirements, stays in the module's plugin `configuration`.

## Quarkus

Maven extension 2.4.0+, Quarkus 3.31.1+: https://docs.develocity.ai/test-distribution/#maven-2
