package com.myorg;

import com.gradle.CommonCustomUserDataGradlePlugin;
import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import com.gradle.enterprise.gradleplugin.GradleEnterprisePlugin;
import com.gradle.scan.plugin.BuildScanExtension;
import com.gradle.scan.plugin.BuildScanPlugin;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.gradle.caching.http.HttpBuildCache;
import org.gradle.util.GradleVersion;

public class GradleEnterpriseConventionsPlugin implements Plugin<Object> {

    @Override
    public void apply(Object target) {
        if (target instanceof Settings) {
            if (!isGradle6OrNewer()) {
                throw new GradleException("For Gradle versions prior to 6.0, gradle-enterprise-conventions must be applied to the Root project");
            }
            applySettingsPlugin((Settings) target);
        } else if (target instanceof Project) {
            if (isGradle6OrNewer()) {
                throw new GradleException("For Gradle versions 6.0 and newer, gradle-enterprise-conventions must be applied to Settings");
            }
            applyProjectPlugin((Project) target);
        }
    }

    private void applySettingsPlugin(Settings settings) {
        settings.getPluginManager().apply(GradleEnterprisePlugin.class);
        settings.getPluginManager().apply(CommonCustomUserDataGradlePlugin.class);

        configureGradleEnterprise(settings.getExtensions().getByType(GradleEnterpriseExtension.class));
        configureBuildCache(settings.getBuildCache());
    }

    private void applyProjectPlugin(Project project) {
        if (!project.equals(project.getRootProject())) {
            throw new GradleException("gradle-enterprise-conventions may only be applied to root project");
        }
        project.getPluginManager().apply(BuildScanPlugin.class);
        project.getPluginManager().apply(CommonCustomUserDataGradlePlugin.class);

        project.getPluginManager().withPlugin("com.gradle.build-scan", __ -> {
            configureGradleEnterprise(project.getExtensions().getByType(GradleEnterpriseExtension.class));
            // Build cache configuration cannot be accessed from a project plugin
        });
    }

    private void configureGradleEnterprise(GradleEnterpriseExtension gradleEnterprise) {
        /* Example of how to configure Gradle Enterprise from the plugin. */
        gradleEnterprise.setServer("https://ge.myorg.com");

        BuildScanExtension buildScan = gradleEnterprise.getBuildScan();
        buildScan.setCaptureTaskInputFiles(true);
        buildScan.setUploadInBackground(true);
        buildScan.tag("Configued with gradle-enterprise-conventions");
    }

    private void configureBuildCache(BuildCacheConfiguration buildCache) {
        // Enable the local build cache for all local and CI builds
        // For short-lived CI agents, it makes sense to disable the local build cache
        buildCache.local(local -> {
            local.setEnabled(true);
        });

        // Only permit store operations to the remote build cache for CI builds
        // Local builds will only read from the remote build cache
        boolean isCiServer = System.getenv().containsKey("CI");
        buildCache.remote(HttpBuildCache.class, remote -> {
            remote.setUrl("https://ge.myorg.com/cache/");
            remote.setEnabled(true);
            remote.setPush(isCiServer);
        });
    }

    private static boolean isGradle6OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("6.0")) >= 0;
    }
}
