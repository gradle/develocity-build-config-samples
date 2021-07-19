## Examples of Gradle Enterprise cache configuration for Maven

This Maven project provides example configuration for enabling caching for certain Maven plugins that are not supported out of the box by Gradle Enterprise.
Although the cache configuration does not cover all possible plugin uses, most standard/default plugin settings should work.

NOTE: These code snippets serve as examples only. It is imperative that you test the cache configuration with your own project.
Misconfigured caching can lead to incorrect cache hits, resulting in incorrect goal outputs.

The following Maven plugins are demonstrated:
- Spotbugs: `com.github.spotbugs:spotbugs-maven-plugin`
- PMD: `org.apache.maven.plugins:maven-pmd-plugin`
- Apache Avro: `org.apache.avro:avro-maven-plugi`
- Protobuf: `com.github.os72:protoc-jar-maven-plugin`
- Antlr3: `org.antlr:antlr3-maven-plugin`
- Clojure: `com.theoryinpractise:clojure-maven-plugin`
- AsciiDoctor: `org.asciidoctor:asciidoctor-maven-plugin`
- Animal Sniffer: `org.codehaus.mojo:animal-sniffer-maven-plugin`
- Enforcer: `org.apache.maven.plugins:maven-enforcer-plugin`
- Exec (driving Yarn Package Manager): `org.codehaus.mojo:exec-maven-plugin`
- Spring Cloud Contract: `org.springframework.cloud:spring-cloud-contract-maven-plugin`
- Duplicate Finder Plugin: `org.basepom.maven:duplicate-finder-maven-plugin`
- Webstart: `org.codehaus.mojo:webstart-maven-plugin` (Java 1.8 only)
