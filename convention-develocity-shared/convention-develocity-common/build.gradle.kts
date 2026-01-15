import com.myorg.EmbedAccessToken

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

val embedAccessToken = tasks.register<EmbedAccessToken>("embedAccessToken") {
    // CHANGE ME: Apply your GitHub access token here
    accessToken = providers.environmentVariable("GITHUB_API_KEY").orElse("")
    outputDirectory = layout.buildDirectory.dir(name)
}

sourceSets {
    main {
        java {
            srcDir(embedAccessToken)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
