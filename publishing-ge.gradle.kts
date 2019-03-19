// See the Gradle Build Scan User Manual for more information: https://docs.gradle.com/build-scan-plugin/
buildScan {
    // Publish a build scan for each build.
    publishAlways()

    // Specify your Gradle Enterprise server, if you have one.
    // Useful for the script to inject a custom value, but in your case, replace System.getProperty
    // with a direct reference to your GE server. For example, "https://gradle.yourcompany.com"
    val geServer = System.getProperty("com.gradle.scan.server")
    if (geServer != null) {
        server = geServer
    }
}
