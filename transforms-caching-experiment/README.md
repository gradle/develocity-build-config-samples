# Remote cache experiments for transforms

## Overview 

This script is used to test the performance of remote caching for transforms.  

It will run 3 builds:

1. Init/seed build, that writes to remote build cache. The scan for this buld will have a tag of `remote-cache-experiment-init`. 
2. Transform caching enabled - pulls from remote cache. The scan for this buld will have a tag of `baseline-transforms`.
3. Transform caching disabled - executes the transforms. The scan for this buld will have a tag of `disabled-cache-transforms`.

You can then compare the build times to see how transforms caching affects your builds.

## Usage

1. Copy the script into your project directory.
2. Modify the script to specify your Develocity instance URL in the `develocityUrl` variable and set the gradle version used in the `gradleVersion` variable - minimum gradle version supported is 8.9.
3. Having the key set in the environment is expected - note that remote build cache write is requiered. You can set the key in the environment by running `export DEVELOCITY_ACCESS_KEY=<develocity-url>=<your-access-key>`.
4. Run the script with `./transforms-caching-experiment.sh`. It will run the gradle `help` task by default, but you can specify a different task by passing it as an argument to the script. 
5. Inspect and compare build times 

## Advanced usage - disabling specific transforms caching

If you are sure you are only suffering negative avoidance savings because of a select list of transforms retreiving outputs from cache you can disable only caching for those. To disable specific transforms caching, you can uncomment the line 48 in the script to enable running also experiments with specific transforms caching disabled. You will also need to modify the `disabledTransforms` variable in the script to specify the transforms you want to disable - there is an example supplied in the script. This will run the last two builds again, but with the specified transforms caching disabled - the tags added in scans for those build will be `baseline-transforms-selected` and `disabled-cache-transforms-selected`.

