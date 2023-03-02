## Gradle Enterprise Git User Count

### Overview

The Gradle Enterprise Git user count script provides a means to automate the counting of the number of unique active developers in separate Git repositories.

### Usage

1. Update the `repositories.txt` file with the list of Git repositories you want to apply the extension on.
   Make sure each line contains a single Git repository URL and that there is a newline at the end of th file.
2. Run the `./rollout.sh` bash script to count the number of users rollout. The script supports the following command line arguments:
3. Remove redundant or CI email from the `gradle-enterprise-unique-users.txt` file.
4. Run `wc -l gradle-enterprise-unique-users.txt` to get the number of unique users.

### How it works

The `rollout.sh` script reads the list of Git repositories from the `repositories.txt` file.
The script creates a temporary folder and clones the listed Git repositories to that folder.
It then writes all the unique emails committed to that repository to a file named `gradle-enterprise-unique-users.txt`.

