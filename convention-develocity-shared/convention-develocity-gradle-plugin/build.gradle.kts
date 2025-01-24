plugins {
    id("com.myorg.java-conventions")
    id("com.myorg.publishing-conventions")
    id("java-gradle-plugin")
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.gradle:develocity-gradle-plugin:3.19.1")
    implementation("com.gradle:common-custom-user-data-gradle-plugin:2.1")
    implementation(project(":convention-develocity-common"))
}

gradlePlugin {
    plugins {
        create("develocityConventions") {
            // CHANGE ME: change for your organization
            id = "com.myorg.convention-develocity-gradle-plugin"
            displayName = "Convention Develocity Gradle Plugin"
            description = "A Gradle plugin to apply and configure the Develocity Gradle plugin for com.myorg"
            implementationClass = "com.myorg.ConventionDevelocityGradlePlugin"
        }
    }
}

tasks.withType<ValidatePlugins>().configureEach {
    failOnWarning = true
    enableStricterValidation = true
}
