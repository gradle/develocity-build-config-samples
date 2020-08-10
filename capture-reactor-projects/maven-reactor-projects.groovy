/**
 * This Groovy script adds a custom value for each Maven project in the reactor.
 */

BuildScanApi buildScan = session.lookup('com.gradle.maven.extension.api.scan.BuildScanApi')
if (!buildScan) {
    return
}
buildScan.value('Project', project.name)
