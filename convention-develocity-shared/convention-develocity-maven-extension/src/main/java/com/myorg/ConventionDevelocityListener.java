package com.myorg;

import com.gradle.develocity.agent.maven.api.DevelocityApi;
import com.gradle.develocity.agent.maven.api.DevelocityListener;
import com.myorg.configurable.MavenDevelocityConfigurable;
import com.myorg.configurable.MavenExecutionContext;
import org.apache.maven.execution.MavenSession;

import static com.myorg.DevelocityConventions.TEST_CACHING_DISABLED_REASON;
import static com.myorg.DevelocityConventions.TEST_CACHING_PROPERTY_NAME;
import static com.myorg.MavenUtils.doNotCachePluginIf;
import static com.myorg.MavenUtils.getBooleanProperty;

/**
 * An example Maven extension for enabling and configuring Develocity features.
 */
final class ConventionDevelocityListener implements DevelocityListener {

    @Override
    public void configure(DevelocityApi develocity, MavenSession session) {
        MavenExecutionContext context = new MavenExecutionContext();
        new DevelocityConventions(context).configureDevelocity(new MavenDevelocityConfigurable(develocity));
        configureBuildCacheDefaults(develocity);
    }

    private void configureBuildCacheDefaults(DevelocityApi develocity) {
        disableTestCachingByDefault(develocity);
    }

    private void disableTestCachingByDefault(DevelocityApi develocity) {
        boolean enableTestCaching = getBooleanProperty(TEST_CACHING_PROPERTY_NAME);
        doNotCachePluginIf(develocity, "maven-failsafe-plugin", TEST_CACHING_DISABLED_REASON, () -> !enableTestCaching);
        doNotCachePluginIf(develocity, "maven-surefire-plugin", TEST_CACHING_DISABLED_REASON, () -> !enableTestCaching);
    }

}
