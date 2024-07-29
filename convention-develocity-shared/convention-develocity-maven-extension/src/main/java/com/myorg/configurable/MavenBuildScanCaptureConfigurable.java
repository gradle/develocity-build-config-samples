package com.myorg.configurable;

import com.gradle.develocity.agent.maven.api.scan.BuildScanCaptureSettings;

final class MavenBuildScanCaptureConfigurable implements BuildScanCaptureConfigurable {

    private final BuildScanCaptureSettings buildScanCapture;

    public MavenBuildScanCaptureConfigurable(BuildScanCaptureSettings buildScanCapture) {
        this.buildScanCapture = buildScanCapture;
    }

    @Override
    public void setFileFingerprints(boolean capture) {
        buildScanCapture.setFileFingerprints(capture);
    }

    @Override
    public void setBuildLogging(boolean capture) {
        buildScanCapture.setBuildLogging(capture);
    }

    @Override
    public void setTestLogging(boolean capture) {
        buildScanCapture.setTestLogging(capture);
    }

}
