package com.gradle;

import com.gradle.scan.plugin.BuildScanExtension;
import org.gradle.api.invocation.Gradle;

import static com.gradle.Utils.execAndGetStdOut;
import static com.gradle.Utils.isNullOrEmpty;

final class CustomBuildScanConfigExtras {

    static void configureBuildScan(BuildScanExtension buildScan, Gradle gradle) {
        if (isCaptureEnabled("JvmProcesses", false)) {
            captureJvmProcesses(buildScan);
        }
        if (isCaptureEnabled("OsProcesses", false)) {
            captureOsProcesses(buildScan);
        }
    }

    private static boolean isCaptureEnabled(String feature, boolean enabledByDefault) {
        String featurePropertyValue = System.getProperty("com.gradle.common-custom-user-data.capture" + feature);
        if (featurePropertyValue == null) {
            return enabledByDefault;
        } else if (featurePropertyValue.isEmpty()) {
            // System property with no value enables capture
            return true;
        } else {
            return Boolean.parseBoolean(featurePropertyValue);
        }
    }
    
    private static void captureJvmProcesses(BuildScanExtension buildScan) {
        buildScan.background(api -> {
            String psOutput = execAndGetStdOut("jps", "-v");
            if (String.valueOf(psOutput).length() >= 100000) {
                psOutput = execAndGetStdOut("jps", "-m");
            }
            if (!isNullOrEmpty(psOutput)) {
                api.value("JVM processes", psOutput);
            }
        });
    }

    private static void captureOsProcesses(BuildScanExtension buildScan) {
        buildScan.background(api -> {
            String psOutput = execAndGetStdOut("ps", "-o pid,ppid,time,command");
            if (!isNullOrEmpty(psOutput)) {
                api.value("OS processes", psOutput);
            }
        });
    }

    private CustomBuildScanConfigExtras() {
    }

}
