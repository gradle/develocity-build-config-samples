package com.gradle;

import com.gradle.maven.extension.api.cache.BuildCacheApi;

/**
 * Provide standardized build cache configuration.
 * By applying the extension, these build cache settings will automatically be applied.
 */
final class CustomBuildCacheConfig {

    static void configureBuildCache(BuildCacheApi buildCache) {
        /* Example of build cache configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        // For short-lived CI agents, it makes sense to disable the local build cache.
        buildCache.getLocal().setEnabled(!isCiServer);

        // Only permit cache store operations for CI builds. Local builds will only read from the remote cache.
        buildCache.getRemote().setEnabled(true);
        buildCache.getRemote().setStoreEnabled(isCiServer);

        */
    }

    private CustomBuildCacheConfig() {
    }

}
