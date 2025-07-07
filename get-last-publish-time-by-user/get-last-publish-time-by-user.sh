#!/usr/bin/env bash

# This script demonstrates how to retrieve the last published build time for each user
# provided via stdin, using the Develocity REST API to fetch build information.
# (https://docs.gradle.com/develocity/api-manual/)
#
# See samples for demos in other languages: https://github.com/gradle/develocity-api-samples
#
# To run it, set variables:
#   export DEVELOCITY_URL="https://develocity.example.com/api/"
#   export DEVELOCITY_ACCESS_KEY"your_access_key"
#
# USAGE:
#   cat users.txt | ./get-last-publish-time-by-user.sh

set -euo pipefail

which jq > /dev/null || echo "jq is required (brew install jq)" >&2

function get_last_build_info() {
    # Returns both build ID and availableAt time as a tab-separated string
    url="${DEVELOCITY_URL}/api/builds?maxBuilds=1&reverse=true&query=user:$1"
    curl -s -H "Authorization: Bearer $DEVELOCITY_ACCESS_KEY" "$url" \
        | jq -r '.[0] | if . != null then "\(.id)\t\(.availableAt)" else "" end'
}

function print_row() {
    printf "%-50s | %-24s | %s\n" "$1" "$2" "$3"
}

# For each user in stdin, get publish time and build ID, then format
while read -r user; do
    build_info=$(get_last_build_info "$user")
    if [[ -n "$build_info" ]]; then
        build_id=$(echo "$build_info" | cut -f1)
        last_publish_time=$(echo "$build_info" | cut -f2)

        if [[ -n "$last_publish_time" && "$last_publish_time" =~ ^[0-9]+$ ]]; then
            last_publish_time_seconds=$(echo "$last_publish_time / 1000" | bc)
            formatted_time=$(date -r "$last_publish_time_seconds" -u +"%Y-%m-%dT%H:%M:%SZ")
            print_row "$user" "$formatted_time" "$build_id"
        else
            print_row "$user" "Invalid timestamp" "$build_id"
        fi
    else
        print_row "$user" "" ""
    fi
done
