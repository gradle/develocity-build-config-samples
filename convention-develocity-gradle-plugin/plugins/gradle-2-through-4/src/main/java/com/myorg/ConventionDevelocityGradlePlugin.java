package com.myorg;

import com.gradle.CommonCustomUserDataGradlePlugin;
import com.gradle.scan.plugin.BuildScanExtension;
import com.gradle.scan.plugin.BuildScanPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.util.GradleVersion;

import java.util.HashMap;
import java.util.Map;

/**
 * An example Gradle plugin for enabling and configuring Develocity features (Build Scan only) for
 * Gradle versions 2.0 through Gradle v4.10.3.
 */
public class ConventionDevelocityGradlePlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        Map<String, Object> args = new HashMap<>();
        args.put("plugin", BuildScanPlugin.class);
        project.apply(args);

        if (isGradle4OrNewer()) {
            args = new HashMap<>();
            args.put("plugin", CommonCustomUserDataGradlePlugin.class);
            project.apply(args);
        }

        /* Example of how to configure build scan publishing from the plugin. */
        BuildScanExtension buildScan = project.getExtensions().getByType(BuildScanExtension.class);
        buildScan.setServer("https://develocity-samples.gradle.com");
        buildScan.publishAlways();
    }

    private static boolean isGradle4OrNewer() {
        return GradleVersion.current().compareTo(GradleVersion.version("4.0")) >= 0;
    }

}
