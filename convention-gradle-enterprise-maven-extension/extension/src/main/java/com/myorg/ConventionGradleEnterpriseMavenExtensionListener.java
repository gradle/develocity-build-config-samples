package com.myorg;

import com.gradle.maven.extension.api.GradleEnterpriseApi;
import com.gradle.maven.extension.api.GradleEnterpriseListener;
import com.gradle.maven.extension.api.cache.BuildCacheApi;
import com.gradle.maven.extension.api.scan.BuildScanApi;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;

@SuppressWarnings("unused")
@Component(
        role = GradleEnterpriseListener.class,
        hint = "convention-gradle-enterprise-maven-extension",
        description = "Configures the Gradle Enterprise Maven Extension for com.myorg"
)
public final class ConventionGradleEnterpriseMavenExtensionListener implements GradleEnterpriseListener {

    @Override
    public void configure(GradleEnterpriseApi gradleEnterprise, MavenSession session) {
        configureGradleEnterprise(gradleEnterprise);
        configureBuildCache(gradleEnterprise.getBuildCache());
    }

    private void configureGradleEnterprise(GradleEnterpriseApi gradleEnterprise) {
        // CHANGE ME: Apply your Gradle Enterprise configuration here
        gradleEnterprise.setServer("https://enterprise-samples.gradle.com");
        configureBuildScan(gradleEnterprise.getBuildScan());
    }

    private void configureBuildScan(BuildScanApi buildScan) {
        // CHANGE ME: Apply your Build Scan configuration here
        buildScan.publishAlways();
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
