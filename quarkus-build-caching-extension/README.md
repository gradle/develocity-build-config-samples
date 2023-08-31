# Custom Maven Extension to make Quarkus build goal cacheable

This project performs programmatic configuration of the Gradle Enterprise Build Cache through a Maven extension. See [here](https://docs.gradle.com/enterprise/maven-extension/#custom_extension) for more details.

This project is derived from [the CCUD Maven extension](https://github.com/gradle/common-custom-user-data-maven-extension).

The instructions required to make the Quarkus build goal cacheable are isolated to the [QuarkusBuildCache](./src/main/java/com/gradle/QuarkusBuildCache.java) class.

**It is important to notice that a native executable can be a very large file, and copying it from/to the local cache, or transferring it from/to the remote cache can be an expensive operation that has to be balanced with the caching benefit**

## Requirements

This extension relies on the Quarkus dump configuration file feature (see [PR](https://github.com/quarkusio/quarkus/pull/34713)).
With this feature **enabled**, the Quarkus `build` goal dumps the Quarkus configuration in `.quarkus/quarkus-prod-config-dump` file, there is
also a `track-config-changes` goal which queries the actual values listed in the `.quarkus/quarkus-prod-config-dump` files and dumps those into
the `target/quarkus-prod-config-check` file.

## Limitations

Only the *native*, *uber-jar*, *jar* and *legacy-jar* [packaging types](https://quarkus.io/guides/maven-tooling#quarkus-package-pkg-package-config_quarkus.package.type) can be made cacheable.

The native packaging is cacheable only if the in-container build strategy (`quarkus.native.container-build=true`) is configured along with a fixed build image (`quarkus.native.builder-image`).
This in-container build strategy means the build is as reproducible as possible. Even so, some timestamps and instruction ordering may be different even when built on the same system in the same environment.

The Quarkus build goal is made not cacheable if the `.quarkus/quarkus-prod-config-dump` file is not present or if a Quarkus property was changed since the last build execution (or after cloning the repository if no local build yet happened).

## Application

Reference the extension in `.mvn/extensions.xml`.
This extension requires the gradle-enterprise-maven-extension.

```xml
<extensions>
    <extension>
        <groupId>com.gradle</groupId>
        <artifactId>gradle-enterprise-maven-extension</artifactId>
        <version>1.18.1</version>
    </extension>
    <extension>
        <groupId>com.gradle</groupId>
        <artifactId>quarkus-build-caching-extension</artifactId>
        <version>0.9</version>
    </extension>
</extensions>
```

Add the `track-prod-config-changes` execution to the `quarkus-maven-plugin` configuration:

```xml
<plugin>
    <groupId>${quarkus.platform.group-id}</groupId>
    <artifactId>quarkus-maven-plugin</artifactId>
    <version>${quarkus.platform.version}</version>
    <extensions>true</extensions>
    <executions>
        <execution>
            <id>track-prod-config-changes</id>
            <phase>process-resources</phase>
            <goals>
                <goal>track-config-changes</goal>
            </goals>
            <configuration>
                <dumpCurrentWhenRecordedUnavailable>true</dumpCurrentWhenRecordedUnavailable>
            </configuration>
        </execution>
        <execution>
            <goals>
                <goal>build</goal>
                <goal>generate-code</goal>
                <goal>generate-code-tests</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The generated `.quarkus/quarkus-prod-config-dump` file must be checked in the project repository to allow a cache hit on a first build after cloning.

## Configuration

The caching can be disabled by setting an environment variable:
```
GRADLE_QUARKUS_CACHE_ENABLED=false
```

By default, the values below are used to compute the dump-config (`.quarkus/quarkus-prod-config-dump`) and
config-check (`target/quarkus-prod-config-check`) file names:
- _file prefix_: quarkus
- _build profile_: prod
- _file suffix_: config-dump

Those values can be overridden with a file, when CI and local have different Quarkus properties for instance:
```properties
DUMP_CONFIG_PREFIX=quarkus
BUILD_PROFILE=prod
DUMP_CONFIG_SUFFIX=config-dump-ci
```

The configuration file location can either be defined:
- as an environment variable:
`GRADLE_QUARKUS_EXTENSION_CONFIG_FILE=.quarkus/extension-ci.properties`
- as a maven property:
`<gradle.quarkus.extension.config.file>.quarkus/extension-local.properties</gradle.quarkus.extension.config.file>`

### Goal Inputs

This extension makes the Quarkus build goal cacheable by configuring the following goal inputs:

#### General inputs
- The compilation classpath
- Generated sources directory
- OS details (name, version, arch)
- JDK version

#### Quarkus properties
See [here](https://quarkus.io/guides/config-reference#configuration-sources) for details

Quarkus' properties are fetched from the *config dump* populated by the Quarkus `build` goal.
The `build` goal is cacheable only if the `track-config-changes` goal generates a *config dump* identical to the one generated by the previous `build` execution.
This ensures that the local Quarkus configuration hasn't changed since last build, otherwise a new `build` execution is required as a configuration can change the produced artifact.

`target/quarkus-prod-config-check` is added as a goal input

#### Quarkus file properties
Some properties are pointing to a file which has to be declared as file input. This allows to have the file content part of the cache key (`RELATIVE_PATH` strategy).
- `quarkus.docker.dockerfile-native-path`
- `quarkus.docker.dockerfile-jvm-path`
- `quarkus.openshift.jvm-dockerfile`
- `quarkus.openshift.native-dockerfile`
