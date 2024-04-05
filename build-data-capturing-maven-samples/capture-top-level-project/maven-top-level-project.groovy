import com.gradle.develocity.agent.maven.adapters.BuildScanApiAdapter

/**
 * This Groovy script captures the top-level project name and artifact id,
 * and adds these as custom values.
 */

buildScan.executeOnce('top-level-project') { BuildScanApiAdapter buildScanApi ->
    buildScanApi.value 'executionRoot.name', session.topLevelProject.name
    buildScanApi.value 'executionRoot.artifactId', session.topLevelProject.artifactId
}
