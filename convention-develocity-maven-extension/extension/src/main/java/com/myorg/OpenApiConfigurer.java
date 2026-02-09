package com.myorg;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "openapi-configurer")
public class OpenApiConfigurer extends AbstractMavenLifecycleParticipant {
    private static final String OPENAPI_PLUGIN_ARTIFACT_ID = "openapi-generator-maven-plugin";

    @Override
    public void afterProjectsRead(MavenSession session) {
        String suppressTimestamp = System.getProperty("openapi.suppressTimestampGeneration", "true");
        if (!Boolean.parseBoolean(suppressTimestamp)) {
            return;
        }

        session.getProjects().stream()
                .flatMap(getPluginsWithId(OPENAPI_PLUGIN_ARTIFACT_ID))
                .flatMap(plugin -> plugin.getExecutions().stream())
                .forEach(OpenApiConfigurer::suppressTimestampGeneration);
    }

    private static void suppressTimestampGeneration(PluginExecution pluginExecution) {
        Xpp3Dom configuration = getOrCreateConfiguration(pluginExecution);
        Xpp3Dom configOptions = getChild(configuration, "configOptions");
        Xpp3Dom hideGenerationTimestamp = getChild(configOptions, "hideGenerationTimestamp");
        hideGenerationTimestamp.setValue("true");
    }

    @SuppressWarnings("SameParameterValue")
    private static Function<MavenProject, Stream<Plugin>> getPluginsWithId(String artifactId) {
        return project -> project.getBuild().getPlugins().stream()
                .filter(p -> artifactId.equals(p.getArtifactId()));
    }

    private static Xpp3Dom getOrCreateConfiguration(PluginExecution pluginExecution) {
        return Optional.ofNullable((Xpp3Dom) pluginExecution.getConfiguration())
                .orElseGet(() -> createConfiguration(pluginExecution));
    }

    private static Xpp3Dom createConfiguration(PluginExecution pluginExecution) {
        Xpp3Dom configuration = new Xpp3Dom("configuration");
        pluginExecution.setConfiguration(configuration);
        return configuration;
    }

    private static Xpp3Dom getChild(Xpp3Dom parent, String name) {
        return Optional.ofNullable(parent.getChild(name))
                .orElseGet(() -> createChild(parent, name));
    }

    private static Xpp3Dom createChild(Xpp3Dom parent, String name) {
        Xpp3Dom child = new Xpp3Dom(name);
        parent.addChild(child);
        return child;
    }
}