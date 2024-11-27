## Convention Develocity Shared

This project demonstrates how a convention Gradle plugin and Maven extension can share the same Develocity build configuration across multiple projects.
It is intended to serve as a starting point for creating your own Gradle plugin and Maven extension that applies your specific Develocity configuration.
Note the inline comments in the build and source code for things to adjust specifically to your needs.

### Applying the extension to your build

#### Gradle

##### Using a dynamic version

For builds using Gradle 6.9 and later, it's recommended to apply the convention plugin using a [dynamic version](https://docs.gradle.org/current/userguide/dynamic_versions.html).
This approach ensures that the latest version of the plugin is automatically used, but only for the specified major version.
For example, the following will automatically use the latest `1.x` version of the plugin up to, but excluding, version `2.0`:

```groovy
plugins {
    id 'com.myorg.convention-develocity-gradle-plugin' version '1.+'
}
```

This allows you to quickly roll out non-breaking changes to all consumers of the convention plugin.
Breaking changes in the plugin should be released under a new major version, e.g., `2.0`.
All consumers of the plugin will then need to update the specified major version, e.g., `2.+`.

> [!IMPORTANT]
> Using dynamic versions should only be done when releases and development versions are published to separate repositories.
> Not having this separation introduces the risk that consumers will use a development version of the plugin not yet ready to be used.
> In these scenarios, using a static version is preferred.

##### Using a static version

For projects using earlier versions of Gradle, the convention plugin must be applied using a static version.
For example:

```groovy
plugins {
    id 'com.myorg.convention-develocity-gradle-plugin' version '1.0'
}
```

For each new release of the convention plugin, consuming builds will need to be explicitly updated.

#### Maven

##### Using a version range

It's recommended to apply the convention extension using a [version range](https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html).
This approach ensures that the latest version of the extension is automatically used, up to the specified maximum version.
For example, the following will automatically use the latest version of the extension up to, but excluding, version `2.0`:

```xml
<extensions>
    <extension>
        <groupId>com.myorg</groupId>
        <artifactId>convention-develocity-maven-extension</artifactId>
        <version>(,2.0)</version>
    </extension>
</extensions>
```

This allows you to quickly roll out non-breaking changes to all consumers of the convention extension.
Breaking changes in the extension should be released under a new major version, e.g., `2.0`.
All consumers of the extension will then need to update the upper boundary of the version range to the next major version, e.g., `(,3.0)`.

> [!IMPORTANT]
> Using version ranges should only be done when releases and development versions are published to separate repositories.
> Not having this separation introduces the risk that consumers will use a development version of the extension not yet ready to be used.
> In these scenarios, using a static version is preferred.

##### Using a static version

In scenarios where a version range can't be used, e.g., when releases and development versions are published to the same repository, a static version should be used.
For example:

```xml
<extensions>
    <extension>
        <groupId>com.myorg</groupId>
        <artifactId>convention-develocity-maven-extension</artifactId>
        <version>1.0</version>
    </extension>
</extensions>
```

For each new release of the convention extension, consuming builds will need to be explicitly updated.

### Content

This project is structured as follows:

* `convention-develocity-common` - Contains the common convention logic shared between build tools
* `convention-develocity-gradle-plugin` - Contains the convention Gradle plugin
* `convention-develocity-maven-extension` - Contains the convention Maven extension
* `examples` - Contains example builds that apply the convention Gradle plugin or Maven extension for different Gradle and Maven versions
    * `gradle_5` - Applies the convention Gradle plugin on a Gradle 5 build
    * `gradle_6` - Applies the convention Gradle plugin on a Gradle 6 build
    * `gradle_6.9_and_later` - Applies the convention Gradle plugin on a Gradle 6.9 and later build
    * `maven_3` - Applies the convention Maven extension on a Maven 3 build

### Running the example builds

Before running the example builds, publish all components to your local Maven repository.

```bash
./gradlew publishToMavenLocal
```

> [!NOTE]
> You would publish these components to your internal artifact provider, e.g., Artifactory or Nexus, for production usage.
> The shared convention plugin at `buildSrc/src/main/kotlin/com.myorg.publishing-conventions.gradle.kts` can be used to configure the publishing of all components.

#### Gradle

Once you have published the plugin, you can run the three example builds under `examples`:

```bash
cd examples/gradle_5
./gradlew build

cd examples/gradle_6
./gradlew build

cd examples/gradle_6.9_and_later
./gradlew build
``` 

#### Maven

Once you have installed the extension, you can run the example build under `examples`:

```bash
cd examples/maven_3
./mvnw clean verify
```

> [!IMPORTANT]
> The artifact provider must be configured as a [Mirror](https://maven.apache.org/guides/mini/guide-mirror-settings.html) to Maven Central in order to correctly resolve the extension.

#### Requirements

To run the example builds, use Java 8 or higher.

---

Example scans showing test caching disabled by default:
  - Gradle 5.0: https://ge.solutions-team.gradle.com/s/ml355gcshr2je/timeline?details=si6uhuz4tdufw
  - Gradle 6.0.1: https://ge.solutions-team.gradle.com/s/fqm4bohx7oouy/timeline?details=si6uhuz4tdufw
  - Gradle 8.11.1: https://ge.solutions-team.gradle.com/s/qpzccsbovorka/timeline?details=si6uhuz4tdufw
  - Maven 3.9.9 (Surefire): https://ge.solutions-team.gradle.com/s/rfgck3uzv7zgq/timeline?details=dvp4sj4dsdhru
  - Maven 3.9.9 (Failsafe): https://ge.solutions-team.gradle.com/s/rfgck3uzv7zgq/timeline?details=tg55nc6hywfxo

Example scans showing test caching re-enabled:
  - Gradle 5.0: https://ge.solutions-team.gradle.com/s/dk5j62ccwj4ci/timeline?details=si6uhuz4tdufw
  - Gradle 6.0.1: https://ge.solutions-team.gradle.com/s/k7bo2hrplpdhy/timeline?details=si6uhuz4tdufw
  - Gradle 8.11.1: https://ge.solutions-team.gradle.com/s/3e3orwlfc6wdk/timeline?details=si6uhuz4tdufw
  - Maven 3.9.9 (Surefire): https://ge.solutions-team.gradle.com/s/k7et7vnf4tozo/timeline?details=dvp4sj4dsdhru
  - Maven 3.9.9 (Failsafe): https://ge.solutions-team.gradle.com/s/k7et7vnf4tozo/timeline?details=tg55nc6hywfxo
