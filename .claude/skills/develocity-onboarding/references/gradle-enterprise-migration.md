# Migrating from Gradle Enterprise

A build that already uses the Gradle Enterprise plugin or extension is migrated, not onboarded from
scratch. Map the old configuration onto the new one, keep every custom tag, value and link the build
already sets, then apply the *Modifications* on top. The official guides hold the full mapping:
[Gradle plugin](https://docs.gradle.com/develocity/legacy/gradle-plugin/legacy/#develocity_migration),
[Maven extension](https://docs.gradle.com/develocity/legacy/maven-extension/legacy/#develocity_migration).

| Tool | Before | After |
| --- | --- | --- |
| Gradle | plugin `com.gradle.enterprise` | `com.gradle.develocity` |
| Gradle | `gradleEnterprise { }` | `develocity { }` |
| Gradle | `remote(gradleEnterprise.buildCache)` | `remote(develocity.buildCache)` |
| Gradle | `com.gradle.build-scan` applied in `build.gradle` (Gradle 5) | `com.gradle.develocity` in the settings file; needs Gradle 6+ |
| Maven | `.mvn/gradle-enterprise.xml` | `.mvn/develocity.xml` |
| Maven | root element `<gradleEnterprise>`, namespace `https://www.gradle.com/gradle-enterprise-maven` | `<develocity>`, namespace `https://www.gradle.com/develocity-maven` |
| Maven | `com.gradle:gradle-enterprise-maven-extension` in `.mvn/extensions.xml` | `com.gradle:develocity-maven-extension` |
| Maven | `<gradleEnterprise>` plugin configuration in `pom.xml` | `<develocity>`, under the new extension's coordinates |
| sbt | `addSbtPlugin("com.gradle" % "sbt-gradle-enterprise" % …)` | `sbt-develocity` |
| sbt | `gradleEnterpriseConfiguration` | `develocityConfiguration` |
| CI | `GRADLE_ENTERPRISE_ACCESS_KEY` | `DEVELOCITY_ACCESS_KEY` |

Elements renamed inside the blocks (`publishAlways()`, `termsOfServiceUrl`, `<publish>ALWAYS</publish>`)
follow the guides above. When an old element has no documented equivalent, say so in the pull
request rather than dropping it silently.

**Rename the access key in CI.** The store gate reads `DEVELOCITY_ACCESS_KEY`, so a pipeline that
still exports `GRADLE_ENTERPRISE_ACCESS_KEY` never writes to the cache. Rename every reference in the
pipeline files, and say in the pull request that the repository secret has to be created under the
new name (the value keeps its format).

If the mapping cannot be done with confidence (a heavily customised configuration, a build
plugin wrapping it), report what is configured and stop, as for an already-onboarded project.

The pull request says this is a migration and lists what was renamed.
