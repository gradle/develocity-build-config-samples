package com.myorg.configurable;

import java.nio.file.Path;
import java.util.Optional;

public interface ExecutionContext {

    Optional<Path> getProjectDirectory();

    Optional<Path> getWritableDirectory();

    Optional<String> environmentVariable(String name);

    Optional<String> systemProperty(String name);

}
