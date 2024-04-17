# Develocity Git User Count

## Overview

There are two scripts in this repository. 
The first script `unique-git-repos.sh` provides a means to gather a list of unique git repositories publishing scans to a Develocity instance.
The second script `user-count.sh` provides a means to automate the counting of the number of unique active developers in separate git repositories.

## Unique Git Repositories

The Develocity configuration must be configured to publish a custom value with the key `Git repository` to Develocity. This can be done automatically via the Common Custom User Data Gradle plugin or Maven extension.

### Usage

1. Run the `./unique-git-repos.sh` bash script. The script supports the following ordered command line arguments:
    * `<develocity-url>`: The URL of the Develocity instance.
    * `<access-key>`: The access token to use when connecting to the Develocity instance.
    * `<days>`: Number of days to go back in history (default: 7).
2. The script will output a list of unique git repositories to `repositories.txt`.

## Git User Count

### Usage

1. Update the `repositories.txt` file with the list of Git repositories that are connected to Develocity.
   Make sure each line contains a single Git repository URL.
2. Run the `./user-count.sh` bash script to count the number of users rollout. The script supports the following command line arguments:
    * `-b <branch-name>`: Count the users from the specified branch name (across all repositories).
    * `-s <since-days>`: Number of days to go back in history (default: 30).
    * `-c`: When specified does not pass `--shallow-since` flag to cloning options. Use this when encountering cloning issues.
    * `-o <git-options>`: Specify additional git cloning options (default: none).
3. Remove redundant users or CI emails from the `develocity-unique-users.txt` file.
4. Run `wc -l develocity-unique-users.txt` to get the number of unique users.

### How it works

The `user-count.sh` script reads the list of Git repositories from the `repositories.txt` file.
The script creates a temporary folder and clones the listed Git repositories to that folder in a minimal state.
It then writes all the unique emails committed to that repository to a file named `develocity-unique-users.txt`.
