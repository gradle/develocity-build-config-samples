#!/bin/sh

./gradlew publishToMavenLocal

(cd examples/gradle_5 && ./gradlew clean build --build-cache && ./gradlew clean build --build-cache)
(cd examples/gradle_6 && ./gradlew clean build --build-cache && ./gradlew clean build --build-cache)
(cd examples/gradle_6.9_and_later && \
  ./gradlew clean build --build-cache --configuration-cache -Dorg.gradle.unsafe.isolated-projects=true && \
  ./gradlew clean build --build-cache --configuration-cache -Dorg.gradle.unsafe.isolated-projects=true)

(cd examples/maven_3 && ./mvnw clean verify && ./mvnw clean verify)

(cd examples/gradle_5 && ./gradlew clean build --build-cache -PenableTestCaching=true && ./gradlew clean build --build-cache -PenableTestCaching=true)
(cd examples/gradle_6 && ./gradlew clean build --build-cache -PenableTestCaching=true && ./gradlew clean build --build-cache -PenableTestCaching=true)
(cd examples/gradle_6.9_and_later && \
  ./gradlew clean build --build-cache -PenableTestCaching=true && \
  ./gradlew clean build --build-cache -PenableTestCaching=true)

(cd examples/maven_3 && ./mvnw clean verify -DenableTestCaching=true && ./mvnw clean verify -DenableTestCaching=true)
