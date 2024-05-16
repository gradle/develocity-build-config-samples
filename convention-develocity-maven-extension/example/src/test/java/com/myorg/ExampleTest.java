package com.myorg;

import com.google.common.base.Joiner;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExampleTest {

    @Test
    void testJoin() {
        assertEquals("a b c", Example.join("a", "b", "c"));
    }

}
