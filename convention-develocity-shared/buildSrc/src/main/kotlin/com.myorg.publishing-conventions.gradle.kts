plugins {
    id("maven-publish")
}

publishing {
    repositories {
        maven {
            // CHANGE ME: change to point to your organization's artifact repository
            url = uri("https://repo.myorg.com/maven")
        }
    }
}
