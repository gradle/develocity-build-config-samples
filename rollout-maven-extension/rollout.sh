#!/bin/bash
set -e

basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
repositories=$basedir/repositories.txt
userListFile="$basedir/gradle-enterprise-users.txt"
userUniqueListFile="$basedir/gradle-enterprise-unique-users.txt"
usersByRepoFile="$basedir/gradle-enterprise-users-by-repo.csv"
checkout_area=.tmp/repos

yellow='\033[1;33m'
nc='\033[0m'

# -b option lets you specify a branch to checkout, store it in a variable
while getopts b:s: option
do
  case "${option}" in
    b) branch=${OPTARG};;
    s) since=${OPTARG};;
    *) echo "Usage: $0 [-b branch_name] [-s since_days]" >&2
       exit 1;;
  esac
done

# print branch name argument if specified, otherwise print a message saying the default branch will be counted
if [ -z "$branch" ]; then
  echo "No branch name specified, counting commits on default branch"
else
  echo "Branch name is $branch"
fi

if [ -z "$since" ]; then
    since=30
fi

echo "Counting commits from the last $since days"

function prepare() {
  echo -n "" > "$userListFile"
  echo -n "" > "$userUniqueListFile"
  echo "repository,users" > "$usersByRepoFile"
}

function process_repositories() {
  numOfRepos=$(sed "/^[[:blank:]]*#/d;s/^[[:blank:]]*//;s/#.*//" "$repositories" | wc -l | tr -d '[:space:]')
  current=1
  while IFS= read -r repo
  do
    echo -e "${yellow}($current/$numOfRepos) Processing ${repo}...${nc}"
    process_repository "$repo"
    ((current++))
  done < <(sed "/^[[:blank:]]*#/d;s/^[[:blank:]]*//;s/#.*//" "$repositories")

  # remove duplicates in list of users
  sort -u "$userListFile" > "$userUniqueListFile"
}

function process_repository() {
  repository_name="${1//\//-}"

  # Clone the repository branch if specified (otherwise default branch)
  if [ -d "$checkout_area/$repository_name" ]; then
    if ! [ -z "$branch" ]; then
      pushd "$checkout_area/$repository_name" >& /dev/null || return
      git checkout "$branch"
      popd >& /dev/null || return
    fi
  elif [ -z "$branch" ]; then
    git clone -n "$1" "$checkout_area/$repository_name" >& /dev/null
  else
    git clone -n -b "$branch" "$1" "$checkout_area/$repository_name"
  fi

  pushd "$checkout_area/$repository_name" >& /dev/null || return
  git reset HEAD . >& /dev/null

  # append unique git usernames from commits in the last X days to a file
  git log --format="%ae" --since=${since}.day | sort -u >> "$userListFile"

  # append the number of unique git committers in the last X days to a file
  git log --format="%ae" --since=${since}.day | sort -u | wc -l | xargs echo "$1," >> "$usersByRepoFile"

  popd >& /dev/null || return
}

function cleanup() {
  echo "Unique usernames are stored in $(basename $userUniqueListFile)"
  echo "User counts by repository are stored in $(basename $usersByRepoFile)"
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
