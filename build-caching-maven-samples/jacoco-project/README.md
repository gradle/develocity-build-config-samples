# Jacoco Maven Caching Sample

`jacoco-project`'s `pom.xml` contains sample Gradle Enterprise caching configuration for both the `jacoco:report` and `jacoco:aggregate-report` goals.

## Project Structure

The `util` submodule contains a sample Java library and executes the `jacoco:report` goal to create a single-module coverage report. The `coverage-report` submodule executes `jacoco:aggregate-report` to create an aggregate coverage report from all dependent modules.