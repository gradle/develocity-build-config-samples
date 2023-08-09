package com.myorg;

import com.gradle.scan.plugin.BuildScanExtension;
import com.gradle.scan.plugin.BuildScanPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.HashMap;
import java.util.Map;

/**
 * An example Gradle plugin for enabling and configuring Gradle Enterprise features (Build Scan only) for
 * Gradle versions 2.0 through Gradle v4.10.3.
 */
public class BuildScansConventionsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        Map<String, Object> args = new HashMap<>();
        args.put("plugin", BuildScanPlugin.class);
        project.apply(args);

        /* Example of how to configure build scan publishing from the plugin. */
        BuildScanExtension buildScan = project.getExtensions().getByType(BuildScanExtension.class);
        buildScan.setServer("https://enterprise-samples.gradle.com");
        buildScan.publishAlways();
    }

}
