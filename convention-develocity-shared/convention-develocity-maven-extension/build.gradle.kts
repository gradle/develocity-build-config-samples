plugins {
    id("com.myorg.java-conventions")
    id("com.myorg.publishing-conventions")
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.apache.maven:maven-core:3.9.8")
    compileOnly("org.codehaus.plexus:plexus-component-annotations:2.2.0")

    implementation("com.gradle:develocity-maven-extension:1.22")
    implementation("com.gradle:common-custom-user-data-maven-extension:2.0")
    implementation(project(":convention-develocity-common"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
