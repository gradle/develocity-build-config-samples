plugins {
    id("com.myorg.java-conventions")
    id("com.myorg.publishing-conventions")
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.apache.maven:maven-core:3.9.9")

    implementation("com.gradle:develocity-maven-extension:1.22.1")
    implementation("com.gradle:common-custom-user-data-maven-extension:2.0.1")
    implementation(project(":convention-develocity-common"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
