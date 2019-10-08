/**
 * This Groovy script captures OS processes as reported by the OS 'ps' command, as custom value.
 */

def buildScan = session.lookup("com.gradle.maven.extension.api.scan.BuildScanApi")

addStringMethods()

buildScan.executeOnce('os-processes') { api ->
    addPsMetadata(api)
}

static void addPsMetadata(api){
    api.background { bck ->
        def psOutput = execAndGetStdout('ps', '-o pid,ppid,time,command')
        bck.value 'OS processes', psOutput
    }
}

static String execAndGetStdout(String... args) {
    def exec = args.toList().execute()
    exec.waitFor()
    exec.text.trimAtEnd()
}

static void addStringMethods() {
    if (!String.metaClass.respondsTo('', 'trimAtEnd')) {
        String.metaClass.trimAtEnd = {
            ('x' + delegate).trim().substring(1)
        }
    }
}
