import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import com.gradle.scan.plugin.BuildScanExtension
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * This Gradle script captures the processor architecture
 * and adds these as a custom value.
 * This should be applied to the root project:
 * <code> apply from: file('gradle-processor-arch.gradle.kts') </code>
 */
project.extensions.configure<GradleEnterpriseExtension>() {
    buildScan {
        background {
            captureProcessorArch(buildScan)
        }
    }
}

fun captureProcessorArch(buildScan: BuildScanExtension): Unit {
  val osName = System.getProperty("os.name")
  buildScan.value("os.name", osName)

  val osArch = System.getProperty("os.arch")
  buildScan.value("os.arch", osArch)

  if (isDarwin(osName)) {
    if (isTranslatedByRosetta()) {
      buildScan.tag("M1-translated")
    } else if (isM1()) {
      buildScan.tag("M1")
    }
  }
}

fun isDarwin(osName: String): Boolean {
  return osName.contains("OS X") || osName.startsWith("Darwin")
}

fun isM1(): Boolean {
  return execAndGetStdout("uname", "-p") == "arm"
}

// On Apple silicon, a universal binary may run either natively or as a translated binary
// https://developer.apple.com/documentation/apple-silicon/about-the-rosetta-translation-environment#Determine-Whether-Your-App-Is-Running-as-a-Translated-Binary
fun isTranslatedByRosetta(): Boolean {
  return execAndGetStdout("sysctl", "sysctl.proc_translated") == "sysctl.proc_translated: 1"
}

fun execAndGetStdout(vararg args: String): String {
    val process = ProcessBuilder(*args)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
    try {
            val finished = process.waitFor(10, TimeUnit.SECONDS)
            val standardText = process.inputStream.bufferedReader().readText()
            val ignore = process.errorStream.bufferedReader().readText()

            return if (finished && process.exitValue() == 0) trimAtEnd(standardText) else return ""
    } finally {
        process.destroyForcibly()
    }
}

fun trimAtEnd(str: String): String {
    return ("x" + str).trim().substring(1)
}
