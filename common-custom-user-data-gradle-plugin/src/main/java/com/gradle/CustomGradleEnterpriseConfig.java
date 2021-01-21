package com.gradle;

import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the plugin, this Gradle Enterprise configuration will automatically be applied.
 */
final class CustomGradleEnterpriseConfig {

    static void configureGradleEnterprise(GradleEnterpriseExtension gradleEnterprise) {
        /* Example of Gradle Enterprise configuration

        gradleEnterprise.setServer("https://localhost");
        gradleEnterprise.setAllowUntrustedServer(true);

        gradleEnterprise.buildScan(buildScan -> {
            buildScan.publishAlways();
            buildScan.setCaptureTaskInputFiles(true);
            buildScan.setUploadInBackground(false);
        });

       */
    }

    private CustomGradleEnterpriseConfig() {
    }
}
