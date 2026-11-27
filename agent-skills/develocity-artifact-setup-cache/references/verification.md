# Verification

The repository is onboarded, so CI usually holds the key already, and the pull request's own CI run
exercises the cache. Two runs of the same job on the same commit prove it: the first fills the image,
the second restores from it.

## Run 1: the pull request's first run

Expect, for each wired job:

- **restore**: nothing restored. On a first rollout no image exists, neither for the branch nor for
  its fallback.
- **the build**: passes as it did before
- **store**: entries stored for each detected tool, with no failed entries

Where to read the store:

| Wiring | Where |
| --- | --- |
| GitHub Action | the job summary's *Store* row, on the run page. `gh` cannot read summaries: use a browser tool when one is available, else ask the user what the row shows. The step logs carry the CLI's output only when a phase warns. |
| Jenkins plugin | the *Store* row of the cache summary on the build page |
| Manual flow | the script's and the CLI's output in the job log, and the store metrics file kept as a job artifact |

A Build Scan cannot show the store: it is published before the store runs.

## Run 2: a re-run

Re-run the same job on the same commit. **Ask the user first**: a re-run shows on the pull request
for its maintainers.

- GitHub: `gh run rerun <run id> --job <job id> --repo <owner>/<name>`, with the job id from
  `gh run view <run id> --json jobs`
- GitLab: `glab ci retry <job id>`
- elsewhere: ask the user to re-run it, or wait for the next push to the pull request

Expect the restore to hit the image run 1 stored. Evidence, per job:

- **from CI**: the tools detected, the entries and size restored, the duration; read from the same
  places as the store, the *Restore* row on GitHub Actions and Jenkins
- **from the Build Scan**, for Gradle, and for Maven with the Common Custom User Data Maven
  extension 2.1.0 or later (without it, Maven scans show no cache activity): the Artifact Cache and
  Setup Cache custom values, which report the restore: artifacts
  restored and their size, **Failed artifacts 0**, **Missing artifacts 0**, and the Setup Cache's
  values; the `ARTIFACT_CACHE` and `SETUP_CACHE` tags. The *Network activity* view under
  *Performance* shows far fewer repository downloads than run 1's scan.

Find each scan's link in the build log, on the line after `Publishing Build Scan`. Read the values
through a connected Develocity MCP server, or the Develocity API (`/api/builds/<id>/gradle-attributes`
or `/maven-attributes`: `values` and `tags`) when this environment already has access to it.
Otherwise give the user the two scan links and the fields to check, and ask what they show. Never
read, ask for or use a key to get there.

## Reading the results

| Seen | Likely cause | Next |
| --- | --- | --- |
| Both phases skipped, a credential warning | no key in that job, or no entry for the endpoint's host | the key wiring in the provider reference |
| A 401 or 403 | the key's user cannot write cache data, an entitlement is off, or project-level access control blocks data without a project | the gate in step 3 |
| Run 2 restores fewer entries than run 1 stored, or Docker images are never restored, with no error | entries above the Edge's maximum artifact size, rejected silently: Gradle-provisioned JDKs, browsers, Docker layers | raise the limit; an administrator's task |
| Run 2 restores nothing | the image name differs between the runs (a segment that changes per run), or run 1's store did not run | compare the names in both logs |
| Failed artifacts above 0 | transfer errors | the CLI log |
| Missing artifacts above 0 | entries evicted on the Edge | Edge storage, an administrator's task |
| No Setup Cache entries on Gradle | the `Setup Cache` entitlement is off, `--disable-setup-cache`, Gradle below 8.1, or the checkout path changed between runs | `references/tools.md` |

## Forks, and secrets that do not reach the pull request

Pull requests from forks get no secrets, and a GitLab merge request from an unprotected branch gets
no protected variables: the cache skips both phases, by design. Say so in the pull request, and
verify on two default-branch runs after the merge: the first stores the default-branch image, the
second restores it. Offer to check them when the user is back after the merge.

## After the merge

The first default-branch run starts cold and stores the image that pull requests and new branches
fall back to from then on.

## Reporting

Report hits as confirmed only with run 2's numbers: entries and size restored, or the scan's
restored artifacts with failed and missing at 0. Without them, write "not verified" and why. For
measurement over time, point to the "Dependency Caching" dashboard in Develocity Reporting and
Visualization, filtered to the Develocity Artifact Cache. That filter needs the Develocity Gradle
plugin 4.6.0 or Maven extension 2.6.0; below them, artifacts show as "Unknown", so filter by the
`ARTIFACT_CACHE` tag instead. Savings are not this skill's claim.
