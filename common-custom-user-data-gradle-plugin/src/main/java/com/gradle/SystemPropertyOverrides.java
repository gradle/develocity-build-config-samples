package com.gradle;

import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.gradle.caching.http.HttpBuildCache;

import static com.gradle.Utils.withBooleanSysProperty;
import static com.gradle.Utils.withDurationSysProperty;
import static com.gradle.Utils.withSysProperty;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the plugin, these settings will automatically be applied.
 */
final class SystemPropertyOverrides {

    // system properties to override Gradle Enterprise configuration
    public static final String GRADLE_ENTERPRISE_URL = "gradle.enterprise.url";

    // system properties to override local build cache configuration
    public static final String LOCAL_CACHE_DIRECTORY = "gradle.cache.local.directory";
    public static final String LOCAL_CACHE_REMOVE_UNUSED_ENTRIES_AFTER_DAYS = "gradle.cache.local.removeUnusedEntriesAfterDays";
    public static final String LOCAL_CACHE_ENABLED = "gradle.cache.local.enabled";
    public static final String LOCAL_CACHE_PUSH = "gradle.cache.local.push";

    // system properties to override remote build cache configuration
    public static final String REMOTE_CACHE_URL = "gradle.cache.remote.url";
    public static final String REMOTE_CACHE_ALLOW_UNTRUSTED_SERVER = "gradle.cache.remote.allowUntrustedServer";
    public static final String REMOTE_CACHE_ENABLED = "gradle.cache.remote.enabled";
    public static final String REMOTE_CACHE_PUSH = "gradle.cache.remote.push";

    static void configureGradleEnterprise(GradleEnterpriseExtension gradleEnterprise, ProviderFactory providers) {
        withSysProperty(GRADLE_ENTERPRISE_URL, gradleEnterprise::setServer, providers);
    }

    static void configureBuildCache(BuildCacheConfiguration buildCache, ProviderFactory providers) {
        buildCache.local(local -> {
            withSysProperty(LOCAL_CACHE_DIRECTORY, local::setDirectory, providers);
            withDurationSysProperty(LOCAL_CACHE_REMOVE_UNUSED_ENTRIES_AFTER_DAYS, v -> local.setRemoveUnusedEntriesAfterDays((int) v.toDays()), providers);
            withBooleanSysProperty(LOCAL_CACHE_ENABLED, local::setEnabled, providers);
            withBooleanSysProperty(LOCAL_CACHE_PUSH, local::setPush, providers);
        });

        // null check required to avoid creating a remote build cache instance when none was already present in the build
        if (buildCache.getRemote() != null) {
            buildCache.remote(HttpBuildCache.class, remote -> {
                withSysProperty(REMOTE_CACHE_URL, remote::setUrl, providers);
                withBooleanSysProperty(REMOTE_CACHE_ALLOW_UNTRUSTED_SERVER, remote::setAllowUntrustedServer, providers);
                withBooleanSysProperty(REMOTE_CACHE_ENABLED, remote::setEnabled, providers);
                withBooleanSysProperty(REMOTE_CACHE_PUSH, remote::setPush, providers);
            });
        }
    }

    private SystemPropertyOverrides() {
    }

}
