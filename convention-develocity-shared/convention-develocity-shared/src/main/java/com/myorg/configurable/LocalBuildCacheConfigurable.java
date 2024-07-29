package com.myorg.configurable;

import java.io.File;

public interface LocalBuildCacheConfigurable {

    void setEnabled(boolean enabled);

    void setStoreEnabled(boolean storeEnabled);

    void setDirectory(File directory);

}
