import com.gradle.maven.extension.api.scan.BuildScanApi

/**
 * This Groovy script captures the Gradle Enterprise Maven Extension version as a custom value.
 */

InputStream stream = BuildScanApi.class.classLoader.getResourceAsStream("com.gradle.scan.plugin.internal.meta.buildAgentVersion.txt")
String buildAgentVersion = new Scanner(stream).next()
buildScan.value("GE Maven extension version", buildAgentVersion)
