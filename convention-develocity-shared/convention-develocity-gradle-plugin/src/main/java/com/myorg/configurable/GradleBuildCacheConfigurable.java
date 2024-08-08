package com.myorg.configurable;

import com.gradle.develocity.agent.gradle.buildcache.DevelocityBuildCache;
import org.gradle.caching.configuration.BuildCacheConfiguration;

public final class GradleBuildCacheConfigurable implements BuildCacheConfigurable {

    private final Class<? extends DevelocityBuildCache> develocityBuildCache;
    private final BuildCacheConfiguration buildCache;

    public GradleBuildCacheConfigurable(Class<? extends DevelocityBuildCache> develocityBuildCache, BuildCacheConfiguration buildCache) {
        this.develocityBuildCache = develocityBuildCache;
        this.buildCache = buildCache;
    }

    @Override
    public LocalBuildCacheConfigurable getLocal() {
        return new GradleLocalBuildCacheConfigurable(buildCache.getLocal());
    }

    @Override
    public RemoteBuildCacheConfigurable getRemote() {
        return new GradleRemoteBuildCacheConfigurable(buildCache.remote(develocityBuildCache));
    }

}
