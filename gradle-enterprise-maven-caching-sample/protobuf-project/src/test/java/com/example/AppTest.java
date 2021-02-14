package com.example;

import junit.framework.Test;
import junit.framework.TestCase;

public class AppTest extends TestCase {
    public void testProtoPerson() {
        assertEquals("Perry", App.getPerson().getName());
    }
}
