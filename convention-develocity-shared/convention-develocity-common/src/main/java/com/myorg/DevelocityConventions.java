package com.myorg;

import com.myorg.configurable.BuildCacheConfigurable;
import com.myorg.configurable.BuildScanConfigurable;
import com.myorg.configurable.DevelocityConfigurable;
import com.myorg.configurable.ExecutionContext;

import java.util.function.Supplier;

final class DevelocityConventions {

    private final ExecutionContext context;

    DevelocityConventions(ExecutionContext context) {
        this.context = context;
    }

    void configureDevelocity(DevelocityConfigurable develocity) {
        // CHANGE ME: Apply your Develocity configuration here
        develocity.setServer("https://develocity-samples.gradle.com");
        configureBuildScan(develocity.getBuildScan(), develocity.getServer());
        configureBuildCache(develocity.getBuildCache());
    }

    private void configureBuildScan(BuildScanConfigurable buildScan, Supplier<String> getDevelocityServer) {
        // CHANGE ME: Apply your Build Scan configuration here
        buildScan.setUploadInBackground(!isCi());
        buildScan.background(new CaptureGitHubCustomPropertyAction(context, getDevelocityServer));
    }

    private void configureBuildCache(BuildCacheConfigurable buildCache) {
        // CHANGE ME: Apply your Build Cache configuration here
        buildCache.getLocal().setEnabled(true);
        buildCache.getLocal().setStoreEnabled(true);
        buildCache.getRemote().setEnabled(true);
        buildCache.getRemote().setStoreEnabled(isCi());
    }

    private boolean isCi() {
        // CHANGE ME: Apply your environment detection logic here
        return context.environmentVariable("CI").isPresent();
    }

}
