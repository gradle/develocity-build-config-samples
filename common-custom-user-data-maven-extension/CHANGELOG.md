# Changelog
All notable changes to this project will be documented in this file.

## [Unreleased]
- Updated dependency versions: 
  - org.codehaus.groovy:3.0.7 -> 3.0.8
  - org.codehaus.plexus:plexus-component-annotations:2.0.0 -> 2.1.0

## [1.7.3] - 2021-07-13
- Fix the git branch detection on Jenkins by using the branch name provided by Jenkins (#109).

## [1.7.2] - 2021-06-28
- Publish a "fat" jar that contains all of the extension's dependencies (to make it easier to use the extension in certian scenarios)

## [1.7.1] - 2021-06-22
- Encode (escape) the TeamCity build number when creating a link to the TeamCity build (fixes Issue #101)

## [1.7] - 2021-06-11
- Add access to the `GradleEnterpriseApi` to configure Gradle Enterprise

## [1.6] - 2021-06-08
- Add custom values for IntelliJ IDEA and Eclipse versions
- Store the full Git commit ID in "Git commit id" custom value and the short ID in "Git commit id short" custom value
- Add `gradle.cache.remote.shard` system property to add an additional path segment to the remote build cache URL

## [1.5] - 2021-05-11
- Add the Git repository URL as a custom value

## [1.4.1] - 2021-03-17
- Add TeamCity build configuration as a custom value and search link

## [1.4] - 2021-03-03

### Fixed
- Fix TeamCity integration by loading properties from the `MavenSession`

## [1.3.2] - 2021-02-15
- Detect generic "CI" environment variable or system property to add `CI` tag
- Detect Hudson and Bitrise CI and add respective custom tags and values

### Fixed
- Fix TeamCity integration by using environment variables that are present in TeamCity 2020.2

## [1.3.1] - 2021-01-21
### Fixed
- Builds with no changes as per `git status` are no longer tagged as `Dirty`

## [1.3] - 2020-09-10
### Added
- Add access to the `BuildCacheApi` to configure the build cache

### Fixed
- Fix _GitLab Source_ custom link

## [1.2] - 2020-08-31
### Added
- Tag builds run in TravisCI
- Tag builds triggered by IntelliJ IDEA

### Removed
- Remove build scan tag for Maven version since Gradle Enterprise `2020.3+` allows filtering by Maven version out-of-the-box

## [1.1.1] - 2020-07-22
### Fixed
- Add `hint` to `CommonCustomUserDataMavenExtension` to allow multiple `AbstractMavenLifecycleParticipant` implementations on the classpath

## [1.1] - 2020-06-18
### Added
- Evaluate `.mvn/gradle-enterprise-custom-user-data.groovy` custom user data Groovy script if present

## [1.0.1] - 2020-06-12
### Fixed
- Improve lookup of `BuildScanApi`

### Removed
- Remove dependency on SLF4J.

## [1.0] - 2020-05-25
Initial release.

[Unreleased]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.7.3...HEAD
[1.7.3]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.7.2...common-custom-user-data-maven-extension-1.7.3
[1.7.2]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.7.1...common-custom-user-data-maven-extension-1.7.2
[1.7.1]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.7...common-custom-user-data-maven-extension-1.7.1
[1.7]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.6...common-custom-user-data-maven-extension-1.7
[1.6]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.5...common-custom-user-data-maven-extension-1.6
[1.5]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.4.1...common-custom-user-data-maven-extension-1.5
[1.4.1]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.4...common-custom-user-data-maven-extension-1.4.1
[1.4]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.3.2...common-custom-user-data-maven-extension-1.4
[1.3.2]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.3.1...common-custom-user-data-maven-extension-1.3.2
[1.3.1]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.3...common-custom-user-data-maven-extension-1.3.1
[1.3]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.2...common-custom-user-data-maven-extension-1.3
[1.2]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.1.1...common-custom-user-data-maven-extension-1.2
[1.1.1]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.1...common-custom-user-data-maven-extension-1.1.1
[1.1]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.0.1...common-custom-user-data-maven-extension-1.1
[1.0.1]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-maven-extension-1.0...common-custom-user-data-maven-extension-1.0.1
[1.0]: https://github.com/gradle/gradle-enterprise-build-config-samples/releases/tag/common-custom-user-data-maven-extension-1.0
