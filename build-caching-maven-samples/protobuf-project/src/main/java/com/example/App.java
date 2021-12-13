package com.example;

import com.example.protbuf.PersonProto.Person;

public class App {
      public static Person getPerson() {
          Person.Builder person = Person.newBuilder();
          person.setId(1);
          person.setName("Perry");
          return person.build();
      }
}
