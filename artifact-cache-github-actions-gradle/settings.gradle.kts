pluginManagement {
    includeBuild("build-logic") // shared build logic, which the Setup Cache caches in compiled form
}

plugins {
    id("com.gradle.develocity") version "4.6.1"
}

develocity {
    server = "https://develocity.example.com" // adjust to your Develocity server
}

rootProject.name = "artifact-cache-github-actions-gradle"
