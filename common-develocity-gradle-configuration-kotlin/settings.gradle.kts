plugins {
    id("com.gradle.enterprise") version "3.17"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "1.13"
}

val isCI = !System.getenv("CI").isNullOrEmpty() // adjust to your CI provider

gradleEnterprise {
    server = "https://develocity-samples.gradle.com" // adjust to your Develocity server
    allowUntrustedServer = false // ensure a trusted certificate is configured

    buildScan {
        capture { isTaskInputFiles = true }
        isUploadInBackground = !isCI
        publishAlways()
    }
}

buildCache {
    local {
        isEnabled = true
    }

    remote(gradleEnterprise.buildCache) {
        isEnabled = true
        isPush = isCI
    }
}

rootProject.name = "common-develocity-gradle-configuration-kotlin" // adjust to your project
