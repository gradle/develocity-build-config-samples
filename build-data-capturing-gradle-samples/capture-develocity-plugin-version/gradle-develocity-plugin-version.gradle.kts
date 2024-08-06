import com.gradle.develocity.agent.gradle.DevelocityConfiguration

/**
 * This Kotlin script captures the Develocity Gradle plugin version as a custom value.
 */

project.extensions.configure<DevelocityConfiguration> {
    buildScan {
        val url = DevelocityConfiguration::class.java.classLoader.getResource("com.gradle.scan.plugin.internal.meta.buildAgentVersion.txt")
        val buildAgentVersion = url.readText()
        value("Develocity Gradle plugin version", buildAgentVersion)
    }
}

