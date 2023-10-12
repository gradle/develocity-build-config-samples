import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension

project.extensions.configure<GradleEnterpriseExtension>() {
    buildScan {
        gradle.afterProject {
            tasks.withType<Test>().configureEach {
                value("${identityPath}#maxParallelForks", "$maxParallelForks")
            }
        }
    }
}
