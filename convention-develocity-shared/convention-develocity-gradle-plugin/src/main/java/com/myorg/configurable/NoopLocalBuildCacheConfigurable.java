package com.myorg.configurable;

import java.io.File;

/*
 * Build cache cannot be configured via a plugin prior to Gradle 6.0, so build
 * cache operations must be a no-op.
 */
final class NoopLocalBuildCacheConfigurable implements LocalBuildCacheConfigurable {

    @Override
    public void setEnabled(boolean enabled) {

    }

    @Override
    public void setStoreEnabled(boolean storeEnabled) {

    }

    @Override
    public void setDirectory(File directory) {

    }

}
