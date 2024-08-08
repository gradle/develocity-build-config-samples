package com.myorg.configurable;

public interface BuildScanCaptureConfigurable {

    void setFileFingerprints(boolean capture);

    void setBuildLogging(boolean capture);

    void setTestLogging(boolean capture);

}
