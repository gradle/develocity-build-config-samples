package com.myorg.configurable;

import java.util.Optional;

public final class MavenExecutionContext implements ExecutionContext {

    @Override
    public Optional<String> environmentVariable(String name) {
        return Optional.ofNullable(System.getenv(name));
    }

    @Override
    public Optional<String> systemProperty(String name) {
        return Optional.ofNullable(System.getProperty(name));
    }

}
