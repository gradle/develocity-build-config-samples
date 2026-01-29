package com.myorg;

import com.gradle.CommonCustomUserDataGradlePlugin;
import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import com.gradle.develocity.agent.gradle.DevelocityPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.PluginManager;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.caching.configuration.BuildCacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static com.myorg.GradleUtils.isGradle5OrNewer;
import static com.myorg.GradleUtils.isGradle6OrNewer;

/**
 * An example Gradle plugin for enabling and configuring Develocity features for
 * Gradle versions 5.x and higher.
 */
@SuppressWarnings("LoggingSimilarMessage")
final class ConventionDevelocityGradlePlugin implements Plugin<Object> {

    private static final String develocityPluginVersion = "4.2";
    private static final String commonCustomUserDataPluginVersion = "2.4.0";

    private static final Logger logger = LoggerFactory.getLogger(ConventionDevelocityGradlePlugin.class);

    private final ProviderFactory providers;

    @Inject
    public ConventionDevelocityGradlePlugin(ProviderFactory providers) {
        this.providers = providers;
    }

    @Override
    public void apply(Object target) {
        try {
            if (target instanceof Settings) {
                applySettings((Settings) target);
            } else if (target instanceof Project) {
                applyProject((Project) target);
            }
        } catch (RuntimeException e) {
            logger.warn("Could not apply Develocity: {}", e.getMessage());
        }
    }

    private void applySettings(Settings settings) {
        if (!isGradle6OrNewer()) {
            logger.warn("For Gradle versions prior to 6.0, the Convention Develocity plugin must be applied to the Root project");
        }
        configureGradle6OrNewer(settings);
    }

    private void applyProject(Project project) {
        if (isGradle6OrNewer()) {
            logger.warn("For Gradle versions 6.0 and newer, the Convention Develocity plugin must be applied to Settings");
        } else if (isGradle5OrNewer()) {
            if (!project.equals(project.getRootProject())) {
                logger.warn("For Gradle versions prior to 6.0, the Convention Develocity plugin must be applied to the Root project");
            }
            configureGradle5(project);
        } else {
            logger.warn("For Gradle versions prior to 5.0, the Convention Develocity plugin is not supported");
        }
    }

    private void configureGradle6OrNewer(Settings settings) {
        PluginLoader.resolveAndLoadIntoClassPath(getPluginsToResolve());
        applyPlugins(settings.getPluginManager());
        configureDevelocity(settings.getExtensions(), settings.getBuildCache());
    }

    private void configureGradle5(Project project) {
        PluginLoader.resolveAndLoadIntoClassPath(getPluginsToResolve());
        applyPlugins(project.getPluginManager());
        // build cache is null because it cannot be configured via a plugin prior to Gradle 6.0
        configureDevelocity(project.getExtensions(), null);
    }

    private void configureDevelocity(ExtensionContainer extensions, BuildCacheConfiguration buildCache) {
        DevelocityConfiguration develocity = extensions.getByType(DevelocityConfiguration.class);
        new DevelocityConventions(providers).configureDevelocity(develocity, buildCache);
    }

    private static List<PluginResolver> getPluginsToResolve() {
        String develocityPluginJarUrl = String.format("https://plugins.gradle.org/m2/com/gradle/develocity-gradle-plugin/%1$s/develocity-gradle-plugin-%1$s.jar", develocityPluginVersion);
        String commonCustomUserDataPluginJarUrl = String.format("https://plugins.gradle.org/m2/com/gradle/common-custom-user-data-gradle-plugin/%1$s/common-custom-user-data-gradle-plugin-%1$s.jar", commonCustomUserDataPluginVersion);
        List<PluginResolver> pluginsToResolve = new ArrayList<>();
        pluginsToResolve.add(new GradlePluginPortalPluginResolver(develocityPluginJarUrl));
        pluginsToResolve.add(new GradlePluginPortalPluginResolver(commonCustomUserDataPluginJarUrl));
        return pluginsToResolve;
    }

    private static void applyPlugins(PluginManager pluginManager) {
        pluginManager.apply(DevelocityPlugin.class);
        pluginManager.apply(CommonCustomUserDataGradlePlugin.class);
    }

}
