package com.myorg;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.util.HashMap;
import java.util.Map;

public class BuildScansConventionsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        Map<String, String> args = new HashMap<>();
        args.put("plugin", "com.gradle.build-scan");
        project.apply(args);

        /* Example of how to configure build scan publishing from the plugin. */
        BuildScanExtension buildScan = project.getExtensions().getByType(BuildScanExtension.class);
        buildScan.setServer("https://ge.myorg.com");
        buildScan.setAllowUntrustedServer(false);
        buildScan.publishAlways();
    }
}
