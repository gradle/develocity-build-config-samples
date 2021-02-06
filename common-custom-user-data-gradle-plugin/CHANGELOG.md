# Changelog
All notable changes to this project will be documented in this file.

## [Unreleased]
- Detect generic "CI" environment variable or system property to add `CI` tag

### Fixed
- Fix TeamCity integration by using environment variables that are present in TeamCity 2020.2

## [1.1.1] - 2021-01-26
### Fixed
- Build will now fail if plugin is applied to `Project` on Gradle 6+. Plugin must be applied to `Settings` together with the `com.gradle.enterprise` plugin.

## [1.1] - 2021-01-21
- Plugin has no effect unless `com.gradle.enterprise` or `com.gradle.build-scan` is also applied
- Test system properties are no longer added as custom values

## [1.0] - 2021-01-14
Initial release.

[Unreleased]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-gradle-plugin-1.1.1...HEAD
[1.1.1]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-gradle-plugin-1.1...common-custom-user-data-gradle-plugin-1.1.1
[1.1]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-gradle-plugin-1.0...common-custom-user-data-gradle-plugin-1.1
[1.0]: https://github.com/gradle/gradle-enterprise-build-config-samples/releases/tag/common-custom-user-data-gradle-plugin-1.0
