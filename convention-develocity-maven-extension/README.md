## Convention Develocity Maven Extension

This project demonstrates how a convention extension can share the same Develocity build configuration across multiple projects.
It is intended to serve as a starting point for creating your own Maven extension that applies your specific Develocity configuration.
Note the inline comments in the build and source code for things to adjust specifically to your needs.

### Applying the extension to your build

#### Using a version range

It's recommended to apply the convention extension using a [version range](https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html).
This approach ensures that the latest version of the extension is resolved, up to the specified maximum version.
For example, the following resolves the latest version of the extension up to, but excluding, version `2.0.0`:

```xml
<extensions>
    <extension>
        <groupId>com.myorg</groupId>
        <artifactId>convention-develocity-maven-extension</artifactId>
        <version>(,2.0.0)</version>
    </extension>
</extensions>
```

This allows you to quickly roll out non-breaking changes to all consumers of the convention extension.
Breaking changes in the extension should be released under a new major version (e.g., `2.0.0`).
All consumers of the extension will then need to update the upper boundary of the version range to the next major version (e.g., `(,3.0.0)`).

### Content

This project is structured as follows:

* `extension` - Contains the convention extension
* `examples` - Contains example builds that apply the convention extension for different Maven versions
  * `maven_3` - Applies the convention extension on a Maven 3 build (the Develocity and CCUD extensions are applied)

### Running the example builds

Before running the example build, install the convention extension to your local Maven repository.

```bash
cd extension
./mvnw clean install
```

Once you have installed the extension, you can run the example build under `examples`:

```bash
cd examples/maven_3
./mvnw clean verify
```

Note that you would publish your convention extension to your internal artifact provider, e.g., Artifactory or Nexus, for production usage.
The artifact provider must be configured as a [Mirror](https://maven.apache.org/guides/mini/guide-mirror-settings.html) to Maven Central in order to correctly resolve the extension.

#### Requirements

To run the example builds, use Java 8 or higher.
