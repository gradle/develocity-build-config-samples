package com.myorg;

import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.util.GradleVersion;

import java.lang.reflect.Method;
import java.util.Optional;

@SuppressWarnings({"JavaReflectionMemberAccess", "unchecked", "BooleanMethodIsAlwaysInverted"})
final class GradleUtils {
    
    private GradleUtils() {
    }

    static Optional<String> environmentVariable(String name, ProviderFactory providers) {
        if (isGradle65OrNewer() && !isGradle74OrNewer()) {
            Provider<String> variable = forUseAtConfigurationTime(providers.environmentVariable(name));
            return Optional.ofNullable(variable.getOrNull());
        }
        return Optional.ofNullable(System.getenv(name));
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

    static boolean isGradle74OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("7.4")) >= 0;
    }

    static boolean isGradle65OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("6.5")) >= 0;
    }

    static boolean isGradle6OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("6.0")) >= 0;
    }

    static boolean isGradle5OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("5.0")) >= 0;
    }

}
