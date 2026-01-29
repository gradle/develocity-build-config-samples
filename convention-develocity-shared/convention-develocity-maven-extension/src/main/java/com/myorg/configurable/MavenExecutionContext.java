package com.myorg.configurable;

import org.jspecify.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class MavenExecutionContext implements ExecutionContext {

    @Nullable
    private final Path topLevelDirectory;

    public MavenExecutionContext(@Nullable Path topLevelDirectory) {
        this.topLevelDirectory = topLevelDirectory;
    }

    @Override
    public Optional<Path> getProjectDirectory() {
        return Optional.ofNullable(topLevelDirectory).filter(Files::isDirectory);
    }

    @Override
    public Optional<Path> getWritableDirectory() {
        return Optional.ofNullable(topLevelDirectory)
                .map(p -> p.resolve(".mvn/.develocity"))
                .filter(Files::isDirectory);
    }

    @Override
    public Optional<String> environmentVariable(String name) {
        return Optional.ofNullable(System.getenv(name));
    }

    @Override
    public Optional<String> systemProperty(String name) {
        return Optional.ofNullable(System.getProperty(name));
    }

}
