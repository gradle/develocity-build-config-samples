## Gradle Enterprise Maven Extension rollout script

### Overview

The Gradle Enterprise Maven Extension rollout script allows to automate the application and upgrade of the [Gradle Enterprise Maven extension](https://docs.gradle.com/enterprise/maven-extension) on multiple Maven projects stored in separate Git repositories.

### Usage

1. Update the `repositories.txt` file with the list of Git repositories you want to apply the extension on.
   Make sure each line contains a single Git repository URL.
1. Update the `extensions.xml` and `gradle-enterprise.xml` files with your desired Gradle Enterprise configuration.
1. Run the `./rollout.sh` bash script to execute the Gradle Enterprise configuration rollout. The script provides the following execution flags:
   * `-p`: Push changes to the listed repositories. When not passing `-p` the script will do a dry run.
   * `-f`: Force overriding of preexisting `extension.xml` and `gradle-enterprise.xml` files in the listed repositories.
   * `-u`: Only update repositories with an existing with `.mvn` folder.

### What it does

`rollout.sh` reads the list of repositories to modify from `repositories.txt`.
It creates a temporary folder and clones the repositories to that folder.
If `-u` is specified the script will only process repositories that already contain a `.mvn` folder.
If `-u` is not specified repositories without `.mvn` folder are skipped.
For each repository the script does the following depending on `-f`:
If `-f` is specified preexisting `extension.xml` and `gradle-enterprise.xml` files will be overridden by the files in this directory under `.mvn`.
If `-f` is not specified preexisting files are not modified. 
The changes are then committed to the cloned repositories.
If `-p` is specified the changes are also pushed back the remote repositories and the temporary work area is deleted.
If `-p` not specified changes will not be pushed back, but the location of the temporary work area will be logged.
This way the result of running the script can be examined without changing the remote repositories.

### Changelog

- 2020-09-01 - Added the `-p`, `-f` and `-u` flags to give more control over the behavior of the script
- 2020-07-27 - Initial release
