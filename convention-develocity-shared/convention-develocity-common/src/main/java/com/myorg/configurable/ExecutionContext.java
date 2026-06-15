package com.myorg.configurable;

import java.util.Optional;

public interface ExecutionContext {

    Optional<String> environmentVariable(String name);

    Optional<String> systemProperty(String name);

}
