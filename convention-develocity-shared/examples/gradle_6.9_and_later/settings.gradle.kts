pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

plugins {
    id("com.myorg.convention-develocity-gradle-plugin") version "1.+"
}

rootProject.name = "example-build"
