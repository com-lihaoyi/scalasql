package scalasql.example

import scalasql.dialects.H2Dialect.*
import scalasql.namedtuples.*
import scalasql.namedtuples.NamedTupleQueryable.given
import scalasql.*
import scalasql.dialects.Dialect

case class Person(name: String, age: Int) extends SimpleTable.Nested
object Person extends SimpleTable[Person]()

case class City(name: String, population: Int, mayor: Person)
object City extends SimpleTable[City]()

def bar(db: DbApi) =
  val m = db.run(
    City.select.filter(_.name === "foo").map(c => (name = c.name, mayor = c.mayor))
  )
  val _: Seq[(name: String, mayor: Person)] = m // demonstrate that mayor maps back to case class.

@main def foo =
  City.select.filter(_.name === "foo").map(_.mayor)
  City.insert.values(City("foo", 42, Person("bar", 23)))
  City.insert.columns(_.name := "foo")
  City.insert.batched(_.name, _.population, _.mayor.name)(("foo", 42, "bar"), ("baz", 23, "qux"))
