plugins {
    id("com.gradle.enterprise") version("3.11.1")
    id("com.gradle.common-custom-user-data-gradle-plugin") version("1.8")
}

val isCI = !System.getenv("CI").isNullOrEmpty() // adjust to your CI provider

gradleEnterprise {
    server = "https://enterprise-samples.gradle.com" // adjust to your GE server
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

    remote<HttpBuildCache> {
        url = uri("https://enterprise-samples.gradle.com/cache/") // adjust to your GE server, and note the trailing slash
        isAllowUntrustedServer = false // ensure a trusted certificate is configured
        credentials {
            username = System.getenv("GRADLE_ENTERPRISE_CACHE_USERNAME")
            password = System.getenv("GRADLE_ENTERPRISE_CACHE_PASSWORD")
        }
        isEnabled = true
        isPush = isCI
    }
}

rootProject.name = "common-gradle-enterprise-gradle-configuration" // adjust to your project
