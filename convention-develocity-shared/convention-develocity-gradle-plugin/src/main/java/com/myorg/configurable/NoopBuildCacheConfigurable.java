package com.myorg.configurable;

/*
 * Build cache cannot be configured via a plugin prior to Gradle 6.0, so build
 * cache operations must be a no-op.
 */
final class NoopBuildCacheConfigurable implements BuildCacheConfigurable {

    @Override
    public LocalBuildCacheConfigurable getLocal() {
        return new NoopLocalBuildCacheConfigurable();
    }

    @Override
    public RemoteBuildCacheConfigurable getRemote() {
        return new NoopRemoteBuildCacheConfigurable();
    }

}
