Example configuration for making the `dump-dependencies` goal of the `gemnasium-maven-plugin` cacheable.

The goal reads the project's resolved dependencies (`project.artifacts`, collected with `TEST` resolution scope) plus the declared dependency list (`project.dependencies`), and writes a JSON file `gemnasium-maven-plugin.json` at the module root (i.e. `${basedir}`, not `target/`). The only user-facing configuration parameter is `ignoredScopes`.

The cache key therefore covers:

- The `ignoredScopes` mojo parameter.
- The GAV + type + classifier + scope of every artifact in `project.artifacts` (the resolved set).
- The GAV + type + scope of every entry in `project.dependencies` (the declared set).

The `baseDir` parameter is ignored because it only influences where the output JSON is written, not its content. If your project uses version ranges or `SNAPSHOT` versions, hashing `project.artifacts` (rather than only `project.dependencies`) ensures the cache key reflects the actually-resolved versions and prevents stale cache hits.

To exercise the cache:

```
./mvnw -pl gemnasium-maven-plugin-project validate
./mvnw -pl gemnasium-maven-plugin-project clean validate   # second run should be a cache hit
```

The build scan emitted by the Develocity Maven extension will show whether the `dump-dependencies` execution was taken from cache or executed. Bumping a dependency version in this `pom.xml` should produce a cache miss on the next run.
