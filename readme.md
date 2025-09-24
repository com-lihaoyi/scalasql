# ScalaSql

ScalaSql is a Scala ORM library that allows type-safe low-boilerplate querying of
SQL databases, using "standard" Scala collections operations running against
typed `Table` descriptions.

```scala
import scalasql._, SqliteDialect._

// Define your table model classes
case class City[T[_]](
    id: T[Int],
    name: T[String],
    countryCode: T[String],
    district: T[String],
    population: T[Long]
)
object City extends Table[City]

// Connect to your database (example uses in-memory sqlite, org.xerial:sqlite-jdbc:3.43.0.0)
val dataSource = new org.sqlite.SQLiteDataSource()
dataSource.setUrl(s"jdbc:sqlite:file.db")
lazy val dbClient = new scalasql.DbClient.DataSource(
  dataSource,
  config = new scalasql.Config {
    override def nameMapper(v: String) = v.toLowerCase() // Override default snake_case mapper
    override def logSql(sql: String, file: String, line: Int) = println(s"$file:$line $sql")
  }
)

dbClient.transaction{ db =>

  // Initialize database table schema and data
  db.updateRaw(os.read(os.Path("scalasql/test/resources/world-schema.sql", os.pwd)))
  db.updateRaw(os.read(os.Path("scalasql/test/resources/world-data.sql", os.pwd)))

  // Adding up population of all cities in China
  val citiesPop = db.run(City.select.filter(_.countryCode === "CHN").map(_.population).sum)
  // SELECT SUM(city0.population) AS res FROM city city0 WHERE city0.countrycode = ?

  println(citiesPop)
  // 175953614

  // Finding the 5-8th largest cities by population
  val fewLargestCities = db.run(
    City.select
        .sortBy(_.population).desc
        .drop(5).take(3)
        .map(c => (c.name, c.population))
  )
  // SELECT city0.name AS res__0, city0.population AS res__1
  // FROM city city0 ORDER BY res__1 DESC LIMIT ? OFFSET ?

  println(fewLargestCities)
  // Seq((Karachi, 9269265), (Istanbul, 8787958), (Ciudad de México, 8591309))
}
```

ScalaSql supports database connections to PostgreSQL, MySQL, Sqlite, and H2 databases.
Support for additional databases can be easily added.

