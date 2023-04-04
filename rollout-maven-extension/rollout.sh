#!/bin/bash
set -e

basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
repositories=$basedir/repositories.txt
checkout_area=

yellow='\033[1;33m'
nc='\033[0m'

# -b option lets you specify a branch to checkout, store it in a variable
while getopts b: option
do
  case "${option}" in
    b) branch=${OPTARG};;
    *) echo "Usage: $0 [-b branch_name]" >&2
       exit 1;;
  esac
done

# print branch name argument if specified, otherwise print a message saying the default branch will be counted
if [ -z "$branch" ]; then
  echo "No branch name specified, counting commits on default branch"
else
  echo "Branch name is $branch"
fi

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

  # Clone the repository branch if specified (otherwise default branch)
  if [ -z "$branch" ]; then
    git clone -n "$1" "$checkout_area/$repository_name" >& /dev/null
  else
    git clone -n -b "$branch" "$1" "$checkout_area/$repository_name"
  fi
  pushd "$checkout_area/$repository_name" >& /dev/null || return
  git reset HEAD . >& /dev/null

  # append the number of git users by email in the last 30 days to a file
  git log --format="%ae" --since=30.day | sort -u >> "$basedir/gradle-enterprise-users.txt"

  popd >& /dev/null || return
}

function cleanup() {
  echo "All cloned repositories available at $checkout_area"
}

# entry point
if [ ! -f "repositories.txt" ]; then
  echo "repositories.txt file is missing" >&2
  exit 1
else
  prepare
  process_repositories
  cleanup
  exit 0
fi
