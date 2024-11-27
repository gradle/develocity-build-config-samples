package com.myorg;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.initialization.Settings;
import org.gradle.api.provider.Provider;
import org.gradle.util.GradleVersion;

import java.util.function.Supplier;

final class GradleUtils {

    private GradleUtils() {
    }

    static void configureAllProjects(Settings settings, Action<Project> action) {
        if (isGradle88OrNewer()) {
            settings.getGradle().getLifecycle().beforeProject(action::execute);
        } else {
            settings.getGradle().allprojects(action);
        }
    }

    static void configureAllProjects(Project project, Action<Project> action) {
        project.allprojects(action);
    }

    static <T extends Task> void doNotCacheTaskIf(Project project, Class<T> taskType, String reason, Supplier<Boolean> doNotCacheIf) {
        project.getTasks().withType(taskType).configureEach(task -> task.getOutputs().doNotCacheIf(reason, spec -> doNotCacheIf.get()));
    }

    static Provider<String> getProperty(Project project, String propertyName) {
        if (isGradle62OrNewer()) {
            return project.getProviders().gradleProperty(propertyName);
        } else {
            return project.provider(() -> (String) project.getRootProject().findProperty(propertyName));
        }
    }

    static Provider<Boolean> getBooleanProperty(Project project, String propertyName) {
        if (isGradle56OrNewer()) {
            return getProperty(project, propertyName).map(Boolean::parseBoolean).orElse(false);
        } else {
            return project.provider(() -> Boolean.parseBoolean((String) project.getRootProject().findProperty(propertyName)));
        }
    }

    static boolean isGradle88OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("8.8")) >= 0;
    }

    static boolean isGradle62OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("6.2")) >= 0;
    }

    static boolean isGradle6OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("6.0")) >= 0;
    }

    static boolean isGradle56OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("5.6")) >= 0;
    }

    static boolean isGradle5OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("5.0")) >= 0;
    }

}
