# Example Gradle Enterprise Conventions Maven Extension

This project demonstrates how to share a common Gradle Enterprise configuration across multiple Maven projects. It is intended to serve as a starting point for creating your own extension to apply your specific Gradle Enterprise configuration.

## Usage

* Create your own copy of the `extension` project
* Address all `todo` comments
    * Update the `groupId` and `artifactId` in the [POM](extension/pom.xml) file
    * Update the server URL to your own Gradle Enterprise instance in the [Gradle Enterprise configuration](extension/.mvn/gradle-enterprise.xml) and [CustomGradleEnterpriseConfig.java](extension/src/main/java/com/example/CustomGradleEnterpriseConfig.java)
    * Update `isCi` in [CiUtils.java](extension/src/main/java/com/example/CiUtils.java) to properly detect your CI environment
* Deploy the extension to your own artifact repository

## Contents

This project contains the following:

* `extension` - Contains the example convention extension that applies and configures Gradle Enterprise on projects
* `example-builds` - A set of example builds that apply the convention extension for various Maven versions

### Running the example builds

Before running the example builds, publish the two example Gradle plugins to your local Maven repository:

```bash
cd extension
./mvnw clean install
```

Once the extension is published, then you can run the example builds under `example-builds` using the Maven wrapper:

```bash
cd example-builds/maven_3.9.x
./mvnw clean install
```

## Differences with the Common Custom User Data Gradle plugin
This extension differs from the [Common Custom User Data Maven Extension](https://github.com/gradle/common-custom-user-data-maven-extension) in that this also applies the Gradle Enterprise extension (the Common Custom User Data Maven Extension does not apply the extension...it assumes the extension has already been applied when it is used).
