package com.myorg;

import com.gradle.CommonCustomUserDataGradlePlugin;
import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import com.gradle.develocity.agent.gradle.DevelocityPlugin;
import com.myorg.configurable.GradleDevelocityConfigurable;
import com.myorg.configurable.GradleExecutionContext;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.util.GradleVersion;

import javax.inject.Inject;

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
        GradleExecutionContext context = new GradleExecutionContext(providers, settings.getRootDir().toPath());
        new DevelocityConventions(context).configureDevelocity(new GradleDevelocityConfigurable(develocity, settings.getBuildCache()));
    }

    private void configureGradle5(Project project) {
        project.getPluginManager().apply(DevelocityPlugin.class);
        project.getPluginManager().apply(CommonCustomUserDataGradlePlugin.class);
        DevelocityConfiguration develocity = project.getExtensions().getByType(DevelocityConfiguration.class);
        GradleExecutionContext context = new GradleExecutionContext(providers, project.getRootDir().toPath());
        new DevelocityConventions(context).configureDevelocity(new GradleDevelocityConfigurable(develocity));
    }

    private static boolean isGradle6OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("6.0")) >= 0;
    }

    private static boolean isGradle5OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("5.0")) >= 0;
    }

}
