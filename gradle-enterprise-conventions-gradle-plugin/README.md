## Example Gradle Enterprise Gradle Convention Plugin

This project demonstrates how to share a common Gradle Enterprise configuration across multiple projects. It is intended to serve as a starting point for creating your own plugin to apply your specific Gradle Enterprise configuration. See inline comments for things to adjust specifically to your needs.

### Contents

This project contains the following:

  * `plugins` - Contains the example convention plugins.
    * `gradle-2-through-4` - Example convention plugin to apply and configure build scans on projects using Gradle 2.0 through Gradle 4.10.2.
    * `gradle-5-or-newer` - Example convention plugin to apply and configure Gradle Enterprise on projects using Gradle 5 or higher.
  * `init-script/add-gradle-enterprise-conventions.gradle` - Example Gradle init script that applies the appropriate convention plugin based on the Gradle version (works with all Gradle versions 2.0 and higher).
  * `example-builds` - A set of example builds that apply the convention plugins.
    * `gradle_2.0` - Example build that uses the convention plugin for Gradle 2.0 through Gradle 4.10.2.
    * `gradle_5.6` - Example build that uses the convention plugin on a Gradle 5 project.
    * `gradle_7.1` - Example build that uses the convention plugin on a Gradle 7.1 project.
    * `via-init-script` - Examples that apply the convention plugins using the init script. Each subfolder contains a build.sh that demonstrates how to apply the init script.

### Differences with the Common Custom User Data Gradle plugin
These plugins differ from the Common Custom User Data Gradle Plugin in that they also apply the Gradle Enterprise plugins (the Common Custom User Data Gradle Plugin does not apply the plugins...it assumes the plugins have already been applied when it is used).
