#!/usr/bin/env bash
#
# Runs Experiment 05 - Validate Gradle Remote Build Caching - CI and Local
#
# Invoke this script with --help to get a description of the command line arguments
#
readonly EXP_NAME="Validate remote build caching - CI and local"
readonly EXP_DESCRIPTION="Validating that a Maven build is optimized for remote build caching when invoked on CI agent and local machine"
readonly EXP_NO="04"
readonly EXP_SCAN_TAG=exp4-maven
readonly BUILD_TOOL="Maven"
readonly SCRIPT_VERSION="<HEAD>"
readonly SHOW_RUN_ID=false

# Needed to bootstrap the script
SCRIPT_NAME=$(basename "$0")
readonly SCRIPT_NAME
SCRIPT_DIR="$(cd "$(dirname "$(readlink -e "${BASH_SOURCE[0]}")")" && pwd)"
readonly SCRIPT_DIR
readonly LIB_DIR="${SCRIPT_DIR}/../lib"

# Include and parse the command line arguments
# shellcheck source=lib/04-cli-parser.sh
source "${LIB_DIR}/${EXP_NO}-cli-parser.sh" || { echo "Couldn't find '${LIB_DIR}/${EXP_NO}-cli-parser.sh' parsing library."; exit 1; }
# shellcheck source=lib/libs.sh
source "${LIB_DIR}/libs.sh" || { echo "Couldn't find '${LIB_DIR}/libs.sh'"; exit 1; }

# These will be set by the config functions (see lib/config.sh)
git_repo=''
project_name=''
git_branch=''
project_dir=''
tasks=''
extra_args=''
ge_server=''
interactive_mode=''

ci_build_scan_url=''
remote_build_cache_url=''
mapping_file=''

main() {
  process_script_arguments
  if [ "${interactive_mode}" == "on" ]; then
    wizard_execute
  else
    execute
  fi
  create_receipt_file
}

execute() {
  print_bl
  validate_required_script_arguments
  fetch_build_scan_data
  validate_build_config

  print_bl
  make_experiment_dir
  git_checkout_project ""
  print_bl
  execute_build

  print_bl
  print_summary
  print_bl
}

wizard_execute() {
  print_bl
  print_introduction

  print_bl
  explain_prerequisites_ccud_maven_plugin

  print_bl
  explain_prerequisites_remote_build_cache_config

  print_bl
  explain_prerequisites_empty_remote_build_cache

  print_bl
  explain_ci_build
  print_bl
  collect_ci_build_scan

  print_bl
  explain_collect_mapping_file
  print_bl
  collect_mapping_file

  print_bl
  fetch_build_scan_data

  print_bl
  explain_collect_git_details
  print_bl
  collect_git_details

  print_bl
  explain_collect_maven_details
  print_bl
  collect_maven_details

  print_bl
  explain_remote_build_cache_url
  print_bl
  collect_remote_build_cache_url

  print_bl
  explain_clone_project
  print_bl
  make_experiment_dir
  git_checkout_project ""

  print_bl
  explain_build
  print_bl
  execute_build

  print_bl
  explain_measure_build_results
  print_bl
  explain_and_print_summary
  print_bl
}

process_script_arguments() {
  ci_build_scan_url="${_arg_first_build_ci}"
  remote_build_cache_url="${_arg_remote_build_cache_url}"
  mapping_file="${_arg_mapping_file}"
}

validate_required_script_arguments() {
  if [ -z "${ci_build_scan_url}" ]; then
    _PRINT_HELP=yes die "ERROR: Missing required argument: --first-build-ci" 1
  fi
}

fetch_build_scan_data() {
  fetch_and_read_build_validation_data "${ci_build_scan_url}"
  read_build_params_from_build_validation_data
}

read_build_params_from_build_validation_data() {
  if [ -z "${git_repo}" ]; then
    git_repo="${git_repos[0]}"
    project_name="$(basename -s .git "${git_repo}")"
  fi
  if [ -z "${git_branch}" ]; then
    git_branch="${git_branches[0]}"
  fi
  if [ -z "${git_commit_id}" ]; then
    git_commit_id="${git_commit_ids[0]}"
  fi
  if [ -z "${remote_build_cache_url}" ]; then
    remote_build_cache_url="${remote_build_cache_urls[0]}"
  fi
  if [ -z "${tasks}" ]; then
    tasks="${requested_tasks[0]}"
  fi
}

