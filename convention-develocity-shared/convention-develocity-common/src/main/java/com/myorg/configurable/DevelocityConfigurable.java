package com.myorg.configurable;

public interface DevelocityConfigurable {

    void setServer(String server);

    void setProjectId(String projectId);

    void setAllowUntrustedServer(boolean untrustedServer);

    void setAccessKey(String accessKey);

    BuildScanConfigurable getBuildScan();

    BuildCacheConfigurable getBuildCache();

}
