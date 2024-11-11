# Artifact transform remote build cache performance experiment

## Overview 

This script is used to test the performance of remote caching for transforms.  

It will run 3 builds:

1. Init/seed build, that writes to remote build cache. The scan for this build will have a tag of `remote-cache-experiment-init`. 
2. Transform caching enabled - pulls from remote cache. The scan for this build will have a tag of `baseline-transforms`.
3. Transform caching disabled - executes the transforms. The scan for this build will have a tag of `disabled-cache-transforms`.

You can then use the published build scans to evaluate how caching artifact transforms affects your overall build time. 

If results show that (remote) caching of transforms is not beneficial for your project, you can disable caching for (specific) transforms using the flag `-Dorg.gradle.internal.transform-caching-disabled`, which is also used in this script.

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
5. Run the script with `./transforms-caching-experiment.sh`. It will run the Gradle `help` task by default, but you can specify a different task by passing it as an argument to the script. 
6. Inspect and compare build times 

### Invocation

To run the script with the `help` task (default), use the following command:
```bash
./transforms-caching-experiment.sh
```

To run the script with a specific Gradle task(s), use the following command:
```bash
./transforms-caching-experiment.sh <gradle-tasks-and-args>
```

## Advanced usage - disabling specific transforms caching

If you are sure you are only suffering negative avoidance savings because of a select list of transforms retreiving outputs from cache you can disable only caching for those. To disable specific transforms caching, you can uncomment the line 48 in the script to enable running also experiments with specific transforms caching disabled. You will also need to modify the `disabledTransforms` variable in the script to specify the transforms you want to disable - there is an example supplied in the script. This will run the last two builds again, but with the specified transforms caching disabled - the tags added in scans for those build will be `baseline-transforms-selected` and `disabled-cache-transforms-selected`. You can use https://github.com/cdsap/ArtifactTransformReport to get statistics on Artifact transforms for a single build or aggregating multiple builds. 

