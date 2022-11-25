# Build Data Capturing Maven Samples

## Overview

This directory contains samples demonstrating various ways to extend and customize a Build Scan with extra data using 
tags, links, and custom values.

To learn more, see the Gradle Enterprise documentation
on [Extending build scans](https://docs.gradle.com/enterprise/maven-extension/#extending_build_scans).

### Capture GE Extension Version

_Demonstrates: Custom values_

This sample demonstrates how to capture the Gradle Enterprise Maven extension version as a custom value. This can be 
used to identify projects using a certain version of the extension within the Build Scan dashboard.

### Capture OS Processes

_Demonstrates: Custom values_

This sample captures the output of the `ps` command and adds it to the Build Scan as a custom value named
`OS processes`.

### Capture Processor Arch

_Demonstrates: Custom values, Custom tags_

This sample adds the operating system name and processor architecture as custom values and adds a tag to the Build Scan
if the build was executed on an M1 processor.

### Capture Profiles

_Demonstrates: Custom tags_

This sample captures each active Maven profile and adds it as a tag on the Build Scan. 

### Capture Quality Check Issues

_Demonstrates: Custom values_

This sample parses the XML reports of several common quality check plugins and includes the findings as custom values on
the corresponding Build Scan.

### Capture Thermal Throttling

_Demonstrates: Custom values, Custom tags_

This sample adds a custom value for macOS builds that captures the average amount of thermal throttling that was 
applied during the build, a technique where the CPU is throttled in an effort to reduce heat. It then applies a tag to 
the Build Scan that maps the percentage to a "level of thermal throttling".

### Capture Top Level Project

_Demonstrates: Custom values_

This sample captures the top-level project name and artifact ID and adds these as custom values on the Build Scan.

## Usage

[//]: # (todo not sure how these are intended to be applied)
