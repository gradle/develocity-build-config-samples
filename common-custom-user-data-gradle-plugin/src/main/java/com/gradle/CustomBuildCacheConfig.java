package com.gradle;


import org.gradle.caching.configuration.BuildCacheConfiguration;

final class CustomBuildCacheConfig {

    static void configureBuildCache(BuildCacheConfiguration buildCache) {
        // custom config goes here
    }

    private CustomBuildCacheConfig() {
    }

}
