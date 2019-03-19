// With build can plugin v1.15+, you can easily add expensive data inside the background {} block. Everything inside
// here is executed in a separate thread, and guaranteed to complete before finishing the build and publishing the
// scan.
buildScan.background {
    // It's simple to capture values from git. `execAndGetStdout` is a custom method defined below.
    val gitCommitId = execAndGetStdout("git", "rev-parse", "--verify", "HEAD")
    val branchName = execAndGetStdout("git", "rev-parse", "--abbrev-ref", "HEAD")
    val status = execAndGetStdout("git", "status", "--porcelain")

    // We can add the branch name as either a custom value or a tag, or both.
    if (branchName.isNotEmpty() && branchName != "HEAD") {
        tag(branchName)
        value("Git branch name", branchName)
    }

    // Tracking whether a build is 'dirty' (that is, has uncommitted changes) can be useful when diagnosing
    // failures.
    if (status.isNotEmpty()) {
        tag("dirty")
        value("Git status", status)
    }

    val commitIdLabel = "Git Commit ID"

    // We can track the commit ID as a custom value
    value(commitIdLabel, gitCommitId)

    // We can also use the git commit ID for generating a custom link, useful in Gradle Enterprise for filtering for
    // all scans built from the same git commit. This will create a URL that looks like:
    // https://«geServer»/scans?search.names=Git%20Commit%20ID&search.values=«gitCommitId»
    // This pattern can be used for creating custom links for arbitrary key-value pairs that you track with built
    // scan custom values.
    link("Git commit scans", customValueSearchUrl(mapOf(commitIdLabel to gitCommitId)))

    // Another use for the git commit ID is for generating a custom link to the source in your git repository.
    link("Source", "https://github.com/gradle/buildscan-snippets/tree/$gitCommitId")
}

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
