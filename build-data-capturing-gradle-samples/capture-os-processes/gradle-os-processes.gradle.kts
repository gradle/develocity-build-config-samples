import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension
import com.gradle.scan.plugin.BuildScanExtension
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * This Gradle script captures the OS processes as reported by the OS 'ps' command,
 * and adds these as a custom value.
 * This should be applied to the root project:
 * <code> apply from: file('gradle-os-processes.gradle.kts') </code>
 */
project.extensions.configure<GradleEnterpriseExtension>() {
    buildScan {
        background {
            captureOsProcesses(buildScan)
        }
    }
}

fun captureOsProcesses(buildScan: BuildScanExtension): Unit {
    val psOutput = execAndGetStdout("ps", "-o pid,ppid,time,command")
    buildScan.value("OS processes", psOutput)
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
