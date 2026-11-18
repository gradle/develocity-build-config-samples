# Maven

## Versions

Read the extension from `.mvn/extensions.xml` and Maven from `.mvn/wrapper/maven-wrapper.properties`
(`mvn --version` without a wrapper). Read Surefire and Failsafe from the effective POM below. Never
upgrade Maven as part of this skill.

| Component | Floor | Below it |
| --- | --- | --- |
| Develocity Maven extension | 1.19.2 | no `pts.enabled` system property, which the checks and the kill switch use |
| Develocity Maven extension | 1.21 | no `fallbackToRegularExecutionOnMissingPrerequisites` (1.20 calls it `...OnMissingJUnitPlatform`) |
| Develocity Maven extension | 1.21.3 | the extension ignores the JVM's proxy settings for PTS requests |
| Surefire, Failsafe | 2.22.2 | PTS cannot run on that goal |

Bump the extension to the latest release when it is below a floor:

```
curl -s https://repo1.maven.org/maven2/com/gradle/develocity-maven-extension/maven-metadata.xml
```

Take `<release>`, never `<latest>`. Bump Surefire or Failsafe below 2.22.2 to the latest 3.x release,
and say so in the pull request: it changes how every test runs.

## Inventory

```
./mvnw -B -ntp help:effective-pom -Doutput=<scratch>/effective-pom.xml
./mvnw -B -ntp dependency:list -DincludeScope=test -DoutputFile=<scratch>/deps-<module>.txt -DappendOutput=false
```

From the effective POM, per module: each `maven-surefire-plugin` and `maven-failsafe-plugin`
execution (its id, version, `<includes>`, `<groups>`), an existing `<predictiveSelection>` block
(PTS already configured: SKILL.md step 1), and `testFailureIgnore`.

From the test dependencies, the engines. Surefire runs a module on JUnit Platform when a JUnit
Platform engine is on its test classpath:

| Dependency | Engine |
| --- | --- |
| `org.junit.jupiter:junit-jupiter-engine` | Jupiter, supported |
| `org.junit.vintage:junit-vintage-engine` | JUnit 3 and 4 through vintage, supported |
| `org.junit.support:testng-engine` | TestNG through JUnit Platform, supported |
| `org.spockframework:spock-core` 2.x | Spock, supported |
| `io.kotest:kotest-runner-junit5*` 5.6 or later | Kotest, supported |
| `net.jqwik:jqwik-engine`, `com.tngtech.archunit:archunit-junit5-engine` | supported |
| `io.cucumber:cucumber-junit-platform-engine` | needs the Cucumber Companion plugin |
| `junit:junit` with no JUnit Platform engine | JUnit 4 provider: not on JUnit Platform |
| `org.testng:testng` with no `testng-engine` | TestNG provider: not on JUnit Platform |

## Fixing blockers

**JUnit 4 or TestNG without JUnit Platform.** Add `junit-vintage-engine` (JUnit 4) or `testng-engine`
(TestNG) as a test dependency in the module, or in the parent's `dependencyManagement` and the
modules that need it. Take the version from a JUnit BOM the build imports; otherwise the latest
release. Report instead of changing when Surefire is configured with `<groups>`, `<excludedGroups>`,
`<suiteXmlFiles>` or a provider dependency: those mean different things on JUnit Platform.

**Cucumber.** Add the Cucumber Companion Maven plugin to each module that runs Cucumber
(`com.gradle.cucumber.companion:cucumber-companion-maven-plugin`, goal
`generate-cucumber-companion-files`, latest release); its
[README](https://github.com/gradle/cucumber-companion) has the snippet.

**File fingerprints.** Remove `<fileFingerprints>false</fileFingerprints>` from `.mvn/develocity.xml`,
and `-Ddevelocity.scan.captureFileFingerprints=false` from CI commands and `.mvn/maven.config`.

**After any engine change, compare test counts.** Run before and after, with PTS off:

```
./mvnw -B -ntp test -Dpts.enabled=false -Ddevelocity.scan.disabled=true
find . -path '*/target/surefire-reports/*' -name 'TEST-*.xml' -exec grep -o '<testcase ' {} + | wc -l
```

A different count is a broken change: revert it and report.

## Server check (dry run)

```
./mvnw -B -ntp test -Dpts.enabled=true -Djunit.platform.execution.dryRun.enabled=true -Ddevelocity.scan.disabled=true
```

- `verify` instead of `test` when Failsafe goals are in scope.
- `-pl <modules>` to limit it to the modules the inventory marked supported: `-Dpts.enabled=true`
  turns PTS on for every Surefire and Failsafe goal, and a goal that is not on JUnit Platform fails
  unless the fallback below is set.
- `-Ddevelocity.scan.disabled=true` keeps the run out of the history the Simulator and the model learn from.
- Per-class reasons are logged at debug level: add `-X` and filter, it is long.

Read the result with `references/selection-reasons.md`:

```
grep -E 'Predictive Test Selection|fewer than fourteen days|fewer than fifty executions'
```

## enforce: the configuration

In the Surefire (and, when in scope, Failsafe) configuration that applies to the in-scope modules,
usually the parent POM's `<pluginManagement>`:

```xml
<plugin>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <properties>
      <predictiveSelection>
        <enabled>${ptsEnabled}</enabled>
        <fallbackToRegularExecutionOnMissingPrerequisites>true</fallbackToRegularExecutionOnMissingPrerequisites>
      </predictiveSelection>
    </properties>
  </configuration>
</plugin>
```

A Surefire `<properties>` block that already exists is extended, never replaced.

In the same POM, the property and one profile per CI provider variable, so a build on any of them
turns PTS off:

```xml
<properties>
  <ptsEnabled>true</ptsEnabled>
</properties>

<profiles>
  <profile>
    <id>pts-off-on-github-actions</id>
    <activation>
      <property>
        <name>env.GITHUB_ACTIONS</name>
      </property>
    </activation>
    <properties>
      <ptsEnabled>false</ptsEnabled>
    </properties>
  </profile>
</profiles>
```

- A profile activates on the presence of its variable, matching onboarding's CI check. Maven allows
  one property per activation, hence one profile per variable.
- `-DptsEnabled=true` on the command line beats the profile: CI pull request jobs add it.
- The fallback keeps a module that is not on JUnit Platform running its tests normally, with a
  warning, instead of failing.
- To leave modules out, put the `<predictiveSelection>` block in the in-scope modules' own Surefire
  configuration instead of the parent's `<pluginManagement>`. `-DptsEnabled=true` does nothing to a
  goal without the block.

## enforce: checks

All with `-Djunit.platform.execution.dryRun.enabled=true -Ddevelocity.scan.disabled=true`, on one
in-scope module:

| Run | Expect |
| --- | --- |
| no extra flags | `Predictive Test Selection enabled` |
| `<CI_VAR>=true`, for each CI variable | no `Predictive Test Selection` line |
| `<CI_VAR>=true` and `-DptsEnabled=true` | `Predictive Test Selection enabled` |
| `-Dpts.enabled=false` | no `Predictive Test Selection` line (the kill switch) |
| `help:effective-pom` with `<CI_VAR>=true` | `<enabled>false</enabled>` in the in-scope executions |
