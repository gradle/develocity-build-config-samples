## Artifact Cache CLI on any CI

This sample demonstrates how to set up the [Develocity Artifact Cache and Setup Cache][artifact-cache] on a CI provider without an Artifact Cache integration, such as GitLab CI, by driving the [Artifact Cache CLI][cli-commands] with plain shell and curl.
GitHub Actions and Jenkins have integrations, see the [GitHub Actions samples](../artifact-cache-github-actions-gradle) and the [Jenkins guide][jenkins].

[`artifact-cache.sh`](artifact-cache.sh) runs one cache operation, `restore` before the build or `store` after it:

1. It exchanges the access key at the Edge for a short-lived token, so the access key itself never reaches the CLI.
2. It downloads the CLI from the Edge, and verifies its SHA-256 against the one pinned in the script before running it.
3. It names the cache image `<project>~<job>~<branch>~<os>~<arch>`, and lets `restore` fall back to the image of a base branch.
4. It runs the CLI against the Edge, with the token in an access key entry for the Edge host.

A cache problem, such as an unreachable Edge or a rejected access key, never fails the build: the script warns and exits 0, as the CLI itself does.
Missing configuration, and a CLI download that fails checksum verification, fail it.

### Requirements

- Develocity 2026.3 or later, an Edge node at version 2.3.0 or later, which serves the CLI, and the Artifact Cache and Setup Cache entitlements on the license
- An access key with the permission to read and write Build Cache data
- Java 21 or later to run the CLI, `curl`, and `sha256sum` or `shasum`
- Gradle 8.1 or later, or Maven 3.6.2 or later

### Usage

Commit `artifact-cache.sh` to the repository, and run it around the build:

```sh
sh artifact-cache.sh restore
./gradlew build
sh artifact-cache.sh store
```

Run `store` only after a successful build.
The script reads its configuration from environment variables:

| Variable | Description |
| --- | --- |
| `DEVELOCITY_EDGE_URL` | Develocity Edge URL, such as `https://edge.example.com`. Develocity 2026.1 and later also serve an Edge at the server URL. |
| `ARTIFACT_CACHE_ACCESS_KEY` | Develocity access key as a bare value, without a `host=` prefix. A short-lived access token valid for at least another hour works too. Keep it in a masked secret. |
| `ARTIFACT_CACHE_PROJECT` | Repository or project identifier, such as `my-group-my-project`. **Required**, see below. |
| `ARTIFACT_CACHE_JOB` | CI job name, such as `build`. |
| `ARTIFACT_CACHE_BRANCH` | Branch being built. |
| `ARTIFACT_CACHE_BASE_BRANCH` | Branch whose image `restore` falls back to while the branch has none of its own, such as the merge request's target branch or the default branch. Optional. |
| `ARTIFACT_CACHE_JAVA_HOME` | Java 21 or later to run the CLI with, when the build's `JAVA_HOME` is older. Optional. |

The project identifier is not optional.
The Edge keys an image by its name alone, so without it, jobs of different repositories that share job and branch names, such as `build` on `main`, read and write the same image, and restore each other's content.

Options after `restore` or `store` go to the CLI, for example `--gradle-home=<path>` or `--verbose`.
To upgrade the CLI, set `cli_version` and `cli_sha256` in the script to a [released version][release-history] and its [published checksum][download].

### GitLab CI

[`gitlab-ci-gradle.yml`](gitlab-ci-gradle.yml) and [`gitlab-ci-maven.yml`](gitlab-ci-maven.yml) are `.gitlab-ci.yml` examples for each build tool.
They name the images after `CI_PROJECT_PATH_SLUG`, `CI_JOB_NAME_SLUG` and `CI_COMMIT_REF_NAME`, and fall back to the merge request's target branch, or the default branch.
They run `store` as the last command of `script` rather than in `after_script`, because `after_script` also runs after a failed build, and has its own timeout, 5 minutes by default.

### Gradle and Maven differences

The script and the CLI work the same for both build tools, with these differences:

- **Access key form.**
  When connecting to an Edge directly, the CLI authenticates with the `DEVELOCITY_ACCESS_KEY` entry for the Edge host, in `<edge-host>=<key>` form.
  The script builds that value itself and passes it to the CLI process only, so it never depends on the build's own `DEVELOCITY_ACCESS_KEY`.
  This matters for Maven: the [Develocity GitLab template][gitlab-templates] for Maven reduces `DEVELOCITY_ACCESS_KEY` to the entry for the Develocity server, as its `enforceUrl` input defaults to `true`, so an entry for the Edge host does not survive into the job.
  The template for Gradle keeps all entries.
- **Gradle User Home.**
  The CLI restores into and stores from `GRADLE_USER_HOME`, or `~/.gradle` when it is unset.
  Set the same `GRADLE_USER_HOME` for the restore, the build and the store, or pass `--gradle-home=<path>` to both operations, and drop any other cache of the Gradle User Home, such as a GitLab `cache:` entry.
- **Maven local repository.**
  The CLI restores into and stores from `~/.m2`, honoring a `localRepository` in `~/.m2/settings.xml`.
  A build that moves the local repository otherwise, for example with `-Dmaven.repo.local`, needs `--maven-repository=<path>` on both operations.
  The Develocity Maven extension 2.4.0 or later keeps dependencies the build no longer uses out of the stored image.
- **Build Scan data.**
  After a restore, the Build Scan carries the `ARTIFACT_CACHE` and `SETUP_CACHE` tags, and the restore metrics as custom values.
  Gradle builds get them with no further setup.
  For Maven, the CLI leaves them in a Groovy script in `~/.m2/.develocity/custom-user-data`, which only the [Common Custom User Data Maven extension][ccud-maven] 2.1.0 or later evaluates, so apply it to see the cache activity in the Build Scan.
- **Setup Cache.**
  The Setup Cache supports Gradle only, from version 8.1.
  The same `restore` and `store` cover it, with no configuration of its own.
  To also cache the compiled build logic of `buildSrc` and included builds, it needs the Develocity Gradle plugin 4.4.1 or later, a checkout before `restore`, and the same absolute checkout path in every job.
  If runners check out to varying paths, fix the path, on GitLab with `GIT_CLONE_PATH`.
  Without the Setup Cache entitlement, pass `--disable-setup-cache` to both operations to avoid a warning.

### Verification

The [verification workflow](../.github/workflows/artifact-cache-cli-verification.yml) runs the script on GitHub Actions around the Gradle and Maven builds of the GitHub Actions samples, against the Develocity Solutions instance.
As the repository authenticates with workload identity rather than an access key, it exchanges a GitHub OIDC token at the Edge for a short-lived token, and passes that to the script.
It fails unless restore and store report no error, and the stored image holds Artifact Cache content, and for Gradle Setup Cache content too.
The GitLab examples are not run.

[artifact-cache]: https://docs.develocity.ai/artifact-cache/current/
[cli-commands]: https://docs.develocity.ai/artifact-cache/current/reference/cli-commands/
[jenkins]: https://docs.develocity.ai/artifact-cache/current/how-to/configure-jenkins/
[release-history]: https://docs.develocity.ai/artifact-cache/current/release-history/
[download]: https://docs.develocity.ai/artifact-cache/current/download-the-cli/
[gitlab-templates]: https://github.com/gradle/develocity-gitlab-templates
[ccud-maven]: https://github.com/gradle/common-custom-user-data-maven-extension
