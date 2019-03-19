// With build can plugin v1.15+, you can easily add expensive data inside the background {} block. Everything inside
// here is executed in a separate thread, and guaranteed to complete before finishing the build and publishing the
// scan.
buildScan.background {
    // We can add the branch name as either a custom value or a tag, or both. It's simple to capture values from git.
    // `execAndGetStdout` is a custom method defined below.
    val branchName = execAndGetStdout("git", "rev-parse", "--abbrev-ref", "HEAD")
    if (branchName.isNotEmpty() && branchName != "HEAD") {
        tag(branchName)
        value("Git branch name", branchName)
    }
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
