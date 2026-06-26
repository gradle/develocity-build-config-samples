plugins {
    id("com.gradle.develocity") version "4.4.3"
    id("com.gradle.common-custom-user-data-gradle-plugin") version "2.6.0"
}

val isCI = System.getenv("CI") != null // adjust to your CI provider
// If your CI runs builds from forked repositories (e.g. GitHub Actions pull_request events),
// secrets are not passed to the runner, causing cache store attempts to fail with HTTP 403.
// In that case, also condition isPush on the access key being present (adjust the env var name):
// val hasCredentials = !System.getenv("DEVELOCITY_ACCESS_KEY").isNullOrEmpty()


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
