package scalasql.example

import scalasql.dialects.H2Dialect.*
import scalasql.namedtuples.*
import scalasql.*

case class Person(name: String, age: Int)
object Person extends SimpleTable[Person]()

case class City(name: String, population: Int, mayor: Person)
object City extends SimpleTable[City]()

case class Person1[T[_]](name: T[String], age: T[Int])
object Person1 extends Table[Person1]()

case class City1[T[_]](name: T[String], population: T[Int], mayor: Person1[T])
object City1 extends Table[City1]()

@main def foo =
  City.select.filter(_.name === "foo").map(_.mayor)
  City.insert.values(City("foo", 42, Person("bar", 23)))
  City.insert.columns(_.name := "foo")
  City.insert.batched(_.name, _.population, _.mayor.name)(("foo", 42, "bar"), ("baz", 23, "qux"))
  City1.insert.batched(_.name, _.population, _.mayor.name)(("foo", 42, "bar"), ("baz", 23, "qux"))

  // TODO: can't use named tuple because the Row[?, ?] is not in implicit scope automatically?
  // City.select.filter(_.name === "foo").map(_.mayor)
