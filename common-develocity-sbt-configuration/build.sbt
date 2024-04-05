// You can configure the Develocity sbt plugin to publish Build Scans to your Develocity server
// by adding the following configuration to your project's build.sbt file

Global / develocityConfiguration :=
  DevelocityConfiguration(
    server = Server(
      url = Some(url("https://develocity-samples.gradle.com")), // adjust to your Develocity server
      allowUntrusted = false), // ensure a trusted certificate is configured
    buildScan = BuildScan(
      backgroundUpload = !sys.env.get("CI").exists(_.toBoolean)))

lazy val `common-develocity-sbt-configuration` = (project in file(".")) // adjust to your project name
