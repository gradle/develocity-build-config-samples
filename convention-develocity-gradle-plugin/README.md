## Convention Develocity Gradle Plugin

This project demonstrates how a convention plugin can share the same Develocity build configuration across multiple projects.
It is intended to serve as a starting point for creating your own Gradle plugin that applies your specific Develocity configuration.
Note the inline comments in the build and source code for things to adjust specifically to your needs.

### Applying the plugin to your build

#### Using a dynamic version

For builds using Gradle 6.9 and later, it's recommended to apply the convention plugin using a [dynamic version](https://docs.gradle.org/current/userguide/dynamic_versions.html).
This approach ensures that the latest version of the plugin is automatically used, but only for the specified major version.
For example, the following will automatically use the latest `1.x` version of the plugin up to, but excluding, version `2.0.0`:

```groovy
plugins {
    id 'com.myorg.convention-develocity-gradle-5-or-newer' version '1.+'
}
```

This allows you to quickly roll out non-breaking changes to all consumers of the convention plugin.
Breaking changes in the plugin should be released under a new major version, e.g., `2.0.0`.
All consumers of the plugin will then need to update the specified major version, e.g., `2.+`.

> [!IMPORTANT]
> Using dynamic versions should only be done when releases and development versions are published to separate repositories.
> Not having this separation introduces the risk that consumers will use a development version of the plugin not yet ready to be used.
> In these scenarios, using a static version is preferred.

#### Using a static version

For projects using earlier versions of Gradle, the convention plugin must be applied using a static version.
For example:

```groovy
plugins {
    id 'com.myorg.convention-develocity-gradle-5-or-newer' version '1.0.0'
}
```

For each new release of the convention plugin, consuming builds will need to be explicitly updated.

### Content

This project is structured as follows:

  * `plugins` - Contains the convention plugins
    * `gradle-2-through-4` - Applies and configures the Build Scan plugin on builds using Gradle 2.0 through Gradle 4.10.3
    * `gradle-5-or-newer` - Applies and configures the Develocity plugin on builds using Gradle 5.0 or higher
  * `examples` - Contains example builds that apply the convention plugin for different Gradle versions
    * `gradle_3_and_earlier` - Applies the convention plugin on a Gradle 3 and earlier (only the Develocity plugin is applied)
    * `gradle_4` - Applies the convention plugin on a Gradle 4 build (the Develocity and CCUD plugins are applied)
    * `gradle_5` - Applies the convention plugin on a Gradle 5 build (the Develocity and CCUD plugins are applied)
    * `gradle_6` - Applies the convention plugin on a Gradle 6 build (the Develocity and CCUD plugins are applied)
    * `gradle_6.9_and_later` - Applies the convention plugin on a Gradle 6.9 and later build (the Develocity and CCUD plugins are applied)

### Running the example builds

Before running the example builds, publish the two convention plugins to your local Maven repository.

```bash
cd plugins/gradle-2-through-4
./gradlew publishToMavenLocal

cd plugins/gradle-5-or-newer
./gradlew publishToMavenLocal
```

Once you have published the plugins, you can run the example builds under `examples`:

```bash
cd examples/gradle_6.9_and_later
./gradlew build
```

> [!NOTE]
> You would publish your convention plugins to your internal artifact provider, e.g., Artifactory or Nexus, for production usage.

#### Requirements

To run the Gradle 3 and earlier example build, use Java 8. To run the Gradle 4 example build, use Java 8 or 9. These builds will fail when used with newer versions of Java.

To run the Gradle 5 and later example builds, use Java 8 or higher.
