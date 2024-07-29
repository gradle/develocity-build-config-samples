package com.myorg.configurable;

import com.gradle.develocity.agent.maven.api.cache.LocalBuildCache;

import java.io.File;

final class MavenLocalBuildCacheConfigurable implements LocalBuildCacheConfigurable {

    private final LocalBuildCache localBuildCache;

    public MavenLocalBuildCacheConfigurable(LocalBuildCache localBuildCache) {
        this.localBuildCache = localBuildCache;
    }

    @Override
    public void setEnabled(boolean enabled) {
        localBuildCache.setEnabled(enabled);
    }

    @Override
    public void setStoreEnabled(boolean storeEnabled) {
        localBuildCache.setStoreEnabled(storeEnabled);
    }

    @Override
    public void setDirectory(File directory) {
        localBuildCache.setDirectory(directory);
    }

}
