// With build can plugin v1.15+, you can easily add expensive data inside the background {} block. Everything inside
// here is executed in a separate thread, and guaranteed to complete before finishing the build and publishing the
// scan.
buildScan.background {
    // Another use for the git commit ID is for generating a custom link to the source in your git repository.
    // It's simple to capture values from git.
    // `execAndGetStdout` is a custom method defined below.
    val gitCommitId = execAndGetStdout("git", "rev-parse", "--verify", "HEAD")
    link("Source", "https://github.com/gradle/buildscan-snippets/tree/$gitCommitId")
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
