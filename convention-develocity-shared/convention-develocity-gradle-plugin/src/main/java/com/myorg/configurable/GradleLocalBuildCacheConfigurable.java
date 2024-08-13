package com.myorg.configurable;

import org.gradle.caching.local.DirectoryBuildCache;

import java.io.File;

final class GradleLocalBuildCacheConfigurable implements LocalBuildCacheConfigurable {

    private final DirectoryBuildCache localBuildCache;

    public GradleLocalBuildCacheConfigurable(DirectoryBuildCache localBuildCache) {
        this.localBuildCache = localBuildCache;
    }

    @Override
    public void setEnabled(boolean enabled) {
        localBuildCache.setEnabled(enabled);
    }

    @Override
    public void setStoreEnabled(boolean storeEnabled) {
        localBuildCache.setPush(storeEnabled);
    }

    @Override
    public void setDirectory(File directory) {
        localBuildCache.setDirectory(directory);
    }

}
