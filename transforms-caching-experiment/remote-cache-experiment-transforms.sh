#!/bin/bash

# Note:
# This is a script to run the transforms remote caching experiments. You need to update it to use your server url, gradle version (8.9+) and set up the access key with remote cache write permission (you can also export it in the terminal if you wish to share the script with teammates). When executing, you can also supply a different task to run this with, it defaults to help. It will run 3 builds:
# 1. Init/seed build, that writes to remote build cache
# 2. Transform caching enabled - pulls from remote cache
# 3. Transform caching disabled - executes the transforms
# You can then compare the build times to see how transforms caching affects your builds

# --------- Required -----------

develocityUrl="https://<your-server-url>"
gradleVersion="8.9" # at least 8.9 is required

# Having the key set in the environment is expected - note that remote build cache write is requiered
#export DEVELOCITY_ACCESS_KEY=$develocityUrl=<access-key>

# --------- End of required -----------


homeDir=build/HOME

# Set 'task' to the first argument or 'help' if no arguments are provided
task=${1:-help}

# Initialize empty Gradle User Home with settings to run build, including Develocity access keys
echo "Initializing Gradle User Home directory at $homeDir"
rm -rf $homeDir
mkdir -p $homeDir
mkdir -p $homeDir/caches/$gradleVersion/
cp ~/.gradle/gradle.properties $homeDir
cp -r ~/.gradle/caches/$gradleVersion/generated-gradle-jars $homeDir/caches/$gradleVersion/

# Note: This is expecting that CCUD gradle plugin is applied
export GRADLE_CACHE_REMOTE_PUSH=true
export GRADLE_CACHE_REMOTE_PATH="cache/$USER-exp-non-task"
export GRADLE_CACHE_REMOTE_URL="$develocityUrl/cache/$USER-exp-non-task"  # Needed if the HTTP cache connector is used

echo "------------------------------------------------------------"
echo "Priming build with task '$task' and HOME=$homeDir"
echo "------------------------------------------------------------"
set -x
./gradlew $task -g $homeDir -Dscan.tag.remote-cache-experiment-init --no-configuration-cache -Ddevelocity.deprecation.muteWarnings=true -Dscan.uploadInBackground=false -Ddevelocity.url=$develocityUrl
set +x

cache='transforms'
runs='transforms'
# runs='transforms transforms-selected' # Uncomment to test with selected transforms disabled

for run in $runs
do
    # Set args based on cache
    if [ "$run" == 'transforms' ]
    then
        disabledCacheArgs='-Dorg.gradle.internal.transform-caching-disabled'
    elif [ "$run" == 'transforms-selected' ]
    then
        # Specify the transforms to disable. Example below:
        disabledTransforms='org.jetbrains.kotlin.gradle.internal.transforms.BuildToolsApiClasspathEntrySnapshotTransform,org.jetbrains.kotlin.gradle.internal.transforms.ClasspathEntrySnapshotTransform'
        disabledCacheArgs="-Dorg.gradle.internal.transform-caching-disabled=$disabledTransforms"
    fi

    for args in "-Dscan.tag.baseline-$run" "-Dscan.tag.disabled-cache-$run $disabledCacheArgs"
    do
        echo "------------------------------------------------------------"
        echo "Test caches/*/$cache removal with $args"
        echo "------------------------------------------------------------"
        set -x
        ./gradlew --stop
        killall -9 java

        # git clean -dfx -e HOME -e cleanup-help.sh
        echo "Removing $cache from $homeDir/caches"
        rm -rf $homeDir/caches/*/$cache
        rm -rf $homeDir/caches/$cache-* # Also remove the transforms for Gradle 8.7

        # Always remove the local build cache, since we are testing connection with remote build cache
        rm -rf $homeDir/caches/build-cache-1

        ./gradlew $task -g $homeDir --no-configuration-cache -Ddevelocity.deprecation.muteWarnings=true -Dscan.uploadInBackground=false -Ddevelocity.url=$develocityUrl $args

        set +x
        echo ""
    done
done