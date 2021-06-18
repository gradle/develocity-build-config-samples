package com.gradle;

import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.gradle.caching.http.HttpBuildCache;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the plugin, these settings will automatically be applied.
 */
final class CustomGradleEnterpriseConfig {

    void configureGradleEnterprise(GradleEnterpriseExtension gradleEnterprise) {
        /* Example of Gradle Enterprise configuration

        gradleEnterprise.setServer("https://enterprise-samples.gradle.com");
        gradleEnterprise.setAllowUntrustedServer(false);

        */
    }

    void configureBuildScanPublishing(BuildScanExtension buildScan) {
        /* Example of build scan publishing configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        buildScan.publishAlways();
        buildScan.setCaptureTaskInputFiles(true);
        buildScan.setUploadInBackground(!isCiServer);

        */
    }

    void configureBuildCache(BuildCacheConfiguration buildCache) {
        /* Example of build cache configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        // Enable the local build cache for all local and CI builds
        // For short-lived CI agents, it makes sense to disable the local build cache
        buildCache.local(local -> {
            local.setEnabled(true);
        });

        // Only permit store operations to the remote build cache for CI builds
        // Local builds will only read from the remote build cache
        buildCache.remote(HttpBuildCache.class, remote -> {
            remote.setUrl("https://enterprise-samples.gradle.com/cache/");
            remote.setEnabled(true);
            remote.setPush(isCiServer);
        });

        */
    }

}
