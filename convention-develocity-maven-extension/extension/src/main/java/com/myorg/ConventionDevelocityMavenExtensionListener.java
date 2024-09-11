package com.myorg;

import com.gradle.develocity.agent.maven.api.DevelocityApi;
import com.gradle.develocity.agent.maven.api.DevelocityListener;
import com.gradle.develocity.agent.maven.api.cache.BuildCacheApi;
import com.gradle.develocity.agent.maven.api.cache.NormalizationProvider.RuntimeClasspathNormalization;
import com.gradle.develocity.agent.maven.api.scan.BuildScanApi;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;

import java.util.function.Consumer;

/**
 * An example Maven extension for enabling and configuring Develocity features.
 */
@Component(
        role = DevelocityListener.class,
        hint = "convention-develocity-maven-extension",
        description = "Configures the Develocity Maven extension for com.myorg"
)
public final class ConventionDevelocityMavenExtensionListener implements DevelocityListener {
    private static final String DISABLE_DEFAULT_NORMALIZATIONS_SYS_PROP = "develocity.disableDefaultNormalizations";

    @Override
    public void configure(DevelocityApi develocity, MavenSession session) {
        configureDevelocity(develocity);
        configureBuildCache(develocity.getBuildCache());
        configureDefaultNormalizations(develocity);
    }

    private void configureDevelocity(DevelocityApi develocity) {
        // CHANGE ME: Apply your Develocity configuration here
        develocity.setServer("https://develocity-samples.gradle.com");
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

    private void configureDefaultNormalizations(DevelocityApi develocity) {
        String disableDefaultNormalizations = System.getProperty(DISABLE_DEFAULT_NORMALIZATIONS_SYS_PROP, "false");
        if (Boolean.parseBoolean(disableDefaultNormalizations)) {
            return;
        }

        Consumer<RuntimeClasspathNormalization> runtimeClasspathNormalization = n -> n.addIgnoredFiles(
                "**/build-info.properties",
                "**/git.properties"
        );

        develocity.getBuildCache().registerNormalizationProvider(context ->
                context.configureRuntimeClasspathNormalization(runtimeClasspathNormalization));
    }
}
