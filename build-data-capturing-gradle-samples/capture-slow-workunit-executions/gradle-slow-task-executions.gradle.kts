import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import com.gradle.scan.plugin.BuildScanExtension

/**
 * This Gradle script captures all tasks of a given type taking longer to execute than a certain threshold,
 * and adds these as custom values.
 * This should be applied to the root project:
 * <code> apply from: file('gradle-slow-task-executions.gradle.kts') </code>
 */
project.extensions.configure<GradleEnterpriseExtension>() {
    buildScan {
        val THRESHOLD_MILLIS = 15 * 60 * 1000 // 15 min

        allprojects {
            tasks.withType<JavaCompile> {
                var start = 0L
                doFirst {
                    start = System.currentTimeMillis()
                }
                doLast {
                    val duration = System.currentTimeMillis() - start
                    if (duration > THRESHOLD_MILLIS) {
                        value("Slow task", identityPath.toString())
                    }
                }
            }
        }
    }
}
