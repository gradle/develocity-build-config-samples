package com.gradle;

import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.gradle.caching.http.HttpBuildCache;

import java.util.Optional;

import static com.gradle.Utils.appendPathAndTrailingSlash;
import static com.gradle.Utils.durationSysProperty;

/**
 * Provide standardized Gradle Enterprise configuration. By applying the plugin, these settings will automatically be applied.
 */
final class Overrides {

    // system properties to override Gradle Enterprise configuration
    static final String GRADLE_ENTERPRISE_URL = "gradle.enterprise.url";
    static final String GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER = "gradle.enterprise.allowUntrustedServer";

    // system properties to override local build cache configuration
    static final String LOCAL_CACHE_DIRECTORY = "gradle.cache.local.directory";
    static final String LOCAL_CACHE_REMOVE_UNUSED_ENTRIES_AFTER_DAYS = "gradle.cache.local.removeUnusedEntriesAfterDays";
    static final String LOCAL_CACHE_ENABLED = "gradle.cache.local.enabled";
    static final String LOCAL_CACHE_PUSH = "gradle.cache.local.push";

    // system properties to override remote build cache configuration
    static final String REMOTE_CACHE_SHARD = "gradle.cache.remote.shard";
    static final String REMOTE_CACHE_URL = "gradle.cache.remote.url";
    static final String REMOTE_CACHE_ALLOW_UNTRUSTED_SERVER = "gradle.cache.remote.allowUntrustedServer";
    static final String REMOTE_CACHE_ENABLED = "gradle.cache.remote.enabled";
    static final String REMOTE_CACHE_PUSH = "gradle.cache.remote.push";

    private final ProviderFactory providers;

    Overrides(ProviderFactory providers) {
        this.providers = providers;
    }

    void configureGradleEnterprise(GradleEnterpriseExtension gradleEnterprise) {
        sysPropertyOrEnvVariable(GRADLE_ENTERPRISE_URL, providers).ifPresent(gradleEnterprise::setServer);
        booleanSysPropertyOrEnvVariable(GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER, providers).ifPresent(gradleEnterprise::setAllowUntrustedServer);
    }

    void configureBuildCache(BuildCacheConfiguration buildCache) {
        buildCache.local(local -> {
            sysPropertyOrEnvVariable(LOCAL_CACHE_DIRECTORY, providers).ifPresent(local::setDirectory);
            durationSysProperty(LOCAL_CACHE_REMOVE_UNUSED_ENTRIES_AFTER_DAYS, providers).ifPresent(v -> local.setRemoveUnusedEntriesAfterDays((int) v.toDays()));
            booleanSysPropertyOrEnvVariable(LOCAL_CACHE_ENABLED, providers).ifPresent(local::setEnabled);
            booleanSysPropertyOrEnvVariable(LOCAL_CACHE_PUSH, providers).ifPresent(local::setPush);
        });

        // Only touch remote build cache configuration if it is already present and of type HttpBuildCache
        // Do nothing in case of another build cache type like AWS S3 being used
        if (buildCache.getRemote() instanceof HttpBuildCache) {
            buildCache.remote(HttpBuildCache.class, remote -> {
                sysPropertyOrEnvVariable(REMOTE_CACHE_SHARD, providers).ifPresent(shard -> remote.setUrl(appendPathAndTrailingSlash(remote.getUrl(), shard)));
                sysPropertyOrEnvVariable(REMOTE_CACHE_URL, providers).ifPresent(remote::setUrl);
                booleanSysPropertyOrEnvVariable(REMOTE_CACHE_ALLOW_UNTRUSTED_SERVER, providers).ifPresent(remote::setAllowUntrustedServer);
                booleanSysPropertyOrEnvVariable(REMOTE_CACHE_ENABLED, providers).ifPresent(remote::setEnabled);
                booleanSysPropertyOrEnvVariable(REMOTE_CACHE_PUSH, providers).ifPresent(remote::setPush);
            });
        }
    }

    static Optional<String> sysPropertyOrEnvVariable(String sysPropertyName, ProviderFactory providers) {
        return Utils.sysPropertyOrEnvVariable(sysPropertyName, toEnvVarName(sysPropertyName), providers);
    }

    static Optional<Boolean> booleanSysPropertyOrEnvVariable(String sysPropertyName, ProviderFactory providers) {
        return Utils.booleanSysPropertyOrEnvVariable(sysPropertyName, toEnvVarName(sysPropertyName), providers);
    }

    private static String toEnvVarName(String sysPropertyName) {
        return sysPropertyName.toUpperCase().replace('.', '_');
    }

}
