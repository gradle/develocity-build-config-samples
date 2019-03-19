// For Android developers, it can be useful to know whether a build was invoked from Android Studio or from the CLI.
buildScan.tag(if (project.hasProperty("android.injected.invoked.from.ide")) "Android Studio" else "CLI")
