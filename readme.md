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

// Adding up population of all cities in China
val query = City.select.filter(_.countryCode === "CHN").map(_.population).sum
db.toSqlQuery(query) ==>
  "SELECT SUM(city0.population) AS res FROM city city0 WHERE city0.countrycode = ?"

db.run(query) ==> 175953614

// Finding the 5-10th largest cities by population
val query = City.select
  .sortBy(_.population).desc
  .drop(5).take(5)
  .map(c => (c.name, c.population))

db.toSqlQuery(query) ==> """
SELECT city0.name AS res__0, city0.population AS res__1
FROM city city0
ORDER BY res__1 DESC
LIMIT ? OFFSET ?
"""

db.run(query) ==> Seq(
  ("Karachi", 9269265),
  ("Istanbul", 8787958),
  ("Ciudad de México", 8591309),
  ("Moscow", 8389200),
  ("New York", 8008278)
)
```

ScalaSql supports PostgreSQL, MySQL, Sqlite, and H2 databases. Support for additional 
databases can be easily added.

ScalaSql is a relatively new library, so please try it out, but be aware you may hit bugs
or missing features! Please open [Discussions](https://github.com/com-lihaoyi/scalasql/discussions)
for any questions, file [Issues](https://github.com/com-lihaoyi/scalasql/issues) for any 
bugs you hit, or send [Pull Requests](https://github.com/com-lihaoyi/scalasql/pulls) if
you are able to investigate and fix them!


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
  * [Expression Ops](docs/reference.md#exprops), covering the different
    types of `Expr[T]` values and the different operations you can do on each one
  * [Option Ops](docs/reference.md#optional), operations on `Expr[Option[T]`
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


## Cheat Sheet

### Call Styles

**Scala Queries**           
```scala
val str = "hello"

// Select Query
db.run(Foo.select.filter(_.myStr === str)): Seq[Foo[Id]]

// Update Query
db.run(Foo.update(_.myStr === str).set(_.myInt := 123)): Int
```

**SQL Queries**
```scala
// SQL Select Query
db.runSql[Seq[Foo[Id]]](sql"SELECT * FROM foo WHERE foo.my_str = $str"): Seq[Foo[Id]]

// SQL Update Query
db.updateSql(sql"UPDATE foo SET my_int = 123 WHERE foo.my_str = $str"): Int
```

**Raw Queries**

```scala
// Raw Select Query
db.runRaw[Seq[Foo[Id]]]("SELECT * FROM foo WHERE foo.my_str = ?", Seq(str)): Seq[Foo[Id]]

// Raw Update Query
db.updateRaw("UPDATE foo SET my_int = 123 WHERE foo.my_str = ?", Seq(str)): Int
```

**Streaming Queries**

```scala
// Streaming Select Query
db.stream(Foo.select.filter(_.myStr === str)): Generator[Foo[Id]]

// Streaming SQL Select Query
db.streamSql[Foo[Id]](sql"SELECT * FROM foo WHERE foo.my_str = $str"): Generator[Foo[Id]]

// Streaming SQL Select Query
db.streamRaw[Foo[Id]]("SELECT * FROM foo WHERE foo.my_str = ?", Seq(str)): Generator[Foo[Id]]
```

### Selects

```scala
Foo.select                                                          // Seq[Foo[Id]]           
// SELECT * FROM foo

Foo.select.map(_.myStr)                                             // Seq[String]
// SELECT my_str FROM foo

Foo.select.map(t => (t.myStr, t.myInt))                             // Seq[(String, Int)]
// SELECT my_str, my_int FROM foo 

Foo.select.sumBy(_.myInt)                                           // Int
// SELECT SUM(my_int) FROM foo

Foo.select.sumByOpt(_.myInt)                                        // Option[Int]
// SELECT SUM(my_int) FROM foo

Foo.select.size                                                     // Int
// SELECT COUNT(1) FROM foo

Foo.select.aggregate(fs => (fs.sumBy(_.myInt), fs.maxBy(_.myInt)))  // (Int, Int)
// SELECT SUM(my_int), MAX(my_int) FROM foo

Foo.select.filter(_.myStr === "hello")                              // Seq[Foo[Id]]
// SELECT * FROM foo WHERE my_str = "hello"

Foo.select.filter(_.myStr === Expr("hello"))                        // Seq[Foo[Id]]
// SELECT * FROM foo WHERE my_str = "hello"

Foo.select.filter(_.myStr === "hello").single                       // Foo[Id]
// SELECT * FROM foo WHERE my_str = "hello"

Foo.select.sortBy(_.myInt).asc                                      // Seq[Foo[Id]]
// SELECT * FROM foo ORDER BY my_int ASC

Foo.select.sortBy(_.myInt).asc.take(20).drop(5)                     // Seq[Foo[Id]] 
// SELECT * FROM foo ORDER BY my_int ASC LIMIT 15 OFFSET 5

Foo.select.map(_.myInt.cast[String])                                // Seq[String]
// SELECT CAST(my_int AS VARCHAR) FROM foo

Foo.select.join(Bar)(_.id === _.fooId)                              // Seq[(Foo[Id], Bar[Id])] 
// SELECT * FROM foo JOIN bar ON foo.id = foo2.foo_id

for(f <- Foo.select; b <- Bar.join(f.id === _.fooId)) yield (f, b)  // Seq[(Foo[Id], Bar[Id])] 
// SELECT * FROM foo JOIN bar ON foo.id = foo2.foo_id
```

### Insert/Update/Delete

```scala
Foo.insert.values(_.myStr := "hello", _.myInt := 123)         // 1
// INSERT INTO foo (my_str, my_int) VALUES ("hello", 123)

Foo.insert.batched(_.myStr, _.myInt)(("a", 1), ("b", 2))      // 2
// INSERT INTO foo (my_str, my_int) VALUES ("a", 1), ("b", 2)

Foo.update(_.myStr === "hello").set(_.myInt := 123)           // Int
// UPDATE foo SET my_int = 123 WHERE foo.my_str = "hello"

Foo.update(_.myStr === "a").set(t => t.myInt := t.myInt+ 1)   // Int
// UPDATE foo SET my_int = foo.my_int + 1 WHERE foo.my_str = "a"

Foo.delete(_.myStr === "hello")                               // Int
// DELETE FROM foo WHERE foo.my_str = "hello"
```


### Type Mapping

|    Scala Primitive Type |  Database Type |        Scala DateTime Type |             Database Type |
|------------------------:|---------------:|---------------------------:|--------------------------:|
|          `scala.String` |  `LONGVARCHAR` |      `java.time.LocalDate` |                    `DATE` |
|            `scala.Byte` |      `TINYINT` |      `java.time.LocalTime` |                    `TIME` |
|           `scala.Short` |     `SMALLINT` |  `java.time.LocalDateTime` |               `TIMESTAMP` |
|             `scala.Int` |      `INTEGER` |  `java.time.ZonedDateTime` | `TIMESTAMP WITH TIMEZONE` |
|            `scala.Long` |       `BIGINT` |        `java.time.Instant` |               `TIMESTAMP` |
|           `scala.Float` |       `DOUBLE` | `java.time.OffsetDateTime` | `TIMESTAMP WITH TIMEZONE` |
|          `scala.Double` |       `DOUBLE` |
| `scala.math.BigDecimal` |       `DOUBLE` |
|         `scala.Boolean` |      `BOOLEAN` |


### Common ScalaSql Functions

**Expression Functions**

|   Scala Function |                     Database Function | Scala Function |                  Database Function |
|-----------------:|--------------------------------------:|---------------:|-----------------------------------:|
|        `a === b` | `a = b` or `a IS NOT DISTINCT FROM b` |      `a !== b` | `a <> b` or `a IS DISTINCT FROM b` |
|          `a < b` |                               `a < b` |       `a <= b` |                           `a <= b` |
|          `a > b` |                               `a > b` |       `a >= b` |                           `a >= b` |
|          `a = b` |                               `a = b` |       `a <> b` |                           `a <> b` |
| `a.cast[String]` |                 `CAST(a AS VARCHAR)`  |

**Numeric Functions**

| Scala Function | Database Function | Scala Function | Database Function |
|---------------:|------------------:|---------------:|------------------:|
|           `+a` |              `+a` |           `-a` |              `-a` |
|        `a + b` |           `a + b` |        `a - b` |           `a - b` |
|        `a * b` |           `a * b` |        `a / b` |           `a / b` |
|        `a % b` |       `MOD(a, b)` |        `a & b` |           `a & b` |
|        `a ^ b` |           `a ^ b` |        `a | b` |           `a | b` |
|       `a.ceil` |         `CEIL(a)` |      `a.floor` |        `FLOOR(a)` |
|    `a.expr(b)` |       `EXP(a, b)` |         `a.ln` |           `LN(a)` |
|     `a.pow(b)` |       `POW(a, b)` |       `a.sqrt` |         `SQRT(a)` |
| `a.abs` | `ABS(a)` |

**Boolean Functions**

| Scala Function | Database Function | Scala Function | Database Function |
|---------------:|------------------:|---------------:|------------------:|
|       `a && b` |         `a AND b` |       `a || b` |          `a OR b` |
|           `!a` |           `NOT a` |

**String Functions**

|      Scala Function |    Database Function |  Scala Function | Database Function |
|--------------------:|---------------------:|----------------:|------------------:|
|             `a + b` |             `a || b` |     `a.like(b)` |        `a LIKE b` |
|  `a.toLowerCase(b)` |           `LOWER(a)` | `a.toUpperCase` |        `UPPER(a)` |
|      `a.indexOf(b)` |     `POSITION(a, b)` |        `a.trim` |         `TRIM(a)` |
|          `a.length` |          `LENGTH(a)` | `a.octetLength` | `OCTET_LENGTH(a)` |
|           `a.ltrim` |           `LTRIM(a)` |       `a.rtrim` |        `RTRIM(a)` |
| `a.substring(b, c)` | `SUBSTRING(a, b, c)` |

**Aggregate Functions**

|                                      Scala Function |       Database Function |
|----------------------------------------------------:|------------------------:|
|                                            `a.size` |              `COUNT(1)` |
|                                   `a.mkString(sep)` | `GROUP_COUNCAT(a, sep)` |
|  `a.sum`, `a.sumBy(_.myInt)`, `a.sumByOpt(_.myInt)` |           `SUM(my_int)` |
|  `a.min`, `a.minBy(_.myInt)`, `a.minByOpt(_.myInt)` |           `MIN(my_int)` |
|  `a.max`, `a.maxBy(_.myInt)`, `a.maxByOpt(_.myInt)` |           `MAX(my_int)` |
|  `a.avg`, `a.avgBy(_.myInt)`, `a.avgByOpt(_.myInt)` |           `AVG(my_int)` |

**Select Functions**

|  Scala Function | Database Function | Scala Function | Database Function |
|----------------:|------------------:|---------------:|------------------:|
| `s.contains(a)` |          `a IN s` |
|    `s.nonEmpty` |        `EXISTS s` |    `s.isEmpty` |    `NOT EXISTS s` |

**Option/Nullable Functions**

|   Scala Function | Database Function |   Scala Function | Database Function |
|-----------------:|------------------:|-----------------:|------------------:|
|    `a.isDefined` |   `a IS NOT NULL` |      `a.isEmpty` |       `a IS NULL` |
|     `a.map(...)` |               `a` | `a.flatMap(...)` |               `a` |
| `a.getOrElse(b)` |  `COALESCE(a, b)` |    `a.orElse(b)` |  `COALESCE(a, b)` |

## Developer Docs

* Running all unit tests: `./mill -i -w "__.test"`
* Running all unit tests on one database: `./mill -i -w "__.test scalasql.sqlite"`
* Re-generating docs: `./mill -i "__.test" + generateTutorial + generateReference`
  * Note that ScalaSql's reference docs are extracted from the test suite, and thus we need
    to make sure to run the test suite before re-generating them.
* Fix all auto-generating and auto-formatting issues at once via
```
./mill -i -w  mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources + "scalasql[2.13.12].test" + generateTutorial + generateReference
```

## Changelog

### 0.1.0

* First release!

# TODO

* Scala 3 support
* Better support for plain SQL queries
* Flatten Nested Tuples 
