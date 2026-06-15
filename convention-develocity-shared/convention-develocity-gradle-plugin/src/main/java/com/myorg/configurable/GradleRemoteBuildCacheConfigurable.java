package com.myorg.configurable;

import org.gradle.caching.configuration.BuildCache;

final class GradleRemoteBuildCacheConfigurable implements RemoteBuildCacheConfigurable {

    private final BuildCache remoteBuildCache;

    public GradleRemoteBuildCacheConfigurable(BuildCache remoteBuildCache) {
        this.remoteBuildCache = remoteBuildCache;
    }

    @Override
    public void setEnabled(boolean enabled) {
        remoteBuildCache.setEnabled(enabled);
    }

    @Override
    public void setStoreEnabled(boolean storeEnabled) {
        remoteBuildCache.setPush(storeEnabled);
    }

}
