package com.gradle;

import com.gradle.maven.extension.api.scan.BuildScanApi;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the plugin, this Gradle Enterprise configuration will automatically be applied.
 */
final class CustomGradleEnterpriseConfig {

    static void configureGradleEnterprise(BuildScanApi buildScans) {
        /* Example of Gradle Enterprise configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        buildScans.publishAlways();
        buildScans.setCaptureGoalInputFiles(true);
        buildScans.setUploadInBackground(!isCiServer);

        */
    }

    private CustomGradleEnterpriseConfig() {
    }

}
