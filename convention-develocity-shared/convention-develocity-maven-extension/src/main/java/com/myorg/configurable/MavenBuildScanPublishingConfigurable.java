package com.myorg.configurable;

import com.gradle.develocity.agent.maven.api.scan.BuildScanPublishing;

import java.util.function.Predicate;

final class MavenBuildScanPublishingConfigurable implements BuildScanPublishingConfigurable {

    private final BuildScanPublishing buildScanPublishing;

    public MavenBuildScanPublishingConfigurable(BuildScanPublishing buildScanPublishing) {
        this.buildScanPublishing = buildScanPublishing;
    }

    @Override
    public void onlyIf(Predicate<PublishingContext> onlyIf) {
        buildScanPublishing.onlyIf(context ->
                onlyIf.test(new PublishingContext(new BuildResult(context.getBuildResult().getFailures()), context.isAuthenticated())));
    }

}
