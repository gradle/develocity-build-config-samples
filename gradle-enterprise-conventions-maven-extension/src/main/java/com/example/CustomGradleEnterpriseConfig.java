package com.example;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import com.gradle.maven.extension.api.cache.BuildCacheApi;
import com.gradle.maven.extension.api.cache.NormalizationProvider.RuntimeClasspathNormalization;
import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.apache.maven.execution.MavenSession;

import static com.example.CiUtils.isCi;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the extension, these settings will automatically be applied.
 */
final class CustomGradleEnterpriseConfig {

    void configureGradleEnterprise(GradleEnterpriseApi gradleEnterprise) {
        //todo: change to your own Gradle Enterprise instance
        gradleEnterprise.setServer("https://ge.example.com");
        gradleEnterprise.setAllowUntrustedServer(false);
    }

    void configureBuildScanPublishing(BuildScanApi buildScans) {
        buildScans.publishAlways();
        buildScans.setUploadInBackground(!isCi());
        buildScans.capture(capture -> capture.setGoalInputFiles(true));
    }

    void configureBuildCache(BuildCacheApi buildCache) {
        // Enable the local build cache for all local and CI builds
        buildCache.getLocal().setEnabled(true);

        // For short-lived CI agents, it makes sense to disable the local build cache:
        // buildCache.getLocal().setEnabled(!isCi());

        buildCache.getRemote().setEnabled(true);
        buildCache.getRemote().setStoreEnabled(isCi());

        buildCache.registerNormalizationProvider(context ->
            context.configureRuntimeClasspathNormalization(this::configureNormalizations)
        );
    }

    void configureNormalizations(RuntimeClasspathNormalization normalization) {
        // https://docs.gradle.com/enterprise/maven-extension/#normalization
        // For example, to normalize the "Implementation-Version" attribute of META-INF/MANIFEST.MF files:
        // normalization.configureMetaInf(metaInf ->
        //     metaInf.addIgnoredAttributes("Implementation-Version")
        // );
    }

    void extendBuildScan(GradleEnterpriseApi api, MavenSession session) {
        BuildScanApi buildScan = api.getBuildScan();

        // Use `buildScan` to create commonly needed tags, values, and links. For example:
        // buildScan.tag("Sample Tag");
        // buildScan.value("Sample Name", "Sample Value");
        // buildScan.link("Sample Link", "https://example.com");
    }
}
