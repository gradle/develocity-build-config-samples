package com.myorg.configurable;

import java.util.function.Consumer;

public interface BuildScanConfigurable {

    void tag(String tag);

    void value(String name, String value);

    void link(String name, String url);

    void background(Consumer<BuildScanConfigurable> action);

    void buildFinished(Consumer<BuildResult> action);

    void buildScanPublished(Consumer<PublishedBuildScan> action);

    void setUploadInBackground(boolean uploadInBackground);

    BuildScanObfuscationConfigurable getObfuscation();

    BuildScanCaptureConfigurable getCapture();

    BuildScanPublishingConfigurable getPublishing();

}
