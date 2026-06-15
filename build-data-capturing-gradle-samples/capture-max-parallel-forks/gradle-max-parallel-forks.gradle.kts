import com.gradle.develocity.agent.gradle.DevelocityConfiguration

project.extensions.configure<DevelocityConfiguration>() {
    buildScan {
        allprojects {
            tasks.withType<Test>().configureEach {
                value("${identityPath}#maxParallelForks", "$maxParallelForks")
            }
        }
    }
}
