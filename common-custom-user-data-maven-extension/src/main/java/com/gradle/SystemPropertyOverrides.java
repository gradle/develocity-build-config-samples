package com.gradle;

import com.gradle.maven.extension.api.cache.BuildCacheApi;
import com.gradle.maven.extension.api.cache.Server;
import com.gradle.maven.extension.api.scan.BuildScanApi;

import static com.gradle.Utils.sysProperty;

/**
 * Provide standardized Gradle Enterprise configuration.
 * By applying the extension, these settings will automatically be applied.
 */
final class SystemPropertyOverrides {

    public static final String REMOTE_CACHE_SHARD = "gradle.cache.remote.shard";

    static void configureBuildCache(BuildCacheApi buildCache) {
        Server remoteBuildCacheServer = buildCache.getRemote().getServer();
        sysProperty(REMOTE_CACHE_SHARD).ifPresent(v -> remoteBuildCacheServer.setUrl(Utils.appendPath(remoteBuildCacheServer.getUrl(), v)));
    }

    private SystemPropertyOverrides() {
    }

}
