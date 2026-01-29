package com.myorg;

import com.gradle.develocity.agent.maven.api.DevelocityApi;
import com.gradle.develocity.agent.maven.api.DevelocityListener;
import com.myorg.configurable.MavenDevelocityConfigurable;
import com.myorg.configurable.MavenExecutionContext;
import org.apache.maven.execution.MavenSession;
import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * An example Maven extension for enabling and configuring Develocity features.
 */
final class ConventionDevelocityListener implements DevelocityListener {

    @Override
    public void configure(DevelocityApi develocity, MavenSession session) {
        Path topLevelDirectory = getTopLevelDirectory(session);
        MavenExecutionContext context = new MavenExecutionContext(topLevelDirectory);
        new DevelocityConventions(context).configureDevelocity(new MavenDevelocityConfigurable(develocity));
    }

    @Nullable
    private static Path getTopLevelDirectory(MavenSession session) {
        Path currentDirectory = session.getCurrentProject().getBasedir().toPath();
        while (currentDirectory != null) {
            if (Files.exists(currentDirectory.resolve(".mvn"))) {
                return currentDirectory;
            }

            currentDirectory = currentDirectory.getParent();
        }

        return null;
    }

}
