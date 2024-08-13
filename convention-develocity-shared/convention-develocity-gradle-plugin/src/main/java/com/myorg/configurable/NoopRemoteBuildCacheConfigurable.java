package com.myorg.configurable;

/*
 * Build cache cannot be configured via a plugin prior to Gradle 6.0, so build
 * cache operations must be a no-op.
 */
final class NoopRemoteBuildCacheConfigurable implements RemoteBuildCacheConfigurable {

    @Override
    public void setEnabled(boolean enabled) {

    }

    @Override
    public void setStoreEnabled(boolean storeEnabled) {

    }

}
