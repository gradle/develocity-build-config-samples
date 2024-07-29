package com.myorg.configurable;

import com.gradle.develocity.agent.gradle.scan.BuildScanPublishingConfiguration;

import java.util.function.Predicate;

final class GradleBuildScanPublishingConfigurable implements BuildScanPublishingConfigurable {

    private final BuildScanPublishingConfiguration buildScanPublishing;

    public GradleBuildScanPublishingConfigurable(BuildScanPublishingConfiguration buildScanPublishing) {
        this.buildScanPublishing = buildScanPublishing;
    }

    @Override
    public void onlyIf(Predicate<PublishingContext> onlyIf) {
        buildScanPublishing.onlyIf(context ->
                onlyIf.test(new PublishingContext(new BuildResult(context.getBuildResult().getFailures()), context.isAuthenticated())));
    }

}
