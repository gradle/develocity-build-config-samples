## Common Custom User Data Maven Extension [![Maven Central](https://img.shields.io/maven-central/v/com.gradle/common-custom-user-data-maven-extension)](https://search.maven.org/artifact/com.gradle/common-custom-user-data-maven-extension) [![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.gradle.org/scans)

### Overview

The Common Custom User Data Maven Extension for Gradle Enterprise enhances published build scans
by adding a set of tags, links and custom values that have proven to be useful for many users of Gradle Enterprise.

You can leverage this extension for your project in one of two ways:
1. Apply the published extension directly in your `.mvn/extensions.xml` and immediately benefit from enhanced build scans.
2. Copy this repository and develop a customized version of the extension which will standardize Gradle Enterprise usage across multiple projects.

Out of the box, this extension enhances all build scans published with additional tags, links and custom values. These include:
- A tag representing the Operating System.
- A tag representing how the build was invoked, be that from your IDE (IDEA, Eclipse) or from the command-line.
- A tag representing builds run on CI, together with a set of tags, links and custom values specific to the CI server running the build.
- For `git` repositories, information on the commit id, branch name, status, and whether the checkout is dirty or not.

### Using the published extension

The Common Custom User Data Maven Extension is available in [Maven Central](https://search.maven.org/artifact/com.gradle/common-custom-user-data-maven-extension).

The Common Custom User Data Maven Extension requires the [Gradle Enterprise Maven extension](https://search.maven.org/artifact/com.gradle/gradle-enterprise-maven-extension) to already be applied in your build in order to have an effect.
In order for the Common Custom User Data Maven Extension to become active, you need to register it in the `.mvn/extensions.xml` file in your root project.
The `extensions.xml` file is the same file where you have already declared the Gradle Enterprise Maven extension. See [here](https://github.com/gradle/gradle-enterprise-build-config-samples/blob/master/common-custom-user-data-maven-extension/.mvn/extensions.xml) for an example.

### Developing a customized version of the plugin

For more flexibility, we recommend creating a copy of this repository so that you may develop a customized version of the extension and publish it locally for your projects to consume.

This approach has a number of benefits:
- Tailor the build scan enhancements to exactly the set of tags, links and custom values you require.
- Standardize the configuration for connecting to Gradle Enterprise in your organization, removing the need for each project to specify this configuration.

If your customized extension provides all required Gradle Enterprise configuration, then a consumer project will get all the benefits of Gradle Enterprise simply by applying the extension. The plugin sources provide a placeholder and example code to get you started.

Please refer to the [Gradle Enterprise Maven Extension User Manual](https://docs.gradle.com/enterprise/maven-extension/#using_the_common_custom_user_data_maven_extension) for more details.

### Changelog

Refer to the [changelog](https://github.com/gradle/gradle-enterprise-build-config-samples/blob/master/common-custom-user-data-maven-extension/CHANGELOG.md) to see detailed changes on the versions.
