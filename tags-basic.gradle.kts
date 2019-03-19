// Tagging builds is a great way to provide additional context on a build scan.
// If your CI server has an appropriate environment variable exported, you can easily differentiate your CI vs LOCAL
// builds. Other CI-related tags might include the build number or ID, or a link to the job.
buildScan.tag(if (System.getenv("CI") != null) "CI" else "LOCAL")

// Tagging the operating system is easy.
buildScan.tag(System.getProperty("os.name"))

// You can also tag your builds based on the build's start parameters.
buildScan.tag(if (gradle.startParameter.isBuildCacheEnabled) "CACHED" else "NOT_CACHED")