ScalaSql is a relatively new library, so please try it out, but be aware you may hit bugs
or missing features! Please open [Discussions](https://github.com/com-lihaoyi/scalasql/discussions)
for any questions, file [Issues](https://github.com/com-lihaoyi/scalasql/issues) for any
bugs you hit, or send [Pull Requests](https://github.com/com-lihaoyi/scalasql/pulls) if
you are able to investigate and fix them!


## Getting Started

To get started with ScalaSql, add it to your `build.sc` file as follows:

```scala
ivy"com.lihaoyi::scalasql:0.2.2"
```

ScalaSql supports Scala 2.13.x and >=3.6.2

### SimpleTable variant based on named tuples

For Scala versions >=3.7.0 supporting named tuples, an alternative way to define tables is supported.

Add the following to your `build.sc` file as follows:

<!-- TODO: scalasql-simple? -->
```scala
ivy"com.lihaoyi::scalasql-namedtuples:0.2.2"
```

And taking the example above, the only thing that needs to change is the following:
```diff
-import scalasql._, SqliteDialect._
+import scalasql.simple._, SqliteDialect._

 // Define your table model classes
-case class City[T[_]](
-    id: T[Int],
-    name: T[String],
-    countryCode: T[String],
-    district: T[String],
-    population: T[Long]
-)
-object City extends Table[City]
+case class City(
+    id: Int,
+    name: String,
+    countryCode: String,
+    district: String,
+    population: Long
+)
+object City extends SimpleTable[City]
```

And you now have the option to return named tuples from queries:
```diff
 val fewLargestCities = db.run(
    City.select
        .sortBy(_.population).desc
        .drop(5).take(3)
-       .map(c => (c.name, c.population))
+       .map(c => (name = c.name, pop = c.population))
  )
```

## Documentation

* ScalaSql Quickstart Examples: self-contained files showing how to set up ScalaSql to
  connect your Scala code to a variety of supported databases and perform simple DDL and
  `SELECT`/`INSERT`/`UPDATE`/`DELETE` operations:
    * [Postgres](scalasql/test/src/example/PostgresExample.scala)
    * [MySql](scalasql/test/src/example/MySqlExample.scala)
    * [Sqlite](scalasql/test/src/example/SqliteExample.scala)
    * [H2](scalasql/test/src/example/H2Example.scala)
    * [Microsoft SQL Server](scalasql/test/src/example/MsSqlExample.scala)
    * [HikariCP](scalasql/test/src/example/HikariCpExample.scala) (and other connection pools)

* [ScalaSql Tutorial](docs/tutorial.md): a structured walkthrough of how to use ScalaSql,
  connecting to a database and writing queries to `SELECT`/`INSERT`/`UPDATE`/`DELETE`
  against it to perform useful work. Ideal for newcomers to work through from top
  to bottom when getting started with the library.

* [ScalaSql Cheat Sheet](docs/cheatsheet.md): a compact summary of the main features
  of ScalaSql and the syntax to make use of them.

* [ScalaSql Reference](docs/reference.md): a detailed listing of ScalaSql functionality,
  comprehensively covering everything that ScalaSql supports, in a single easily searchable
  place. Ideal for looking up exactly methods/operators ScalaSql supports, looking up
  how ScalaSql code translates to SQL, or looking up SQL syntax to find out how to
  express it using ScalaSql. Useful subsections include:
  * [DbApi](docs/reference.md#dbapi), covering the main methods you can all
    to execute queries
  * [Transaction](docs/reference.md#transaction), covering usage of transactions
    and savepoints
  * [Select](docs/reference.md#select), [Insert](docs/reference.md#insert),
    [Update](docs/reference.md#update), [Delete](docs/reference.md#delete):
    covering operations on the primary queries you are likely to use
  * [Join](docs/reference.md#join), covering different kinds of joins
  * [Returning](docs/reference.md#returning), [On Conflict](docs/reference.md#onconflict):
    covering these modifiers on `INSERT` and `UPDATE` for the databases that support them
  * [Expression Operations](docs/reference.md#exprops), covering the different
    types of `Expr[T]` values and the different operations you can do on each one
  * [Option Operations](docs/reference.md#optional), operations on `Expr[Option[T]`
  * [Window Functions](docs/reference.md#windowfunctions),
    [With-Clauses/Common-Table-Expressions](docs/reference.md#withcte)
  * [Postgres](docs/reference.md#postgresdialect), [MySql](docs/reference.md#mysqldialect),
    [Sqlite](docs/reference.md#sqlitedialect), [H2](docs/reference.md#h2dialect) Dialects:
    operations that are specific to each database that may not be generally applicable

* [ScalaSql Design](docs/design.md): discusses the design of the ScalaSql library, why it
  is built the way it is, what tradeoffs it makes, and how it compares to other
  common Scala database query libraries. Ideal for contributors who want to understand
  the structure of the ScalaSql codebase, or for advanced users who may need to
  understand enough to extend ScalaSql with custom functionality.

* [Developer Docs](docs/developer.md): things you should read if you want to make changes
  to the `com-lihaoyi/scalasql` codebase

## Changelog

### 0.2.2

* Adding COUNT and COUNT(DISTINCT) aggregation support [#97](https://github.com/com-lihaoyi/scalasql/pull/97)

### 0.2.1

* Initial support for Microsoft SQL Server via `scalasql.dialects.MsSqlDialect` [#94](https://github.com/com-lihaoyi/scalasql/pull/94)

### 0.1.20

* Support for `SimpleTable` classes which do not need a `T[_]` higher kinded parameter,
  and support for use of Scala 3.7.0 named tuples in queries [#81](https://github.com/com-lihaoyi/scalasql/pull/81).
  See this blog post for more details: [Making ScalaSql boring again (with interesting new internals)](https://bishabosha.github.io/articles/scalasql-simpletable.html)

### 0.1.19

*  Escape table in returning clause [#80](https://github.com/com-lihaoyi/scalasql/pull/80)

### 0.1.18

*  Fix for escaped update with where clause [#79](https://github.com/com-lihaoyi/scalasql/pull/79)

### 0.1.17

* Add `filterIf` and `filterOpt` utility methds to Select [#77](https://github.com/com-lihaoyi/scalasql/pull/77)
* Escape table names [#73](https://github.com/com-lihaoyi/scalasql/pull/73)

### 0.1.16

* Add TransactionListener interface  [#71](https://github.com/com-lihaoyi/scalasql/pull/71)
* JoinNullable deserialisation fix [#70](https://github.com/com-lihaoyi/scalasql/pull/70)
* Insert.select on conflict [#68](https://github.com/com-lihaoyi/scalasql/pull/68)
* Add OnConflict to InsertValues [#61](https://github.com/com-lihaoyi/scalasql/pull/61)

### 0.1.15

* Support `schemaName` in non-SELECT queries [#57](https://github.com/com-lihaoyi/scalasql/pull/57)

### 0.1.14

* Remove uneeded Numeric constraint from ordering-related functions [#47](https://github.com/com-lihaoyi/scalasql/pull/47)
* Add `SELECT FOR UPDATE` support for Postgres and MySQL [#45](https://github.com/com-lihaoyi/scalasql/pull/45)

### 0.1.11

* Support null value passthrough for all `TypeMappers` [#39](https://github.com/com-lihaoyi/scalasql/pull/39)
* Fix optional null values failing to put [#42](https://github.com/com-lihaoyi/scalasql/pull/42)

### 0.1.10

* Fix a typo in isNormalCharacter [#37](https://github.com/com-lihaoyi/scalasql/pull/37)
* Instant type mapper now allows NULL values [#38](https://github.com/com-lihaoyi/scalasql/pull/38)

### 0.1.9

* Add support for Float data type [#32](https://github.com/com-lihaoyi/scalasql/pull/32)

### 0.1.8

* Introduce `TypeMapper#bimap` to make creating related `TypeMapper`s easier [#27](https://github.com/com-lihaoyi/scalasql/pull/27)

### 0.1.7

* Add support for columns of type `java.util.Date` [#24](https://github.com/com-lihaoyi/scalasql/pull/24)

### 0.1.6

* Add support for non-default database schemas in Postgres [#23](https://github.com/com-lihaoyi/scalasql/pull/23)

### 0.1.5

* Properly pass ON CONFLICT column names through `columnNameMapper` [#19](https://github.com/com-lihaoyi/scalasql/pull/19)

### 0.1.4

* Second attempt at fixing invalid version of scala-reflect dependency

### 0.1.3

* Support for Scala 3.4.2 and [#11](https://github.com/com-lihaoyi/scalasql/pull/11)

### 0.1.2

* Support `.getGeneratedKeys[R]` [#9](https://github.com/com-lihaoyi/scalasql/pull/9)

### 0.1.1

* Fix invalid version of scala-reflect dependency

### 0.1.0

* First release!

# TODO

* JSON columns
* Add datetime functions
* Make `implicit ctx =>` for defining `sql"..."` snippets optional
