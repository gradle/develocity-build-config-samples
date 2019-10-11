/**
 * This Groovy script captures OS processes as reported by the OS 'ps' command, as custom value.
 */

def buildScan = session.lookup('com.gradle.maven.extension.api.scan.BuildScanApi')

buildScan.executeOnce('os-processes') { api ->
    addPsMetadata(api)
}

static void addPsMetadata(api) {
    api.background { bck ->
        def psOutput = execAndGetStdout('ps', '-o pid,ppid,time,command')
        bck.value 'OS processes', psOutput
    }
}

static String execAndGetStdout(String... args) {
    def exec = args.toList().execute()
    exec.waitFor()
    trimAtEnd(exec.text)
}

static String trimAtEnd(String str) {
    ('x' + str).trim().substring(1)
}
