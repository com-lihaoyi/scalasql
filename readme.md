# ScalaSql

ScalaSql is a small SQL library that allows type-safe low-boilerplate querying of
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
  "SELECT SUM(city0.population) as res FROM city city0 WHERE city0.countrycode = ?"

db.run(query) ==> 175953614
```

ScalaSql supports PostgreSQL, MySQL, Sqlite, H2, and HsqlDb databases.

## Getting Started

To get started with ScalaSql, add it to your `build.sc` file as follows:

```scala
ivy"com.lihaoyi::scalasql:0.1.0"
```

## Documentation

* [ScalaSql Tutorial](tutorial.md): walks you through getting started with ScalaSql,
  connecting to a database and writing queries to `SELECT`/`INSERT`/`UPDATE`/`DELETE`
  against it to perform useful work. Ideal for newcomers to work through from top
  to bottom when getting started with the library.

* [ScalaSql Reference](reference.md): a detailed listing of ScalaSql functionality,
  comprehensively covering everything that ScalaSql supports, in a single easily searchable
  place. Ideal for looking up exactly methods/operators ScalaSql supports, looking up
  how ScalaSql code translates to SQL, or looking up SQL syntax to find out how to
  express it using ScalaSql

* [ScalaSql Design](design.md): discusses the design of the ScalaSql library, why it
  is built the way it is, what tradeoffs it makes, and how it compares to other 
  common Scala database query libraries. Ideal for contributors who want to understand
  the structure of the ScalaSql codebase, or for advanced users who may need to
  understand enough to extend ScalaSql with custom functionality.

## Changelog

### 0.1.0

* First release!

# TODO

* Scala 3 support
* Flat joins (Quill Style)
* Dot-delimited result names
* Tutorial docs for `Option`/`NULL` handling
* Docs for type mapping
* Docs for custom expressions
* Integrate `java.sql.DataSource` as an alternative to `java.sql.Connection`, test/example with HiikariCP
* Casting syntax for `Expr[T] => Expr[V]`
* Docs/Tests for extending ScalaSql with custom expressions and custom queries  
* `string_agg`, `array_agg`, other agg functions
* Turn on Acyclic enforcement
