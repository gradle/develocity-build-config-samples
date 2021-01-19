## Common Custom User Data Gradle Plugin

### Overview

The Common Custom User Data Gradle Plugin for Gradle Enterprise serves multiple purposes:
- an example how to create a custom plugin that will standardise your Gradle Enterprise usage
- a reusable artifact deployed to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.gradle.common-custom-user-data-gradle-plugin) that you can apply to your Gradle build 
  to add useful tags, link and custom values to your published build scans
  
This plugin requires the `com.gradle.enterprise` or `com.gradle.build-scan` plugin to be applied in order to have an effect.
  
This plugin is able to be applied as either a `Settings` plugin or a regular `Project` plugin:
- for newer versions of Gradle (6.0 and later), this plugin should be applied to `settings.gradle[.kts]` (together with the `com.gradle.enterprise` plugin)
- for older Gradle versions (5.x and older), this plugin should be applied to the root project in `build.gradle[.kts]` (together with the `com.gradle.build-scan` plugin)

### Changelog

Refer to the [changelog](https://github.com/gradle/gradle-enterprise-build-config-samples/blob/master/common-custom-user-data-gradle-plugin/CHANGELOG.md) to see detailed changes on the versions.
