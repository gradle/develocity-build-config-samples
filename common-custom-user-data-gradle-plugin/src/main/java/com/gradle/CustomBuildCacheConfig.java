package com.gradle;

import org.gradle.caching.configuration.BuildCacheConfiguration;

/**
 * Provide standardized build cache configuration.
 * By applying the plugin, these build cache settings will automatically be applied.
 */
final class CustomBuildCacheConfig {

    static void configureBuildCache(BuildCacheConfiguration buildCache) {
        /* Example of build cache configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        // For short-lived CI agents, it makes sense to disable the local build cache
        buildCache.local(local -> {
            local.setEnabled(!isCiServer);
        });

        buildCache.remote(HttpBuildCache.class, remote -> {
            remote.setUrl("https://your-gradle-enterprise-server.com/cache/");
            remote.setAllowUntrustedServer(true);
            remote.setPush(isCiServer);
        });

        */
    }

    private CustomBuildCacheConfig() {
    }
    
}
