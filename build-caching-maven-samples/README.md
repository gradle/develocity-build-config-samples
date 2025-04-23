## Examples of Develocity build cache configuration for Maven

This Maven project provides example configuration for enabling build caching for certain Maven plugins that are not supported out of the box by Develocity.
Although the cache configuration does not cover all possible plugin uses, most standard/default plugin settings should work.

NOTE: These code snippets serve as examples only. It is imperative that you test the build caching configuration with your own project.
Misconfigured build caching can lead to incorrect cache hits, resulting in incorrect goal outputs.

The following Maven plugins are demonstrated:
- Animal Sniffer: `org.codehaus.mojo:animal-sniffer-maven-plugin`
- Antlr3: `org.antlr:antlr3-maven-plugin`
- AsciiDoctor: `org.asciidoctor:asciidoctor-maven-plugin`
- AspectJ: `org.codehaus.mojo:aspectj-maven-plugin`
- Avro (Apache): `org.apache.avro:avro-maven-plugin`
- Clojure: `com.theoryinpractise:clojure-maven-plugin`
- CXF codegen plugin: `org.apache.cxf:cxf-codegen-plugin` 
- Duplicate Finder Plugin: `org.basepom.maven:duplicate-finder-maven-plugin`
- Enforcer: `org.apache.maven.plugins:maven-enforcer-plugin`
- Exec (driving Yarn Package Manager): `org.codehaus.mojo:exec-maven-plugin`
- Kotlin: `org.jetbrains.kotlin:kotlin-maven-plugin`
- PMD: `org.apache.maven.plugins:maven-pmd-plugin`
- Protobuf: `com.github.os72:protoc-jar-maven-plugin`
- Spotbugs: `com.github.spotbugs:spotbugs-maven-plugin`
- Spring Cloud Contract: `org.springframework.cloud:spring-cloud-contract-maven-plugin`
- Webstart: `org.codehaus.mojo:webstart-maven-plugin` (Java 1.8 only)
