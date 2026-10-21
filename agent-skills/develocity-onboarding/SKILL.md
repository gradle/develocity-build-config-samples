---
name: develocity-onboarding
description: Onboards a repository to Develocity. Detects the build tool (Maven, Gradle, npm, sbt, Bazel), adapts the official configuration sample, wires the access key into CI, checks the build still loads, and opens a pull request. Use when asked to onboard a project or repository to Develocity, integrate a build with a Develocity server, or add Build Scan configuration.
---

# Develocity onboarding

Integrate a repository with Develocity, and hand the result over as a reviewable pull request.

The configuration is not stored here. It is fetched from [develocity-build-config-samples](https://github.com/gradle/develocity-build-config-samples), 
which contains the most up-to-date version.
The goal is to automate the onboarding and apply the *Modifications* described below.

Reference files (read only the ones the run needs):

- `references/<tool>.md`: the modifications, merge rules and checks for one build tool: `maven.md`,
  `gradle.md`, `sbt.md`, `npm.md`, `bazel.md`
- `references/ci-providers.md`: CI provider to the environment variable that identifies it
- `references/gradle-enterprise-migration.md`: for a build that still uses Gradle Enterprise

## Inputs

| Input | Required | Default |
| --- | --- | --- |
| Repository | no | the current working directory |
| Develocity server URL | no | asks the user |
| Tracking reference | no | none; an issue, JIRA ticket or other URL the pull request must cite |
| `allowUntrusted` | no | left out of the generated config |
| Local build cache | no | enabled |
| Remote build cache | no | enabled |
| CI provider | no | detected from the repository |

Comments
- Ask for the server URL if it was not given (do not invent one)
- Never ask for, read, or write an access key: it is supplied at build time
- If a repository URL was given instead of a local path, clone it into a scratch directory and work there

## 1. Detect the build tool

Inspect the repository root to find a match.
When several build tools coexist (Gradle and Maven, sbt and Bazel, a `package.json` beside a JVM build), list
them to the user with the marker each was found by, and ask which ones to onboard. Onboard each one chosen.

| Marker | Tool | Sample directory | Files to take |
| --- | --- | --- | --- |
| `settings.gradle.kts`, else `build.gradle.kts` | Gradle (Kotlin) | `common-develocity-gradle-configuration-kotlin` | `settings.gradle.kts` |
| `settings.gradle`, else `build.gradle` or `gradlew` alone | Gradle (Groovy) | `common-develocity-gradle-configuration-groovy` | `settings.gradle` |
| `pom.xml` | Maven | `common-develocity-maven-configuration` | `.mvn/develocity.xml`, `.mvn/extensions.xml` |
| `build.sbt` | sbt | `common-develocity-sbt-configuration` | `build.sbt`, `project/plugins.sbt` |
| `MODULE.bazel`, `WORKSPACE`, `WORKSPACE.bazel` | Bazel | `common-develocity-bazel-configuration` | `.bazelrc` |
| `package.json` | npm | `common-develocity-npm-configuration` | `develocity.config.js` (written as `.cjs` in an ES module package) |

Take only those files. The samples are working projects, so they also carry other files not relevant to our context.

Check for configuration that is already there:

- **Already onboarded**: an existing `develocity` block or `.mvn/develocity.xml`. Report what is
  configured and stop rather than opening a PR.
- **Gradle Enterprise**: `com.gradle.enterprise`, `com.gradle.build-scan`,
  `gradle-enterprise-maven-extension`, `.mvn/gradle-enterprise.xml` or `sbt-gradle-enterprise`. This is
  a migration: follow `references/gradle-enterprise-migration.md`.
- **Another remote cache or build event backend**: a Gradle `buildCache { remote(...) }` that is not
  Develocity's, another Maven build cache extension, or a Bazel `--remote_cache`, `--remote_executor`,
  `--bes_backend` or `--bes_results_url` pointing at another service (BuildBuddy, EngFlow, a
  self-hosted cache). The Develocity configuration would silently replace infrastructure the build
  relies on, since Gradle settings take a single remote, and Bazel a single cache and BES backend. Stop and ask
  the user: keep the existing one and onboard Build Scans only, replace it, or abandon.

## 2. Detect the CI provider

Unless the provider was given as an input, infer it from the repository: `.github/workflows/`,
`Jenkinsfile`, `.gitlab-ci.yml`, `.teamcity/`, `.circleci/`, `azure-pipelines.yml`, `.buildkite/`,
`bitbucket-pipelines.yml`, `.travis.yml`, `bitrise.yml`, `bamboo-specs/`.

Read the provider's environment variable out of `references/ci-providers.md`. If the provider is not
listed there, search the web for its predefined environment variables, pick one that is always set
and unique to it, and tell the user the row to add, with a link to the vendor's page. Do not edit
the reference file yourself: it usually sits in a shared checkout.

**Several providers can match**, GitHub Actions for pull requests and Jenkins for releases, say.
Take every provider whose marker is present and make the CI check an OR of their variables, so a
build on any of them counts as CI; each tool's reference file shows the combined form. List the
providers and variables in the pull request, and wire the key into each pipeline in step 5.

If no provider can be identified, fall back to `CI` and note in the pull request that it is a guess.

From here on, `<CI_VAR>` stands for that check: one variable, or the OR of several.

## 3. Fetch the sample

```
https://raw.githubusercontent.com/gradle/develocity-build-config-samples/main/<sample directory>/<file>
```

If a sample no longer looks like what *Modifications* describes (a block was renamed, the DSL changed, 
a condition is already expressed differently), the sample's structure wins and the modifications still apply 
to whatever now plays that role.

## 4. Modifications

The samples are meant to be changed: they hard-code `CI`, hard-code a server, and gate the cache on nothing else. Every change below is required.

- **Server URL**: the input, replacing `https://develocity-samples.gradle.com` (Bazel's sample uses a `<<develocity-server>>` placeholder, and takes the host only).
- **`allowUntrusted` / `allowUntrustedServer`: delete the whole line unless the input was given.** A server with a trusted certificate does not need such setting.
- **CI detection**: replace the sample's `CI` check with `<CI_VAR>` from step 2, testing for *presence*, never for a value.
- **Background Build Scan upload**: on when not CI, off on CI.
- **Remote cache store**: on only when CI **and** `DEVELOCITY_ACCESS_KEY` is set and non-empty.
  For Bazel the remote cache itself is gated too.
- **Local and remote cache enablement**: the inputs, both `true` unless turned off.

Then apply the tool-specific changes in `references/<tool>.md`. They are required too, merge rules
included: the target's existing files are extended, never replaced.

Drop the samples' comments. They are written for someone reading the sample, not for the project that
ends up with the file: `.mvn/develocity.xml` carries a paragraph about Jenkins credential plugins, and
`build.sbt` opens by explaining that you configure the plugin by adding this block to `build.sbt`.
Both read as leftovers once applied, as does anything saying "adjust to your CI provider" or "adjust
to your Develocity server": that adjustment has been made. Ship the configuration on its own.

The exception is the comments the reference files prescribe verbatim, such as the `.bazelrc` block:
they explain flags whose removal silently breaks a build, so they stay.

## 5. Wire the access key into CI

The build publishes nothing without a key, so this belongs in the same change. How a secret reaches
the build is provider-specific and changes over time, so **look it up rather than assuming**. 
Search the vendor's own documentation for how a secret becomes an environment variable in that provider's
pipeline file, and settle two questions before touching anything:

1. **Is the secret injected into every job automatically?** Several providers do this once the
   variable exists in their UI. Then the pipeline file needs no change at all, and the pull request
   just has to say precisely what to create and where.
2. **Does it need mapping per job or per step?** Some providers deliberately keep secrets out of the
   environment until a step opts in, so a single job-level declaration silently yields nothing.

Apply what the documentation says, for every provider found in step 2. If the search settles neither
question, leave the pipeline file alone and write what you found in the pull request.

The result should read like this (an illustration of the shape, not a recipe to copy):

```yaml
    env:
      DEVELOCITY_ACCESS_KEY: ${{ secrets.DEVELOCITY_ACCESS_KEY }}
```

npm and Bazel need more than the key: see *CI wiring* in their reference files.

Never create the secret, and never put its value in a file.

### The secret's value

The value differs by tool, and getting it wrong means every build fails to authenticate. The pull
request must spell out the exact value for the tool it configures:

| Tool | `DEVELOCITY_ACCESS_KEY` value |
| --- | --- |
| Gradle, Maven, sbt, npm | `<server host>=<access key>`, e.g. `develocity.example.com=abc123…` |
| Bazel | `<access key>` alone, as it goes into `Authorization: Bearer` |

The server host is the URL's host, without scheme or path. The key comes from the server under
*My settings → Access keys*.
The key's owner needs the permissions to publish Build Scans and to write to the build cache;
without the second, CI logs a `403` on every cache store and the remote cache stays empty. Say this
next to the secret in the pull request.

## 6. Resolve versions

The sample's versions are usually current, but check: a sample can lag a release.

```
curl -s https://plugins.gradle.org/m2/com/gradle/develocity/com.gradle.develocity.gradle.plugin/maven-metadata.xml
curl -s https://plugins.gradle.org/m2/com/gradle/common-custom-user-data-gradle-plugin/com.gradle.common-custom-user-data-gradle-plugin.gradle.plugin/maven-metadata.xml
curl -s https://repo1.maven.org/maven2/com/gradle/develocity-maven-extension/maven-metadata.xml
curl -s https://repo1.maven.org/maven2/com/gradle/common-custom-user-data-maven-extension/maven-metadata.xml
curl -s https://repo1.maven.org/maven2/com/gradle/sbt-develocity_2.12_1.0/maven-metadata.xml
curl -s https://repo1.maven.org/maven2/com/gradle/sbt-develocity-common-custom-user-data_2.12_1.0/maven-metadata.xml
npm view @gradle-tech/develocity-agent version
```

Take `<release>` from each rather than `<latest>`, which can be a pre-release, and skip any `-rc`
or `-milestone` version. If a lookup fails, keep the sample's version.

Then check it against the build tool the repository actually uses: the Gradle wrapper in
`gradle/wrapper/gradle-wrapper.properties`, the Maven wrapper in `.mvn/wrapper/maven-wrapper.properties`, `project/build.properties` for sbt.
Without a wrapper, ask the installed `gradle --version` / `mvn --version` and say in the pull request
that the project does not pin one.
On a mismatch, use the newest compatible version and call it out rather than shipping a config that will not load.

## 7. Check it loads

Run the cheapest command that parses the configuration, from the repository root, with the versions
step 6 settled. What is checked must be what ships:

| Tool | Command |
| --- | --- |
| Gradle | `./gradlew help --no-scan`, or `gradle help --no-scan` without a wrapper |
| Maven | `./mvnw -B -ntp validate`, or `mvn -B -ntp validate` without a wrapper |
| sbt | `sbt about`, then `sbt "print develocityConfiguration"` to see the resolved settings |
| npm | see *Check* in `references/npm.md` |
| Bazel | `bazel build --nobuild --announce_rc //...` (`bazel info` does not apply the `build` config) |

What is being checked is that the build still loads and the configuration resolves the way it was
meant to. 

**Then check the store gate**, the pull request's main claim. Run once more with the CI variable set
and `DEVELOCITY_ACCESS_KEY` empty, which is what a fork's CI build looks like, and confirm the remote cache
does not store:

| Tool | Command | Expect |
| --- | --- | --- |
| Gradle | `<CI_VAR>=1 DEVELOCITY_ACCESS_KEY= ./gradlew help --no-scan --info` | `Using remote Develocity build cache … (pull-only, …)` |
| Maven | `<CI_VAR>=1 DEVELOCITY_ACCESS_KEY= ./mvnw -B -ntp -X clean validate` | `storeEnabled: false` under `Remote build cache` |
| sbt | `<CI_VAR>=1 DEVELOCITY_ACCESS_KEY= sbt "print develocityConfiguration"` | `RemoteBuildCacheImpl(true,false,…)`: enabled, store off |

With several CI variables, set one at a time. Maven only prints the remote cache's configuration
when the server answers; otherwise it logs `Remote build cache: disabled`, and the pull request says
the gate was not observed. npm has no cache, and Bazel's gate is the key check in the CI script,
which the pull request's own run exercises.

**A published Build Scan is not the bar.** Publishing needs an access key, which usually only exists
once the reviewer creates the secret, so a build that logs a credential error has still passed this
check. If a scan does go up, say so, but never hold the pull request for one, and never create a key to manufacture it. 
If the build tool is not available locally, say so in the pull request instead of claiming it was checked.

**Commit on a new branch, never on the checked-out one**: on the user's own checkout that is
usually the default branch. Read the target's branch naming first (step 8), create the branch, then
commit only the files this skill changed: configuration, pipeline, `.gitignore`, lockfile, scripts.
Check `git status` before staging rather than after pushing. If step 8 later settles a different
name, an issue key say, rename the branch with `git branch -m` before pushing.

## 8. Prepare and open the pull request

Read the target's own rules before writing anything: `.github/pull_request_template.md`,
`.github/PULL_REQUEST_TEMPLATE/`, `CONTRIBUTING.md` and `AI_POLICY.md`. The template is the least of
them: `CONTRIBUTING.md` is usually where the binding requirement lives: a tracker the template never
mentions, a branch named after the issue key, a commit trailer. Follow all of them.
Use the repository's own branch naming; do not carry over conventions from elsewhere. If there is no push access, fork and open the PR from the fork (see *Rules*: the fork is shown and confirmed first).

**The title is often linted.** Look for a workflow that reads `github.event.pull_request.title`
(`pr-title-check.yml`, `lint-pr.yml`, or a step using `amannn/action-semantic-pull-request`) and take
the rule from its configuration rather than from the examples in the template: the regex it greps, or
its `types`, `scopes` and `requireScope` inputs. `Module: Description`, `[#123] fix(core): subject`
and conventional commits with a mandatory scope are all in use, and a title that fails the check is a
red pull request on arrival.

**If the project has an AI policy, follow it.** State plainly what was used and what it did: the
configuration adapted from the official samples, the pipeline edited, the check run, the description
written. Where the policy asks for something that cannot be produced here, an independent review pass
or a signed declaration, write that it was not done instead of ticking the box. Every checklist item
is either answered truthfully or left unticked with the reason beside it.

If the template or the contributing guide requires a linked issue or ticket, use the **tracking reference** input. 
If none was given, offer to file one: show the exact title and body you would create, and give three choices:
- create it as written
- edit it first
- supply an existing reference instead

Wait for the answer. Do not open a pull request that fails the target's own rules on arrival, and do not file anything the user has not seen.
When a tracking reference was given and the template has no field for it, cite it in the description.

For a GitHub issue, write a closing keyword: `Closes #123`. 

Keep the description short: what was added, what changes for the people building the project, and
what the reviewer still has to do:

```
Publishes Build Scans to <Develocity server URL> and enables the Develocity build cache.

- <files added or changed, with the plugin or extension versions>
- The <workflow/pipeline> exports DEVELOCITY_ACCESS_KEY from a secret
- CI is detected via <ENV_VAR> (<provider>)
- <one line per behaviour change, see below>

Checked with <command> on <tool version>: <what it showed>. 
<Scan published, or the configuration resolved as intended; publishing needs the secret below.>

Before merging: add DEVELOCITY_ACCESS_KEY as a repository secret, with the value
<the exact format for this tool, from step 5: `<server host>=<key>`, or the bare key for Bazel>.
Get a key from <server> → My settings → Access keys.
```

For npm, which has no build cache, the first line stops at the Build Scans.

**Behaviour changes** are anything a maintainer would otherwise first notice in their own builds.
State each as what changed, without commentary:

- `org.gradle.caching=true` added to `gradle.properties`: the build cache is now on for every build, local ones included
- `clean` added to the Maven invocations in CI, each one listed
- `NODE_OPTIONS` now inherited by the scripts that override it, each one listed
- a test reporter added, or one that replaces the console reporter

The "Checked with" line is the outcome of step 7, the store gate included. Write what actually ran
and what it showed, including a check that could not run. 

Mention `allowUntrusted` only when it was set. Leaving it out is the normal case and needs no line in the description. 
Add anything that was guessed, and nothing else.

**Then stop and confirm before anything leaves the machine.** Show the user the diff, the branch
name, the target (upstream or fork), the title and the description, and wait. Push and open the pull
request only on their go-ahead: a pushed branch and an opened pull request are visible to the
target's maintainers and cannot be taken back quietly. Apply any edits they ask for and show the
result again.

## 9. Follow the checks

The pull request is not finished when it is opened. Wait for its checks and read any failure.
Opening a red pull request and walking away hands the reviewer your homework.

On GitHub:

```
gh pr checks <n> --watch
gh run view --job <job id> --log-failed
```

The triage below holds on any forge; only the commands are GitHub's. On GitLab, Bitbucket, Azure
Repos or a self-hosted forge, use that forge's CLI or API (`glab ci status`, the Bitbucket
pipelines API, `az pipelines runs show`). If the result cannot be read from here, say the checks were
not seen rather than reporting a pull request as green.

Every failure falls into one of three buckets, and they are handled differently:

- **The change broke the build.** Configuration that does not parse, a pipeline edit that is wrong
  for that provider, a lockfile that no longer matches. Fix it.
- **The change broke the build only because there is no access key yet.** This is still the change's
  fault. Nothing about onboarding may make a build fail before the secret exists (see *Rules*). Fix
  the configuration so the missing key degrades to a warning; do not answer it by asking for the
  secret to be created.
- **The repository's own gate, which the change cannot satisfy.** A contributor licence agreement or
  DCO sign-off, a required approval, a fork with no access to secrets, a commit-signing rule, a
  codeowner review. These are not defects in the configuration and cannot be fixed by editing it.
  Say in the pull request exactly which gate is red and what unblocks it (a CLA is signed by the
  author, a DCO needs `git commit -s`, secrets are unavailable to pull requests from forks by design)
  and tell the user, since most of these need a human.

**A fix-up is confirmed like the pull request was.** The step 8 approval covered that diff, not the
next one. Show the user each fix-up's diff and the failure it answers, and push only on their
go-ahead. Add commits rather than force-pushing over what maintainers may already have read.

Before calling any failure unrelated, pre-existing or flaky, **check the base branch**: look at its
own recent runs for the same job. A failure that is already red on the base is not yours; one that is
green there is, whatever it looks like. Never assert this without having looked.

Report the final state plainly: green, or red with which bucket and why.

## Rules

- **Never commit an access key**, in any file, including `.bazelrc`.
- **Nothing is created in someone's tracker without them seeing it first.** An issue, a ticket, a
  fork, a branch or a commit pushed to a shared remote, anything the skill has to produce: show the
  content, offer to create it as written or to edit it, and wait. This holds for every such step,
  not only the linked issue and the pull request in step 8.
- **Existing infrastructure is never replaced silently.** Another remote cache or build event
  backend means stop and ask (step 1).
- **CI is detected by the provider's own environment variable**, not `CI`. Jenkins, TeamCity,
  Bamboo and GoCD do not set `CI`, so the samples' `CI` check leaves those builds looking local,
  uploading Build Scans in the background and never populating the cache. Conversely a developer
  who exports `CI` locally would look like CI.
- **Writing to the remote cache requires CI *and* an access key.** Without the second condition a
  fork's CI build tries to push to a cache it cannot authenticate against, and every build logs the failure.
- **We are assuming that reading from the remote cache stays on for everyone.** Only writing is gated. Bazel is the
  exception: it fails the build on an unreachable cache, so its cache is on only with a key.
- **A missing access key must never fail the build.** The Gradle plugin and Maven extension already
  warn and carry on; Bazel does not, hence `--bes_upload_mode=nowait_for_upload_complete` and the
  key-gated `remote-cache` config. Check this
  on the pull request's own CI run, which is the first build to execute with no secret configured.
