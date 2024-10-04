package com.myorg;

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration;
import org.gradle.api.Action;
import org.gradle.api.Task;
import org.gradle.api.invocation.Gradle;
import org.gradle.api.tasks.testing.Test;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"Convert2Lambda", "CodeBlock2Expr"})
final class CustomGradleBuildScanEnhancements {

    private final BuildScanConfiguration buildScan;
    private final Gradle gradle;

    public CustomGradleBuildScanEnhancements(BuildScanConfiguration buildScan, Gradle gradle) {
        this.buildScan = buildScan;
        this.gradle = gradle;
    }

    public void apply() {
        captureIgnoreTestFailures();
    }

    private void captureIgnoreTestFailures() {
        gradle.afterProject(project -> {
            project.getTasks().withType(Test.class).configureEach(captureIgnoreTestFailures(buildScan));
        });
    }

    private static Action<Test> captureIgnoreTestFailures(BuildScanConfiguration buildScan) {
        return test -> {
            test.doFirst(new Action<Task>() {
                // use anonymous inner class to keep Test task instance cacheable
                // additionally, using lambdas as task actions is deprecated
                @Override
                public void execute(@NotNull Task task) {
                    if (test.getIgnoreFailures()) {
                        buildScan.value(test.getIdentityPath() + "#ignoreFailures", "true");
                    }
                }
            });
        };
    }

}
