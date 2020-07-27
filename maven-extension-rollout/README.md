# Gradle Enterprise Maven Extension rollout script

This directory contains a script for rolling out the [Gradle Enterprise Maven Extension](https://docs.gradle.com/enterprise/maven-get-started/) to many repositories.

## Repository layout

- `rollout.sh` - the main rollout script.
- `repository.txt.template` - a template file for the list of repositories to roll out the extension to.
- `extensions.xml.template` - a template file for the `extensions.xml` file that will be committed to each repository.

## Usage

1. Copy `repositories.txt.template` to `repositories.txt` in this directory
2. Replace the list of repositories in `repositories.txt` with the list of repositories you want to apply the extension to.
   Make sure sure each line contains one git repository URL.
3. Copy `extensions.xml.template` to `extensions.xml`. This file will be committed to the repositories in `repositories.txt`.
4. Customize `extensions.xml` to your liking.
5. Run `./rollout.sh`

## What it does

`rollout.sh` reads the list of repositories to modify from `repositories.txt`.
It creates a temporary folder and clones the repositories to that folder.
For each repository the script checks whether `.mvn/extensions.xml` does already exists.
If so, the repository is not modifed and script.
If not the `extensions.xml` file is copied to the cloned repository.
The change is then committed and pushed back to the default branch of the repository.
Afterwards the temporary work area is deleted.

## Limitations

- Updating an existing extensions.xml file inside a repository is not supported.

## Changelog

- 2020-07-27 - Initial release
