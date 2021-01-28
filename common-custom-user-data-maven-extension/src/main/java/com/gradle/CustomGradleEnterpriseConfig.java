package com.gradle;

import com.gradle.maven.extension.api.cache.BuildCacheApi;
import com.gradle.maven.extension.api.scan.BuildScanApi;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the extension, these settings will automatically be applied.
 */
final class CustomGradleEnterpriseConfig {

    static void configureBuildScanPublishing(BuildScanApi buildScans) {
        /* Example of build scan configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        buildScans.publishAlways();
        buildScans.setCaptureGoalInputFiles(true);
        buildScans.setUploadInBackground(!isCiServer);

        */
    }

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

    private CustomGradleEnterpriseConfig() {
    }

}
