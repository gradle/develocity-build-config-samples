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

### Applying the published extension

The Common Custom User Data Maven Extension is available in [Maven Central](https://search.maven.org/artifact/com.gradle/common-custom-user-data-maven-extension). This extension requires the [Gradle Enterprise Maven extension](https://search.maven.org/artifact/com.gradle/gradle-enterprise-maven-extension) to also be applied in your build in order to have an effect.

In order for the Common Custom User Data Maven Extension to become active, you need to register it in the `.mvn/extensions.xml` file in your root project.
The `extensions.xml` file is the same file where you have already declared the Gradle Enterprise Maven extension. See [here](https://github.com/gradle/gradle-enterprise-build-config-samples/blob/master/common-custom-user-data-maven-extension/.mvn/extensions.xml) for an example.

#### Capturing additional tag, links and values in your build scans

You can add further build scan configuration and enhancements to the core set provided by the Common Custom User Data Maven Extension.
The extension checks for a Groovy script in `.mvn/gradle-enterprise-custom-user-data.groovy` in your project directory. 
If the file exists, it evaluates the script with the following bindings:

- `buildScan` (type: [BuildScanApi](https://docs.gradle.com/enterprise/maven-extension/api/com/gradle/maven/extension/api/scan/BuildScanApi.html)): _enhance build scans and configure build scan publishing_
- `buildCache` (type: [BuildCacheApi](https://docs.gradle.com/enterprise/maven-extension/api/com/gradle/maven/extension/api/cache/BuildCacheApi.html)): _configure build cache_
- `log` (type: [`Log`](https://maven.apache.org/ref/current/maven-plugin-api/apidocs/org/apache/maven/plugin/logging/Log.html)): _write to the build log_
- `project` (type: [`MavenProject`](https://maven.apache.org/ref/current/maven-core/apidocs/org/apache/maven/project/MavenProject.html)): _the top-level Maven project_
- `session` (type: [`MavenSession`](https://maven.apache.org/ref/current/maven-core/apidocs/org/apache/maven/execution/MavenSession.html)): _the Maven session_

See [here](https://github.com/gradle/gradle-enterprise-build-config-samples/blob/master/common-custom-user-data-maven-extension/.mvn/gradle-enterprise-custom-user-data.groovy) for an example.

### Developing a customized version of the extension

For more flexibility, we recommend creating a copy of this repository so that you may develop a customized version of the extension and publish it internally for your projects to consume.

This approach has a number of benefits:
- Tailor the build scan enhancements to exactly the set of tags, links and custom values you require.
- Standardize the configuration for connecting to Gradle Enterprise and Gradle Build Cache in your organization, removing the need for each project to specify this configuration.

The project sources provide a good template to get started with your own Maven extension. 
Refer to the [Javadoc](https://docs.gradle.com/enterprise/maven-extension/api/) for more details on the key types available for use.

#### Providing Gradle Enterprise configuration in your custom Maven extension

Using a custom extension, it is possible to completely remove the need for each project to declare a `gradle-enterprise.xml` file.
Much of the configuration can be supplied programmatically. See `CustomGradleEnterpriseConfig` for examples.

At this time, the Gradle Enterprise server URL cannot be provided programmatically, and must be specified in a `gradle-enterprise.xml` file.
To avoid adding this file to every project, this configuration file can be added to the `src/main/resources` directory so that it will be packaged in the root of you custom extension, and discovered by Gradle Enterprise when loading the extension.
This project includes a `gradle-enterprise.sample.xml` file, which should be modified with your server URL and renamed to `gradle-enterprise.xml`.

Once your custom extension provides all required Gradle Enterprise configuration, then a consumer project will get all the benefits of Gradle Enterprise simply by applying the extension.

### Changelog

Refer to the [changelog](https://github.com/gradle/gradle-enterprise-build-config-samples/blob/master/common-custom-user-data-maven-extension/CHANGELOG.md) to see detailed changes on the versions.
