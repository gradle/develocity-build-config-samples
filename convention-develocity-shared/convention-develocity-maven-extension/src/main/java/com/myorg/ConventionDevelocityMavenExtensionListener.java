package com.myorg;

import com.gradle.develocity.agent.maven.api.DevelocityApi;
import com.gradle.develocity.agent.maven.api.DevelocityListener;
import com.myorg.configurable.MavenDevelocityConfigurable;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;

/**
 * An example Maven extension for enabling and configuring Develocity features.
 */
@Component(
        role = DevelocityListener.class,
        hint = "convention-develocity-maven-extension",
        description = "Configures the Develocity Maven extension for com.myorg"
)
final class ConventionDevelocityMavenExtensionListener implements DevelocityListener {

    @Override
    public void configure(DevelocityApi develocity, MavenSession session) {
        new DevelocityConventions().configureDevelocity(new MavenDevelocityConfigurable(develocity));
    }

}
