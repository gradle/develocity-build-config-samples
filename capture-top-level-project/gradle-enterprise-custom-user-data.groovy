// This script is meant to be used with the common-custom-user-data-maven-extension.
// It needs to be placed in .mvn/gradle-enterprise-custom-user-data.groovy in the root project directory.
// (see https://docs.gradle.com/enterprise/maven-extension/#using_the_common_custom_user_data_maven_extension)

buildScan.value('topLevelProject.name', session.topLevelProject.name)
buildScan.value('topLevelProject.artifactId', session.topLevelProject.artifactId)
