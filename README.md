> _This repository is maintained by the Develocity Solutions team, as one of several publicly available repositories:_
> - _[Android Cache Fix Gradle Plugin][android-cache-fix-plugin]_
> - _[Common Custom User Data Gradle Plugin][ccud-gradle-plugin]_
> - _[Common Custom User Data Maven Extension][ccud-maven-extension]_
> - _[Common Custom User Data sbt Plugin][ccud-sbt-plugin]_
> - _[Develocity Build Configuration Samples][develocity-build-config-samples] (this repository)_
> - _[Develocity Build Validation Scripts][develocity-build-validation-scripts]_
> - _[Develocity Open Source Projects][develocity-oss-projects]_
> - _[Quarkus Build Caching Extension][quarkus-build-caching-extension]_

# Develocity Build Configuration Samples

[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.solutions-team.gradle.com/scans)

This repository contains samples that demonstrate how to customize and optimize your Develocity build configuration using Gradle or Maven.

The samples aim to demonstrate customizations, optimizations, API usages, as well as patterns for applying and reusing Develocity build configuration across multiple projects.

## Samples

The samples can be grouped into the following categories:

 - Configuration snippets to capture additional data in build scans
 - Configuration snippets to optimize build performance
 - Template projects for Gradle and Maven when using Develocity
 - Scripts and code to apply and reuse Develocity across multiple projects and build tools

## Onboarding skill for Claude Code

[`develocity-onboarding`](.claude/skills/develocity-onboarding) is a [Claude Code][claude-code] skill that onboards a repository to Develocity. It detects the build tool (Maven, Gradle, npm, sbt or Bazel), adapts the matching `common-develocity-*-configuration` sample from this repository, wires the access key into CI, checks that the build still loads, and opens a pull request for review.

On top of the samples, it detects CI through the provider's own environment variable rather than `CI`, and only lets CI write to the remote build cache when an access key is present, so builds from forks keep working.

It is available in any Claude Code session started in a checkout of this repository. To use it from any repository, symlink it into your personal skills directory:

```bash
ln -s "$(git rev-parse --show-toplevel)/.claude/skills/develocity-onboarding" ~/.claude/skills/develocity-onboarding
```

Then, from the repository to onboard:

```
/develocity-onboarding https://develocity.example.com
```

See the [skill's README](.claude/skills/develocity-onboarding/README.md) for its inputs and what it changes.

## Learn more

Visit our website to learn more about [Develocity][develocity].

## License

The Develocity build configuration samples are open-source software released under the [Apache 2.0 License][apache-license].

[android-cache-fix-plugin]: https://github.com/gradle/android-cache-fix-gradle-plugin
[ccud-gradle-plugin]: https://github.com/gradle/common-custom-user-data-gradle-plugin
[ccud-maven-extension]: https://github.com/gradle/common-custom-user-data-maven-extension
[ccud-sbt-plugin]: https://github.com/gradle/common-custom-user-data-sbt-plugin
[develocity-build-config-samples]: https://github.com/gradle/develocity-build-config-samples
[develocity-build-validation-scripts]: https://github.com/gradle/develocity-build-validation-scripts
[develocity-oss-projects]: https://github.com/gradle/develocity-oss-projects
[quarkus-build-caching-extension]: https://github.com/gradle/quarkus-build-caching-extension
[develocity]: https://develocity.ai
[claude-code]: https://claude.com/claude-code
[apache-license]: https://www.apache.org/licenses/LICENSE-2.0.html
