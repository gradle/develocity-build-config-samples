pluginManagement {
    repositories {
        // CHANGE ME: change to your organization's mirror of the Gradle Plugin Portal
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        // CHANGE ME: change to your organization's mirror of the Gradle Plugin Portal
        gradlePluginPortal()

        // CHANGE ME: change to your organization's mirror of Maven Central
        mavenCentral()
    }
}

rootProject.name = "convention-develocity-shared"

include("convention-develocity-common")
include("convention-develocity-gradle-plugin")
include("convention-develocity-maven-extension")
