# sbt: `build.sbt`, `project/plugins.sbt`

Use `~=` rather than the sample's `develocityConfiguration :=`. It composes with any Develocity
configuration the build already has instead of replacing it, and `DevelocityConfiguration.apply`,
what the assignment calls, has been deprecated since sbt-develocity 1.1.2. The sample stops at the
server and the scan, so the cache block has to be added:

```scala
val isCI = sys.env.contains("<CI_VAR>")
val hasAccessKey = sys.env.get("DEVELOCITY_ACCESS_KEY").exists(_.nonEmpty)

ThisBuild / develocityConfiguration ~= { previous =>
  previous
    .withServer(
      previous.server
        .withAllowUntrusted(<bool>)
        .withUrl(url("<server>")))
    .withBuildScan(
      previous.buildScan.withBackgroundUpload(!isCI))
    .withBuildCache(
      previous.buildCache
        .withLocal(previous.buildCache.local.withEnabled(<bool>))
        .withRemote(
          previous.buildCache.remote
            .withEnabled(<bool>)
            .withStoreEnabled(isCI && hasAccessKey)))
}
```

With several CI providers: `val isCI = Seq("A", "B").exists(sys.env.contains)`.

`withAllowUntrusted` goes above `withUrl`: deleting the top call of a chain leaves valid Scala,
deleting the bottom one takes the closing parentheses with it.

## Merging

Both files usually exist. Append to them; never replace them.

- `build.sbt`: add the two `val`s and the `ThisBuild / develocityConfiguration ~=` block at the top
  level, after the imports. Take nothing else from the sample's `build.sbt`: its closing
  `lazy val … = (project in file("."))` defines the sample's own project, not this build's.
- `project/plugins.sbt`: add the sample's `addSbtPlugin(...)` lines, with the versions from step 7,
  next to the existing ones. Leave every other line alone.
