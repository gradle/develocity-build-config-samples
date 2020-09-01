# Gradle Enterprise Maven Extension rollout script

This directory contains a script for rolling out the [Gradle Enterprise Maven Extension](https://docs.gradle.com/enterprise/maven-get-started/) to many repositories.

## Repository layout

- `rollout.sh` - the main rollout script.
- `repository.txt.template` - a template file for the list of repositories to roll out the extension to.
- `.mvn/extensions.xml` - the `extensions.xml` file that will be committed to each repository.
- `.mvn/gradle-enterprise.xml` - the `gradle-enterprise.xml` file that will be committed to each repository.

## Usage

1. Copy `repositories.txt.template` to `repositories.txt` in this directory
2. Replace the list of repositories in `repositories.txt` with the list of repositories you want to apply the extension to.
   Make sure each line contains one git repository URL.
3. Customize `.mvn/extensions.xml` and `.mvn/gradle-enterprise.xml` to your liking.
5. Run `./rollout.sh`. The script provides the following flags to control the behavior:
   * `-p`: Push changes to the listed repositories. When not passing `-p` the script will do a dry run.
   * `-f`: Force overriding of preexisting `extension.xml` and `gradle-enterprise.xml` files in the listed repositories.
   * `-u`: Only update repositories with an existing with `.mvn` folder.

## What it does

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

## Changelog

- 2020-09-01 - Added the `-p`, `-f` and `-u` flags to give more control over the behavior of the script
- 2020-07-27 - Initial release
