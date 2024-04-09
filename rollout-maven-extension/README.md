## Develocity Maven Extension rollout script

### Overview

The Develocity Maven Extension rollout script provides a means to automate the application and upgrade of the [Develocity Maven extension](https://docs.gradle.com/enterprise/maven-extension) on multiple Maven projects stored in separate Git repositories.

### Usage

1. Update the `repositories.txt` file with the list of Git repositories you want to apply the extension on.
   Make sure each line contains a single Git repository URL.
1. Update the Maven `.mvn/extensions.xml` and the Develocity `.mvn/develocity.xml` files with your desired Develocity configuration.
1. Run the `./rollout.sh` bash script to execute the Develocity configuration rollout. The script supports the following command line arguments:
   * `-u`: Only update those Git repositories that already contain the `.mvn` folder where the configuration files are stored.
   * `-f`: Force-override any pre-existing `extensions.xml` and `develocity.xml` configuration files in the `.mvn` folder.
   * `-p`: Push the applied changes to the listed Git repositories. Omit the `-p` flag to do a dry run.

### How it works

The `rollout.sh` script reads the list of Git repositories with Maven projects to instrument from the `repositories.txt` file.
The script creates a temporary folder and clones the listed Git repositories to that folder.

If the `-u` flag is specified, the script only processes those repositories that already contain a `.mvn` folder.
If the `-u` flag is not specified, the script will process all repositories and create the `.mvn` folder for those repositories that do not already contain it.

For each processed repository, if the `-f` flag is specified, any pre-existing `extensions.xml` and `develocity.xml` configuration files in the `.mvn` folder are overridden.
If the `-f` flag is not specified, any pre-existing configuration files are not modified. The modifications are committed to the cloned repository.

For each repository, if the `-p` flag is specified, the committed changes are pushed to the remote Git repository, and the temporary folder is deleted once all repositories have been processed.
If the `-p` flag is not specified, the committed changes are not pushed, and the temporary folder with all the cloned repositories is kept for further inspection.

### Changelog

- 2020-09-01 - Add the `-p`, `-f`, and `-u` flags
- 2020-07-27 - Initial release
