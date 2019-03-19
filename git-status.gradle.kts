// With build can plugin v1.15+, you can easily add expensive data inside the background {} block. Everything inside
// here is executed in a separate thread, and guaranteed to complete before finishing the build and publishing the
// scan.
buildScan.background {
    // Tracking whether a build is 'dirty' (that is, has uncommitted changes) can be useful when diagnosing
    // failures. It's simple to capture values from git.
    // `execAndGetStdout` is a custom method defined below.
    val status = execAndGetStdout("git", "status", "--porcelain")

    if (status.isNotEmpty()) {
        tag("dirty")
        value("Git status", status)
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
