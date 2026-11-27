# Jenkins

Pipeline jobs use the Develocity Artifact Cache Jenkins plugin's `withDevelocityArtifactCache`
step. Freestyle jobs, which the plugin does not support, and controllers where it cannot be
installed use the manual flow (`generic-ci.md`).

## The plugin is an administrator's prerequisite

The plugin is not in the Jenkins Update Center: an administrator installs its `.hpi` (Configuring
Jenkins → Installing the Plugin). This is the one prerequisite that cannot degrade to a warning: a
Jenkinsfile that calls the step on a controller without the plugin fails at once with
`No such DSL method 'withDevelocityArtifactCache'`, the pull request's own build included. Ask before
choosing it:

- **installed, or will be before the pull request builds**: use the plugin, and name the version to
  install under *Before merging*
- **not possible now**: use the manual flow

Ask for the installed plugin version too: it decides the CLI version (below). Agents that run the
step need Java 21 as a JDK tool installation with a fixed home (the plugin does not run installers);
the Configuring Jenkins page lists the core and companion plugin versions.

## Placement

Around the build commands, after `checkout scm`. An illustration of the shape:

```groovy
stage('Build') {
    steps {
        withDevelocityArtifactCache(
            develocityUrl: 'https://develocity.example.com',
            develocityAccessKeyCredentialId: '<credential id>'
        ) {
            sh './gradlew build'
        }
    }
}
```

The `options { withDevelocityArtifactCache(...) }` form covers all stages, but only on the pipeline's
top-level agent: it fails under `agent none` and skips stages with an agent of their own. Prefer the
block form, in the one stage that builds. Image names carry no stage, so several wrapped stages
would share one image and overwrite each other's store: when more than one stage needs the cache,
give each explicit `imageNames` with `env.STAGE_NAME` in both names (*Image names*).

## Endpoint and credentials

- **Endpoint**: `develocityUrl` for discovery, `develocityEdgeUrl` for a dedicated or the built-in
  Edge (plugin 1.1.0 or later). Exactly one.
- **`develocityAccessKeyCredentialId`**: the Secret text credential the pipeline already uses for
  `DEVELOCITY_ACCESS_KEY` (`withCredentials`, `credentials('…')` in the Jenkinsfile). When the key
  reaches builds another way (the Gradle Jenkins plugin's injection, a global property), ask for the
  credential's ID. Never create one.
- The plugin takes a bare key or `<host>=<key>` entries, and uses the entry for the endpoint's host.
  A credential holding only `<server host>=<key>` does not cover a dedicated Edge on its own host: use
  discovery then, or list under *Before merging* a credential with the bare key or an entry for the
  Edge host.
- **Instance-wide defaults** (Manage Jenkins → System): when an administrator set the endpoint and
  credential there, omit both from the step. They fall back only as a pair: set both or neither.

## CLI version

The version that runs is `cliVersion` when set, else the one the installed plugin bundles: the
Jenkins Plugin Release History lists it per release ("Uses Artifact Cache CLI … by default"). Read
the CLI Release History from the bundled version to the latest for tools and fixes a chosen job
needs: pip arrived in 1.5.0, Playwright and Cypress in 1.6.0, Docker in 1.9.0, and 1.7.1 fixed store
for Gradle composite builds that nest or share included builds, and Maven on Windows. When one
applies, set `cliVersion` to the latest release, with `cliSha256` from the release's published
`.sha256` when the plugin does not know that version. Check the release's compatibility line against
the server and Edge versions from step 3.

## Image names

The CLI generates names from `JOB_NAME`, and the plugin sets the branch and base branch from a
multibranch pipeline's `BRANCH_NAME` and `CHANGE_TARGET`. Below CLI 1.11.0, a multibranch pipeline
needs explicit `imageNames`: see `image-names.md`. A declarative `matrix` needs its axes in the names
in every version, and several wrapped stages need `env.STAGE_NAME`.

## Other options

- `additionalCliArgs`: a list, e.g. `['--no-autodetect=DOCKER']`, for the flags in step 5. Endpoint
  and trust flags in it are dropped.
- `cacheReadOnly`: leave it off, as on GitHub Actions.
- `debug: true` writes the CLI output to the build log; useful only while diagnosing.

## Conflicting caches

Remove Job Cacher steps (`cache(caches: [...]) { … }`) whose paths are a home the CLI covers, and name
each in the pull request.

## Pipelines outside the repository

A job configured on the controller, or a step from a shared library: change nothing there. Say in
the pull request what has to change, and where. If nothing in the repository changes, there is no
pull request: give the user the exact snippet instead.

## Results

The plugin adds a cache summary to each build page: one row per phase, with the outcome, detected
tools, entries, size and duration. The manual flow has no summary; it keeps the CLI log as a build
artifact instead.
