# Build Data Capturing Gradle Samples

## Overview

This directory contains samples demonstrating various ways to extend and customize a Build Scan with extra data using 
tags, links, and custom values.

To learn more, see the Gradle Enterprise documentation
on [Extending build scans](https://docs.gradle.com/enterprise/gradle-plugin/#extending_build_scans). 

### Capture Dependency Resolution

[//]: # (todo figure out what this sample is doing)

### Capture GE Plugin Version

_Demonstrates: Custom values_

This sample demonstrates how to capture the Gradle Enterprise Gradle plugin version as a custom value. This can be used
to identify and filter for projects using a certain version of the plugin within the Build Scan dashboard.

### Capture Git Diffs

_Demonstrates: Custom links_

This sample creates a private GitHub gist containing the difference between the current working directory and the index.
The URL of the gist is included as a custom link named `Git diff` at the top of the Build Scan.

### Capture OS Processes

_Demonstrates: Custom values_

This sample captures the output of the `ps` command and adds it to the Build Scan as a custom value named
`OS processes`.

### Capture Processor Arch

_Demonstrates: Custom values, Custom tags_

This sample adds the operating system name and processor architecture as custom values and adds a tag to the Build Scan
if the build was executed on an M1 processor.

### Capture Quality Check Issues

_Demonstrates: Custom values_

This sample parses the XML reports of several common quality check plugins and includes the findings as custom values on
the corresponding Build Scan.

### Capture Slow Work-unit Executions

_Demonstrates: Custom values_

This sample adds a custom value to the Build Scan for each task that has an execution time greater than a specified 
threshold.

### Capture Test Execution System Properties

_Demonstrates: Custom values_

This sample adds the system properties of test tasks to the Build Scan. In order to not leak sensitive information, the
value of the system property is hashed. This can be useful in Build Scan comparison as it helps identify any system 
properties which differed between two builds.

### Capture Test PTS Support

_Demonstrates: Custom values_

This sample adds a custom value
showing [Test Distribution](https://gradle.com/gradle-enterprise-solutions/test-distribution/)
and [Predictive Test Selection](https://gradle.com/gradle-enterprise-solutions/predictive-test-selection/) compatibility
for each test task.

### Capture Thermal Throttling

_Demonstrates: Custom values, Custom tags_

This sample adds a custom value for macOS builds that captures the average amount of thermal throttling that was 
applied during the build, a technique where the CPU is throttled in an effort to reduce heat. It then applies a tag to 
the Build Scan that maps the percentage to a level of thermal throttling.

## Usage

Each sample has been written as
a [script plugin](https://docs.gradle.org/current/userguide/plugins.html#sec:script_plugins). This means any sample can
be downloaded and used directly within a build.

```groovy
// build.gradle
apply from: 'build-data-capturing-sample.gradle'
```

```kotlin
// build.gradle.kts
apply(from = "build-data-capturing-sample.gradle.kts")
```
