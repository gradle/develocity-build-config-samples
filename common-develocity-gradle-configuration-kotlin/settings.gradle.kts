plugins {
    id("com.gradle.enterprise") version "3.16.1"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "1.12.1"
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

    // Use the Develocity connector's access key based authentication.
    // This is available in Develocity 2022.3+ and Develocity Plugin 3.11+.
    remote(gradleEnterprise.buildCache) {
        isEnabled = true
        isPush = isCI
    }

    // Use Gradle's built-in access credentials.
    // This is available in all Develocity and Develocity Plugin versions.
    /**
    remote(HttpBuildCache::class) {
        url = uri("https://develocity-samples.gradle.com/cache/") // adjust to your Develocity server, and note the trailing slash
        isAllowUntrustedServer = false // ensure a trusted certificate is configured
        credentials {
            username = System.getenv("GRADLE_ENTERPRISE_CACHE_USERNAME")
            password = System.getenv("GRADLE_ENTERPRISE_CACHE_PASSWORD")
        }
        isEnabled = true
        isPush = isCI
    }
    */
}

rootProject.name = "common-develocity-gradle-configuration-kotlin" // adjust to your project
