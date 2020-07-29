#!/bin/bash

commit_msg="Apply Gradle Enterprise Maven Extension"
basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
checkout_area=`mktemp -d`
repositories=$basedir/repositories.txt

# process arguments "$1", "$2", ... (i.e. "$@")
while getopts "pf" opt; do
    case $opt in
    p) push=true ;; 
    f) force=true ;;
    \?) ;; # Handle error: unknown option or missing required argument.
    esac
done

function process_repositories() {
  num=`wc -l "$repositories" | awk '{ print $1 }'`
  i=1
  while read line; do
  echo "($i/$num) Updating $line..."
  process_repository $line
  ((i++))
  done < $repositories
}

function process_repository() {
  repository_name="${1##*/}"
  # clone the repository without actually downloading files or history
  git clone -n "$1" "$checkout_area/$repository_name" --depth 1 >& /dev/null
  pushd "$checkout_area/$repository_name" >& /dev/null
  git reset HEAD . >& /dev/null
  if [ ! -z  "$(git ls-tree -r HEAD --name-only | grep '^.mvn')" ] && [ -z "$force" ]; then
    echo ".mvn directory already exists in $1, skipping..." >&2
  else
    # Only process maven projects
    if [ -z "$(git ls-tree -r HEAD --name-only | grep pom.xml)" ]; then  
      echo "$1 is not a mvn project, skipping..." >&2
    else 
      cp -R $basedir/.mvn/ .mvn
      git add .mvn/. >& /dev/null
      git commit -m "$commit_msg" >& /dev/null

      if [ ! -z "$push" ]; then
        git push >& /dev/null
      fi
    fi
  fi
  popd >& /dev/null
}

function cleanup() {
  if [ ! -z "$push" ]; then
    rm -rf $checkout_area
  else
    echo "Stored repos to " $checkout_area
  fi
}

if [ ! -d ".mvn" ]; then
  echo ".mvn directory is missing" >&2
  exit 1
else
  process_repositories
  cleanup
  exit 0
fi
