package com.myorg.configurable;

import com.gradle.develocity.agent.gradle.scan.BuildScanCaptureConfiguration;

final class GradleBuildScanCaptureConfigurable implements BuildScanCaptureConfigurable {

    private final BuildScanCaptureConfiguration buildScanCapture;

    public GradleBuildScanCaptureConfigurable(BuildScanCaptureConfiguration buildScanCapture) {
        this.buildScanCapture = buildScanCapture;
    }

    @Override
    public void setFileFingerprints(boolean capture) {
        buildScanCapture.getFileFingerprints().set(capture);
    }

    @Override
    public void setBuildLogging(boolean capture) {
        buildScanCapture.getBuildLogging().set(capture);
    }

    @Override
    public void setTestLogging(boolean capture) {
        buildScanCapture.getTestLogging().set(capture);
    }

}
