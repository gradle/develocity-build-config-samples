import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    id("java")
}

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
    }
}
