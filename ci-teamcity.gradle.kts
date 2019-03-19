// Adding custom links to your scans is easy. Here, we're adding a link from the build scan to the
// build on your TeamCity CI server. If you're using the Gradle plugin for Jenkins, then there will
// also be a link to the build scan from Jenkins, in which case you will now have a two-way connection
// between your build scan and your CI server.
val buildUrl = System.getenv("CI_BUILD_URL")
if (buildUrl != null) {
    buildScan {
        link("CI Build", buildUrl)
    }
}
