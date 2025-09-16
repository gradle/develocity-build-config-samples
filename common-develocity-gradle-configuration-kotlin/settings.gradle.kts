plugins {
    id("com.gradle.develocity") version "4.1.1"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.4.0"
}

val isCI = System.getenv("CI") != null // adjust to your CI provider

develocity {
    server = "https://develocity-samples.gradle.com" // adjust to your Develocity server
    allowUntrustedServer = false // ensure a trusted certificate is configured

    buildScan {
        uploadInBackground = !isCI
    }
}

buildCache {
    local {
        isEnabled = true
    }

    remote(develocity.buildCache) {
        isEnabled = true
        isPush = isCI
    }
}

rootProject.name = "common-develocity-gradle-configuration-kotlin" // adjust to your project
