# Image names

An image is the Edge's list of what one job restores, and its name decides which runs share it. Too
broad, and different jobs overwrite each other's image; too narrow, and every run starts cold. A
good name holds the project, the job, the branch, the operating system and the architecture, and
nothing that changes from run to run (no run number, commit SHA or timestamp).

The CLI's `restore` takes several `--image-name` options and tries them in order: the first is the
job's own image, the rest are fallbacks, typically the pull request's target branch or the default
branch, so a new branch starts from a warm image. `store` takes one name: the first.

## Generated names

When no name is given, the CLI generates one, plus a restore-only fallback to the base branch, on
exactly these providers: **GitHub Actions, GitLab, Jenkins, TeamCity and Bamboo**. The segments are
joined with `~` and stored as a hash, so logs show a compact form. The CLI reference names only GitHub
Actions and Jenkins; the other three are generated too.

**Every other provider gets no generated name, in any version**: CircleCI, Azure Pipelines,
Buildkite, Bitbucket Pipelines, Travis CI, Bitrise, GoCD and the rest always need explicit names
(*Explicit names*, below). The `artifact-cache-cli` sample script used on those providers writes them.

## CLI versions before 1.11.0

Every released CLI up to 1.10.0 generates wrong names on three providers; 1.11.0 fixes them. Find the
CLI version that will run (the provider reference says how). **Below 1.11.0, apply the workaround
for the provider; at 1.11.0 or later, use the generated names** (unless a matrix needs explicit ones,
below). A pre-release does not count as 1.11.0, and neither does a release whose Release History
entry does not list the naming fix: read the entry before dropping a workaround.

| Provider | Before 1.11.0 | What 1.11.0 generates from |
| --- | --- | --- |
| GitHub Actions | Names leave out the repository: repositories sharing an Edge whose workflows have the same `name:` and job id share, and overwrite, one image | `GITHUB_REPOSITORY_ID`, `GITHUB_WORKFLOW`, `GITHUB_JOB`, `GITHUB_REF` (fallback: `refs/heads/` + `GITHUB_BASE_REF`, or `DEVELOCITY_ARTIFACT_CACHE_BASE_BRANCH_NAME`), `RUNNER_OS`, `RUNNER_ARCH` |
| GitLab | Names hold `CI_PIPELINE_ID`, not the job: every pipeline starts cold, the jobs of one pipeline share one image, and merge requests do not fall back to their target branch | `CI_PROJECT_PATH_SLUG`, `CI_JOB_NAME`, `CI_COMMIT_REF_NAME` (fallback: `CI_MERGE_REQUEST_TARGET_BRANCH_NAME`, else `CI_DEFAULT_BRANCH`), `CI_RUNNER_EXECUTABLE_ARCH` |
| Jenkins multibranch | Names come from the branch job, so the fallback never matches | project: `JOB_NAME` without `/${JOB_BASE_NAME}` when `BRANCH_NAME` is set. A change request (`CHANGE_ID` set) keeps the full `JOB_NAME` in its own name only; its fallback, `<project>~${CHANGE_TARGET}~<os>~<arch>`, uses the shortened project, so it matches the target branch's image |
| TeamCity, Bamboo | Correct | unchanged |

The workarounds below give explicit names of the same shape. Each carries a one-line comment in the
pipeline saying why, and that it can go once the CLI is 1.11.0 or later. The pull request says so
too, and what removing it later costs (below).

**What removing a workaround costs.** The CLI keys an explicit name exactly as it keys a generated
one: any name with a character outside `A-Z`, `a-z`, `0-9` and `_` (every name here has `~`) is
stored under the same hash of the string. So an explicit name that is character for character the
string 1.11.0 generates keeps its image when the workaround goes; any other string starts cold once.
The GitHub Actions and GitLab workarounds below are written to match the segments in the table; the
Jenkins one is not, since its platform segment comes from the JVM. Until a released 1.11.0 is in the
Release History with the naming fix, write in the pull request that removing a workaround should keep
the cache and at worst starts it cold once, not that it will.

### GitHub Actions

On the Action step:

```yaml
          # Artifact Cache CLI before 1.11.0 leaves the repository out of generated image names; remove image-names once it is 1.11.0 or later
          image-names: |
            ${{ github.repository_id }}~${{ github.workflow }}~${{ github.job }}~${{ github.ref }}~${{ runner.os }}~${{ runner.arch }}
            ${{ github.repository_id }}~${{ github.workflow }}~${{ github.job }}~refs/heads/${{ github.base_ref || github.event.repository.default_branch }}~${{ runner.os }}~${{ runner.arch }}
```

Use `github.repository_id`, even where a sample or a sibling repository uses `github.repository`:
the ID is what 1.11.0 generates from, and it survives a rename or transfer. Existing wiring that uses
`github.repository` works; leave it, since switching now costs the same one cold restore as removing
it later.

### GitLab

Only for a pipeline that calls the CLI directly. The `artifact-cache-cli` sample script names its
images itself, in every CLI version (`generic-ci.md`).

```sh
# Artifact Cache CLI before 1.11.0 names images after the pipeline, not the job; remove the --image-name options once it is 1.11.0 or later
AC_IMAGE="${CI_PROJECT_PATH_SLUG}~${CI_JOB_NAME}~${CI_COMMIT_REF_NAME}~${CI_RUNNER_EXECUTABLE_ARCH}"
AC_FALLBACK="${CI_PROJECT_PATH_SLUG}~${CI_JOB_NAME}~${CI_MERGE_REQUEST_TARGET_BRANCH_NAME:-$CI_DEFAULT_BRANCH}~${CI_RUNNER_EXECUTABLE_ARCH}"
```

