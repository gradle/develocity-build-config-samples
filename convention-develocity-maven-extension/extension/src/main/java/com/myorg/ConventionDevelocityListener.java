package com.myorg;

import com.gradle.develocity.agent.maven.api.DevelocityApi;
import com.gradle.develocity.agent.maven.api.DevelocityListener;
import com.gradle.develocity.agent.maven.api.cache.BuildCacheApi;
import com.gradle.develocity.agent.maven.api.scan.BuildScanApi;
import org.apache.maven.execution.MavenSession;

/**
 * An example Maven extension for enabling and configuring Develocity features.
 */
public final class ConventionDevelocityListener implements DevelocityListener {

    @Override
    public void configure(DevelocityApi develocity, MavenSession session) {
        configureDevelocity(develocity);
        configureBuildCache(develocity.getBuildCache());
    }

    private void configureDevelocity(DevelocityApi develocity) {
        // CHANGE ME: Apply your Develocity configuration here
        develocity.setServer("https://ge.solutions-team.gradle.com");
        configureBuildScan(develocity.getBuildScan());
    }

    private void configureBuildScan(BuildScanApi buildScan) {
        // CHANGE ME: Apply your Build Scan configuration here
        buildScan.setUploadInBackground(!isCi());
    }

    private void configureBuildCache(BuildCacheApi buildCache) {
        // CHANGE ME: Apply your Build Cache configuration here
        buildCache.getRemote().setEnabled(true);
        buildCache.getRemote().setStoreEnabled(isCi());

        buildCache.getLocal().setEnabled(true);
        buildCache.getLocal().setStoreEnabled(true);
    }

    private static boolean isCi() {
        // CHANGE ME: Apply your environment detection logic here
        return System.getenv().containsKey("CI");
    }

}
