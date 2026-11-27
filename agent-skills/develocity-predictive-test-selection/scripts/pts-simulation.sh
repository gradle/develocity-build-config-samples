#!/usr/bin/env bash
# Fetches the PTS Simulator's per-task numbers for one project from a Develocity server, as JSON.
#
#   pts-simulation.sh <server url> <project> [days=28] [profile=standard] [tags]
#
# An empty <project> ("") returns every project the key may see.
#
# The access key is taken from DEVELOCITY_ACCESS_KEY (host=key or host1,host2=key entries separated
# by ';') or from the Gradle or Maven keys.properties for the server's host, and goes only into the
# request header, through stdin. It is never printed. Without a key the request is anonymous.
#
# Prints the response body on stdout and "HTTP <status>" on stderr (000 when the server cannot be reached).
# Exits 1 unless the status is 200.
set -euo pipefail

if [[ $# -lt 2 ]]; then
  echo "usage: $0 <server url> <project> [days=28] [profile=standard] [tags]" >&2
  exit 2
fi

server="${1%/}"
project="$2"
days="${3:-28}"
profile="${4:-standard}"
tags="${5:-}"

host="${server#*://}"
host="${host%%/*}"
host="${host%%:*}"

# Reads "host1,host2=key" lines on stdin and prints the key of the first line that names $host.
key_in() {
  local line h
  local -a hosts
  while IFS= read -r line; do
    [[ "$line" == *=* && "$line" != \#* ]] || continue
    IFS=',' read -ra hosts <<< "${line%%=*}"
    for h in "${hosts[@]}"; do
      if [[ "${h// /}" == "$host" ]]; then
        printf '%s' "${line#*=}"
        return 0
      fi
    done
  done
  return 1
}

key_for_host() {
  local file
  if [[ -n "${DEVELOCITY_ACCESS_KEY:-}" ]] && tr ';' '\n' <<< "$DEVELOCITY_ACCESS_KEY" | key_in; then
    return
  fi
  for file in "${GRADLE_USER_HOME:-$HOME/.gradle}/develocity/keys.properties" "$HOME/.m2/.develocity/keys.properties"; do
    [[ -f "$file" ]] && key_in < "$file" && return
  done
  return 0
}

iso_days_ago() {
  date -u -d "$1 days ago" +%Y-%m-%dT%H:%M:%SZ 2>/dev/null || date -u -v-"$1"d +%Y-%m-%dT%H:%M:%SZ
}

start="$(iso_days_ago "$days")"
end="$(date -u +%Y-%m-%dT%H:%M:%SZ)"

params=(
  --data-urlencode "startTimeMin=${start}"
  --data-urlencode "startTimeMax=${end}"
  --data-urlencode "selectionProfile=${profile}"
)
if [[ -n "$project" ]]; then
  params+=(--data-urlencode "project=${project}")
fi
if [[ -n "$tags" ]]; then
  params+=(--data-urlencode "tags=${tags}")
fi

body="$(mktemp)"
trap 'rm -f "$body"' EXIT

key="$(key_for_host)"
if [[ -n "$key" ]]; then
  status="$(printf 'Authorization: Bearer %s\n' "$key" | curl -sS -G -H @- -o "$body" -w '%{http_code}' \
    "${params[@]}" "${server}/predictive-test-selection/info/simulation-performance" || true)"
else
  status="$(curl -sS -G -o "$body" -w '%{http_code}' \
    "${params[@]}" "${server}/predictive-test-selection/info/simulation-performance" || true)"
fi
unset key

cat "$body"
echo "HTTP ${status} (window ${start} to ${end}, profile ${profile})" >&2
[[ "$status" == "200" ]]
