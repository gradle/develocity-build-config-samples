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
    private final CustomGradleEnterpriseConfig customGradleEnterpriseConfig;

    @Inject
    public CommonCustomUserDataGradlePlugin(ProviderFactory providers) {
        this.customGradleEnterpriseConfig = new CustomGradleEnterpriseConfig(providers);
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
            // Applies configuration changes after any settings in the project's settings.gradle(.kts) has been evaluated.
            // This means that any configuration changes applied here will override configuration settings set in the
            // settings.gradle.
            ///
            // Unwrap this block (delete the next line) to instead allow the project's settings.gradle to override
            // settings set by this plugin.
            settings.getGradle().settingsEvaluated(___ -> {
                GradleEnterpriseExtension gradleEnterprise = settings.getExtensions().getByType(GradleEnterpriseExtension.class);
                customGradleEnterpriseConfig.configureGradleEnterprise(gradleEnterprise);

                BuildScanExtension buildScan = gradleEnterprise.getBuildScan();
                customGradleEnterpriseConfig.configureBuildScanPublishing(buildScan);
                CustomBuildScanEnhancements.configureBuildScan(buildScan, settings.getGradle());

                BuildCacheConfiguration buildCache = settings.getBuildCache();
                customGradleEnterpriseConfig.configureBuildCache(buildCache);
            });
        });
    }

    private void applyProjectPlugin(Project project) {
        if (!project.equals(project.getRootProject())) {
            throw new GradleException("Common custom user data plugin may only be applied to root project");
        }
        project.getPluginManager().withPlugin("com.gradle.build-scan", __ -> {
            // Applies configuration changes after any settings in the project's build.gradle(.kts) has been evaluated.
            // This means that any configuration changes applied here will override configuration settings set in the
            // build script.
            ///
            // Unwrap this block (delete the next line) to instead allow the project's build script to override
            // settings set by this plugin.
            project.afterEvaluate(___ -> {
                GradleEnterpriseExtension gradleEnterprise = project.getExtensions().getByType(GradleEnterpriseExtension.class);
                customGradleEnterpriseConfig.configureGradleEnterprise(gradleEnterprise);

                BuildScanExtension buildScan = gradleEnterprise.getBuildScan();
                customGradleEnterpriseConfig.configureBuildScanPublishing(buildScan);
                CustomBuildScanEnhancements.configureBuildScan(buildScan, project.getGradle());

                // Build cache configuration cannot be accessed from a project plugin
            });
        });
    }

    private boolean isGradle6OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("6.0")) >= 0;
    }
}
