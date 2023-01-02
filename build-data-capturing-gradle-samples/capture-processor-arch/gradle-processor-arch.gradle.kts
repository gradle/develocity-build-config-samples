import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import com.gradle.scan.plugin.BuildScanExtension
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * This Gradle script captures the processor architecture
 * and adds these as a custom value.
 */

project.extensions.configure<GradleEnterpriseExtension>() {
    buildScan {
        background {
            Capture.captureProcessorArch(buildScan)
        }
    }
}

class Capture {
    companion object {
        fun captureProcessorArch(api: BuildScanExtension): Unit {
            val osName = System.getProperty("os.name")
            api.value("os.name", osName)

            val osArch = System.getProperty("os.arch")
            api.value("os.arch", osArch)

            if (isDarwin(osName)) {
                if (isTranslatedByRosetta()) {
                    api.tag("M1-translated")
                } else if (isM1()) {
                    api.tag("M1")
                }
            }
        }

        private fun isDarwin(osName: String): Boolean {
            return osName.contains("OS X") || osName.startsWith("Darwin")
        }

        private fun isM1(): Boolean {
            return execAndGetStdout("uname", "-p") == "arm"
        }

        // On Apple silicon, a universal binary may run either natively or as a translated binary
        // https://developer.apple.com/documentation/apple-silicon/about-the-rosetta-translation-environment#Determine-Whether-Your-App-Is-Running-as-a-Translated-Binary
        private fun isTranslatedByRosetta(): Boolean {
            return execAndGetStdout("sysctl", "sysctl.proc_translated") == "sysctl.proc_translated: 1"
        }

        private fun execAndGetStdout(vararg args: String): String {
            val process = ProcessBuilder(*args)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()
            try {
                val finished = process.waitFor(10, TimeUnit.SECONDS)
                val standardText = process.inputStream.bufferedReader().use { it.readText() }
                val ignore = process.errorStream.bufferedReader().use { it.readText() }

                return if (finished && process.exitValue() == 0) trimAtEnd(standardText) else return ""
            } finally {
                process.destroyForcibly()
            }
        }

        private fun trimAtEnd(str: String): String {
            return ("x" + str).trim().substring(1)
        }
    }
}

