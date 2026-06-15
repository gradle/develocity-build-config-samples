package com.myorg.configurable;

import com.gradle.develocity.agent.maven.api.cache.RemoteBuildCache;

final class MavenRemoteBuildCacheConfigurable implements RemoteBuildCacheConfigurable {

    private final RemoteBuildCache remoteBuildCache;

    public MavenRemoteBuildCacheConfigurable(RemoteBuildCache remoteBuildCache) {
        this.remoteBuildCache = remoteBuildCache;
    }

    @Override
    public void setEnabled(boolean enabled) {
        remoteBuildCache.setEnabled(enabled);
    }

    @Override
    public void setStoreEnabled(boolean storeEnabled) {
        remoteBuildCache.setStoreEnabled(storeEnabled);
    }

}
