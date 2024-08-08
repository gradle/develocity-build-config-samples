package com.myorg;

import com.myorg.configurable.BuildCacheConfigurable;
import com.myorg.configurable.BuildScanConfigurable;
import com.myorg.configurable.DevelocityConfigurable;

final class DevelocityConventions {

    void configureDevelocity(DevelocityConfigurable develocity) {
        // CHANGE ME: Apply your Develocity configuration here
        develocity.setServer("https://develocity-samples.gradle.com");
        configureBuildScan(develocity.getBuildScan());
    }

    private void configureBuildScan(BuildScanConfigurable buildScan) {
        // CHANGE ME: Apply your Build Scan configuration here
        buildScan.setUploadInBackground(!isCi());
    }

    void configureBuildCache(BuildCacheConfigurable buildCache) {
        // CHANGE ME: Apply your Build Cache configuration here
        buildCache.getLocal().setEnabled(true);
        buildCache.getLocal().setStoreEnabled(true);
        buildCache.getRemote().setEnabled(true);
        buildCache.getRemote().setStoreEnabled(isCi());
    }

    private static boolean isCi() {
        // CHANGE ME: Apply your environment detection logic here
        return System.getenv().containsKey("CI");
    }

}
