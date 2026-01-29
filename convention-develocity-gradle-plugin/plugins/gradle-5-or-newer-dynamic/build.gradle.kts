plugins {
    id("java-gradle-plugin")
    id("maven-publish")
}

// CHANGE ME: change to your organization's group ID
group = "com.myorg"
version = "1.0"

repositories {
    gradlePluginPortal()
}

dependencies {
    compileOnly("com.gradle:develocity-gradle-plugin:4.2.1")
    compileOnly("com.gradle:common-custom-user-data-gradle-plugin:2.4.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}

gradlePlugin {
    plugins {
        create("develocityConventions") {
            // CHANGE ME: change for your organization
            id = "com.myorg.convention-develocity-gradle-5-or-newer"
            displayName = "Develocity Convention Plugin for Gradle 5.0 and higher"
            description = "A Gradle plugin to apply and configure the Develocity Gradle plugin for com.myorg"
            implementationClass = "com.myorg.ConventionDevelocityGradlePlugin"
        }
    }
}

tasks.withType<ValidatePlugins>().configureEach {
    failOnWarning = true
    enableStricterValidation = true
}
