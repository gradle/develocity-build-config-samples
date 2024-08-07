import com.gradle.develocity.agent.gradle.DevelocityConfiguration
import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * This Gradle script captures the OS processes as reported by the OS 'ps' command,
 * and adds these as a custom value.
 */

project.extensions.configure<DevelocityConfiguration>() {
    buildScan {
        background {
            Capture.captureOsProcesses(buildScan)
        }
    }
}

class Capture {
    companion object {
        fun captureOsProcesses(api: BuildScanConfiguration) {
            val psOutput = execAndGetStdout("ps", "-o pid,ppid,time,command")
            api.value("OS processes", psOutput)
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

