#!/bin/bash

commit_msg="Apply Gradle Enterprise Maven Extension"
basedir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
repositories=$basedir/repositories.txt
extensions_xml=$basedir/extensions.xml
checkout_area=`mktemp -d`

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
  git checkout .mvn/extensions.xml >& /dev/null
  if [ $? -eq 0 ]; then
    echo ".mvn/extensions.xml already exists in $1, skipping..." >&2
  else
    # Only process maven projects
    if [ -z "$(git ls-tree -r HEAD --name-only | grep pom.xml)" ]; then  
      echo "$1 is not a mvn project, skipping..." >&2
    else 
      mkdir -p .mvn
      cp "$extensions_xml" .mvn
      git add .mvn/extensions.xml >& /dev/null
      git checkout .gitignore >& /dev/null
      echo ".mvn/.gradle-enterprise/" >> .gitignore
      git add .gitignore
      git commit -m "$commit_msg" >& /dev/null

      git push >& /dev/null
    fi
  fi
  popd >& /dev/null
}

function cleanup() {
  rm -rf $checkout_area
}

if [ ! -f "$repositories" ]; then
  echo "File $repositories is missing" >&2
  exit 1
elif [ ! -f "$extensions_xml" ]; then
  echo "File $extensions_xml is missing" >&2
  exit 1
else
  process_repositories
  cleanup
  exit 0
fi
