package com.myorg.configurable;

import com.gradle.develocity.agent.gradle.DevelocityConfiguration;

public final class GradleDevelocityConfigurable implements DevelocityConfigurable {

    private final DevelocityConfiguration develocity;

    public GradleDevelocityConfigurable(DevelocityConfiguration develocity) {
        this.develocity = develocity;
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
        return new GradleBuildScanConfigurable(develocity.getBuildScan());
    }

}
