package com.myorg.configurable;

import com.gradle.develocity.agent.maven.api.scan.BuildScanApi;

import java.util.function.Consumer;

final class MavenBuildScanConfigurable implements BuildScanConfigurable {

    private final BuildScanApi buildScan;
    private final MavenBuildScanObfuscationConfigurable buildScanObfuscation;
    private final MavenBuildScanCaptureConfigurable buildScanCapture;
    private final MavenBuildScanPublishingConfigurable buildScanPublishing;

    public MavenBuildScanConfigurable(BuildScanApi buildScan) {
        this.buildScan = buildScan;
        this.buildScanObfuscation = new MavenBuildScanObfuscationConfigurable(buildScan.getObfuscation());
        this.buildScanCapture = new MavenBuildScanCaptureConfigurable(buildScan.getCapture());
        this.buildScanPublishing = new MavenBuildScanPublishingConfigurable(buildScan.getPublishing());
    }

    @Override
    public void tag(String tag) {
        buildScan.tag(tag);
    }

    @Override
    public void value(String name, String value) {
        buildScan.value(name, value);
    }

    @Override
    public void link(String name, String url) {
        buildScan.link(name, url);
    }

    @Override
    public void background(Consumer<BuildScanConfigurable> action) {
        buildScan.background(__ -> action.accept(this));
    }

    @Override
    public void buildFinished(Consumer<BuildResult> action) {
        buildScan.buildFinished(buildResult -> action.accept(new BuildResult(buildResult.getFailures())));
    }

    @Override
    public void buildScanPublished(Consumer<PublishedBuildScan> action) {
        buildScan.buildScanPublished(publishedBuildScan ->
                action.accept(new PublishedBuildScan(publishedBuildScan.getBuildScanId(), publishedBuildScan.getBuildScanUri())));
    }

    @Override
    public void setUploadInBackground(boolean uploadInBackground) {
        buildScan.setUploadInBackground(uploadInBackground);
    }

    @Override
    public BuildScanObfuscationConfigurable getObfuscation() {
        return buildScanObfuscation;
    }

    @Override
    public BuildScanCaptureConfigurable getCapture() {
        return buildScanCapture;
    }

    @Override
    public BuildScanPublishingConfigurable getPublishing() {
        return buildScanPublishing;
    }

}
