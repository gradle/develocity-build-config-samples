package com.gradle;

import com.gradle.maven.extension.api.cache.BuildCacheApi;

final class CustomBuildCacheConfig {

    static void configureBuildCache(BuildCacheApi buildCache) {
        // custom config goes here
    }

    private CustomBuildCacheConfig() {
    }

}
