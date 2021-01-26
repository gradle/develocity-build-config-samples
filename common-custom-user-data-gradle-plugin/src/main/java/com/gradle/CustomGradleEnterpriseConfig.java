package com.gradle;

import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the plugin, this Gradle Enterprise configuration will automatically be applied.
 */
final class CustomGradleEnterpriseConfig {

    static void configureGradleEnterprise(GradleEnterpriseExtension gradleEnterprise) {
        /* Example of Gradle Enterprise configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        gradleEnterprise.setServer("https://your-gradle-enterprise-server.com");
        gradleEnterprise.setAllowUntrustedServer(true);

        gradleEnterprise.buildScan(buildScan -> {
            buildScan.publishAlways();
            buildScan.setCaptureTaskInputFiles(true);
            buildScan.setUploadInBackground(!isCiServer);
        });

       */
    }

    private CustomGradleEnterpriseConfig() {
    }

}
