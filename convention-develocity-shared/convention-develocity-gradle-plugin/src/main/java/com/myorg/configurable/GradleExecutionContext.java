package com.myorg.configurable;

import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.util.GradleVersion;

import java.util.Optional;

public final class GradleExecutionContext implements ExecutionContext {

    private final ProviderFactory providers;

    public GradleExecutionContext(ProviderFactory providers) {
        this.providers = providers;
    }

    // Environment variables must be accessed differently in some Gradle
    // versions in order to detect changes when configuration cache is enabled.
    @Override
    public Optional<String> environmentVariable(String name) {
        if (isGradle65OrNewer() && !isGradle74OrNewer()) {
            @SuppressWarnings("deprecation") Provider<String> variable = providers.environmentVariable(name).forUseAtConfigurationTime();
            return Optional.ofNullable(variable.getOrNull());
        }
        return Optional.ofNullable(System.getenv(name));
    }

    // System properties must be accessed differently in some Gradle
    // versions in order to detect changes when configuration cache is enabled.
    @Override
    public Optional<String> systemProperty(String name) {
        if (isGradle65OrNewer() && !isGradle74OrNewer()) {
            @SuppressWarnings("deprecation") Provider<String> property = providers.systemProperty(name).forUseAtConfigurationTime();
            return Optional.ofNullable(property.getOrNull());
        }
        return Optional.ofNullable(System.getProperty(name));
    }

    private static boolean isGradle65OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("6.5")) >= 0;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isGradle74OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("7.4")) >= 0;
    }

}
