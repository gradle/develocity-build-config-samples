#!/bin/bash

commit_msg="Apply latest Gradle Enterprise configuration"
basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
repositories=$basedir/repositories.txt
checkout_area=

yellow='\033[1;33m'
nc='\033[0m'

# process arguments
while getopts "ufp" opt; do
    case $opt in
    u) do_update=true ;;
    f) force=true ;;
    p) push=true ;;
    \?) ;;
    esac
done

function prepare() {
  checkout_area=$( mktemp -d )
}

function process_repositories() {
  numOfRepos=$( sed "/^[[:blank:]]*#/d;s/^[[:blank:]]*//;s/#.*//" "$repositories" | wc -l )
  current=1
  while IFS= read -r repo
  do
    echo -e "${yellow}($current/$numOfRepos) Processing ${repo}...${nc}"
    process_repository "$repo"
    ((current++))
  done < <(sed "/^[[:blank:]]*#/d;s/^[[:blank:]]*//;s/#.*//" "$repositories")

  # remove duplicates in list of users
  sort -u "$basedir/gradle-enterprise-users.txt" > "$basedir/gradle-enterprise-unique-users.txt"
}

function process_repository() {
  repository_name="${1##*/}"

  # clone the Git epository without actually downloading files or history
  git clone -n "$1" "$checkout_area/$repository_name" >& /dev/null
  pushd "$checkout_area/$repository_name" >& /dev/null || return
  git reset HEAD . >& /dev/null

  # append the number of git users by email in the last 30 days to a file
  git log --format="%ae" --since=30.day | sort -u >> "$basedir/gradle-enterprise-users.txt"

  popd >& /dev/null || return
}

function cleanup() {
  if [ "$push" ]; then
    rm -rf "$checkout_area"
  else
    echo "All cloned repositories available at $checkout_area"
  fi
}

# entry point
if [ ! -d ".mvn" ]; then
  echo ".mvn directory is missing" >&2
  exit 1
elif [ ! -f "repositories.txt" ]; then
  echo "repositories.txt file is missing" >&2
  exit 1
else
  prepare
  process_repositories
  cleanup
  exit 0
fi
