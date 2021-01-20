## Common Custom User Data Gradle Plugin [![Revved up by Gradle Enterprise](https://img.shields.io/maven-metadata/v?metadataUrl=https://plugins.gradle.org/m2/com/gradle/common-custom-user-data-gradle-plugin/maven-metadata.xml&label=Plugin Portal)](https://plugins.gradle.org/plugin/com.gradle.common-custom-user-data-gradle-plugin) [![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.gradle.org/scans)

### Overview

The Common Custom User Data Gradle Plugin for Gradle Enterprise serves multiple purposes:
- an example how to create a custom Gradle plugin that will standardise your Gradle Enterprise usage
- a reusable artifact deployed to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.gradle.common-custom-user-data-gradle-plugin) that you can apply to your Gradle builds, and it just works 
  
This plugin requires the the [Gradle Enterprise Gradle plugin](https://plugins.gradle.org/plugin/com.gradle.enterprise) to already be applied in your build in order to have an effect.

### Usage

In order for the Common Custom User Gradle plugin to become active, you need to apply it in the `settings.gradle[.kts]` file of your project. The `settings.gradle[.kts]` file is the same file where you have already declared the Gradle Enterprise Gradle plugin. See [here](https://github.com/gradle/gradle-enterprise-build-config-samples/blob/master/common-custom-user-data-gradle-plugin/settings.gradle) for an example.

If you are still using a 5.x version of Gradle, apply the Common Custom User Gradle plugin to the `build.gradle[.kts]` file of your root project.

### Changelog

Refer to the [changelog](https://github.com/gradle/gradle-enterprise-build-config-samples/blob/master/common-custom-user-data-gradle-plugin/CHANGELOG.md) to see detailed changes on the versions.
