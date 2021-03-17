## Common Custom User Data Gradle Plugin [![Plugin Portal](https://img.shields.io/maven-metadata/v?metadataUrl=https://plugins.gradle.org/m2/com/gradle/common-custom-user-data-gradle-plugin/maven-metadata.xml&label=Plugin%20Portal)](https://plugins.gradle.org/plugin/com.gradle.common-custom-user-data-gradle-plugin) [![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.gradle.org/scans)

### Overview

The Common Custom User Data Gradle Plugin for Gradle Enterprise enhances published build scans
by adding a set of tags, links and custom values that have proven to be useful for many users of Gradle Enterprise.

You can leverage this plugin for your project in one of two ways:
1. Apply the published plugin directly in your build and immediately benefit from enhanced build scans.
2. Copy this repository and develop a customized version of the plugin which will standardize Gradle Enterprise usage across multiple projects.

Out of the box, this plugin enhances all build scans published with additional tags, links and custom values. These include:
- A tag representing the operating system.
- A tag representing how the build was invoked, be that from your IDE (IDEA, Eclipse, Android Studio) or from the command-line.
- A tag representing builds run on CI, together with a set of tags, links and custom values specific to the CI server running the build.
- For `git` repositories, information on the commit id, branch name, status, and whether the checkout is dirty or not.

### Applying the published plugin

The Common Custom User Data Gradle Plugin is available in the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.gradle.common-custom-user-data-gradle-plugin). This plugin
requires the [Gradle Enterprise Gradle plugin](https://plugins.gradle.org/plugin/com.gradle.enterprise) to also be applied in your build in order to have an effect.

#### Gradle 6.x (or newer)

The plugin needs to be applied in `settings.gradle`, alongside the `com.gradle.enterprise` plugin:

```groovy
plugins {
    // …
    id 'com.gradle.enterprise' version '3.6'
    id 'com.gradle.common-custom-user-data-gradle-plugin' version '1.2'
    // …
}
```

#### Gradle 5.x

The plugin needs to be applied in `build.gradle` of the root project, alongside the `com.gradle.build-scan` plugin:

```groovy
plugins {
    // …
    id 'com.gradle.build-scan' version '3.6'
    id 'com.gradle.common-custom-user-data-gradle-plugin' version '1.2'
    // …
}
```

### Developing a customized version of the plugin

For more flexibility, we recommend creating a copy of this repository so that you may develop a customized version of the plugin and publish it internally for your projects to consume.

This approach has a number of benefits:
- Tailor the build scan enhancements to exactly the set of tags, links and custom values you require.
- Standardize the configuration for connecting to Gradle Enterprise and the remote build cache in your organization, removing the need for each project to specify this configuration.

If your customized plugin provides all required Gradle Enterprise configuration, then a consumer project will get all the benefits of Gradle Enterprise simply by applying the plugin. The
project sources provide a good template to get started with your own plugin.

Refer to the [Javadoc](https://docs.gradle.com/enterprise/gradle-plugin/api/) for more details on the key types available for use.

See the [Gradle User Manual](https://docs.gradle.org/current/userguide/publishing_gradle_plugins.html#custom-plugin-repositories) for more details on publishing Gradle plugins to an internal repository.

### Changelog

Refer to the [changelog](https://github.com/gradle/gradle-enterprise-build-config-samples/blob/master/common-custom-user-data-gradle-plugin/CHANGELOG.md) to see detailed changes on the versions.
