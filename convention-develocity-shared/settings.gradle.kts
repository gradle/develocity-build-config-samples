plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "convention-develocity-shared"

include("convention-develocity-common")
include("convention-develocity-gradle-plugin")
include("convention-develocity-maven-extension")
