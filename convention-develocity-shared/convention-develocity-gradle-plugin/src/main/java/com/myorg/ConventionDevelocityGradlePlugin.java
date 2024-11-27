package com.myorg;

import com.gradle.CommonCustomUserDataGradlePlugin;
import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import com.gradle.develocity.agent.gradle.DevelocityPlugin;
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import com.myorg.configurable.GradleDevelocityConfigurable;
import com.myorg.configurable.GradleExecutionContext;
import org.gradle.StartParameter;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.testing.Test;

import javax.inject.Inject;

import static com.myorg.DevelocityConventions.TEST_CACHING_DISABLED_REASON;
import static com.myorg.DevelocityConventions.TEST_CACHING_PROPERTY_NAME;
import static com.myorg.GradleUtils.configureAllProjects;
import static com.myorg.GradleUtils.doNotCacheTaskIf;
import static com.myorg.GradleUtils.getBooleanProperty;
import static com.myorg.GradleUtils.isGradle5OrNewer;
import static com.myorg.GradleUtils.isGradle6OrNewer;

/**
 * An example Gradle plugin for enabling and configuring Develocity features
 */
final class ConventionDevelocityGradlePlugin implements Plugin<Object> {

    private final ProviderFactory providers;

    @Inject
    public ConventionDevelocityGradlePlugin(ProviderFactory providers) {
        this.providers = providers;
    }

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
        DevelocityConfiguration develocity = settings.getExtensions().getByType(DevelocityConfiguration.class);
        GradleExecutionContext context = new GradleExecutionContext(providers);
        new DevelocityConventions(context).configureDevelocity(new GradleDevelocityConfigurable(develocity, settings.getBuildCache()));
        configureBuildScan(develocity.getBuildScan(), settings.getGradle().getStartParameter());
        configureBuildCacheDefaults(settings);
    }

    private void configureGradle5(Project project) {
        project.getPluginManager().apply(DevelocityPlugin.class);
        project.getPluginManager().apply(CommonCustomUserDataGradlePlugin.class);
        DevelocityConfiguration develocity = project.getExtensions().getByType(DevelocityConfiguration.class);
        GradleExecutionContext context = new GradleExecutionContext(providers);
        new DevelocityConventions(context).configureDevelocity(new GradleDevelocityConfigurable(develocity));
        configureBuildScan(develocity.getBuildScan(), project.getGradle().getStartParameter());
        configureBuildCacheDefaults(project);
    }

    private void configureBuildScan(BuildScanConfiguration buildScan, StartParameter startParameter) {
        buildScan.getCapture().getBuildLogging().set(!containsPropertiesTask(startParameter));
    }

    private boolean containsPropertiesTask(StartParameter startParameter) {
        return startParameter.getTaskNames().contains("properties")
                || startParameter.getTaskNames().stream().anyMatch(it -> it.endsWith(":properties"));
    }

    private void configureBuildCacheDefaults(Settings settings) {
        configureAllProjects(settings, this::disableTestCachingByDefault);
    }

    private void configureBuildCacheDefaults(Project project) {
        configureAllProjects(project, this::disableTestCachingByDefault);
    }

    private void disableTestCachingByDefault(Project project) {
        Provider<Boolean> enableTestCaching = getBooleanProperty(project, TEST_CACHING_PROPERTY_NAME);
        doNotCacheTaskIf(project, Test.class, TEST_CACHING_DISABLED_REASON, () -> !enableTestCaching.get());
    }

}
