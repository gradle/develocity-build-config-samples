#!/bin/bash

#
# Script that runs the Maven wrapper generation goal, and then modifies the mvnw wrapper shell script such that
# a 'Wrapper' custom tag is added to all build scans.
#
# Having such a 'Wrapper' tag allows to query for all builds that have been invoked via Maven wrapper.
#
# Note: You can invoke the Maven wrapper generation tool in a custom directory by running
#       this script and passing the working directory as the first argument, e.g.:
#
#       mkdir /tmp/custom-maven-wrapper && ./create-custom-maven-wrapper.sh /tmp/custom-maven-wrapper
#

current_dir=$PWD
maven_version=3.8.6

yellow='\033[1;33m'
nc='\033[0m'

if [ -n "$1" ]
then
  cd "$1" || exit
fi

echo -e "${yellow}Installing Maven wrapper${nc}"
mvn -N io.takari:maven:0.7.7:wrapper -Dmaven=$maven_version

echo -e "${yellow}Customizing Maven wrapper script by adding a custom 'Wrapper' tag${nc}"
grep -q 'scan.tag.Wrapper' mvnw || sed -i '' $'s/^MAVEN_OPTS/&="-Dscan.tag.Wrapper $&"\\\n&/' mvnw

cd "$current_dir" || exit

exit 0
