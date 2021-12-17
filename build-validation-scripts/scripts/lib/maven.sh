#!/usr/bin/env bash

readonly CAPTURE_SCANS_EXTENSION_JAR="${LIB_DIR}/maven/capture-published-build-scan-maven-extension-1.0.0-SNAPSHOT.jar"

invoke_maven() {
  local args
  args=()

  pushd "${project_dir}" > /dev/null 2>&1 || die "ERROR: The subdirectory ${project_dir} (set with --project-dir) does not exist in ${project_name}." 3

  local extension_classpath
  extension_classpath="${CAPTURE_SCANS_EXTENSION_JAR}"

  if [ "$enable_ge" == "on" ]; then
    # Reset the extension classpath and add all of the jars in the lib/maven dir
    # The lib/maven dir includes:
    #  - the Gradle Enterprise Maven extension
    #  - the Common Custom User Data Maven extension
    #  - the capture-publish-build-scan Maven extension
    extension_classpath=""
    for jar in "${LIB_DIR}"/maven/*; do
      if [ "${extension_classpath}" == "" ]; then
        extension_classpath="${jar}"
      else
        extension_classpath="${extension_classpath}:${jar}"
      fi
    done
  fi

  args+=(
    -Dmaven.ext.class.path="${extension_classpath}"
    -Dcom.gradle.enterprise.build_validation.experimentDir="${EXP_DIR}"
    -Dscan.tag."${EXP_SCAN_TAG}"
    -Dscan.tag."${RUN_ID}"
    -Dorg.slf4j.simpleLogger.log.gradle.goal.cache=debug
  )
  if [ -n "${ge_server}" ]; then
    args+=("-Dgradle.enterprise.url=${ge_server}")
  fi

  # shellcheck disable=SC2206
  args+=(${extra_args})
  args+=("$@")

  debug ./mvnw "${args[@]}"
  if ./mvnw "${args[@]}"; then
    build_outcomes+=("SUCCESSFUL")
  else
    build_outcomes+=("FAILED")
  fi

  # defined in build_scan.sh
  read_build_data_from_current_dir

  #shellcheck disable=SC2164  # This is extremely unlikely to fail, and even if it does, nothing terrible will happen.
  popd > /dev/null 2>&1
}
