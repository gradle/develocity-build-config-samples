package com.myorg.configurable;

import com.gradle.develocity.agent.maven.api.cache.BuildCacheApi;

public final class MavenBuildCacheConfigurable implements BuildCacheConfigurable {

    private final LocalBuildCacheConfigurable localBuildCache;
    private final RemoteBuildCacheConfigurable remoteBuildCache;

    public MavenBuildCacheConfigurable(BuildCacheApi buildCache) {
        this.localBuildCache = new MavenLocalBuildCacheConfigurable(buildCache.getLocal());
        this.remoteBuildCache = new MavenRemoteBuildCacheConfigurable(buildCache.getRemote());
    }

    @Override
    public LocalBuildCacheConfigurable getLocal() {
        return localBuildCache;
    }

    @Override
    public RemoteBuildCacheConfigurable getRemote() {
        return remoteBuildCache;
    }

}