validate_build_config() {
  if [ -z "${git_repo}" ]; then
    _PRINT_HELP=yes die "ERROR: The Git repository URL was not found in the build scan. Please specify --git-repo and try again." 1
  fi
  if [ -z "${tasks}" ]; then
      _PRINT_HELP=yes die "ERROR: The Maven goals were not found in the build scan. Please specify --goals and try again." 1
  fi
  if [ -z "${git_commit_id}" ]; then
      _PRINT_HELP=yes die "ERROR: The Git commit id was not found in the build scan. Please specify --git-commit-id and try again." 1
  fi

  if [[ "${enable_ge}" == "on" ]]; then
    if [ -z "${ge_server}" ]; then
      _PRINT_HELP=yes die "ERROR: --gradle-enterprise-server is required when using --enable-gradle-enterprise."
    fi
  fi
}

execute_build() {
  local args
  args=(-Dgradle.cache.local.enabled=false -Dgradle.cache.remote.enabled=true)
  if [ -n "${remote_build_cache_url}" ]; then
    args+=("-Dgradle.cache.remote.url=${remote_build_cache_url}")
  fi

  # shellcheck disable=SC2206  # we want tasks to expand with word splitting in this case
  args+=(${tasks})

  info "Running build:"
  info "./mvnw -Dscan.tag.${EXP_SCAN_TAG} -Dscan.value.runId=${RUN_ID} clean ${tasks}$(print_extra_args)"

  # shellcheck disable=SC2086  # we want tasks to expand with word splitting in this case
  invoke_maven "${args[@]}"
}

# Overrides info.sh#print_experiment_specific_info
print_experiment_specific_info() {
  summary_row "Custom value mapping file:" "${mapping_file:-<none>}"
}

print_quick_links() {
  info "Investigation Quick Links"
  info "-------------------------"
  summary_row "Goal execution overview:" "${base_urls[0]}/s/${build_scan_ids[1]}/performance/execution"
  summary_row "Executed goals timeline:" "${base_urls[0]}/s/${build_scan_ids[1]}/timeline?outcome=successful,failed&sort=longest"
  summary_row "Executed cacheable goals:" "${base_urls[0]}/s/${build_scan_ids[1]}/timeline?cacheability=cacheable&outcome=successful,failed&sort=longest"
  summary_row "Executed non-cacheable goals:" "${base_urls[0]}/s/${build_scan_ids[1]}/timeline?cacheability=any_non-cacheable&outcome=successful,failed&sort=longest"
  summary_row "Build caching statistics:" "${base_urls[0]}/s/${build_scan_ids[1]}/performance/build-cache"
  summary_row "Goal inputs comparison:" "${base_urls[0]}/c/${build_scan_ids[0]}/${build_scan_ids[1]}/goal-inputs?cacheability=cacheable"
}

print_introduction() {
  local text
  IFS='' read -r -d '' text <<EOF
$(print_introduction_title)

In this experiment, you will validate how well a given project leverages
Gradle Enterprise's remote build caching functionality when running the build from
a CI agent and then on a local machine. A build is considered fully cacheable
if it can be invoked twice in a row with build caching enabled and, during the
second invocation, all cacheable goals avoid performing any work because:

  * The cacheable goals' inputs have not changed since their last invocation and
  * The cacheable goals' outputs are present in the remote build cache and
  * No cacheable goals were excluded from build caching to ensure correctness

The experiment will reveal goals with volatile inputs, for example goals that
contain a timestamp in one of their inputs. It will also reveal goals that
produce non-deterministic outputs consumed by cacheable goals downstream, for
example goals generating code with non-deterministic method ordering or goals
producing artifacts that include timestamps. It will also reveal goals that
contain an absolute file path in one of their inputs.

The experiment will assist you to first identify those goals whose outputs are
not taken from the remote build cache due to changed inputs or to ensure
correctness of the build, to then make an informed decision which of those goals
are worth improving to make your build faster, to then investigate why they are
not taken from the remote build cache, and to finally fix them once you
understand the root cause.

The first part of the experiment runs in your CI environment and the second
part of the experiment runs on any developer's machine. It logically consists of
the following steps:

  1. Enable only remote build caching and use an empty remote build cache
  2. On a given CI agent, run a typical CI configuration from a fresh checkout
  3. On a developer machine, run the build with the same goal invocation including the ‘clean’ goal with the same commit id
  4. Determine which cacheable goals are still executed in the second run and why
  5. Assess which of the executed, cacheable goals are worth improving
  6. Fix identified goals

The script you have invoked does not automate the execution of step 1 and step 2.
You will need to complete these steps manually. The script automates the
execution of step 3 without modifying the project. Build scans support your
investigation in step 4 and step 5.

After improving the build to make it better leverage the remote build cache, you
can push your changes and run the experiment again. This creates a cycle of run
→ measure → improve → run.

${USER_ACTION_COLOR}Press <Enter> to get started with the experiment.${RESTORE}
EOF

  print_wizard_text "${text}"
  wait_for_enter
}