Restore with `--image-name="$AC_IMAGE" --image-name="$AC_FALLBACK"`, store with
`--image-name="$AC_IMAGE"` only. `CI_JOB_NAME` carries the `parallel:matrix` values, so matrix jobs
already get separate images.

### Jenkins multibranch

With the plugin, around the existing build steps:

```groovy
script {
    // Artifact Cache CLI before 1.11.0 names multibranch images after the branch job, so branches never fall back; remove imageNames once it is 1.11.0 or later
    def slash = env.JOB_NAME.lastIndexOf('/')
    def project = slash > 0 ? env.JOB_NAME.substring(0, slash) : env.JOB_NAME
    def platform = isUnix() ? sh(script: 'echo "$(uname -s)~$(uname -m)"', returnStdout: true).trim() : 'Windows~' + env.PROCESSOR_ARCHITECTURE
    def base = env.CHANGE_TARGET ?: '<default branch>'
    withDevelocityArtifactCache(
        develocityUrl: '<server URL>', // or develocityEdgeUrl, see jenkins.md
        develocityAccessKeyCredentialId: '<credential id>',
        imageNames: [project + '~' + env.BRANCH_NAME + '~' + platform, project + '~' + base + '~' + platform]
    ) {
        sh './gradlew build'
    }
}
```

`BRANCH_NAME` is `PR-<n>` on a change request, so each pull request keeps its own image and falls
back to its target. The fallback serves only Artifact Cache content: each branch builds in its own
workspace, and the Setup Cache needs the same absolute checkout path (`tools.md`), so expect Setup
Cache misses on a first pull request build unless the job pins `customWorkspace`.

Only for a pipeline that calls the CLI directly, the same in shell. The `artifact-cache-cli` sample
script names its images itself, from variables `generic-ci.md` sets from `JOB_NAME`, `BRANCH_NAME`
and `CHANGE_TARGET`; a second `--image-name` makes its store fail.

```sh
AC_PROJECT="${JOB_NAME%/*}"
AC_PLATFORM="$(uname -s)~$(uname -m)"
AC_IMAGE="${AC_PROJECT}~${BRANCH_NAME}~${AC_PLATFORM}"
AC_FALLBACK="${AC_PROJECT}~${CHANGE_TARGET:-<default branch>}~${AC_PLATFORM}"
```

A Pipeline or Freestyle job that is not multibranch builds one branch, and its generated name is
correct in every version.

## Matrix jobs

Generated names separate jobs, operating systems and architectures, but not other matrix axes: jobs
that differ only in a Java, Gradle or dependency version share one image and overwrite each other's
content. Give an axis that changes what the job downloads, or the Gradle version it runs, its own
segment; when unsure, separate it (sharing costs overwrites, separating costs one cold image per
value). This holds at 1.11.0 and later as well.

An explicit name replaces the generated fallback too, so write the fallback with the same segment.
On GitHub Actions:

```yaml
          image-names: |
            ${{ github.repository_id }}~${{ github.workflow }}~${{ github.job }}~gradle-${{ matrix.gradle }}~${{ github.ref }}~${{ runner.os }}~${{ runner.arch }}
            ${{ github.repository_id }}~${{ github.workflow }}~${{ github.job }}~gradle-${{ matrix.gradle }}~refs/heads/${{ github.base_ref || github.event.repository.default_branch }}~${{ runner.os }}~${{ runner.arch }}
```

GitLab's `CI_JOB_NAME` already includes the matrix values. Jenkins' declarative `matrix` does not:
add the axis values to both names.

## Reviewing existing names

For wiring that is already there (step 5 of `SKILL.md`), check each job's names:

- **No names, CLI below 1.11.0**, on GitHub Actions, GitLab or a Jenkins multibranch pipeline: add
  the workaround above.
- **Explicit names without a project segment** (`ccud-gradle-${{ matrix.gradle-version }}`, `build`):
  repositories on the same Edge can share them. Replace them with the full shape, keeping any matrix
  segment they had.
- **A matrix job whose names lack an axis** that changes its downloads: add the segment.
- **A primary name without a fallback**: add the fallback, or a new branch starts cold.

A renamed image starts cold once; say so in the pull request.

## Explicit names

The `artifact-cache-cli` sample script builds these names from its variables (`generic-ci.md`). For
a provider without generated names that calls the CLI another way, build the same shape from the
provider's predefined variables, looked up in its own documentation at run time:

```
primary:  <project>~<job>~<branch>~<os>~<arch>
fallback: <project>~<job>~<pull request target branch, else the default branch>~<os>~<arch>
```

- **project**: the repository's slug or id
- **job**: the job or step name as the pipeline declares it; a literal when the provider exposes none
- **branch**: the branch being built; on a pull request build, something unique to that pull request
  (its source branch or its number)
- **fallback branch**: the pull request's target when the provider exposes it, else the default
  branch as a literal
- **os, arch**: the provider's agent variables, else `$(uname -s)` and `$(uname -m)`

Echo both names in the job log, so the first run shows what they resolved to. A segment that comes
out empty means a wrong variable name, and jobs that should be apart would share an image.
