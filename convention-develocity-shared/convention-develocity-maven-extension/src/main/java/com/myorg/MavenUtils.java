package com.myorg;

import com.gradle.develocity.agent.maven.api.DevelocityApi;

import java.util.function.Supplier;

final class MavenUtils {

    private MavenUtils() {
    }

    static void doNotCachePluginIf(DevelocityApi develocity, String artifactId, String reason, Supplier<Boolean> doNotCacheIf) {
        if (doNotCacheIf.get()) {
            develocity.getBuildCache().registerMojoMetadataProvider(context -> {
                context.withPlugin(artifactId, () -> {
                    context.outputs(outputs -> {
                        outputs.notCacheableBecause(reason);
                    });
                });
            });
        }
    }

    static boolean getBooleanProperty(String propertyName) {
        return Boolean.parseBoolean(System.getProperty(propertyName));
    }

}
