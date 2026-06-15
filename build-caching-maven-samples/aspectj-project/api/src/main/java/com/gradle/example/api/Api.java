package com.gradle.example.api;

public interface Api {
    default int getTheAnswer() {
        return 42;
    }
}
