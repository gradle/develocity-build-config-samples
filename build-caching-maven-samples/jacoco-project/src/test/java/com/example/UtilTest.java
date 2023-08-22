package com.example;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilTest {
    @Test
    public void testAdd() {
        assertEquals(2, new Util().add(1, 1));
    }
}
