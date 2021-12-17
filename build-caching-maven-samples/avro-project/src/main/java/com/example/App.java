package com.example;

import com.example.avro.User;

public class App {
    public static User getAvroUser() {
        return new User("Andrew", 7, "red");
    }
}
