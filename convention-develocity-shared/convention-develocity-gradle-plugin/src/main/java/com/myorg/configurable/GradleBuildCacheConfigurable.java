package com.myorg.configurable;

import com.gradle.develocity.agent.gradle.buildcache.DevelocityBuildCache;
import org.gradle.caching.configuration.BuildCacheConfiguration;

public final class GradleBuildCacheConfigurable implements BuildCacheConfigurable {

    private final LocalBuildCacheConfigurable localBuildCache;
    private final RemoteBuildCacheConfigurable remoteBuildCache;

    public GradleBuildCacheConfigurable(Class<? extends DevelocityBuildCache> develocityBuildCache, BuildCacheConfiguration buildCache) {
        this.localBuildCache = new GradleLocalBuildCacheConfigurable(buildCache.getLocal());
        this.remoteBuildCache = new GradleRemoteBuildCacheConfigurable(buildCache.remote(develocityBuildCache));
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
