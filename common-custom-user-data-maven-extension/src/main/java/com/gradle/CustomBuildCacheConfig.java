package com.gradle;

import com.gradle.maven.extension.api.cache.BuildCacheApi;

/**
 * Provide standardized build cache configuration.
 * By applying the extension, these build cache settings will automatically be applied.
 */
final class CustomBuildCacheConfig {

    static void configureBuildCache(BuildCacheApi buildCache) {
        // custom config goes here
    }

    private CustomBuildCacheConfig() {
    }

}
