pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

plugins {
    id("com.myorg.convention-develocity-gradle-plugin") version "1.0.0"
}

rootProject.name = "example-build"
