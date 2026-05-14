Example configuration for making the `tree` goal of `maven-dependency-plugin` cacheable, configured to emit the JSON graph file consumed by GitLab's dependency-scanning analyzer (`Jobs/Dependency-Scanning.v2.gitlab-ci.yml`).

The goal is invoked as:

```
mvn org.apache.maven.plugins:maven-dependency-plugin:3.7.0:tree -DoutputType=json -DoutputFile=maven.graph.json
```

It walks `project.artifacts` (resolved set) to render the dependency graph and reads `project.dependencies` (declared set) during resolution. Both feed the cache key.

The cache key covers:

- Mojo formatting parameters: `outputType`, `scope`, `includes`, `excludes`, `verbose`, `appendOutput`, `tokens`, `outputEncoding`.
- The GAV + type + classifier + scope of every resolved artifact (`project.artifacts`).
- The GAV + type + scope of every declared dependency (`project.dependencies`).

`outputFile` and `skip` are ignored — they affect output location and execution flow but not content. If the project uses version ranges or `SNAPSHOT` versions, hashing `project.artifacts` ensures cache invalidation when resolved versions shift.

To exercise the cache:

```
./mvnw -pl maven-dependency-tree-project validate
./mvnw -pl maven-dependency-tree-project clean validate   # cache hit on the second run
```

The build scan emitted by the Develocity Maven extension confirms whether the `dependency-tree-json` execution was taken from cache. Bumping any dependency version in this `pom.xml` produces a cache miss on the next run.

The generated `maven.graph.json` is the exact file GitLab's `dependency-scanning` analyzer detects as `FileTypeGraphExport` and parses with its Maven parser, so caching this goal directly accelerates the SBOM-generation half of a GitLab dependency-scanning pipeline.
