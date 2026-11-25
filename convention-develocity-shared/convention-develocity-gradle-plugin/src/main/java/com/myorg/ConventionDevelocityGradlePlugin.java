package com.myorg;

import com.gradle.CommonCustomUserDataGradlePlugin;
import com.gradle.develocity.agent.gradle.DevelocityConfiguration;
import com.gradle.develocity.agent.gradle.DevelocityPlugin;
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import com.gradle.develocity.agent.gradle.test.DevelocityTestConfiguration;
import com.myorg.configurable.GradleDevelocityConfigurable;
import com.myorg.configurable.GradleExecutionContext;
import org.gradle.StartParameter;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.testing.Test;
import org.gradle.util.GradleVersion;

import javax.inject.Inject;

/**
 * An example Gradle plugin for enabling and configuring Develocity features
 */
final class ConventionDevelocityGradlePlugin implements Plugin<Object> {

    private final ProviderFactory providers;
    private final ObjectFactory objects;

    @Inject
    public ConventionDevelocityGradlePlugin(ProviderFactory providers, ObjectFactory objects) {
        this.providers = providers;
        this.objects = objects;
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
        configureTestRetry(settings.getGradle(), context);
    }

    private void configureGradle5(Project project) {
        project.getPluginManager().apply(DevelocityPlugin.class);
        project.getPluginManager().apply(CommonCustomUserDataGradlePlugin.class);
        DevelocityConfiguration develocity = project.getExtensions().getByType(DevelocityConfiguration.class);
        GradleExecutionContext context = new GradleExecutionContext(providers);
        new DevelocityConventions(context).configureDevelocity(new GradleDevelocityConfigurable(develocity));
        configureTestRetry(project.getGradle(), context);
    }

    private void configureTestRetry(Gradle gradle, GradleExecutionContext context) {
        // CHANGE ME: Remove the CI check to also retry failed tests in local builds
        if (!context.environmentVariable("CI").isPresent()) {
            return;
        }
        if (isGradle88OrNewer() && IsolatedTestRetryAction.isIsolatedProjectsActive(objects)) {
            IsolatedTestRetryAction.register(gradle);
        } else {
            gradle.allprojects(ConventionDevelocityGradlePlugin::applyTestRetry);
        }
    }

    static void applyTestRetry(Project project) {
        project.getTasks().withType(Test.class).configureEach(test ->
            test.getExtensions().getByType(DevelocityTestConfiguration.class).testRetry(testRetry -> {
                // CHANGE ME: Apply your test retry configuration here
                testRetry.getMaxRetries().set(2);
                testRetry.getFailOnPassedAfterRetry().set(true);
            }));
    }

    private static boolean isGradle6OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("6.0")) >= 0;
    }

    private static boolean isGradle5OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("5.0")) >= 0;
    }

    private static boolean isGradle88OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("8.8")) >= 0;
    }

}
