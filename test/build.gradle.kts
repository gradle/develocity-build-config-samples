plugins {
    id("java")
    id("com.gradle.build-scan") version "2.2.1"
    id("java-gradle-plugin")
    id("groovy")
}

group = "org.gradle"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.12")
    testImplementation("org.spockframework:spock-core:1.3-groovy-2.5") {
        exclude(module = "groovy-all")
    }
}

apply(from = "../publishing-ge.gradle")
apply(from = "../capture-task-input-files.gradle")
apply(from = "../tags-basic.gradle")
apply(from = "../tags-android.gradle")
//apply(from = "../git-all.gradle")
//apply(from = "../gist.gradle")

// Put your gistToken, if you have one, in ~/.gradle/gradle.properties, and this will ensure that value gets into
// the test as a System property.
tasks.named<Test>("test").configure {
    systemProperty("gistToken", findProperty("gistToken") ?: "")
}
