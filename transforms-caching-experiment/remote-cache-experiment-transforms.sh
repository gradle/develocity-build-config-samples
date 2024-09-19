#!/bin/bash

# REQUIRED: Having the key set in the environment is expected - note that remote build cache write is requiered. It can be set here or exported in the terminal before running the script.
#export DEVELOCITY_ACCESS_KEY=$develocityUrl=<access-key>

gradleVersion=$(./gradlew --version | grep "Gradle" | awk '{print $2}')

major=$(echo "$gradleVersion" | cut -d. -f1)
minor=$(echo "$gradleVersion" | cut -d. -f2)

# Check if the version is lower than 8.9
if [ "$major" -lt 8 ] || { [ "$major" -eq 8 ] && [ "$minor" -lt 9 ]; }; then
  echo "Gradle version $gradleVersion is lower than 8.9"
  exit 1
else
    echo "Gradle version detected: $gradleVersion"
fi

homeDir=build/HOME

# Set 'task' to the first argument or 'help' if no arguments are provided
tasks=${*:-help}

# Initialize empty Gradle User Home with settings to run build
echo "Initializing Gradle User Home directory at $homeDir"
rm -rf $homeDir
mkdir -p $homeDir
mkdir -p $homeDir/caches/"$gradleVersion"/
cp ~/.gradle/gradle.properties $homeDir
cp -r ~/.gradle/caches/"$gradleVersion"/generated-gradle-jars $homeDir/caches/"$gradleVersion"/
cp -r ~/.gradle/develocity/ $homeDir/develocity/
cp -r ~/.gradle/enterprise/ $homeDir/enterprise/

# Note: This is expecting that CCUD Gradle plugin is applied
export GRADLE_CACHE_REMOTE_PUSH=true
export GRADLE_CACHE_REMOTE_PATH="cache/$USER-exp-non-task"
#export GRADLE_CACHE_REMOTE_URL="<develocityUrl>/cache/$USER-exp-non-task"  # Needed if the HTTP cache connector is used

echo "------------------------------------------------------------"
echo "Priming build with task '$tasks' and HOME=$homeDir"
echo "------------------------------------------------------------"
set -x
# shellcheck disable=SC2086
./gradlew $tasks -g $homeDir -Dscan.tag.remote-cache-experiment-init --no-configuration-cache -Ddevelocity.deprecation.muteWarnings=true -Dscan.uploadInBackground=false -Dgradle.cache.local.enabled=false --no-daemon
set +x

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
        echo "Test caches/*/transforms removal with $args"
        echo "------------------------------------------------------------"
        set -x

        echo "Removing transforms from $homeDir/caches"
        rm -rf $homeDir/caches/*/transforms
        rm -rf $homeDir/caches/transforms-* # Also remove the transforms for Gradle 8.7

        # shellcheck disable=SC2086
        ./gradlew $tasks -g $homeDir --no-configuration-cache -Ddevelocity.deprecation.muteWarnings=true -Dscan.uploadInBackground=false -Dgradle.cache.local.enabled=false --no-daemon $args

        set +x
        echo ""
    done
done