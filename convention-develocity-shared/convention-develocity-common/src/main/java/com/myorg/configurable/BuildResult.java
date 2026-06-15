package com.myorg.configurable;

import java.util.List;

public final class BuildResult {

    private final List<Throwable> failures;

    BuildResult(List<Throwable> failures) {
        this.failures = failures;
    }

    public List<Throwable> getFailures() {
        return failures;
    }

}
