## Examples of Gradle Enterprise cache configuration for Maven

This Maven project provides example configuration for enabling caching for certain Maven plugins that are not supported out of the box by Gradle Enterprise.
Although the cache configuration does not cover all possible plugin uses, most standard/default plugin settings should work.

The following Maven plugins are demonstrated:
- Spotbugs: `com.github.spotbugs:spotbugs-maven-plugin:4.1.4`
- PMD: `org.apache.maven.plugins:maven-pmd-plugin:3.13.0`
- Apache Avro: `org.apache.avro:avro-maven-plugin: 1.10.1`
- Protobuf: `com.github.os72:protoc-jar-maven-plugin:3.11.4`
- Antlr3: `org.antlr:antlr3-maven-plugin:3.4`
- Webstart: `org.codehaus.mojo:webstart-maven-plugin:1.0-beta-7` (Java 1.8 only)
