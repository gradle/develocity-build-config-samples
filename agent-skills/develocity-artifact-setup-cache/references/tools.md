# Tools the CLI caches

The CLI detects tools per job, from their home directories, whatever builds the project. This table
is the state of CLI 1.10; the current requirements page is the authority on the list and the minimum
versions.

| Tool | `--autodetect` id | Signs of it in a job | Minimum version | Caches |
| --- | --- | --- | --- | --- |
| Gradle | `GRADLE` | `./gradlew`, `gradle`, `gradle/actions/setup-gradle` | 8.1 | Artifact Cache, Setup Cache |
| Maven | `MAVEN` | `./mvnw`, `mvn` | 3.6.2 | Artifact Cache |
| npm | `NPM` | `npm ci`, `npm install`, `npx` | npm 5 | Artifact Cache |
| pip | `PIP` | `pip install`, `python -m pip`, `pip3` | any | Artifact Cache |
| Cypress | `CYPRESS` | `cypress run`, `cypress install`, `cypress` in `package.json` | 3.0 | Artifact Cache |
| Playwright | `PLAYWRIGHT` | `playwright install`, `@playwright/test` | 1.9 | Artifact Cache |
| SonarScanner | `SONAR` | `sonar-scanner`, a `sonar` Gradle task or Maven goal, `pysonar`, SonarSource's scan actions | any | Artifact Cache |
| Docker | `DOCKER` | `docker`, `docker compose`, Testcontainers, `docker/build-push-action`, service containers | Engine 25 | Artifact Cache |

Nothing else is cached: not yarn's or pnpm's stores, sbt's Coursier cache, Bazel's repository
cache, Go modules or Cargo. A job whose only tools are those has nothing to gain.

Read versions from what the job actually runs: `distributionUrl` in
`gradle/wrapper/gradle-wrapper.properties`, `.mvn/wrapper/maven-wrapper.properties`, the Playwright
and Cypress versions in `package.json` and its lockfile, the Docker Engine of the runner image.

## Home directories

| Tool | Detected from | Override |
| --- | --- | --- |
| Gradle | `GRADLE_USER_HOME`, else `~/.gradle` | `--gradle-home` |
| Maven | `~/.m2`; the local repository from `<localRepository>` in its `settings.xml`, else `~/.m2/repository` | `--maven-home`; `--maven-repository` when the build sets `-Dmaven.repo.local` (often in `MAVEN_OPTS`) or reads another `settings.xml` |
| npm | `NPM_CONFIG_CACHE`, else `~/.npm` | `--npm-home`, also when `.npmrc` sets `cache=` |
| pip | `PIP_CACHE_DIR`, else `$XDG_CACHE_HOME/pip`, else the platform default | `--pip-home` |
| Cypress | an absolute `CYPRESS_CACHE_FOLDER`, else the platform default | `--cypress-home`, also for a relative `CYPRESS_CACHE_FOLDER` or one set through npm configuration |
| Playwright | `PLAYWRIGHT_BROWSERS_PATH`, else the platform default; `0` puts the browsers in `node_modules`, which is not cached | `--playwright-home` |
| SonarScanner | `SONAR_USER_HOME`, else `~/.sonar` | `--sonar-home`, when the directory only appears during the build |
| Docker | the daemon's image store | none: `--no-autodetect=DOCKER`, `--docker-exclude`, `--enable-docker-builders` |

The CLI reads these variables in its own step. A home set only on the build step (`env:` on the
`./gradlew` step, an `export` inside the build script) is invisible to the restore: move it to the
job level, or pass the override with the same path to both commands.

A build that runs inside a container the job starts (`docker run … ./gradlew build`) keeps its homes
in that container. The CLI, on the runner, sees them only through a mounted volume: pass the host
side of the mount as the override, or leave the job out.

## Per tool

- **Gradle**: the Setup Cache needs the same absolute checkout path on every run; compare it across
  two runs' logs (a runner that puts a concurrency index or a run id in the path breaks it). Caching
  `buildSrc` and included builds needs the Develocity Gradle plugin 4.4.1. JDKs that Gradle
  provisions into `<Gradle home>/jdks` are single entries larger than the Edge's default 100 MiB.
- **Maven**: smart cleanup, which keeps unused artifacts out of the image, needs the Develocity Maven
  extension 2.4.0. SNAPSHOT dependencies are left out unless the store gets
  `--include-snapshot-dependencies`.
- **npm**: smart cleanup needs the Develocity npm agent 4.1.0.
- **pip**: smart cleanup needs the Develocity Python agent 0.10.1.
- **Cypress**: Cypress never removes an old binary, so the image keeps every version ever installed.
  Run `cypress cache prune` right after the install (Cypress 5.4 or later).
- **Playwright**: `PLAYWRIGHT_SKIP_BROWSER_GC` stops Playwright's own pruning; the image then keeps
  old browser revisions until a refresh.
- **SonarScanner**: each analyzer version adds roughly 50 to 100 MB to the image.
- **Docker**: see *Docker* in step 5 of `SKILL.md`, and the Docker Image Caching page.
