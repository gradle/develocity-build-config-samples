package com.gradle;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.testing.Test;

import static com.gradle.Utils.*;

final class CustomBuildScanConfigExtras {

    static void configureBuildScan(BuildScanExtension buildScan, Gradle gradle) {
        if (isCaptureEnabled("TestTaskConfiguration")) {
            captureTestTaskConfiguration(buildScan, gradle);
        }
        if (isCaptureEnabled("JvmProcesses")) {
            captureJvmProcesses(buildScan);
        }
        if (isCaptureEnabled("OsProcesses")) {
            captureOsProcesses(buildScan);
        }
    }

    private static boolean isCaptureEnabled(String feature) {
        String featurePropertyValue = System.getProperty("com.gradle.common-custom-user-data.capture" + feature);
        if (featurePropertyValue == null) {
            return false;
        } else if (featurePropertyValue.isEmpty()) {
            // System property with no value enables capture
            return true;
        } else {
            return Boolean.parseBoolean(featurePropertyValue);
        }
    }

    private static void captureTestTaskConfiguration(BuildScanExtension buildScan, Gradle gradle) {
        gradle.allprojects(p ->
                p.getTasks().withType(Test.class).configureEach(test ->
                        test.doFirst("capture configuration for build scans", new Action<Task>() {
                                    @Override
                                    public void execute(Task task) {
                                        buildScan.value(test.getIdentityPath() + "#maxParallelForks", String.valueOf(test.getMaxParallelForks()));
                                        test.getSystemProperties().forEach((key, val) ->
                                                buildScan.value(test.getIdentityPath() + "#sysProps-" + key, hashValue(val)));
                                    }
                                }
                        )
                )
        );
    }

    private static void captureJvmProcesses(BuildScanExtension buildScan) {
        buildScan.background(api -> {
            String psOutput = execAndGetStdOut("jps", "-v");
            if (String.valueOf(psOutput).length() >= 100000) {
                psOutput = execAndGetStdOut("jps", "-m");
            }
            if (isNotEmpty(psOutput)) {
                api.value("JVM processes", psOutput);
            }
        });
    }

    private static void captureOsProcesses(BuildScanExtension buildScan) {
        buildScan.background(api -> {
            String psOutput = execAndGetStdOut("ps", "-o pid,ppid,time,command");
            if (isNotEmpty(psOutput)) {
                api.value("OS processes", psOutput);
            }
        });
    }

    private CustomBuildScanConfigExtras() {
    }

}
