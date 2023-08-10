## Convention Gradle Enterprise Gradle Plugin

This project demonstrates how the same Gradle Enterprise build configuration can be shared across multiple projects using a convention plugin. It 
is intended to serve as a starting point for creating your own Gradle plugin that applies your specific Gradle Enterprise configuration. See inline
comments for things to adjust specifically to your needs.

### Contents

This project contains the following:

  * `plugins` - Contains the convention plugins
    * `gradle-2-through-4` - Applies and configures the Build Scan plugin on builds using Gradle 2.0 through Gradle 4.10.3
    * `gradle-5-or-newer` - Applies and configures the Gradle Enterprise plugin on builds using Gradle 5.0 or higher
  * `examples` - Contains example builds that apply the convention plugin for different Gradle versions
    * `gradle_2.0` - Applies the convention plugin on a Gradle 2.0 build (only the GE plugin is applied)
    * `gradle_4.1` - Applies the convention plugin on a Gradle 4.1 build (the GE and CCUD plugins are applied)
    * `gradle_5.1.1` - Applies the convention plugin on a Gradle 5 build (the GE and CCUD plugins are applied)
    * `gradle_6.0.1` - Applies the convention plugin on a Gradle 6 build (the GE and CCUD plugins are applied)

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
cd example-builds/via-build-script/gradle_6.0.1
./gradlew build
```

Note that for production usage, you would publish your convention plugins to your internal dependency storage, e.g. Artifactory.

#### Requirements

To run the Gradle 2.0 example build, use Java 8. To run the Gradle 4.1 example build, use Java 8 or 9. These builds will fail when used with newer versions of Java.

To run the Gradle 5.1.1 and Gradle 6.0.1 example builds, use Java 8 or higher.
