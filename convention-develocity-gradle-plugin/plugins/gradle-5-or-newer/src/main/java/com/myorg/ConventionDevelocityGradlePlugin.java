package com.myorg;

import com.gradle.CommonCustomUserDataGradlePlugin;
import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import com.gradle.develocity.agent.gradle.DevelocityPlugin;
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.StartParameter;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.gradle.util.GradleVersion;

/**
 * An example Gradle plugin for enabling and configuring Develocity features for
 * Gradle versions 5.x and higher.
 */
public class ConventionDevelocityGradlePlugin implements Plugin<Object> {

    @Override
    public void apply(Object target) {
        if (target instanceof Settings) {
            if (!isGradle6OrNewer()) {
                throw new GradleException("For Gradle versions prior to 6.0, the Convention Develocity plugin must be applied to the Root project");
            }
            configureGradle6OrNewer((Settings) target);
        } else if (target instanceof Project) {
            if (isGradle6OrNewer()) {
                throw new GradleException("For Gradle versions 6.0 and newer, the Convention Develocity plugin must be applied to Settings");
            } else if (isGradle5OrNewer()) {
                Project project = (Project) target;
                if (!project.equals(project.getRootProject())) {
                    throw new GradleException("For Gradle versions prior to 6.0, the Convention Develocity plugin must be applied to the Root project");
                }
                configureGradle5(project);
            } else {
                throw new GradleException("For Gradle versions prior to 5.0, the Convention Develocity plugin is not supported");
            }
        }
    }

    private void configureGradle6OrNewer(Settings settings) {
        settings.getPluginManager().apply(DevelocityPlugin.class);
        settings.getPluginManager().apply(CommonCustomUserDataGradlePlugin.class);
        configureDevelocity(settings.getExtensions().getByType(DevelocityConfiguration.class), settings.getGradle().getStartParameter());
        configureBuildCache(settings.getBuildCache(), settings.getExtensions().getByType(DevelocityConfiguration.class));
    }

    private void configureGradle5(Project project) {
        project.getPluginManager().apply(DevelocityPlugin.class);
        project.getPluginManager().apply(CommonCustomUserDataGradlePlugin.class);
        configureDevelocity(project.getExtensions().getByType(DevelocityConfiguration.class), project.getGradle().getStartParameter());
        // configureBuildCache is not called because the build cache cannot be configured via a plugin prior to Gradle 6.0
    }

    private void configureDevelocity(DevelocityConfiguration develocity, StartParameter startParameter) {
        // CHANGE ME: Apply your Develocity configuration here
        develocity.getServer().set("https://develocity-samples.gradle.com");
        if (!containsPropertiesTask(startParameter)) {
            configureBuildScan(develocity.getBuildScan());
        }
    }

    private boolean containsPropertiesTask(StartParameter startParameter) {
        return startParameter.getTaskNames().contains("properties")
                || startParameter.getTaskNames().stream().anyMatch(it -> it.endsWith(":properties"));
    }

    private void configureBuildScan(BuildScanConfiguration buildScan) {
        // CHANGE ME: Apply your Build Scan configuration here
        buildScan.getUploadInBackground().set(!isCi());
    }

    private void configureBuildCache(BuildCacheConfiguration buildCache, DevelocityConfiguration develocity) {
        // CHANGE ME: Apply your Build Cache configuration here
        buildCache.remote(develocity.getBuildCache(), remote -> {
            remote.setEnabled(true);
            remote.setPush(isCi());
        });
        buildCache.local(local -> {
            local.setEnabled(true);
            local.setPush(true);
        });
    }

    private static boolean isCi() {
        // CHANGE ME: Apply your environment detection logic here
        return System.getenv().containsKey("CI");
    }

    private static boolean isGradle6OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("6.0")) >= 0;
    }

    private static boolean isGradle5OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("5.0")) >= 0;
    }

}
