import java.nio.charset.Charset
import java.util.concurrent.TimeUnit
import com.gradle.maven.extension.api.scan.BuildScanApi

/**
 * This Groovy script captures the processor architecture
 * and adds these as a custom value.
 */

buildScan.executeOnce('processor-arch') { BuildScanApi buildScanApi ->
    buildScanApi.background { api ->
        captureProcessorArch(api)
    }
}

static void captureProcessorArch(def api) {
    def osName = System.getProperty("os.name")
    api.value("os.name", osName)

    def osArch = System.getProperty("os.arch")
    api.value("os.arch", osArch)

    if (isDarwin(osName)) {
        if (isM1()) {
            api.tag("M1")
        }
        if (isTranslatedByRosetta()) {
            api.tag("M1-translated")
        }
    }
}

static boolean isDarwin(String osName) {
    return osName.contains("OS X") || osName.startsWith("Darwin")
}

static boolean isM1() {
    return execAndGetStdout("uname", "-p") == "arm"
}

// On Apple silicon, a universal binary may run either natively or as a translated binary
// https://developer.apple.com/documentation/apple-silicon/about-the-rosetta-translation-environment#Determine-Whether-Your-App-Is-Running-as-a-Translated-Binary
static boolean isTranslatedByRosetta() {
    return execAndGetStdout("sysctl", "sysctl.proc_translated") == "sysctl.proc_translated: 1"
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
