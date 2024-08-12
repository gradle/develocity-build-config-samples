package com.myorg.configurable;

import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import org.gradle.caching.configuration.BuildCacheConfiguration;

public final class GradleDevelocityConfigurable implements DevelocityConfigurable {

    private final DevelocityConfiguration develocity;
    private final BuildScanConfigurable buildScan;
    private final BuildCacheConfigurable buildCache;

    public GradleDevelocityConfigurable(DevelocityConfiguration develocity) {
        this.develocity = develocity;
        this.buildScan = new GradleBuildScanConfigurable(develocity.getBuildScan());
        this.buildCache = new NoopBuildCacheConfigurable();
    }

    public GradleDevelocityConfigurable(DevelocityConfiguration develocity, BuildCacheConfiguration buildCache) {
        this.develocity = develocity;
        this.buildScan = new GradleBuildScanConfigurable(develocity.getBuildScan());
        this.buildCache = new GradleBuildCacheConfigurable(develocity.getBuildCache(), buildCache);
    }

    @Override
    public void setServer(String server) {
        develocity.getServer().set(server);
    }

    @Override
    public void setProjectId(String projectId) {
        develocity.getProjectId().set(projectId);
    }

    @Override
    public void setAllowUntrustedServer(boolean allowUntrustedServer) {
        develocity.getAllowUntrustedServer().set(allowUntrustedServer);
    }

    @Override
    public void setAccessKey(String accessKey) {
        develocity.getAccessKey().set(accessKey);
    }

    @Override
    public BuildScanConfigurable getBuildScan() {
        return buildScan;
    }

    @Override
    public BuildCacheConfigurable getBuildCache() {
        return buildCache;
    }

}
