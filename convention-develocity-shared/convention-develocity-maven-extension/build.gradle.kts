plugins {
    id("com.myorg.java-conventions")
    id("com.myorg.publishing-conventions")
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.apache.maven:maven-core:3.9.12")
    implementation("com.gradle:develocity-maven-extension:2.3.4")
    implementation("com.gradle:common-custom-user-data-maven-extension:2.1.0")
    implementation(project(":convention-develocity-common"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.jar {
    from(zipTree(configurations.compileClasspath.map { it.files.first { jar -> jar.name.contains("develocity-maven-extension") }})) {
        include("META-INF/maven/extension.xml")
    }
}
