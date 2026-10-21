# CI provider environment variables

The variable identifying each provider. Presence is the check, never the value, since providers
disagree on whether it is `true`, `1` or a URL.

The *Source* column says where each row comes from. *CCUD* rows match the
[Common Custom User Data Gradle plugin](https://github.com/gradle/common-custom-user-data-gradle-plugin/blob/main/src/main/java/com/gradle/CiUtils.java),
though not perfectly: CCUD also counts a set `CI` as CI, while a config generated from this table
only checks the provider's own variable. A developer who exports `CI` locally therefore gets a CI
tag from CCUD on a build this config treats as local.

| Provider | Environment variable | Marker in the repository | Source |
| --- | --- | --- | --- |
| GitHub Actions | `GITHUB_ACTIONS` | `.github/workflows/` | CCUD |
| GitLab CI | `GITLAB_CI` | `.gitlab-ci.yml` | CCUD |
| Jenkins | `JENKINS_URL` | `Jenkinsfile` | CCUD |
| Hudson | `HUDSON_URL` | none | CCUD |
| TeamCity | `TEAMCITY_VERSION` | `.teamcity/` | CCUD |
| CircleCI | `CIRCLE_BUILD_URL` | `.circleci/config.yml` | CCUD |
| Bamboo | `bamboo_resultsUrl` | `bamboo-specs/` | CCUD |
| Travis CI | `TRAVIS_JOB_ID` | `.travis.yml` | CCUD |
| Bitrise | `BITRISE_BUILD_URL` | `bitrise.yml` | CCUD |
| GoCD | `GO_SERVER_URL` | none | CCUD |
| Azure Pipelines | `TF_BUILD` | `azure-pipelines.yml` | CCUD |
| Buildkite | `BUILDKITE` | `.buildkite/` | CCUD |
| Bitbucket Pipelines | `BITBUCKET_BUILD_NUMBER` | `bitbucket-pipelines.yml` | [Atlassian](https://support.atlassian.com/bitbucket-cloud/docs/variables-and-secrets/) |

`CI` is the fallback when no provider can be identified, not a row of this table. Plenty of
providers leave it unset, and some developers export it locally, so a config built on `CI` is wrong
in both directions.

To add a provider: find its predefined-variables page, pick a variable that is always set and unique
to it (a build id or a server URL, not something a user configures) and add a row with the link
as its source. The skill only proposes such a row; a person adds it.
