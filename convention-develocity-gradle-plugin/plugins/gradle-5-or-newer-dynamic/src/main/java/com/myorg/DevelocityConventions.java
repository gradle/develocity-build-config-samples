package com.myorg;

import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import com.gradle.develocity.agent.gradle.scan.BuildScanPublishingConfiguration.PublishingContext;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.BuildCacheConfiguration;

import static com.myorg.GradleUtils.environmentVariable;

@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "SameParameterValue"})
final class DevelocityConventions {

    private final ProviderFactory providers;

    DevelocityConventions(ProviderFactory providers) {
        this.providers = providers;
    }

    void configureDevelocity(DevelocityConfiguration develocity, BuildCacheConfiguration buildCache) {
        develocity.getServer().set("https://ge.solutions-team.gradle.com");
        configureBuildScan(develocity.getBuildScan());
        configureBuildCache(develocity, buildCache);
    }

    private void configureBuildScan(BuildScanConfiguration buildScan) {
        buildScan.getUploadInBackground().set(!isCi());
        buildScan.publishing(it -> it.onlyIf(PublishingContext::isAuthenticated));
    }

    private void configureBuildCache(DevelocityConfiguration develocity, BuildCacheConfiguration buildCache) {
        if (buildCache == null) {
            return;
        }

        buildCache.remote(develocity.getBuildCache(), remote -> {
            remote.setEnabled(true);
            remote.setPush(isCi());
        });
        buildCache.local(local -> {
            local.setEnabled(true);
            local.setPush(true);
        });
    }

    private boolean isCi() {
        return environmentVariable("CI", providers).isPresent();
    }

}
