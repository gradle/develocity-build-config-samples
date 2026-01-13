plugins {
    id("com.myorg.java-conventions")
    id("com.myorg.publishing-conventions")
    id("java-library")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.0")
    implementation("com.google.code.gson:gson:2.13.2")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
