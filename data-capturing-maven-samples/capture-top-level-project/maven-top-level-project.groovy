import com.gradle.maven.extension.api.scan.BuildScanApi

/**
 * This Groovy script captures the top-level project name and artifact id,
 * and adds these as custom values.
 */

buildScan.executeOnce('top-level-project') { BuildScanApi buildScanApi ->
    buildScanApi.value 'executionRoot.name', session.topLevelProject.name
    buildScanApi.value 'executionRoot.artifactId', session.topLevelProject.artifactId
}
