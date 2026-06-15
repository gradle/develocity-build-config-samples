package com.myorg.configurable;

import com.gradle.develocity.agent.maven.api.DevelocityApi;

import java.util.function.Supplier;

public final class MavenDevelocityConfigurable implements DevelocityConfigurable {

    private final DevelocityApi develocity;
    private final BuildScanConfigurable buildScan;
    private final BuildCacheConfigurable buildCache;

    public MavenDevelocityConfigurable(DevelocityApi develocity) {
        this.develocity = develocity;
        this.buildScan = new MavenBuildScanConfigurable(develocity.getBuildScan());
        this.buildCache = new MavenBuildCacheConfigurable(develocity.getBuildCache());
    }

    @Override
    public Supplier<String> getServer() {
        return develocity::getServer;
    }

    @Override
    public void setServer(String server) {
        develocity.setServer(server);
    }

    @Override
    public void setProjectId(String projectId) {
        develocity.setProjectId(projectId);
    }

    @Override
    public void setAllowUntrustedServer(boolean allowUntrustedServer) {
        develocity.setAllowUntrustedServer(allowUntrustedServer);
    }

    @Override
    public void setAccessKey(String accessKey) {
        develocity.setAccessKey(accessKey);
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
