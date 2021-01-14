## Gradle Enterprise Maven Extension rollout script

### Overview

The Gradle Enterprise Maven Extension rollout script provides a means to automate the application and upgrade of the [Gradle Enterprise Maven extension](https://docs.gradle.com/enterprise/maven-extension) on multiple Maven projects stored in separate Git repositories.

### Usage

1. Update the `repositories.txt` file with the list of Git repositories you want to apply the extension on.
   Make sure each line contains a single Git repository URL.
1. Update the Maven `.mvn/extensions.xml` and the Gradle Enterprise `.mvn/gradle-enterprise.xml` files with your desired Gradle Enterprise configuration.
1. Run the `./rollout.sh` bash script to execute the Gradle Enterprise configuration rollout. The script supports the following command line arguments:
   * `-u`: Only update those Git repositories that already contain the `.mvn` folder where the configuration files are stored.
   * `-f`: Force override of any pre-existing `extensions.xml` and `gradle-enterprise.xml` configuration files in the `.mvn` folder.
   * `-p`: Push the applied changes to the listed Git repositories.

### How it works

The `rollout.sh` script reads the list of Git repositories with Maven projects to instrument from the `repositories.txt` file.
The script creates a temporary folder and clones the listed Git repositories to that folder.

If the `-u` flag is specified, the script only processes those repositories that already contain a `.mvn` folder.

For each repository, if the `-f` flag is specified, any pre-existing `extensions.xml` and `gradle-enterprise.xml` configuration files in the `.mvn` folder are overridden.
If the `-f` flag is not specified, any pre-existing configuration files are not modified. The modifications are then committed to the cloned Git repositories.

If the `-p` flag is specified, the committed changes are pushed to the remote Git repositories and the temporary work area is deleted.

### Changelog

- 2020-09-01 - Add the `-p`, `-f`, and `-u` flags
- 2020-07-27 - Initial release
