## Example Gradle Enterprise Conventions Gradle Plugin

This project demonstrates how to share a common Gradle Enterprise configuration across multiple projects. It is intended to serve as a starting point for creating your own plugin to apply your specific Gradle Enterprise configuration. See inline comments for things to adjust specifically to your needs.

### Contents

This project contains the following:

  * `plugins` - Contains the example convention plugins
    * `gradle-2-through-4` - Applies and configure build scans on projects using Gradle 2.0 through Gradle 4.10.2
    * `gradle-5-or-newer` - Applies and configure Gradle Enterprise on projects using Gradle 5 or higher
  * `example-builds` - A set of example builds that apply the convention plugins
    * `via-build-script` - Example builds that apply the convention plugins directly in build scripts
      * `gradle_2.0` - Uses the convention plugin for Gradle 2.0 through Gradle 4.10.2
      * `gradle_5.1.1` - Uses the convention plugin on a Gradle 5 project
      * `gradle_6.0.1` - Uses the convention plugin on a Gradle 6 project

### Running the example builds

Before running the example builds, publish the two example Gradle plugins to your local Maven repository:

```bash
cd plugins/gradle-2-through-4
./gradlew publishToMavenLocal

cd plugins/gradle-5-or-newer
./gradlew publishToMavenLocal
```

Once the plugins are published, then you can run the example builds under `example-builds/via-build-script` using the Gradle wrapper:

```bash
cd example-builds/via-build-script/gradle_6.0.1
./gradlew build
```

#### Example Builds and Java Versions

To run the Gradle 2.0 example builds, use Java 8. The Gradle 2.0 example builds will fail when used with newer versions of Java.

It is recommended to use Java 11 for the Gradle 5.1.1 and Gradle 6.0.1 example builds.

### Differences with the Common Custom User Data Gradle plugin
These plugins differ from the Common Custom User Data Gradle Plugin in that they also apply the Gradle Enterprise plugins (the Common Custom User Data Gradle Plugin does not apply the plugins...it assumes the plugins have already been applied when it is used).
