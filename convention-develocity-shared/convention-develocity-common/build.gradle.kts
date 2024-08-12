plugins {
    id("com.myorg.java-conventions")
    id("com.myorg.publishing-conventions")
    id("java-library")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
