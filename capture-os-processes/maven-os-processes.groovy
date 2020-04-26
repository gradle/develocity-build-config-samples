import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * This Groovy script captures OS processes as reported by the OS 'ps' command, as custom value.
 */

BuildScanApi buildScan = session.lookup('com.gradle.maven.extension.api.scan.BuildScanApi')
if (!buildScan) {
    return
}

buildScan.executeOnce('os-processes') { BuildScanApi api ->
    addPsOutput(api)
}

static void addPsOutput(def buildScan) {
    buildScan.background { BuildScanApi api ->
        def psOutput = execAndGetStdout('ps', '-o pid,ppid,time,command')
        api.value 'OS processes', psOutput
    }
}

static String execAndGetStdout(String... args) {
    Process process = args.toList().execute()
    try {
        def standardText = process.getInputStream().getText(Charset.defaultCharset().name())
        def ignore = process.getErrorStream().getText(Charset.defaultCharset().name())

        def finished = process.waitFor(10, TimeUnit.SECONDS)
        finished && process.exitValue() == 0 ? trimAtEnd(standardText) : null
    } finally {
        process.destroyForcibly()
    }
}

static String trimAtEnd(String str) {
    ('x' + str).trim().substring(1)
}
