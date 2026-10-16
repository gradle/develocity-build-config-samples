package com.myorg;

import com.gradle.develocity.agent.maven.api.DevelocityApi;
import com.gradle.develocity.agent.maven.api.DevelocityListener;
import com.myorg.configurable.MavenDevelocityConfigurable;
import com.myorg.configurable.MavenExecutionContext;
import org.apache.maven.execution.MavenSession;

/**
 * An example Maven extension for enabling and configuring Develocity features.
 */
final class ConventionDevelocityListener implements DevelocityListener {

    @Override
    public void configure(DevelocityApi develocity, MavenSession session) {
        MavenExecutionContext context = new MavenExecutionContext();
        new DevelocityConventions(context).configureDevelocity(new MavenDevelocityConfigurable(develocity));
        // CHANGE ME: Remove the CI check to also retry failed tests in local builds
        if (context.environmentVariable("CI").isPresent()) {
            configureTestRetry(session);
        }
    }

    private static void configureTestRetry(MavenSession session) {
        session.getProjects().forEach(project -> {
            // CHANGE ME: Apply your test retry configuration here
            project.getProperties().putIfAbsent("surefire.rerunFailingTestsCount", "2");
            project.getProperties().putIfAbsent("surefire.failOnFlakeCount", "1");
            project.getProperties().putIfAbsent("failsafe.rerunFailingTestsCount", "2");
            project.getProperties().putIfAbsent("failsafe.failOnFlakeCount", "1");
        });
    }

}
