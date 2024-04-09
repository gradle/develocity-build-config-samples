import com.gradle.develocity.agent.maven.adapters.BuildScanApiAdapter

/**
 * This Groovy script captures the active profiles and add them as tags to the Build Scan.
 */

buildScan.executeOnce('tag-profiles') { BuildScanApiAdapter buildScanApi ->
    project.activeProfiles.each { profile -> buildScanApi.tag profile.id }
}
