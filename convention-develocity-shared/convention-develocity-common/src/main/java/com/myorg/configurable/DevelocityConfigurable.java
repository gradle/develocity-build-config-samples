package com.myorg.configurable;

import java.util.function.Supplier;

public interface DevelocityConfigurable {

    Supplier<String> getServer();

    void setServer(String server);

    void setProjectId(String projectId);

    void setAllowUntrustedServer(boolean untrustedServer);

    void setAccessKey(String accessKey);

    BuildScanConfigurable getBuildScan();

    BuildCacheConfigurable getBuildCache();

}
