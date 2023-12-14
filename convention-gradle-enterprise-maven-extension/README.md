## Convention Gradle Enterprise Maven Extension

This project demonstrates how a convention extension can share the same Gradle Enterprise build configuration across multiple projects. It
is intended to serve as a starting point for creating your own Maven extension that applies your specific Gradle Enterprise configuration. Note
the inline comments in the build and source code for things to adjust specifically to your needs.

### Content

This project is structured as follows:

* `extension` - Contains the convention extension
* `example` - Contains an example build that applies the convention extension

### Running the example builds

Before running the example build, install the convention extension to your local Maven repository.

```bash
cd extension
./mvnw clean install
```

Once the extension is published, you can run the example build under `example`:

```bash
cd example
./mvnw clean verify
```

Note that you would publish your convention extension to your internal artifact provider, e.g., Artifactory or Nexus, for 
production usage. The artifact provider must be configured as a [Mirror](https://maven.apache.org/guides/mini/guide-mirror-settings.html) 
to Maven Central in order to correctly resolve the extension.

#### Requirements

To run the example build, use Java 8.
