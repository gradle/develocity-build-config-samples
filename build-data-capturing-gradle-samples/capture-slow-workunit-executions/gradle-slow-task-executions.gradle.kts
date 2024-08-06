import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration

/**
 * This Gradle script captures all tasks of a given type taking longer to execute than a certain threshold,
 * and adds these as custom values.
 */

project.extensions.configure<DevelocityConfiguration>() {
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
        fun addbuildScanValue(api: BuildScanConfiguration, key: String, value: String) {
            api.value(key, value)
        }
    }
}
