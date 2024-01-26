#!/bin/bash

# Command line options for GRADLE_ENTERPRISE_URL and ACCESS_KEY
if [[ $# -lt 2 ]]; then
    echo "Usage: $0 <gradle_enterprise_url> <access_key> [number_of_days]"
    exit 1
fi

GRADLE_ENTERPRISE_URL="$1"
ACCESS_KEY="$2"
NUMBER_OF_DAYS="${3:-7}" # Default to 7 days if not provided
FROM_INSTANT=$(date -d "$NUMBER_OF_DAYS days ago" +%s)000  # Days ago in milliseconds

# Function to fetch builds
fetch_builds() {
    local from_build=$1
    local url="$GRADLE_ENTERPRISE_URL/api/builds?models=gradle-attributes&models=maven-attributes&fromInstant=$FROM_INSTANT&maxBuilds=1000"
    if [[ -n "$from_build" ]]; then
        url="$url&fromBuild=$from_build"
    fi
    curl -s -H "Authorization: Bearer $ACCESS_KEY" -H "Content-Type: application/json" "$url"
}

# Main loop to fetch all builds with pagination and extract Git repositories
git_repos=()
from_build=""
while :; do
    response=$(fetch_builds "$from_build")
    num_builds=$(echo "$response" | jq -r '. | length')

    # Progress message
    echo "Fetched $num_builds builds"
    
    # Extract Git repository values
    new_repos=$(echo "$response" | jq -r '.[] | .models.gradleAttributes.model.values[]?, .models.mavenAttributes.model.values[]? | select(.name=="Git repository").value // empty')
    git_repos+=("$new_repos")

    if [[ num_builds -eq 0 || num_builds -lt 1000 ]]; then
        break  # No more builds to fetch or fewer than 1000 builds
    fi

    from_build=$(echo "$response" | jq -r '.[-1].id')  # Set fromBuild for the next request
done

# Remove duplicates and count unique Git repositories
unique_git_repos=($(echo "${git_repos[@]}" | tr ' ' '\n' | sort -u | tr '\n' ' '))
num_unique_repos=${#unique_git_repos[@]}

# Print unique Git repositories and their count
echo "Number of unique Git repositories: $num_unique_repos"
printf "%s\n" "${unique_git_repos[@]}" > repositories.txt
echo "List of unique Git repositories saved to repositories.txt"
