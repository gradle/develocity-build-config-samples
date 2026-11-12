#!/bin/sh
#
# Restores or stores the Develocity Artifact Cache and Setup Cache of a CI job, on any CI provider.
#
# Usage: artifact-cache.sh restore|store [Artifact Cache CLI options]
#
# Run 'restore' before the build and 'store' after a successful build. Each run is self-contained: it exchanges the
# access key for a short-lived token, gets the checksum-verified CLI from the Edge, and names the cache images.
#
# Environment variables:
#   DEVELOCITY_EDGE_URL         Develocity Edge URL, e.g. https://edge.example.com. Develocity 2026.1 and later also
#                               serve an Edge at the Develocity server URL
#   ARTIFACT_CACHE_ACCESS_KEY   Develocity access key, or a short-lived access token valid for at least another hour,
#                               as a bare value without a 'host=' prefix
#   ARTIFACT_CACHE_PROJECT      Repository or project identifier, e.g. my-group-my-project (required, see below)
#   ARTIFACT_CACHE_JOB          CI job name, e.g. build
#   ARTIFACT_CACHE_BRANCH       Branch being built, e.g. feature/x
#   ARTIFACT_CACHE_BASE_BRANCH  Branch whose image 'restore' falls back to while this branch has none, e.g. main
#                               (optional)
#   ARTIFACT_CACHE_JAVA_HOME    Java 21 or later to run the CLI with (optional, defaults to JAVA_HOME, then java on PATH)
#
# Images are named <project>~<job>~<branch>~<os>~<arch>. The Edge keys an image by its name only, so without the
# project identifier, jobs of different repositories with the same job and branch names would share one image.
#
# Cache problems never fail the build: the script warns and exits 0. Missing configuration, and a CLI download that
# fails its checksum verification, exit with an error.

set -u

# The CLI version to run, and the SHA-256 published for it at https://docs.develocity.ai/artifact-cache/current/download-the-cli/
cli_version=1.10.0
cli_sha256=664bfb44046db0fccb2518fff2e38eaa4c3e1ec2265adc54de6f3f30ee869db3

operation=${1:-}
case $operation in
  restore | store) shift ;;
  *) echo "Usage: $0 restore|store [Artifact Cache CLI options]" >&2 && exit 1 ;;
esac

edge_url=${DEVELOCITY_EDGE_URL:?must be set}
edge_url=${edge_url%/}
access_key=${ARTIFACT_CACHE_ACCESS_KEY:?must be set}
project=${ARTIFACT_CACHE_PROJECT:?must be set to a repository or project identifier}
job=${ARTIFACT_CACHE_JOB:?must be set}
branch=${ARTIFACT_CACHE_BRANCH:?must be set}
base_branch=${ARTIFACT_CACHE_BASE_BRANCH:-}
java_home=${ARTIFACT_CACHE_JAVA_HOME:-${JAVA_HOME:-}}

skip() {
  echo "WARNING: Develocity Artifact Cache: $1, skipping $operation" >&2
  exit 0
}

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1"; else shasum -a 256 "$1"; fi | cut -d ' ' -f 1
}

# 1. Exchange the credential for a short-lived token, so the access key itself never reaches the CLI
token=$(curl --fail --silent --show-error --retry 3 --request POST \
  --header "Authorization: Bearer $access_key" \
  "$edge_url/api/auth/token?expiresInHours=1") || skip "could not get a short-lived token from $edge_url"

# 2. Download the CLI from the Edge (Edge 2.3.0 or later), and verify its checksum before it ever runs
cli_jar="${TMPDIR:-/tmp}/develocity-artifact-cache-cli-$cli_version.jar"
if [ ! -f "$cli_jar" ] || [ "$(sha256 "$cli_jar")" != "$cli_sha256" ]; then
  curl --fail --silent --show-error --location \
    --connect-timeout 5 --max-time 120 --retry 3 --retry-delay 3 --retry-max-time 180 \
    --header "Authorization: Bearer $token" \
    --output "$cli_jar.$$" \
    "$edge_url/edge/assets/artifact-cache-cli/$cli_version" || skip "could not download the CLI from $edge_url"
  if [ "$(sha256 "$cli_jar.$$")" != "$cli_sha256" ]; then
    rm -f "$cli_jar.$$"
    echo "ERROR: Develocity Artifact Cache: the downloaded CLI does not match the SHA-256 $cli_sha256" >&2
    exit 1
  fi
  mv -f "$cli_jar.$$" "$cli_jar"
fi

# 3. Name the images: one per project, job, branch and platform, and restore falls back to the base branch's image
platform="$(uname -s | tr '[:upper:]' '[:lower:]')~$(uname -m)"
if [ "$operation" = restore ] && [ -n "$base_branch" ] && [ "$base_branch" != "$branch" ]; then
  set -- --image-name="$project~$job~$base_branch~$platform" "$@"
fi
set -- --image-name="$project~$job~$branch~$platform" "$@"

# 4. Run the CLI against the Edge, which takes the token from an access key entry for the Edge host
edge_host=${edge_url#*://}
edge_host=${edge_host%%/*}
edge_host=${edge_host%:*}
DEVELOCITY_ACCESS_KEY="$edge_host=$token" "${java_home:+$java_home/bin/}java" -jar "$cli_jar" "$operation" --dv-edge="$edge_url" "$@" ||
  skip "the CLI failed"
