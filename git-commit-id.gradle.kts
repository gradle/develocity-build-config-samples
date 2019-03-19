// With build can plugin v1.15+, you can easily add expensive data inside the background {} block. Everything inside
// here is executed in a separate thread, and guaranteed to complete before finishing the build and publishing the
// scan.
buildScan.background {
    // We can track the commit ID as a custom value. It's simple to capture values from git.
    // `execAndGetStdout` is a custom method defined below.
    val gitCommitId = execAndGetStdout("git", "rev-parse", "--verify", "HEAD")
    value("Git Commit ID", gitCommitId)
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
