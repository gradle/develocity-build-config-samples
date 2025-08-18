package com.myorg.configurable;

import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.util.GradleVersion;

import java.lang.reflect.Method;
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
            Provider<String> variable = forUseAtConfigurationTime(providers.environmentVariable(name));
            return Optional.ofNullable(variable.getOrNull());
        }
        return Optional.ofNullable(System.getenv(name));
    }

    // System properties must be accessed differently in some Gradle
    // versions in order to detect changes when configuration cache is enabled.
    @Override
    public Optional<String> systemProperty(String name) {
        if (isGradle65OrNewer() && !isGradle74OrNewer()) {
            Provider<String> property = forUseAtConfigurationTime(providers.systemProperty(name));
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

    private static Provider<String> forUseAtConfigurationTime(Provider<String> provider) {
        if (isGradle65OrNewer() && !isGradle74OrNewer()) {
            try {
                // Use reflection to access the forUseAtConfigurationTime method as it was removed in Gradle 9.
                Method method = Provider.class.getMethod("forUseAtConfigurationTime");
                return (Provider<String>) method.invoke(provider);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke forUseAtConfigurationTime via reflection", e);
            }
        } else {
            return provider;
        }
    }

}
