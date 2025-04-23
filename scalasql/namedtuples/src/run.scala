package scalasql.namedtuples

import scala.NamedTuple.AnyNamedTuple

case class Person(name: String, age: Int)
object Person extends SimpleTable[Person]()

case class City(name: String, population: Int, mayor: Person)

object City extends SimpleTable[City]()

@main def run =
  val foo: AnyNamedTuple = (x = 23, y = 42)
  println(s"Hello, world!!, $foo")
