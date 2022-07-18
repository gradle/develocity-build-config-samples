import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension

/**
 * This Groovy script captures the Gradle Enterprise Gradle plugin version as a custom value.
 *
 * <code> apply from: file('gradle-ge-plugin-version.gradle.kts') </code>
 */
project.extensions.configure<GradleEnterpriseExtension>() {
    buildScan {
        val url = GradleEnterpriseExtension::class.java.classLoader.getResource("com.gradle.scan.plugin.internal.meta.buildAgentVersion.txt")
        val buildAgentVersion = url.readText()
        value("GE Gradle plugin version", buildAgentVersion)
    }
}

