import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import org.gradle.api.internal.artifacts.configurations.ConfigurationInternal
import org.gradle.api.internal.artifacts.configurations.ResolutionStrategyInternal

/**
 * This Gradle script captures any dependency configurations that are resolved early to build the task graph,
 * and adds these as custom values.
 * This should be applied to the root project:
 * <code> apply from: file('gradle-dependency-resolution.gradle.kts') </code>
 */
project.extensions.configure<GradleEnterpriseExtension>() {
    buildScan {
        buildFinished {
            val confs = mutableListOf<String>()
            allprojects {
                configurations.forEach {
                    if (it is ConfigurationInternal) {
                        if (it.getState() == Configuration.State.RESOLVED && (it.getResolutionStrategy() as ResolutionStrategyInternal).resolveGraphToDetermineTaskDependencies()) {
                            confs.add(it.getIdentityPath().toString())
                        }
                    }
                }
            }

            confs.forEach {
                value("Configuration resolved for task graph calculation", it)
            }
        }
    }
}
