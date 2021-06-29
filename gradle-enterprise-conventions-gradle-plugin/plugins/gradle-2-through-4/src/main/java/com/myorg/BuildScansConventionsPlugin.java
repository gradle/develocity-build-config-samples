package com.myorg;

import com.gradle.scan.plugin.BuildScanExtension;
import com.gradle.scan.plugin.BuildScanPlugin;
import groovy.lang.Closure;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ObjectConfigurationAction;

import java.util.HashMap;
import java.util.Map;

public class BuildScansConventionsPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        Map<String, Object> args = new HashMap<>();
        args.put("plugin", BuildScanPlugin.class);
        project.apply(args);

        /* Example of how to configure build scan publishing from the plugin. */
        BuildScanExtension buildScan = project.getExtensions().getByType(BuildScanExtension.class);
        buildScan.setServer("https://ge.myorg.com");
        buildScan.publishAlways();
    }
}
