package com.example;

import com.example.avro.User;

import junit.framework.Test;
import junit.framework.TestCase;

public class AppTest extends TestCase {
    public void testUser() {
        User user = App.getAvroUser();
        assertEquals("Andrew", user.getName());
    }
}
