# Build Data Capturing Maven Samples

## Usage

Each sample has been written to be fully compatible with
the [Common Custom User Data Maven extension](https://github.com/gradle/common-custom-user-data-maven-extension). This
means any sample can be downloaded and included in a Maven build that uses the extension. See
the [Capturing additional tags, links, and values in your build scans](https://github.com/gradle/common-custom-user-data-maven-extension#capturing-additional-tags-links-and-values-in-your-build-scans)
section of the extension documentation for more information.

## Overview

This directory contains samples demonstrating various ways to extend and customize a Build Scan with extra data using 
tags, links, and custom values.

To learn more, see the Develocity documentation
on [Extending build scans](https://docs.gradle.com/develocity/maven-extension/current/#extending_build_scans).

### Capture Develocity Extension Version

_Demonstrates: Custom values_

This sample demonstrates how to capture the Develocity Maven extension version as a custom value. This can be 
used to identify and filter for projects using a certain version of the extension on the Develocity dashboards.

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
