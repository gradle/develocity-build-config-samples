## Common Custom User Data Maven Extension [![Maven Central](https://img.shields.io/maven-central/v/com.gradle/common-custom-user-data-maven-extension)](https://search.maven.org/artifact/com.gradle/common-custom-user-data-maven-extension) [![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.gradle.org/scans)

### Overview

The Common Custom User Data Maven Extension for Gradle Enterprise serves multiple purposes:
- an example how to build and deploy your own Gradle Enterprise extension 
- a reusable artifact deployed to [Maven Central](https://search.maven.org/artifact/com.gradle/common-custom-user-data-maven-extension) that you can apply to your build and it just works

Please refer to the [Gradle Enterprise Maven Extension User Manual](https://docs.gradle.com/enterprise/maven-extension/#using_the_common_custom_user_data_maven_extension) for more details.

### Usage

In order for the Common Custom User Data Maven extension to take effect, you need to register it in the `.mvn/extensions.xml` file in your root project. The `extensions.xml` file is the same file where you have already declared the Gradle Enterprise Maven extension. See [here](https://github.com/gradle/gradle-enterprise-build-config-samples/blob/master/common-custom-user-data-maven-extension/.mvn/extensions.xml) for an example.

### Changelog

Refer to the [changelog](https://github.com/gradle/gradle-enterprise-build-config-samples/blob/master/common-custom-user-data-maven-extension/CHANGELOG.md) to see detailed changes on the versions.
