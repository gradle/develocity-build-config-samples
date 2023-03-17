package com.example;

import java.util.Optional;

final class CiUtils {

    private CiUtils() {
    }

    static boolean isCi() {
        return envVariable("CI").isPresent() || sysProperty("CI").isPresent();
    }

    static Optional<String> envVariable(String name) {
        return Optional.ofNullable(System.getenv(name));
    }

    static Optional<String> sysProperty(String name) {
        return Optional.ofNullable(System.getProperty(name));
    }

}
