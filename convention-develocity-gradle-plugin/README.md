## Convention Develocity Gradle Plugin

This project demonstrates how a convention plugin can share the same Develocity build configuration across multiple projects. It 
is intended to serve as a starting point for creating your own Gradle plugin that applies your specific Develocity configuration. Note
the inline comments in the build and source code for things to adjust specifically to your needs.

### Content

This project is structured as follows:

  * `plugins` - Contains the convention plugins
    * `gradle-2-through-4` - Applies and configures the Build Scan plugin on builds using Gradle 2.0 through Gradle 4.10.3
    * `gradle-5-or-newer` - Applies and configures the Develocity plugin on builds using Gradle 5.0 or higher
  * `examples` - Contains example builds that apply the convention plugin for different Gradle versions
    * `gradle_2.0` - Applies the convention plugin on a Gradle 2.0 build (only the Develocity plugin is applied)
    * `gradle_4.1` - Applies the convention plugin on a Gradle 4.1 build (the Develocity and CCUD plugins are applied)
    * `gradle_5.1.1` - Applies the convention plugin on a Gradle 5 build (the Develocity and CCUD plugins are applied)
    * `gradle_6.0.1` - Applies the convention plugin on a Gradle 6+ build (the Develocity and CCUD plugins are applied)

### Running the example builds

Before running the example builds, publish the two convention plugins to your local Maven repository.

```bash
cd plugins/gradle-2-through-4
./gradlew publishToMavenLocal

cd plugins/gradle-5-or-newer
./gradlew publishToMavenLocal
```

Once you have published the plugins, you can run the four example builds under `examples`:

```bash
cd examples/gradle_6.0.1
./gradlew build
```

Note that you would publish your convention plugins to your internal artifact provider, e.g., Artifactory or Nexus, for production usage.

#### Requirements

To run the Gradle 2.0 example build, use Java 8. To run the Gradle 4.1 example build, use Java 8 or 9. These builds will fail when used with newer versions of Java.

To run the Gradle 5.1.1 and Gradle 6.0.1 example builds, use Java 8 or higher.
