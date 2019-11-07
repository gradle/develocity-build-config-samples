/**
 * This Groovy script adds a custom value for each Maven project in the reactor.
 */

def buildScan = session.lookup('com.gradle.maven.extension.api.scan.BuildScanApi')

buildScan.value('project', project.name)
