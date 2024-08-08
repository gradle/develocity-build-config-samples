package com.myorg.configurable;

import com.gradle.develocity.agent.maven.api.cache.BuildCacheApi;

public final class MavenBuildCacheConfigurable implements BuildCacheConfigurable {

    private final BuildCacheApi buildCache;

    public MavenBuildCacheConfigurable(BuildCacheApi buildCache) {
        this.buildCache = buildCache;
    }

    @Override
    public LocalBuildCacheConfigurable getLocal() {
        return new MavenLocalBuildCacheConfigurable(buildCache.getLocal());
    }

    @Override
    public RemoteBuildCacheConfigurable getRemote() {
        return new MavenRemoteBuildCacheConfigurable(buildCache.getRemote());
    }

}
