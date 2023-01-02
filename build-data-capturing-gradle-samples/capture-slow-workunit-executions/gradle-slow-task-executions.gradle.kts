import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import com.gradle.scan.plugin.BuildScanExtension

/**
 * This Gradle script captures all tasks of a given type taking longer to execute than a certain threshold,
 * and adds these as custom values.
 */

project.extensions.configure<GradleEnterpriseExtension>() {
    buildScan {
        val THRESHOLD_MILLIS = 15 * 60 * 1000 // 15 min
        val api = buildScan
        allprojects {
            tasks.withType<JavaCompile>().configureEach {
                var start = 0L
                doFirst {
                    start = System.currentTimeMillis()
                }
                doLast {
                    val duration = System.currentTimeMillis() - start
                    if (duration > THRESHOLD_MILLIS) {
                        Capture.addbuildScanValue(api, "Slow task", identityPath.toString())
                    }
                }
            }
        }
    }
}

class Capture {
    companion object {
        fun addbuildScanValue(api: BuildScanExtension, key: String, value: String): Unit {
            api.value(key, value)
        }
    }
}
