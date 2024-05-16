## Convention Develocity Maven Extension

This project demonstrates how a convention extension can share the same Develocity build configuration across multiple projects. It
is intended to serve as a starting point for creating your own Maven extension that applies your specific Develocity configuration. Note
the inline comments in the build and source code for things to adjust specifically to your needs.

### Content

This project is structured as follows:

* `extension` - Contains the convention extension
* `examples` - Contains an example build that applies the convention extension
  * `maven_3.9.4` - Applies the convention extension on a Maven 3.9.4 build

### Running the example builds

Before running the example build, install the convention extension to your local Maven repository.

```bash
cd extension
./mvnw clean install
```

Once you have installed the extension, you can run the example build under `examples`:

```bash
cd examples/maven_3.9.4
./mvnw clean verify
```

Note that you would publish your convention extension to your internal artifact provider, e.g., Artifactory or Nexus, for production usage.
The artifact provider must be configured as a [Mirror](https://maven.apache.org/guides/mini/guide-mirror-settings.html) to Maven Central in order to correctly resolve the extension.

#### Requirements

To run the example build, use Java 8 or higher.
