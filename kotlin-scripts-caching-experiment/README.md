# Kotlin script compilation remote build cache performance experiment

## Overview

This script is used to test the performance of remote caching for Kotlin scripts.

It will run 3 builds:

1. Init/seed build, that writes to remote build cache. The scan for this build will have a tag of `remote-cache-experiment-init`.
2. Kotlin script caching enabled - pulls from remote cache. The scan for this build will have a tag of `kotlin-script-caching-enabled`.
3. Kotlin script caching disabled - compiles the Kotlin scripts. The scan for this build will have a tag of `kotlin-script-caching-disabled`.

You can then use the published build scans to evaluate how caching Kotlin script compilation affects your overall build time.

If results show that (remote) caching of Kotlin script compilation is not beneficial for your project, you can disable caching for Kotlin script compilation using the flag `-Dorg.gradle.internal.kotlin-script-caching-disabled`, which is also used in this script.

## Requirements

- A Develocity instance to publish scans to
- Access key with remote build cache write permission
- Gradle version 8.9 or higher
- The [Common Custom User Data Gradle plugin](https://github.com/gradle/common-custom-user-data-gradle-plugin) is expected to be applied to the project

## Usage

1. Ensure you have an access key with remote build cache write permission.
2. Copy the script into your project directory.
3. (Optional) If you're using a HTTP cache connector, uncomment line 31 and set the remote cache URL in the script to use a cache shard instead of the default cache.
4. Having the key set in the environment is expected - note that **remote build cache write is required**. If the key stored in your Gradle user home is missing the remote build cache write permission you can set the key in the environment by running `export DEVELOCITY_ACCESS_KEY=<develocity-url>=<your-access-key>`, either in the script, or in the terminal before running the script.
5. Run the script with `./remote-cache-experiment-kotlin-scripts.sh`. It will run the Gradle `help` task by default, but you can specify a different task by passing it as an argument to the script.
6. Inspect and compare build times

### Invocation

To run the script with the `help` task (default), use the following command:
```bash
./remote-cache-experiment-kotlin-scripts.sh
```

To run the script with a specific Gradle task(s), use the following command:
```bash
./remote-cache-experiment-kotlin-scripts.sh <gradle-tasks-and-args>
```


