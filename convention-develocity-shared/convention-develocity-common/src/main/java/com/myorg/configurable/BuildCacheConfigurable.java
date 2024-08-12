package com.myorg.configurable;

public interface BuildCacheConfigurable {

    LocalBuildCacheConfigurable getLocal();

    RemoteBuildCacheConfigurable getRemote();

}
