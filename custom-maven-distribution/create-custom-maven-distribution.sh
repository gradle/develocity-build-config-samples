#!/bin/bash

#
# Script that demonstrates how to build a custom Maven distribution that bundles
#
#  - Gradle Enterprise Maven extension
#  - Common Custom User Data Maven extension that captures additional build metadata
#  - gradle-enterprise.xml with typical configuration for CI and developers building locally
#
# Ideally, the custom Maven distribution is then
#
#  - deployed to a repository like Artifactory, Nexus, etc.
#  - applied when building a Maven project via Maven Wrapper
#
# Having such a custom Maven distribution that bundles all Gradle Enterprise configuration is one way to share
# the configuration across multiple projects, in case the build master fully controls the Maven distribution
# used by CI and developers. In most cases though, the recommended way for sharing the Gradle Enterprise configuration
# across multiple projects is by referencing the extensions via .mvn/extensions.xml file checked in to every project.
#
# Note: You can build a Maven distribution that connects to _your_ Gradle Enterprise instance by running
#       this script and passing the root URL of your Gradle Enterprise server as the first argument, e.g.:
#
#       ./create-custom-maven-distribution.sh https://gradle-enterprise.mycompany.com
#

maven_version=3.8.6
maven_dir=apache-maven-${maven_version}
maven_zip=${maven_dir}-bin.zip
maven_lib_ext=${maven_dir}/lib/ext
maven_conf=${maven_dir}/conf

custom_maven_version=1.0.0
custom_maven_zip=${maven_dir}-sample-${custom_maven_version}-bin.zip

ge_ext_version=1.15.4
ge_ext_jar=gradle-enterprise-maven-extension-${ge_ext_version}.jar

ge_sample_ext_version=1.11.1
ge_sample_ext_jar=common-custom-user-data-maven-extension-${ge_sample_ext_version}.jar

ge_sample_ext_xml=gradle-enterprise.xml

server_url=

tmp_dir=.tmp
out_dir=out

yellow='\033[1;33m'
nc='\033[0m'

if [ -n "$1" ]
then
  server_url=$1
fi

mkdir -p $tmp_dir
rm -rf $out_dir

### Downloading and extracting Maven distribution

if [ ! -f "$tmp_dir/$maven_zip" ]; then
  echo -e "${yellow}Downloading Maven distribution $maven_version${nc}"
  maven_download_url=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/${maven_version}/${maven_zip}
  wget $maven_download_url -P $tmp_dir
fi

echo -e "${yellow}Extracting Maven distribution${nc}"
unzip -q $tmp_dir/$maven_zip -d $out_dir

### Downloading and embedding Gradle Enterprise Maven extension

if [ ! -f "$tmp_dir/$ge_ext_jar" ]; then
  echo -e "${yellow}Downloading Gradle Enterprise Maven extension $ge_ext_version${nc}"
  ge_ext_download_url=https://repo1.maven.org/maven2/com/gradle/gradle-enterprise-maven-extension/${ge_ext_version}/${ge_ext_jar}
  wget $ge_ext_download_url  -P $tmp_dir
fi

echo -e "${yellow}Copying Gradle Enterprise Maven extension into Maven distribution${nc}"
cp $tmp_dir/$ge_ext_jar $out_dir/$maven_lib_ext

### Downloading and embedding Common Custom User Data Maven extension

if [ ! -f "$tmp_dir/$ge_sample_ext_jar" ]; then
  echo -e "${yellow}Downloading common custom user data capturing Maven extension $ge_sample_ext_version${nc}"
  ge_sample_ext_download_url=https://repo1.maven.org/maven2/com/gradle/common-custom-user-data-maven-extension/${ge_sample_ext_version}/${ge_sample_ext_jar}
  wget $ge_sample_ext_download_url  -P $tmp_dir
fi

echo -e "${yellow}Copying common custom user data capturing Maven extension into Maven distribution${nc}"
cp $tmp_dir/$ge_sample_ext_jar $out_dir/$maven_lib_ext

### Downloading and embedding common gradle-enterprise.xml configuration for CI builds and developers building locally

if [ ! -f "$tmp_dir/$ge_sample_ext_xml" ]; then
  echo -e "${yellow}Downloading common gradle-enterprise.xml configuration for CI builds and developers building locally${nc}"
  ge_sample_ext_xml_download_url=https://raw.githubusercontent.com/gradle/gradle-enterprise-build-config-samples/master/common-gradle-enterprise-xml-maven-configuration/${ge_sample_ext_xml}
  wget $ge_sample_ext_xml_download_url  -P $tmp_dir
fi

echo -e "${yellow}Copying common gradle-enterprise.xml for developers building locally into Maven distribution${nc}"
cp $tmp_dir/$ge_sample_ext_xml $out_dir/$maven_conf

### If present, replace the default urls in the gradle-enterprise.xml file with the url passed on the command line

if [ -n "$server_url" ]; then
  echo -e "${yellow}Replacing the urls in gradle-enterprise.xml with ${server_url}${nc}"
  sed -i '' -e 's+https://enterprise-samples.gradle.com+'"$server_url"'+g' $out_dir/$maven_conf/$ge_sample_ext_xml
fi

### Packing the customized Maven distribution

echo -e "${yellow}Packaging custom Maven distribution${nc}"
(cd $out_dir && zip -qrX $custom_maven_zip $maven_dir)

exit 0
