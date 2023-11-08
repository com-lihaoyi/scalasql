[//]: # (GENERATED SOURCES, DO NOT EDIT DIRECTLY)

## Importing Your Database Dialect
To begin using ScalaSql, you need the following imports:
```scala
import scalasql._
import scalasql.dialects.H2Dialect._

```
This readme will use the H2 database for simplicity, but you can change the `Dialect`
above to other databases as necessary. ScalaSql supports H2, Sqlite, HsqlDb,
Postgres, and MySql out of the box. The `Dialect` import provides the
various operators and functions that may be unique to each specific database

For these examples, we will be using the
[MySql World Statistics Example Database](https://dev.mysql.com/doc/world-setup/en/),
adjusted for compatibility with H2

```sql
CREATE TABLE IF NOT EXISTS city (
    id integer AUTO_INCREMENT PRIMARY KEY,
    name varchar NOT NULL,
    countrycode character(3) NOT NULL,
    district varchar NOT NULL,
    population integer NOT NULL
);

CREATE TABLE IF NOT EXISTS country (
    code character(3) PRIMARY KEY,
    name varchar NOT NULL,
    continent varchar NOT NULL,
    region varchar NOT NULL,
    surfacearea real NOT NULL,
    indepyear smallint,
    population integer NOT NULL,
    lifeexpectancy real,
    gnp numeric(10,2),
    gnpold numeric(10,2),
    localname varchar NOT NULL,
    governmentform varchar NOT NULL,
    headofstate varchar,
    capital integer,
    code2 character(2) NOT NULL
);

CREATE TABLE IF NOT EXISTS countrylanguage (
    countrycode character(3) NOT NULL,
    language varchar NOT NULL,
    isofficial boolean NOT NULL,
    percentage real NOT NULL
);
```

## Modeling Your Schema

Next, you need to define your data model classes. In ScalaSql, your data model
is defined using `case class`es with each field wrapped in the wrapper type
parameter `+T[_]`. This allows us to re-use the same case class to represent
both database values (when `T` is `scalasql.Expr`) as well as Scala values
(when `T` is `scalasql.Id`).

Here, we define three classes `Country` `City` and `CountryLanguage`, modeling
the database tables we saw above

```scala
case class Country[+T[_]](
    code: T[String],
    name: T[String],
    continent: T[String],
    region: T[String],
    surfaceArea: T[Int],
    indepYear: T[Option[Int]],
    population: T[Long],
    lifeExpectancy: T[Option[Double]],
    gnp: T[Option[scala.math.BigDecimal]],
    gnpOld: T[Option[scala.math.BigDecimal]],
    localName: T[String],
    governmentForm: T[String],
    headOfState: T[Option[String]],
    capital: T[Option[Int]],
    code2: T[String]
)

object Country extends Table[Country]() {
  val metadata = initMetadata()
}

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

case class CountryLanguage[+T[_]](
    countryCode: T[String],
    language: T[String],
    isOfficial: T[Boolean],
    percentage: T[Double]
)

object CountryLanguage extends Table[CountryLanguage]() {
  val metadata = initMetadata()
}
```

## Creating Your Database Client
Lastly, we need to initialize our `scalasql.DatabaseClient`. This requires
passing in a `java.sql.Connection`, a `scalasql.Config` object, and the SQL dialect
you are targeting (in this case `H2Dialect`).

```scala
val dbClient = new DatabaseClient(
  java.sql.DriverManager
    .getConnection("jdbc:h2:mem:testdb" + scala.util.Random.nextInt(), "sa", ""),
  new Config {
    override def columnNameMapper(v: String) = v.toLowerCase()
    override def tableNameMapper(v: String) = v.toLowerCase()
  },
  scalasql.dialects.H2Dialect
)

val db = dbClient.autoCommit
db.runRawUpdate(os.read(os.pwd / "test" / "resources" / "world-schema.sql"))
db.runRawUpdate(os.read(os.pwd / "test" / "resources" / "world-data.sql"))


```
We use `dbClient.autoCommit` in order to create a client that will automatically
run every SQL command in a new transaction and commit it. For the majority of
examples in this page, the exact transaction configuration doesn't matter, so
using the auto-committing `db` API will help focus on the queries at hand.

Lastly, we will run the `world.sql` script to initialize the database, and
we're ready to begin writing queries!

## Expressions
The simplest thing you can query are `scalasql.Expr`s. These represent the SQL
expressions that are part of a query, and can be evaluated even without being
part of any database table.

Here, we construct `Expr`s to represent the SQL query `1 + 3`. We can use
`db.toSqlQuery` to see the generated SQL code, and `db.run` to send the
query to the database and return the output `4`
```scala
val query = Expr(1) + Expr(3)
db.toSqlQuery(query) ==> "SELECT ? + ? as res"
db.run(query) ==> 4

```
In general, most primitive types that can be mapped to SQL can be converted
to `scalasql.Expr`s: `Int`s and other numeric types, `String`s, `Boolean`s,
etc., each returning an `Expr[T]` for the respective type `T`. Each type of
`Expr[T]` has a set of operations representing what operations the database
supports on that type of expression.

All database operations run in a _transaction_, represented by the
`.transaction{}` block. If a the block fails with an exception, changes
made in the transaction are rolled back. This does not affect the read-only
queries we are doing now, but becomes more important once we start doing
create/update/delete queries later on.


## Select

The next type of query to look at are simple `SELECT`s. Each table
that we modelled earlier has `.insert`, `.select`, `.update`, and `.delete`
methods to help construct the respective queries. You can run a `Table.select`
on its own in order to retrieve all the data in the table:

```scala
val query = City.select
db.toSqlQuery(query) ==> """
SELECT
  city0.id as res__id,
  city0.name as res__name,
  city0.countrycode as res__countrycode,
  city0.district as res__district,
  city0.population as res__population
FROM city city0
""".trim.replaceAll("\\s+", " ")

db.run(query).take(3) ==> Seq(
  City[Id](1, "Kabul", "AFG", district = "Kabol", population = 1780000),
  City[Id](2, "Qandahar", "AFG", district = "Qandahar", population = 237500),
  City[Id](3, "Herat", "AFG", district = "Herat", population = 186800)
)


```
Notice that `db.run` returns instances of type `City[Id]`. `Id` is `scalasql.Id`,
short for the "Identity" type, representing a `City` object containing normal Scala
values. The `[Id]` type parameter must be provided explicitly whenever creating,
type-annotating, or otherwise working with these `City` values.

In this example, we do `.take(3)` after running the query to show only the first
3 table entries for brevity, but by that point the `City.select` query had already
fetched the entire database table into memory. This can be a problem with non-trivial
datasets, and for that you


## Filtering

To avoid loading the entire database table into your Scala program, you can
add filters to the query before running it. Below, we add a filter to only
query the city whose name is "Singapore"

```scala
val query = City.select.filter(_.name === "Singapore").single

db.toSqlQuery(query) ==> """
SELECT
  city0.id as res__id,
  city0.name as res__name,
  city0.countrycode as res__countrycode,
  city0.district as res__district,
  city0.population as res__population
FROM city city0
WHERE city0.name = ?
""".trim.replaceAll("\\s+", " ")

db.run(query) ==> City[Id](3208, "Singapore", "SGP", district = "", population = 4017733)


```
Note that we use `===` rather than `==` for the equality comparison. The
function literal passed to `.filter` is given a `City[Expr]` as its parameter,
representing a `City` that is part of the database query, in contrast to the
`City[Id]`s that `db.run` returns , and so `_.name` is of type `Expr[String]`
rather than just `String` or `Id[String]`. You can use your IDE's
auto-complete to see what operations are available on `Expr[String]`: typically
they will represent SQL string functions rather than Scala string functions and
take and return `Expr[String]`s rather than plain Scala `String`s. Database
value equality is represented by the `===` operator.

Note also the `.single` operator. This tells ScalaSql that you expect exactly
own result row from this query: not zero rows, and not more than one row. This
causes `db.run` to return a `City[Id]` rather than `Seq[City[Id]]`, and throw
an exception if zero or multiple rows are returned by the query.


Apart from filtering by name, it is also very common to filter by ID,
as shown below:
```scala
val query = City.select.filter(_.id === 3208).single
db.toSqlQuery(query) ==> """
SELECT
  city0.id as res__id,
  city0.name as res__name,
  city0.countrycode as res__countrycode,
  city0.district as res__district,
  city0.population as res__population
FROM city city0
WHERE city0.id = ?
""".trim.replaceAll("\\s+", " ")

db.run(query) ==> City[Id](3208, "Singapore", "SGP", district = "", population = 4017733)
```

You can filter on multiple things, e.g. here we look for cities in China
with population more than 5 million:
```scala
val query = City.select.filter(c => c.population > 5000000 && c.countryCode === "CHN")
db.toSqlQuery(query) ==> """
  SELECT
    city0.id as res__id,
    city0.name as res__name,
    city0.countrycode as res__countrycode,
    city0.district as res__district,
    city0.population as res__population
  FROM city city0
  WHERE
    city0.population > ?
    AND city0.countrycode = ?
  """.trim.replaceAll("\\s+", " ")

db.run(query).take(2) ==> Seq(
  City[Id](1890, "Shanghai", "CHN", district = "Shanghai", population = 9696300),
  City[Id](1891, "Peking", "CHN", district = "Peking", population = 7472000)
)

```
Again, all the operations within the query work on `Expr`s: `c` is a `City[Expr]`,
`c.population` is an `Expr[Int]`, `c.countryCode` is an `Expr[String]`, and
`===` and `>` and `&&` on `Expr`s all return `Expr[Boolean]`s that represent
a SQL expression that can be sent to the Database as part of your query.

You can also stack multiple separate filters together, as shown below:
```scala
val query = City.select.filter(_.population > 5000000).filter(_.countryCode === "CHN")
db.toSqlQuery(query) ==> """
SELECT
  city0.id as res__id,
  city0.name as res__name,
  city0.countrycode as res__countrycode,
  city0.district as res__district,
  city0.population as res__population
FROM city city0
WHERE
  city0.population > ?
  AND city0.countrycode = ?
""".trim.replaceAll("\\s+", " ")

db.run(query).take(2) ==> Seq(
  City[Id](1890, "Shanghai", "CHN", district = "Shanghai", population = 9696300),
  City[Id](1891, "Peking", "CHN", district = "Peking", population = 7472000)
)
```

## Lifting
Conversion of simple primitive `T`s into `Expr[T]`s happens implicitly. Below,
`===` expects both left-hand and right-hand values to be `Expr`s. `_.id` is
already an `Expr[Int]`, but `cityId` is a normal `Int` that is "lifted" into
a `Expr[Int]` automatically

```scala
def find(cityId: Int) = db.run(City.select.filter(_.id === cityId))

assert(find(3208) == List(City[Id](3208, "Singapore", "SGP", "", 4017733)))
assert(find(3209) == List(City[Id](3209, "Bratislava", "SVK", "Bratislava", 448292)))

```

This implicit lifting can be done explicitly using the `Expr(...)` syntax
as shown below
```scala
def find(cityId: Int) = db.run(City.select.filter(_.id === Expr(cityId)))

assert(find(3208) == List(City[Id](3208, "Singapore", "SGP", "", 4017733)))
assert(find(3209) == List(City[Id](3209, "Bratislava", "SVK", "Bratislava", 448292)))
```

## Mapping

You can use `.map` to select exactly what values you want to return from a query.
Below, we query the `country` table, but only want the `name` and `continent` of
each country, without all the other metadata:
```scala
val query = Country.select.map(c => (c.name, c.continent))
db.toSqlQuery(query) ==> """
  SELECT
    country0.name as res__0,
    country0.continent as res__1
  FROM country country0
  """.trim.replaceAll("\\s+", " ")

db.run(query).take(5) ==> Seq(
  ("Afghanistan", "Asia"),
  ("Netherlands", "Europe"),
  ("Netherlands Antilles", "North America"),
  ("Albania", "Europe"),
  ("Algeria", "Africa")
)
```

These `.map` calls can contains arbitrarily complex data: below, we query
the `city` table to look for `Singapore` and get the entire row as a `City[Id]`,
but also want to fetch the uppercase name and the population-in-millions. As
you would expect, you get a tuple of `(City[Id], String, Int)` back.
```scala
val query = City.select
  .filter(_.name === "Singapore")
  .map(c => (c, c.name.toUpperCase, c.population / 1000000))
  .single

db.toSqlQuery(query) ==> """
  SELECT
    city0.id as res__0__id,
    city0.name as res__0__name,
    city0.countrycode as res__0__countrycode,
    city0.district as res__0__district,
    city0.population as res__0__population,
    UPPER(city0.name) as res__1,
    city0.population / ? as res__2
  FROM
    city city0
  WHERE
    city0.name = ?
  """.trim.replaceAll("\\s+", " ")

db.run(query) ==>
  (
    City[Id](3208, "Singapore", "SGP", district = "", population = 4017733),
    "SINGAPORE",
    4 // population in millions
  )
```

## Aggregates

You can perform simple aggregates like `.sum` as below, where we
query all cities in China and sum up their populations
```scala
val query = City.select.filter(_.countryCode === "CHN").map(_.population).sum
db.toSqlQuery(query) ==>
  "SELECT SUM(city0.population) as res FROM city city0 WHERE city0.countrycode = ?"

db.run(query) ==> 175953614
```

Many aggregates have a `By` version, e.g. `.sumBy`, which allows you to
customize exactly what you are aggregating:
```scala
val query = City.select.sumBy(_.population)
db.toSqlQuery(query) ==> "SELECT SUM(city0.population) as res FROM city city0"

db.run(query) ==> 1429559884
```

`.size` is a commonly used function that translates to the SQL aggregate
`COUNT(1)`. Below, we count how many countries in our database have population
greater than one million
```scala
val query = Country.select.filter(_.population > 1000000).size
db.toSqlQuery(query) ==>
  "SELECT COUNT(1) as res FROM country country0 WHERE country0.population > ?"

db.run(query) ==> 154
```

If you want to perform multiple aggregates at once, you can use the `.aggregate`
function. Below, we run a single query that returns the minimum, average, and
maximum populations across all countries in our dataset
```scala
val query = Country.select
  .aggregate(cs => (cs.minBy(_.population), cs.avgBy(_.population), cs.maxBy(_.population)))
db.toSqlQuery(query) ==> """
SELECT
  MIN(country0.population) as res__0,
  AVG(country0.population) as res__1,
  MAX(country0.population) as res__2
FROM country country0
""".trim.replaceAll("\\s+", " ")

db.run(query) ==> (0, 25434098, 1277558000)
```

## Sort/Drop/Take

You can use `.sortBy` to order the returned rows, and `.drop` and `.take`
to select a range of rows within the entire result set:
```scala
val query = City.select
  .sortBy(_.population)
  .desc
  .drop(5)
  .take(5)
  .map(c => (c.name, c.population))

db.toSqlQuery(query) ==> """
  SELECT city0.name as res__0, city0.population as res__1
  FROM city city0
  ORDER BY res__1 DESC
  LIMIT 5 OFFSET 5
  """.trim.replaceAll("\\s+", " ")

db.run(query) ==> Seq(
  ("Karachi", 9269265),
  ("Istanbul", 8787958),
  ("Ciudad de México", 8591309),
  ("Moscow", 8389200),
  ("New York", 8008278)
)

```
You can also use `.drop` and `.take` without `.sortBy`, but in that case
the order of returned rows is arbitrary and may differ between databases
and implementations


## Joins

You can perform SQL inner `JOIN`s between tables via the `.join` or `.joinOn`
methods. Below, we use a `JOIN` to look for cities which are in the country
named "Liechtenstein":
```scala
val query = City.select
  .joinOn(Country)(_.countryCode === _.code)
  .filter { case (city, country) => country.name === "Liechtenstein" }
  .map { case (city, country) => city.name }

db.toSqlQuery(query) ==> """
SELECT city0.name as res
FROM city city0
JOIN country country1 ON city0.countrycode = country1.code
WHERE country1.name = ?
""".trim.replaceAll("\\s+", " ")

db.run(query) ==> Seq("Schaan", "Vaduz")
```

`LEFT JOIN`, `RIGHT JOIN`, and `OUTER JOIN`s are also supported, e.g.
```scala
val query = City.select
  .rightJoin(Country)(_.countryCode === _.code)
  .filter { case (cityOpt, country) => cityOpt.isEmpty }
  .map { case (cityOpt, country) =>
    (cityOpt.map(_.name), country.name)
  }

db.toSqlQuery(query) ==> """
SELECT city0.name as res__0, country1.name as res__1
FROM city city0
RIGHT JOIN country country1 ON city0.countrycode = country1.code
WHERE ?
""".trim.replaceAll("\\s+", " ")

db.run(query) ==> Seq()

```
Note that when you use a left/right/outer join, the corresponding
rows are provided to you as `Option[T]` rather than plain `T`s, e.g.
`cityOpt: Option[City[Expr]]` above.


ScalaSql also supports performing `JOIN`s via Scala's for-comprehension syntax:
```scala
val query = for {
  city <- City.select
  country <- Country.select if city.countryCode === country.code
  if country.name === "Liechtenstein"
} yield city.name

db.toSqlQuery(query) ==> """
  SELECT city0.name as res
  FROM city city0, country country1
  WHERE city0.countrycode = country1.code AND country1.name = ?
  """.trim.replaceAll("\\s+", " ")

db.run(query) ==> Seq("Schaan", "Vaduz")
```

ScalaSql in general allows you to use SQL Subqueries anywhere you would use
a table. e.g. you can pass a Subquery to `.joinOn`, as we do in the below
query to find language and the name of the top 2 most populous countries:
```scala
val query = CountryLanguage.select
  .joinOn(Country.select.sortBy(_.population).desc.take(2))(_.countryCode === _.code)
  .map { case (language, country) => (language.language, country.name) }
  .sortBy(_._1)

db.toSqlQuery(query) ==> """
  SELECT countrylanguage0.language as res__0, subquery1.res__name as res__1
  FROM countrylanguage countrylanguage0
  JOIN (SELECT
      country0.code as res__code,
      country0.name as res__name,
      country0.population as res__population
    FROM country country0
    ORDER BY res__population DESC
    LIMIT 2) subquery1
  ON countrylanguage0.countrycode = subquery1.res__code
  ORDER BY res__0
  """.trim.replaceAll("\\s+", " ")

db.run(query).take(5) ==> Seq(
  ("Asami", "India"),
  ("Bengali", "India"),
  ("Chinese", "China"),
  ("Dong", "China"),
  ("Gujarati", "India")
)
```

Some operations automatically generate subqueries where necessary, e.g.
performing a `.joinOn` after you have done a `.take`:
```scala
val query = Country.select
  .sortBy(_.population)
  .desc
  .take(2)
  .joinOn(CountryLanguage)(_.code === _.countryCode)
  .map { case (country, language) =>
    (language.language, country.name)
  }
  .sortBy(_._1)

db.toSqlQuery(query) ==> """
  SELECT countrylanguage1.language as res__0, subquery0.res__name as res__1
  FROM (SELECT
      country0.code as res__code,
      country0.name as res__name,
      country0.population as res__population
    FROM country country0
    ORDER BY res__population DESC
    LIMIT 2) subquery0
  JOIN countrylanguage countrylanguage1
  ON subquery0.res__code = countrylanguage1.countrycode
  ORDER BY res__0
  """.trim.replaceAll("\\s+", " ")

db.run(query).take(5) ==> List(
  ("Asami", "India"),
  ("Bengali", "India"),
  ("Chinese", "China"),
  ("Dong", "China"),
  ("Gujarati", "India")
)
```


## Realistic Queries

### Languages Spoken In Most Cities
Here's a more complicated query using the techniques we've learned so far:
a query fetching the top 10 languages spoken by the largest number of cities
```scala
val query = City.select
  .joinOn(CountryLanguage)(_.countryCode === _.countryCode)
  .map { case (city, language) => (city.id, language.language) }
  .groupBy { case (city, language) => language }(_.size)
  .sortBy { case (language, cityCount) => cityCount }.desc
  .take(10)

db.toSqlQuery(query) ==> """
 SELECT countrylanguage1.language as res__0, COUNT(1) as res__1
 FROM city city0
 JOIN countrylanguage countrylanguage1 ON city0.countrycode = countrylanguage1.countrycode
 GROUP BY countrylanguage1.language
 ORDER BY res__1 DESC
 LIMIT 10
""".trim.replaceAll("\\s+", " ")

db.run(query) ==> Seq(
  ("Chinese", 1083),
  ("German", 885),
  ("Spanish", 881),
  ("Italian", 857),
  ("English", 823),
  ("Japanese", 774),
  ("Portuguese", 629),
  ("Korean", 608),
  ("Polish", 557),
  ("French", 467)
)
```

### Population-Weighted Average Life Expectancy Per Continent
Another non-trivia query: listing the population-weighted
average life expectancy per continent
```scala
val query = Country.select
  .groupBy(_.continent)(group =>
    group.sumBy(c => c.lifeExpectancy.get * c.population) / group.sumBy(_.population)
  )
  .sortBy(_._2).desc

db.toSqlQuery(query) ==> """
SELECT
  country0.continent as res__0,
  SUM(country0.lifeexpectancy * country0.population) / SUM(country0.population) as res__1
FROM country country0
GROUP BY country0.continent
ORDER BY res__1 DESC
""".trim.replaceAll("\\s+", " ")

db.run(query) ==> Seq(
  ("Oceania", 75.90188415576932),
  ("North America", 74.91544123695004),
  ("Europe", 73.82361172661305),
  ("South America", 67.54433544271905),
  ("Asia", 67.35222776275229),
  ("Africa", 52.031677405178264),
  ("Antarctica", 0.0)
)
```

### Most Populous City in each of the Three Most Populous Countries
This example uses first gets the three largest Countries, `JOIN`s the
Cities, uses a filter with a subquery to pick the city with the largest
population in each country, and then returns the name and population of
each city/country pair. The "top 3 countries" part of the query before
the `JOIN` is automatically converted to a subquery to be compliant
with SQL syntax

```scala
val query = Country.select
  .sortBy(_.population).desc
  .take(3)
  .joinOn(City)(_.code === _.countryCode)
  .filter{case (country, city) =>
    city.id ===
    City.select
      .filter(_.countryCode === country.code)
      .sortBy(_.population).desc
      .map(_.id)
      .take(1)
      .toExpr
  }
  .map {
    case (country, city) => (country.name, country.population, city.name, city.population)
  }

db.toSqlQuery(query) ==> """
SELECT
  subquery0.res__name as res__0,
  subquery0.res__population as res__1,
  city1.name as res__2,
  city1.population as res__3
FROM (SELECT
    country0.code as res__code,
    country0.name as res__name,
    country0.population as res__population
  FROM country country0
  ORDER BY res__population DESC
  LIMIT 3) subquery0
JOIN city city1 ON subquery0.res__code = city1.countrycode
WHERE city1.id = (SELECT
    city0.id as res
    FROM city city0
    WHERE city0.countrycode = subquery0.res__code
    ORDER BY city0.population DESC
    LIMIT 1)
""".trim.replaceAll("\\s+", " ")

db.run(query) ==> Seq(
  ("China", 1277558000, "Shanghai", 9696300),
  ("India", 1013662000, "Mumbai (Bombay)", 10500000),
  ("United States", 278357000, "New York", 8008278)
)
```

## Inserts
ScalaSql supports SQL `INSERT`s with multiple styles. You can insert
a single row via `.insert.values`, passing the columns you want to insert
(and leaving out any that the database would auto-populate)
```scala
val query = City.insert.values( // ID provided by database AUTO_INCREMENT
  _.name := "Sentosa",
  _.countryCode := "SGP",
  _.district := "South",
  _.population := 1337
)
db.toSqlQuery(query) ==>
  "INSERT INTO city (name, countrycode, district, population) VALUES (?, ?, ?, ?)"

db.run(query)

db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq(
  City[Id](3208, "Singapore", "SGP", district = "", population = 4017733),
  City[Id](4080, "Sentosa", "SGP", district = "South", population = 1337)
)
```

You can perform batch inserts via `.insert.batched`, passing in both a set of
columns and a list of tuples that provide the data inserted into those columns:
```scala
val query = City.insert.batched(_.name, _.countryCode, _.district, _.population)(
  ("Sentosa", "SGP", "South", 1337), // ID provided by database AUTO_INCREMENT
  ("Loyang", "SGP", "East", 31337),
  ("Jurong", "SGP", "West", 313373)
)
db.toSqlQuery(query) ==> """
  INSERT INTO city (name, countrycode, district, population) VALUES
  (?, ?, ?, ?),
  (?, ?, ?, ?),
  (?, ?, ?, ?)
  """.trim.replaceAll("\\s+", " ")

db.run(query)

db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq(
  City[Id](3208, "Singapore", "SGP", district = "", population = 4017733),
  City[Id](4080, "Sentosa", "SGP", district = "South", population = 1337),
  City[Id](4081, "Loyang", "SGP", district = "East", population = 31337),
  City[Id](4082, "Jurong", "SGP", district = "West", population = 313373)
)
```

Or you can provide an entire `SELECT` query via `.insert.select`, allowing
you to select arbitrary data from the same table (or another table) to insert:
```scala
val query = City.insert.select(
  c => (c.name, c.countryCode, c.district, c.population),
  City.select
    .filter(_.name === "Singapore")
    .map(c => (Expr("New-") + c.name, c.countryCode, c.district, Expr(0L)))
)

db.toSqlQuery(query) ==> """
  INSERT INTO city (name, countrycode, district, population)
  SELECT ? || city0.name as res__0, city0.countrycode as res__1, city0.district as res__2, ? as res__3
  FROM city city0 WHERE city0.name = ?
  """.trim.replaceAll("\\s+", " ")

db.run(query)

db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq(
  City[Id](3208, "Singapore", "SGP", district = "", population = 4017733),
  City[Id](4080, "New-Singapore", "SGP", district = "", population = 0)
)

```
These three styles of inserts that ScalaSql provides correspond directly to the
various `INSERT` syntaxes supported by the underlying database.


## Updates

ScalaSql allows updates via the `.update` syntax, that takes a filter
and a list of columns to update:
```scala
val query = City
  .update(_.countryCode === "SGP")
  .set(_.population := 0, _.district := "UNKNOWN")

db.toSqlQuery(query) ==>
  "UPDATE city SET population = ?, district = ? WHERE city.countrycode = ?"

db.run(query)

db.run(City.select.filter(_.countryCode === "SGP").single) ==>
  City[Id](3208, "Singapore", "SGP", district = "UNKNOWN", population = 0)
```

You can perform computed updates by referencing columns as part of the
expressions passed to the `.set` call:
```scala
val query = City
  .update(_.countryCode === "SGP")
  .set(c => c.population := (c.population + 1000000))
db.toSqlQuery(query) ==>
  "UPDATE city SET population = city.population + ? WHERE city.countrycode = ?"

db.run(query)

db.run(City.select.filter(_.countryCode === "SGP").single) ==>
  City[Id](3208, "Singapore", "SGP", district = "", population = 5017733)
```

The filter predicate to `.update` is mandatory, to avoid performing updates across
an entire database table accidentally . If you really want to perform an update
on every single row, you can pass in `_ => true` as your filter:
```scala
val query = City.update(_ => true).set(_.population := 0)
db.toSqlQuery(query) ==> "UPDATE city SET population = ? WHERE ?"

db.run(query)

db.run(City.select.filter(_.countryCode === "LIE")) ==> Seq(
  City[Id](2445, "Schaan", "LIE", district = "Schaan", population = 0),
  City[Id](2446, "Vaduz", "LIE", district = "Vaduz", population = 0)
)
```

## Deletes
Deletes are performed by the `.delete` method, which takes a predicate
letting you specify what rows you want to delete.
```scala
val query = City.delete(_.countryCode === "SGP")
db.toSqlQuery(query) ==> "DELETE FROM city WHERE city.countrycode = ?"
db.run(query)

db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq()
```

## Transactions
You can use `.transaction` to perform an explicit database transaction.
This transaction is opened when `.transaction` begins, is commited when
`.transaction` terminates successfully, and is rolled back if
`.transaction` terminates with an error.

Below, we can see how `.delete` immediately takes effect within the
transaction, but when it fails due to an exception the deletion is rolled
back and a subsequent transaction can see the deleted city re-appear
```scala
try {
  dbClient.transaction { implicit db =>
    db.run(City.delete(_.countryCode === "SGP"))

    db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq()

    throw new Exception()
  }
} catch { case e: Exception => /*do nothing*/ }

dbClient.transaction { implicit db =>
  db.run(City.select.filter(_.countryCode === "SGP").single) ==>
    City[Id](3208, "Singapore", "SGP", district = "", population = 4017733)
}
```

You can also roll back a transaction explicitly via the `.rollback()` method:
```scala
dbClient.transaction { implicit db =>
  db.run(City.delete(_.countryCode === "SGP"))

  db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq()

  db.rollback()
}

dbClient.transaction { implicit db =>
  db.run(City.select.filter(_.countryCode === "SGP").single) ==>
    City[Id](3208, "Singapore", "SGP", district = "", population = 4017733)
}
```

## Savepoints
Most database support Savepoints, which are sort of "nested transactions"
allowing you to roll back portions of a transaction without rolling back
everything.

ScalaSql supports these via the `.savepoint` method, which works similarly
to `.transaction`: if the provided block terminates successfully the savepoint
is committed ("released"), if it terminates with an exception the savepoint
is rolled back and all changes are undoned (as seen below)
```scala
dbClient.transaction { implicit db =>
  try {
    db.savepoint { implicit sp =>
      db.run(City.delete(_.countryCode === "SGP"))

      db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq()
      throw new Exception()
    }
  } catch { case e: Exception => /*do nothing*/ }

  db.run(City.select.filter(_.countryCode === "SGP").single) ==>
    City[Id](3208, "Singapore", "SGP", district = "", population = 4017733)

}
```

Savepoints support an explicit `.rollback()` method, just as transactions do:
```scala
dbClient.transaction { implicit db =>
  db.savepoint { implicit sp =>
    db.run(City.delete(_.countryCode === "SGP"))

    db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq()

    sp.rollback()
  }

  db.run(City.select.filter(_.countryCode === "SGP").single) ==>
    City[Id](3208, "Singapore", "SGP", district = "", population = 4017733)
}
```