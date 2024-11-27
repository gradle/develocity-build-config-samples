plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
