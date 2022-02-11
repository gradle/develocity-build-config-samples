import com.gradle.maven.extension.api.scan.BuildScanApi

/**
 * This Groovy script captures the active profiles and add them as tags to the Build Scan.
 */

buildScan.executeOnce('tag-profiles') { BuildScanApi buildScanApi ->
    project.activeProfiles.each { profile -> buildScanApi.tag profile.id }
}
