package com.gradle;

import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.gradle.caching.http.HttpBuildCache;

import java.time.Duration;

import static com.gradle.Utils.withBooleanSysProperty;
import static com.gradle.Utils.withSysProperty;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the plugin, these settings will automatically be applied.
 */
final class CustomGradleEnterpriseConfig {

    // system properties to override Gradle Enterprise, Build Cache, and Build Scan configuration
    public static final String GRADLE_ENTERPRISE_URL = "gradle.enterprise.url";
    public static final String LOCAL_CACHE_ENABLED = "gradle.cache.local.enabled";
    public static final String LOCAL_CACHE_DIRECTORY = "gradle.cache.local.directory";
    public static final String LOCAL_CACHE_CLEANUP_ENABLED = "gradle.cache.local.cleanup.enabled";
    public static final String LOCAL_CACHE_CLEANUP_RETENTION = "gradle.cache.local.cleanup.retention";
    public static final String REMOTE_CACHE_URL = "gradle.cache.remote.url";
    public static final String REMOTE_CACHE_ALLOW_UNTRUSTED_SERVER = "gradle.cache.remote.allowUntrustedServer";
    public static final String REMOTE_CACHE_ENABLED = "gradle.cache.remote.enabled";
    public static final String REMOTE_CACHE_PUSH_ENABLED = "gradle.cache.remote.push";

    static void configureGradleEnterprise(GradleEnterpriseExtension gradleEnterprise, ProviderFactory providers) {
        /* Example of Gradle Enterprise configuration

        gradleEnterprise.setServer("https://your-gradle-enterprise-server.com");

        */

        withSysProperty(GRADLE_ENTERPRISE_URL, gradleEnterprise::setServer, providers);
    }

    static void configureBuildScanPublishing(BuildScanExtension buildScan, ProviderFactory providers) {
        /* Example of build scan publishing configuration

        boolean isCiServer = System.getenv().containsKey("CI");

        buildScan.publishAlways();
        buildScan.setCaptureTaskInputFiles(true);
        buildScan.setUploadInBackground(!isCiServer);

        */
    }

    static void configureBuildCache(BuildCacheConfiguration buildCache, ProviderFactory providers) {
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
            remote.setUrl("https://your-gradle-enterprise-server.com/cache/");
            remote.setEnabled(true);
            remote.setPush(isCiServer);
        });

        */

        buildCache.local(local -> {
            withBooleanSysProperty(LOCAL_CACHE_ENABLED, local::setEnabled, providers);
            withSysProperty(LOCAL_CACHE_DIRECTORY, local::setDirectory, providers);
            withSysProperty(LOCAL_CACHE_CLEANUP_RETENTION, value -> {
                Duration retention = Duration.parse(value);
                local.setRemoveUnusedEntriesAfterDays((int) retention.toDays());
            }, providers);
            withBooleanSysProperty(LOCAL_CACHE_CLEANUP_ENABLED, localCacheCleanupEnabled -> {
                if (!localCacheCleanupEnabled) {
                    local.setRemoveUnusedEntriesAfterDays(Integer.MAX_VALUE);
                }
            }, providers);
        });

        buildCache.remote(HttpBuildCache.class, remote -> {
            withSysProperty(REMOTE_CACHE_URL, remote::setUrl, providers);
            withBooleanSysProperty(REMOTE_CACHE_ALLOW_UNTRUSTED_SERVER, remote::setAllowUntrustedServer, providers);
            withBooleanSysProperty(REMOTE_CACHE_ENABLED, remote::setEnabled, providers);
            withBooleanSysProperty(REMOTE_CACHE_PUSH_ENABLED, remote::setPush, providers);
        });
    }

    private CustomGradleEnterpriseConfig() {
    }

}
