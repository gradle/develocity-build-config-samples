/**
 * This Groovy script adds a custom value for each Maven project in the reactor.
 */

def buildScan = session.lookup('com.gradle.maven.extension.api.scan.BuildScanApi')

buildScan.executeOnce('maven-reactor-projects') { api ->
    session.projects.each {
        api.value('project', it.name)
    }
}
