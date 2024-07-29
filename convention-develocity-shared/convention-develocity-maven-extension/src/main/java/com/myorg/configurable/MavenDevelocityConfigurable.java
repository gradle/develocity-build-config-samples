package com.myorg.configurable;

import com.gradle.develocity.agent.maven.api.DevelocityApi;

public final class MavenDevelocityConfigurable implements DevelocityConfigurable {

    private final DevelocityApi develocity;
    private final BuildScanConfigurable buildScan;

    public MavenDevelocityConfigurable(DevelocityApi develocity) {
        this.develocity = develocity;
        this.buildScan = new MavenBuildScanConfigurable(develocity.getBuildScan());
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

}
