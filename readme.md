# ScalaSql

ScalaSql is a Scala library that allows type-safe low-boilerplate querying of
SQL databases, using "standard" Scala collections operations running against
typed `Table` descriptions.

```scala
case class City[+T[_]](
    id: T[Int],
    name: T[String],
    countryCode: T[String],
    district: T[String],
    population: T[Long]
)

object City extends Table[City]() {
  val metadata = initMetadata()
}

val db: DbApi = ???

val query = City.select.filter(_.countryCode === "CHN").map(_.population).sum
db.toSqlQuery(query) ==>
  "SELECT SUM(city0.population) AS res FROM city city0 WHERE city0.countrycode = ?"

db.run(query) ==> 175953614
```

ScalaSql supports PostgreSQL, MySQL, Sqlite, and H2 databases.

## Getting Started

To get started with ScalaSql, add it to your `build.sc` file as follows:

```scala
ivy"com.lihaoyi::scalasql:0.1.0"
```

## Documentation

* ScalaSql Quickstart Examples: self-contained files showing how to set up ScalaSql with
  a variety of supported databases and perform simple DDL and 
  `SELECT`/`INSERT`/`UPDATE`/`DELETE` operations:
    * [Postgres](scalasql/test/src/example/PostgresExample.scala)
    * [MySql](scalasql/test/src/example/MySqlExample.scala)
    * [Sqlite](scalasql/test/src/example/SqliteExample.scala)
    * [H2](scalasql/test/src/example/H2Example.scala)
    * [HikariCP](scalasql/test/src/example/HikariCpExample.scala) (and other connection pools)

* [ScalaSql Tutorial](docs/tutorial.md): a structured walkthrough of how to use ScalaSql,
  connecting to a database and writing queries to `SELECT`/`INSERT`/`UPDATE`/`DELETE`
  against it to perform useful work. Ideal for newcomers to work through from top
  to bottom when getting started with the library.

* [ScalaSql Reference](docs/reference.md): a detailed listing of ScalaSql functionality,
  comprehensively covering everything that ScalaSql supports, in a single easily searchable
  place. Ideal for looking up exactly methods/operators ScalaSql supports, looking up
  how ScalaSql code translates to SQL, or looking up SQL syntax to find out how to
  express it using ScalaSql

* [ScalaSql Design](docs/design.md): discusses the design of the ScalaSql library, why it
  is built the way it is, what tradeoffs it makes, and how it compares to other 
  common Scala database query libraries. Ideal for contributors who want to understand
  the structure of the ScalaSql codebase, or for advanced users who may need to
  understand enough to extend ScalaSql with custom functionality.

## Developer Docs

* Running all unit tests: `./mill -i -w "scalasql[2.13.8].test"`
* Running all unit tests on one database: `./mill -i -w "scalasql[2.13.8].test scalasql.sqlite"`
* Re-generating docs: `./mill -i "scalasql[2.13.8].test" + generateTutorial + generateReference`
  * Note that ScalaSql's reference docs are extracted from the test suite, and thus we need
    to make sure to run the test suite before re-generating them.

## Changelog

### 0.1.0

* First release!

# TODO

* Scala 3 support
* `string_agg`, `array_agg`, other agg functions