/**
 * This Groovy script captures the Gradle Enterprise Maven extension version as a custom value.
 */

URL url = buildScan.class.classLoader.getResource("com.gradle.scan.plugin.internal.meta.buildAgentVersion.txt")
String buildAgentVersion = url.text
buildScan.value("GE Maven extension version", buildAgentVersion)
