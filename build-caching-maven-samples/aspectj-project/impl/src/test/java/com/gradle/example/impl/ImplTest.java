package com.gradle.example.impl;

import org.junit.*;

import static org.junit.Assert.*;

public class ImplTest {

    @Test
    public void testTheAnswer() {
        assertEquals(42, new Impl().getTheAnswer());
    }

}