explain_prerequisites_ccud_maven_plugin() {
  local text
  IFS='' read -r -d '' text <<EOF
$(print_separator)
${HEADER_COLOR}Preparation I. - Configure build with Common Custom User Data Maven extension${RESTORE}

To get the most out of this experiment and also when building with Gradle
Enterprise during daily development, it is advisable that you apply the Common
Custom User Data Maven extension to your build, if not already the case. Gradle
provides the Common Custom User Data Maven extension as a free, open-source add-on.

https://github.com/gradle/gradle-enterprise-build-config-samples/tree/master/common-custom-user-data-maven-extension

An extract of a typical build configuration is described below.

.mvn/extensions.xml:
<?xml version="1.0" encoding="UTF-8"?>
<extensions>
    <extension>
        <groupId>com.gradle</groupId>
        <artifactId>gradle-enterprise-maven-extension</artifactId>
        <version>1.11.1</version>
    </extension>
    <extension>
        <groupId>com.gradle</groupId>
        <artifactId>common-custom-user-data-maven-extension</artifactId>
        <version>1.9</version>
    </extension>
</extensions>

Your updated build configuration should be pushed before proceeding.

${USER_ACTION_COLOR}Press <Enter> once you have (optionally) configured your build with the Common Custom User Data Maven extension and pushed the changes.${RESTORE}
EOF
  print_wizard_text "${text}"
  wait_for_enter
}

explain_prerequisites_remote_build_cache_config() {
  local text
  IFS='' read -r -d '' text <<EOF
$(print_separator)
${HEADER_COLOR}Preparation II. - Configure build for remote build caching${RESTORE}

You must first configure your build for remote build caching. An extract of a
typical build configuration is described below.

.mvn/gradle-enterprise.xml:
<gradleEnterprise>
  <buildCache>
    <local>
      <enabled>false</enabled>                         <!-- must be false for this experiment -->
    </local>
    <remote>
      <server>
        <url>https://ge.example.com/cache/exp3/</url>  <!-- adjust to your GE hostname, and note the trailing slash -->
        <allowUntrusted>true</allowUntrusted>          <!-- set to false if a trusted certificate is configured for the GE server -->
        <credentials>
          <username>\${env.GRADLE_ENTERPRISE_CACHE_USERNAME}</username>
          <password>\${env.GRADLE_ENTERPRISE_CACHE_PASSWORD}</password>
        </credentials>
      </server>
      <enabled>true</enabled>                          <!-- must be true for this experiment -->
    </remote>
  </buildCache>
</gradleEnterprise>

On CI, you will also need to pass -Dgradle.cache.remote.storeEnabled=true to the Maven invocation
so that the CI build populates the remote build cache.

Your updated build configuration needs to be pushed to a separate branch that
is only used for running the experiments.

${USER_ACTION_COLOR}Press <Enter> once you have configured your build for remote build caching and pushed the changes.${RESTORE}
EOF
  print_wizard_text "${text}"
  wait_for_enter
}

explain_prerequisites_empty_remote_build_cache() {
  local text
  IFS='' read -r -d '' text <<EOF
$(print_separator)
${HEADER_COLOR}Preparation III. - Purge remote build cache${RESTORE}

It is important to use an empty remote build cache and to avoid that other
builds write to the same remote build cache while this experiment is running.
Ensuring that these preconditions are met will maximize the reproducibility
and reliability of the experiment. Two different ways to meet these conditions
are described below.

a) If none of your builds are yet writing to the remote build cache besides
the builds of this experiment, purge the remote build cache that your build
is configured to connect to. You can purge the remote build cache by navigating
in the browser to the 'Build Cache' admin section from the user menu of your
Gradle Enterprise UI, selecting the build cache node the build is pointing to,
and then clicking the 'Purge cache' button.

