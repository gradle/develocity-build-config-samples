package com.gradle;

import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.gradle.caching.http.HttpBuildCache;

import static com.gradle.Utils.appendPathAndTrailingSlash;
import static com.gradle.Utils.booleanSysProperty;
import static com.gradle.Utils.durationSysProperty;
import static com.gradle.Utils.sysProperty;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the plugin, these settings will automatically be applied.
 */
final class SystemPropertyOverrides {

    // system properties to override Gradle Enterprise configuration
    public static final String GRADLE_ENTERPRISE_URL = "gradle.enterprise.url";
    public static final String GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER = "gradle.enterprise.allowUntrustedServer";

    // system properties to override local build cache configuration
    public static final String LOCAL_CACHE_DIRECTORY = "gradle.cache.local.directory";
    public static final String LOCAL_CACHE_REMOVE_UNUSED_ENTRIES_AFTER_DAYS = "gradle.cache.local.removeUnusedEntriesAfterDays";
    public static final String LOCAL_CACHE_ENABLED = "gradle.cache.local.enabled";
    public static final String LOCAL_CACHE_PUSH = "gradle.cache.local.push";

    // system properties to override remote build cache configuration
    public static final String REMOTE_CACHE_SHARD = "gradle.cache.remote.shard";
    public static final String REMOTE_CACHE_URL = "gradle.cache.remote.url";
    public static final String REMOTE_CACHE_ALLOW_UNTRUSTED_SERVER = "gradle.cache.remote.allowUntrustedServer";
    public static final String REMOTE_CACHE_ENABLED = "gradle.cache.remote.enabled";
    public static final String REMOTE_CACHE_PUSH = "gradle.cache.remote.push";

    private final ProviderFactory providers;

    SystemPropertyOverrides(ProviderFactory providers) {
        this.providers = providers;
    }

    void configureGradleEnterprise(GradleEnterpriseExtension gradleEnterprise) {
        sysProperty(GRADLE_ENTERPRISE_URL, providers).ifPresent(gradleEnterprise::setServer);
        booleanSysProperty(GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER, providers).ifPresent(gradleEnterprise::setAllowUntrustedServer);
    }

    void configureBuildCache(BuildCacheConfiguration buildCache) {
        buildCache.local(local -> {
            sysProperty(LOCAL_CACHE_DIRECTORY, providers).ifPresent(local::setDirectory);
            durationSysProperty(LOCAL_CACHE_REMOVE_UNUSED_ENTRIES_AFTER_DAYS, providers).ifPresent(v -> local.setRemoveUnusedEntriesAfterDays((int) v.toDays()));
            booleanSysProperty(LOCAL_CACHE_ENABLED, providers).ifPresent(local::setEnabled);
            booleanSysProperty(LOCAL_CACHE_PUSH, providers).ifPresent(local::setPush);
        });

        // Only touch remote build cache configuration if it is already present and of type HttpBuildCache
        // Do nothing in case of another build cache type like AWS S3 being used
        if (buildCache.getRemote() instanceof HttpBuildCache) {
            buildCache.remote(HttpBuildCache.class, remote -> {
                sysProperty(REMOTE_CACHE_SHARD, providers).ifPresent(shard -> remote.setUrl(appendPathAndTrailingSlash(remote.getUrl(), shard)));
                sysProperty(REMOTE_CACHE_URL, providers).ifPresent(remote::setUrl);
                booleanSysProperty(REMOTE_CACHE_ALLOW_UNTRUSTED_SERVER, providers).ifPresent(remote::setAllowUntrustedServer);
                booleanSysProperty(REMOTE_CACHE_ENABLED, providers).ifPresent(remote::setEnabled);
                booleanSysProperty(REMOTE_CACHE_PUSH, providers).ifPresent(remote::setPush);
            });
        }
    }

}
