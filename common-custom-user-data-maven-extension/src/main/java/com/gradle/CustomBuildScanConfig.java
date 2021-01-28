package com.gradle;

import com.gradle.maven.extension.api.scan.BuildScanApi;

/**
 * Provide standardized configuration for publishing build scans.
 * By applying the plugin, this configuration will automatically be applied.
 */
final class CustomBuildScanConfig {

    static void configureBuildScanPublishing(BuildScanApi buildScans) {
        /* Example of build scan configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        buildScans.publishAlways();
        buildScans.setCaptureGoalInputFiles(true);
        buildScans.setUploadInBackground(!isCiServer);

        */
    }

    private CustomBuildScanConfig() {
    }

}
