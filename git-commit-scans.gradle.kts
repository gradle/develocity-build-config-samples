// With build can plugin v1.15+, you can easily add expensive data inside the background {} block. Everything inside
// here is executed in a separate thread, and guaranteed to complete before finishing the build and publishing the
// scan.
buildScan.background {
    // We can also use the git commit ID for generating a custom link, useful in Gradle Enterprise for filtering for
    // all scans built from the same git commit. This will create a URL that looks like:
    // https://«geServer»/scans?search.names=Git%20Commit%20ID&search.values=«gitCommitId»
    // This pattern can be used for creating custom links for arbitrary key-value pairs that you track with built
    // scan custom values.
    // It's simple to capture values from git
    // `execAndGetStdout` is a custom method defined below.
    val gitCommitId = execAndGetStdout("git", "rev-parse", "--verify", "HEAD")

    // In order to be able to create this custom link, we need a custom value with the expected key name.
    value("Git Commit ID", gitCommitId)
    link("Git commit scans", customValueSearchUrl(mapOf("Git Commit ID" to gitCommitId)))
}

// Build scans can be extended with custom values, which are just key-value pairs. This method builds
// a url that includes a search for a specific key-value pair. Because every aspect of the Gradle
// Enterprise UI is specified by the URL, this enables some pretty clever things.
fun customValueSearchUrl(search: Map<String, String>): String {
    val query = search.map { (name, value) ->
        "search.names=${name.urlEncode()}&search.values=${value.urlEncode()}"
    }.joinToString("&")

    // Useful for the script to inject a custom value, but in your case, replace System.getProperty
    // with a direct reference to your GE server. For example, "https://gradle.yourcompany.com"
    val geServer = System.getProperty("com.gradle.scan.server")
    return "$geServer/scans?$query"
}

// This method can be used for executing arbitrary shell processes
fun execAndGetStdout(vararg args: String): String {
    val stdout = java.io.ByteArrayOutputStream()
    exec {
        commandLine(*args)
        standardOutput = stdout
    }
    return stdout.toString().trim()
}

// An extension method on String
fun String.urlEncode() = java.net.URLEncoder.encode(this, "UTF-8")
