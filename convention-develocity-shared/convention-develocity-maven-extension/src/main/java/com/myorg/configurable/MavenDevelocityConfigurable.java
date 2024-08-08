package com.myorg.configurable;

import com.gradle.develocity.agent.maven.api.DevelocityApi;

public final class MavenDevelocityConfigurable implements DevelocityConfigurable {

    private final DevelocityApi develocity;

    public MavenDevelocityConfigurable(DevelocityApi develocity) {
        this.develocity = develocity;
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
        return new MavenBuildScanConfigurable(develocity.getBuildScan());
    }

}
