package com.gradle;

import com.gradle.maven.extension.api.cache.BuildCacheApi;
import com.gradle.maven.extension.api.cache.Server;

import static com.gradle.Utils.appendPathAndTrailingSlash;
import static com.gradle.Utils.sysProperty;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the extension, these settings will automatically be applied.
 */
final class SystemPropertyOverrides {

    public static final String REMOTE_CACHE_SHARD = "gradle.cache.remote.shard";

    static void configureBuildCache(BuildCacheApi buildCache) {
        Server server = buildCache.getRemote().getServer();
        sysProperty(REMOTE_CACHE_SHARD).ifPresent(shard -> server.setUrl(appendPathAndTrailingSlash(server.getUrl(), shard)));
    }

    private SystemPropertyOverrides() {
    }

}
