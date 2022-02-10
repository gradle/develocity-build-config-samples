import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import com.gradle.maven.extension.api.scan.BuildScanApi

/**
 * This Groovy script captures the OS processes as reported by the OS 'ps' command,
 * and adds these as a custom value.
 */
URL url = BuildScanApi.class.getClassLoader().getResource("com.gradle.scan.plugin.internal.meta.buildAgentVersion.txt");
String buildAgentVersion = Resources.toString(url, Charsets.UTF_8);
buildScan.value("GE Maven extension version", buildAgentVersion);
