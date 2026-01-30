package com.example

import org.scalatest.funsuite.AnyFunSuite

class AppTest extends AnyFunSuite {
  test("greet returns correct greeting") {
    assert(App.greet("Scala") == "Hello, Scala!")
  }
}
