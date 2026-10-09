package com.myorg;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;

import java.io.File;

public abstract class GitRepositoryProjectIdValueSource implements ValueSource<String, GitRepositoryProjectIdValueSource.Parameters> {

    public interface Parameters extends ValueSourceParameters {

        DirectoryProperty getRootDirectory();

    }

    static Provider<String> create(ProviderFactory providers, File rootDirectory) {
        return providers.of(GitRepositoryProjectIdValueSource.class, spec -> spec.getParameters().getRootDirectory().set(rootDirectory));
    }

    @Override
    public String obtain() {
        return GitRepositoryProjectId.fromGitRepository(getParameters().getRootDirectory().get().getAsFile()).orElse(null);
    }

}
