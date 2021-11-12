# Changelog
All notable changes to this project will be documented in this file.

## [Unreleased]

## [1.5] - 2021-11-11
- Add custom value and search link for GitHub Actions run ID
- Fix generation of search links for custom values
- Redact any `user:password@` portion of the 'Git repository' custom value

## [1.4.2] - 2021-07-13
- Fix configuration cache compatibility when capturing test maxParallelForks
- Fix the git branch detection on Jenkins by using the branch name provided by Jenkins

## [1.4.1] - 2021-06-22
- Encode (escape) the TeamCity build number when creating a link to the TeamCity build

## [1.4] - 2021-06-08
- Fully support the [Gradle configuration cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
- Add custom values for IntelliJ IDEA and Eclipse versions
- Store the full Git commit ID in "Git commit id" custom value and the short ID in "Git commit id short" custom value
- Add the ability to override various Gradle Enterprise configuration settings via system properties

## [1.3] - 2021-05-11
- Add the Git repository URL as a custom value

## [1.2.1] - 2021-03-17
- Add TeamCity build configuration as a custom value and search link
- Fix configuration-cache support

## [1.2] - 2021-03-03

### Fixed
- Fix TeamCity integration by loading environment via Gradle properties

## [1.1.2]
- Detect generic "CI" environment variable or system property to add `CI` tag
- Detect Hudson and Bitrise CI and add respective custom tags and values

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

[Unreleased]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-gradle-plugin-1.5...HEAD
[1.5]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-gradle-plugin-1.4.2...common-custom-user-data-gradle-plugin-1.5
[1.4.2]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-gradle-plugin-1.4.1...common-custom-user-data-gradle-plugin-1.4.2
[1.4.1]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-gradle-plugin-1.4...common-custom-user-data-gradle-plugin-1.4.1
[1.4]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-gradle-plugin-1.3...common-custom-user-data-gradle-plugin-1.4
[1.3]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-gradle-plugin-1.2.1...common-custom-user-data-gradle-plugin-1.3
[1.2.1]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-gradle-plugin-1.2...common-custom-user-data-gradle-plugin-1.2.1
[1.2]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-gradle-plugin-1.1.2...common-custom-user-data-gradle-plugin-1.2
[1.1.2]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-gradle-plugin-1.1.1...common-custom-user-data-gradle-plugin-1.1.2
[1.1.1]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-gradle-plugin-1.1...common-custom-user-data-gradle-plugin-1.1.1
[1.1]: https://github.com/gradle/gradle-enterprise-build-config-samples/compare/common-custom-user-data-gradle-plugin-1.0...common-custom-user-data-gradle-plugin-1.1
[1.0]: https://github.com/gradle/gradle-enterprise-build-config-samples/releases/tag/common-custom-user-data-gradle-plugin-1.0
