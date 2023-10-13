import com.gradle.enterprise.gradleplugin.GradleEnterpriseExtension

project.extensions.configure<GradleEnterpriseExtension>() {
    buildScan {
        allprojects {
            tasks.withType<Test>().configureEach {
                value("${identityPath}#maxParallelForks", "$maxParallelForks")
            }
        }
    }
}
