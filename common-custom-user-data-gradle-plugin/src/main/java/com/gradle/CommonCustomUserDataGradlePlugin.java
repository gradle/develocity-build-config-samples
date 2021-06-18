package com.gradle;

import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension;
import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.gradle.util.GradleVersion;

import javax.inject.Inject;

public class CommonCustomUserDataGradlePlugin implements Plugin<Object> {

    private final ProviderFactory providers;

    @Inject
    public CommonCustomUserDataGradlePlugin(ProviderFactory providers) {
        this.providers = providers;
    }

    public void apply(Object target) {
        if (target instanceof Settings) {
            if (!isGradle6OrNewer()) {
                throw new GradleException("For Gradle versions prior to 6.0, common-custom-user-data-gradle-plugin must be applied to the Root project");
            }
            applySettingsPlugin((Settings) target);
        } else if (target instanceof Project) {
            if (isGradle6OrNewer()) {
                throw new GradleException("For Gradle versions 6.0 and newer, common-custom-user-data-gradle-plugin must be applied to Settings");
            }
            applyProjectPlugin((Project) target);
        }
    }

    private void applySettingsPlugin(Settings settings) {
        settings.getPluginManager().withPlugin("com.gradle.enterprise", __ -> {
            CustomGradleEnterpriseConfig customGradleEnterpriseConfig = new CustomGradleEnterpriseConfig();

            GradleEnterpriseExtension gradleEnterprise = settings.getExtensions().getByType(GradleEnterpriseExtension.class);
            customGradleEnterpriseConfig.configureGradleEnterprise(gradleEnterprise);

            BuildScanExtension buildScan = gradleEnterprise.getBuildScan();
            customGradleEnterpriseConfig.configureBuildScanPublishing(buildScan);
            new CustomBuildScanEnhancements(buildScan, providers, settings.getGradle()).apply();

            BuildCacheConfiguration buildCache = settings.getBuildCache();
            customGradleEnterpriseConfig.configureBuildCache(buildCache);

            // configuration changes applied in this block will override earlier configuration settings,
            // including those set in the settings.gradle(.kts)
            settings.getGradle().settingsEvaluated(___ -> {
                SystemPropertyOverrides overrides = new SystemPropertyOverrides(providers);
                overrides.configureGradleEnterprise(gradleEnterprise);
                overrides.configureBuildCache(buildCache);
            });
        });
    }

    private void applyProjectPlugin(Project project) {
        if (!project.equals(project.getRootProject())) {
            throw new GradleException("Common custom user data plugin may only be applied to root project");
        }
        project.getPluginManager().withPlugin("com.gradle.build-scan", __ -> {
            CustomGradleEnterpriseConfig customGradleEnterpriseConfig = new CustomGradleEnterpriseConfig();

            GradleEnterpriseExtension gradleEnterprise = project.getExtensions().getByType(GradleEnterpriseExtension.class);
            customGradleEnterpriseConfig.configureGradleEnterprise(gradleEnterprise);

            BuildScanExtension buildScan = gradleEnterprise.getBuildScan();
            customGradleEnterpriseConfig.configureBuildScanPublishing(buildScan);
            new CustomBuildScanEnhancements(buildScan, providers, project.getGradle()).apply();

            // Build cache configuration cannot be accessed from a project plugin

            // configuration changes applied within this block will override earlier configuration settings,
            // including those set in the root project's build.gradle(.kts)
            project.afterEvaluate(___ -> {
                new SystemPropertyOverrides(providers).configureGradleEnterprise(gradleEnterprise);
            });
        });
    }

    private static boolean isGradle6OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("6.0")) >= 0;
    }

}
