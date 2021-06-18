package com.gradle;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import com.gradle.maven.extension.api.cache.BuildCacheApi;
import com.gradle.maven.extension.api.scan.BuildScanApi;

import java.net.URI;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the extension, these settings will automatically be applied.
 */
final class CustomGradleEnterpriseConfig {

    void configureGradleEnterprise(GradleEnterpriseApi gradleEnterprise) {
        /* Example of Gradle Enterprise configuration

        gradleEnterprise.setServer("https://enterprise-samples.gradle.com");
        gradleEnterprise.setAllowUntrustedServer(false);

        */
    }

    void configureBuildScanPublishing(BuildScanApi buildScans) {
        /* Example of build scan publishing configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        buildScans.publishAlways();
        buildScans.setCaptureGoalInputFiles(true);
        buildScans.setUploadInBackground(!isCiServer);

        */
    }

    void configureBuildCache(BuildCacheApi buildCache) {
        /* Example of build cache configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        // Enable the local build cache for all local and CI builds
        // For short-lived CI agents, it makes sense to disable the local build cache
        buildCache.getLocal().setEnabled(true);

        // Only permit store operations to the remote build cache for CI builds
        // Local builds will only read from the remote build cache
        buildCache.getRemote().getServer().setUrl(URI.create("https://enterprise-samples.gradle.com/cache/"));
        buildCache.getRemote().setEnabled(true);
        buildCache.getRemote().setStoreEnabled(isCiServer);

        */
    }

}
