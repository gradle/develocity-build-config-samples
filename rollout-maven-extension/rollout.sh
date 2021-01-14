#!/bin/bash

commit_msg="Apply Gradle Enterprise Maven Extension"
basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
checkout_area=`mktemp -d`
repositories=$basedir/repositories.txt

# process arguments "$1", "$2", ... (i.e. "$@")
while getopts "pfu" opt; do
    case $opt in
    p) push=true ;;   # Push only if -p is set
    f) force=true ;;  # Override existing gradle settings
    u) do_update=true ;; # Only update existing project with .mvn Folders 
    \?) ;; # Handle error: unknown option or missing required argument.
    esac
done

function process_repositories() {
  num=$(cat $repositories | sed '/^\s*$/d' | wc -l | awk '{ print $1 }')
  i=1
  for line in $(cat repositories.txt | grep .); do
    echo "($i/$num) Updating $line..."
    process_repository $line
    ((i++))
  done
}

function process_repository() {
  repository_name="${1##*/}"
  # clone the repository without actually downloading files or history
  git clone -n "$1" "$checkout_area/$repository_name" --depth 1 >& /dev/null
  pushd "$checkout_area/$repository_name" >& /dev/null
  git reset HEAD . >& /dev/null
  if [ ! -z  "$(git ls-tree -r HEAD --name-only | grep '^.mvn')" ] && [ -z "$force" ] && [ -z "$do_update" ]; then
    echo ".mvn directory already exists in $repository_name, skipping..." >&2
  elif [ -z "$(git ls-tree -r HEAD --name-only | grep pom.xml)" ]; then  
    # Only process maven projects
    echo "$repository_name is not a mvn project, skipping..." >&2
  elif [ -z  "$(git ls-tree -r HEAD --name-only | grep '^.mvn')" ] && [ ! -z "$do_update" ]; then
    echo "$repository_name is not enabled for gradle caching, skipping..." >&2
  else
    cp -R $basedir/.mvn/ .mvn
    
    git add .mvn/. >& /dev/null
    insert_gitignore
    git commit -m "$commit_msg" >& /dev/null

    if [ ! -z "$push" ]; then
      git push >& /dev/null
    fi
  fi
  popd >& /dev/null
}

function insert_gitignore() {
  git checkout -- .gitignore >& /dev/null
  if [ $? -eq 0 ]; then
    if ! grep -Fxq "/.mvn/.gradle-enterprise/" .gitignore ; then
      echo "/.mvn/.gradle-enterprise/" >>  .gitignore
    fi
  else
    echo "/.mvn/.gradle-enterprise/" > .gitignore
  fi
  git add .gitignore >& /dev/null
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
