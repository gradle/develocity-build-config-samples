import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import com.gradle.develocity.agent.maven.adapters.BuildScanApiAdapter

/**
 * This Groovy script captures the OS processes as reported by the OS 'ps' command,
 * and adds these as a custom value.
 */

buildScan.executeOnce('os-processes') { BuildScanApiAdapter buildScanApi ->
    buildScanApi.background { api ->
        captureOsProcesses(api)
    }
}

static void captureOsProcesses(def api) {
    def psOutput = execAndGetStdout('ps', '-o pid,ppid,time,command')
    api.value 'OS processes', psOutput
}

static String execAndGetStdout(String... args) {
    Process process = args.toList().execute()
    try {
        def standardText = process.inputStream.withStream { s -> s.getText(Charset.defaultCharset().name()) }
        def ignore = process.errorStream.withStream { s -> s.getText(Charset.defaultCharset().name()) }

        def finished = process.waitFor(10, TimeUnit.SECONDS)
        finished && process.exitValue() == 0 ? trimAtEnd(standardText) : null
    } finally {
        process.destroyForcibly()
    }
}

static String trimAtEnd(String str) {
    ('x' + str).trim().substring(1)
}
