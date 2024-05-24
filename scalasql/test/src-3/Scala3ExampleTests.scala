package scalasql

import utest._

object Scala3ExampleTests extends TestSuite:
  def tests = Tests:
    test("h2") - example.Scala3H2Example.main(Array())
