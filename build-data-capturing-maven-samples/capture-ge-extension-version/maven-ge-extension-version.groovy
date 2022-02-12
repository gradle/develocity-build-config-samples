/**
 * This Groovy script captures the Gradle Enterprise Maven extension version as a custom value.
 */

InputStream stream = buildScan.class.classLoader.getResourceAsStream("com.gradle.scan.plugin.internal.meta.buildAgentVersion.txt")
String buildAgentVersion = new Scanner(stream).next()
buildScan.value("GE Maven extension version", buildAgentVersion)
