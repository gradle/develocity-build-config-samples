package com.myorg.configurable;

public final class PublishingContext {

    private final BuildResult buildResult;
    private final boolean authenticated;

    PublishingContext(BuildResult buildResult, boolean authenticated) {
        this.buildResult = buildResult;
        this.authenticated = authenticated;
    }

    public BuildResult getBuildResult() {
        return buildResult;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

}