b) If you are not in a position to purge the remote build cache, you can connect
to a unique shard of the remote build cache each time you run this experiment.
A shard is accessed via an identifier that is appended to the path of the remote
build cache URL, for example https://ge.example.com/cache/exp4-2021-Dec31-take1/
which encodes the experiment type, the current date, and a counter that needs
to be increased every time the experiment is rerun. Using such an encoding
schema ensures that for each run of the experiment an empty remote build cache
will be used. You need to push the changes to the path of the remote build cache
URL every time before you run the experiment.

If you choose option b) and do not want to interfere with an already existing
build caching configuration in your build and you are using the Common Custom
User Data Maven extension, you can override the local and remote build cache
configuration via system properties or environment variables right when
triggering the build on CI. Details on how to provide the overrides are
available from the documentation of the plugin.

https://docs.gradle.com/enterprise/maven-extension/#configuring_the_remote_cache

${USER_ACTION_COLOR}Press <Enter> once you have prepared the experiment to run with an empty remote build cache.${RESTORE}
EOF
  print_wizard_text "${text}"
  wait_for_enter
}

explain_ci_build() {
  local text
  IFS='' read -r -d '' text <<EOF
$(print_separator)
${HEADER_COLOR}Run first build on CI agent${RESTORE}

You can now trigger the first build on one of your CI agents. The invoked CI
configuration should be a configuration that is typically triggered when
building the project as part of your pipeline during daily development.

Make sure the CI configuration uses the proper branch and performs a fresh
checkout to avoid any build artifacts lingering around from a previous build
that could influence the experiment.

Once the build completes, make a note of the commit id that was used, and enter
the URL of the build scan produced by the build.
EOF
  print_wizard_text "${text}"
}

collect_ci_build_scan() {
  prompt_for_setting "What is the build scan URL of the build run on CI?" "${_arg_first_build_ci}" "" ci_build_scan_url
}

explain_collect_mapping_file() {
  local text
  IFS='' read -r -d '' text <<EOF
$(print_separator)
${HEADER_COLOR}Fetch build scan data${RESTORE}

Now that the build on CI has finished successfully, some of the build scan
data will be fetched from the provided build scan to assist you in your
investigation.

The build scan data will be fetched via the Gradle Enterprise Export API. It is
not strictly necessary that you have permission to call the Export API while
doing this experiment, but the summary provided at the end of the experiment
will be more comprehensive if the build scan data is accessible. You can check
your granted permissions by navigating in the browser to the 'My Settings'
section from the user menu of your Gradle Enterprise UI. Your Gradle Enterprise
access key must be specified in the ~/.m2/.gradle-enterprise/keys.properties file.

https://docs.gradle.com/enterprise/gradle-plugin/#via_file

Some of the fetched build scan data is expected to be present as custom values.
By default, this experiment assumes that these custom values have been created
by the Common Custom User Data Maven extension. If you are not using that extension
but your build still captures the same data under different custom value names,
you can provide a mapping file so that the required data can be extracted from
your build scans. An example mapping file named 'mapping.example' can be found
at the same location as the script.
EOF
  print_wizard_text "${text}"
}

# This overrides explain_collect_git_details found in lib/wizard.sh
explain_collect_git_details() {
  local text
  IFS='' read -r -d '' text <<EOF
$(print_separator)
${HEADER_COLOR}Configure local build${RESTORE}

Now that the first build has finished successfully on CI and the build scan
data has been fetched or at least attempted to be fetched, the second build
can be run locally with the same commit id, the same Gradle tasks, and the
same remote build cache as was used by the first build.

The local build will run after a fresh checkout of the given project stored in
Git. The fresh checkout ensures reproducibility of the experiment across machines
and users since no local changes and commits will be accidentally included in the
validation process.

Make sure the local build uses the proper branch and commit id.
EOF
  print_wizard_text "${text}"
}

# This overrides explain_collect_gradle_details found in lib/wizard.sh
explain_collect_gradle_details() {
  local text
  IFS='' read -r -d '' text <<EOF
Once the project is checked out from Git, the experiment will invoke the
project’s contained Maven build with a given set of goals and an optional set
of arguments. The Maven goals to invoke should be the same, or very similar to
the goals invoked by the CI build.

The build will be invoked from the project’s root directory or from a given
sub-directory.
EOF
  print_wizard_text "${text}"
}

explain_remote_build_cache_url() {
  local text
  IFS='' read -r -d '' text <<EOF
The local build will connect to the given remote build cache. The remote build
cache to use should be the same as the one used by the previous CI build.
EOF
  print_wizard_text "${text}"
}

