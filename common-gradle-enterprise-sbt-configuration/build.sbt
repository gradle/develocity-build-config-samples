// You can configure the Gradle Enterprise sbt plugin to publish Build Scans to your Gradle Enterprise server
// by adding the following configuration to your project's build.sbt file

Global / gradleEnterpriseConfiguration :=
  GradleEnterpriseConfiguration(
    server = Server(
      url = Some(url("https://enterprise-samples.gradle.com")), // adjust to your GE server
      allowUntrusted = false), // ensure a trusted certificate is configured
    buildScan = BuildScan(
      publishConfig = PublishConfig.Always,
      backgroundUpload = !sys.env.get("CI").exists(_.toBoolean)))