collect_remote_build_cache_url() {
  local default_remote_cache="<project default>"
  prompt_for_setting "What is the remote build cache url to use?" "${remote_build_cache_url}" "${default_remote_cache}" remote_build_cache_url

  if [[ "${remote_build_cache_url}" == "${default_remote_cache}" ]]; then
    remote_build_cache_url=''
  fi
}

# This overrides explain_clone_project found in lib/wizard.sh
explain_clone_project() {
  local text
  IFS='' read -r -d '' text <<EOF
$(print_separator)
${HEADER_COLOR}Check out project from Git for local build${RESTORE}

All configuration to run the local build has been collected. The Git repository
that contains the project to validate will be checked out.

${USER_ACTION_COLOR}Press <Enter> to check out the project from Git.${RESTORE}
EOF
  print_wizard_text "${text}"
  wait_for_enter
}

explain_build() {
  local text
  IFS='' read -r -d '' text <<EOF
$(print_separator)
${HEADER_COLOR}Run local build${RESTORE}

Now that the project has been checked out, the local build can be run with the
given Maven goals.

${USER_ACTION_COLOR}Press <Enter> to run the local build of the experiment.${RESTORE}
EOF
  print_wizard_text "${text}"
  wait_for_enter
}

explain_measure_build_results() {
  local text
  IFS='' read -r -d '' text <<EOF
$(print_separator)
${HEADER_COLOR}Measure build results${RESTORE}

At this point, you are ready to measure in Gradle Enterprise how well your
build leverages Gradle Enterprise's remote build cache for the set of Gradle tasks invoked
from a CI agent and then on a local machine.

${USER_ACTION_COLOR}Press <Enter> to measure the build results.${RESTORE}
EOF
  print_wizard_text "${text}"
  wait_for_enter
}

#Overrides config.sh#print_command_to_repeat_experiment
print_command_to_repeat_experiment() {
  local cmd
  cmd=("./${SCRIPT_NAME}")

  if [ -n "${git_repo}" ] && [[ "${git_repo}" != "${git_repos[0]}" ]]; then
    cmd+=("-r" "${git_repo}")
  fi

  if [ -n "${git_branch}" ] && [[ "${git_branch}" != "${git_branches[0]}" ]]; then
    cmd+=("-b" "${git_branch}")
  fi

  if [ -n "${git_commit_id}" ] && [[ "${git_commit_id}" != "${git_commit_ids[0]}" ]]; then
    cmd+=("-c" "${git_commit_id}")
  fi

  if [ -n "${project_dir}" ]; then
    cmd+=("-p" "${project_dir}")
  fi

  if [ -n "${tasks}" ] && [[ "${tasks}" != "${requested_tasks[0]}" ]]; then
    cmd+=("-g" "${tasks}")
  fi

  if [ -n "${extra_args}" ]; then
    cmd+=("-a" "${extra_args}")
  fi

  if [ -n "${ci_build_scan_url}" ]; then
    cmd+=("-1" "${ci_build_scan_url}")
  fi

  if [ -n "${mapping_file}" ]; then
    cmd+=("-m" "${mapping_file}")
  fi

  if [ -n "${remote_build_cache_url}" ]; then
    cmd+=("-u" "${remote_build_cache_url}")
  fi

  if [ -n "${ge_server}" ]; then
    cmd+=("-s" "${ge_server}")
  fi

  if [[ "${enable_ge}" == "on" ]]; then
    cmd+=("-e")
  fi

  info "Command Line Invocation"
  info "-----------------------"
  info "$(printf '%q ' "${cmd[@]}")"
}

explain_and_print_summary() {
  read_build_scan_metadata
  local text
  IFS='' read -r -d '' text <<EOF
The ‘Summary’ section below captures the configuration of the experiment and the
two build scans that were published as part of running the experiment.  The
build scan of the second build is particularly interesting since this is where
you can inspect what goals were not leveraging Gradle Enterprise's remote build cache.

The ‘Investigation Quick Links’ section below allows quick navigation to the
most relevant views in build scans to investigate what goal outputs were fetched
from the remote cache and what goals executed in the second build with cache
misses, which of those goals had the biggest impact on build performance, and
what caused the cache misses.

The ‘Command Line Invocation’ section below demonstrates how you can rerun the
experiment with the same configuration and in non-interactive mode.

$(print_summary)

$(print_command_to_repeat_experiment)

Once you have addressed the issues surfaced in build scans and pushed the
changes to your Git repository, you can rerun the experiment and start over the
cycle of run → measure → improve → run.
EOF
  print_wizard_text "${text}"
}

process_arguments "$@"
main

