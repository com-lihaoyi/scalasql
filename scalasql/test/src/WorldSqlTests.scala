package test.scalasql
import utest._

import java.sql.{JDBCType, PreparedStatement, ResultSet}

object WorldSqlTests extends TestSuite {
  // +DOCS
  //
  // This tutorials is a tour of how to use ScalaSql, from the most basic concepts
  // to writing some realistic queries. If you are browsing this on Github, you
  // can open the `Outline` pane on the right to browse the section headers to
  // see what we will cover and find anything specific of interest to you.
  //
  // ## Setup
  // ### Importing Your Database Dialect
  // To begin using ScalaSql, you need the following imports:
  import scalasql._
  import scalasql.H2Dialect._
  // This readme will use the H2 database for simplicity, but you can change the `Dialect`
  // above to other databases as necessary. ScalaSql supports H2, Sqlite, HsqlExpr,
  // Postgres, and MySql out of the box. The `Dialect` import provides the
  // various operators and functions that may be unique to each specific database
  //
  // For these examples, we will be using the
  // [MySql World Statistics Example Database](https://dev.mysql.com/doc/world-setup/en/),
  // adjusted for compatibility with H2
  //
  // ```sql
  // +INCLUDE scalasql/test/resources/world-schema.sql
  // ```
  //
  // You can also check out the self-contained examples below if you want to use other
  // supported databases, to see what kind of set up is necessary for each one
  //
  // * [Postgres](scalasql/test/src/example/PostgresExample.scala)
  // * [MySql](scalasql/test/src/example/MySqlExample.scala)
  // * [Sqlite](scalasql/test/src/example/SqliteExample.scala)
  // * [H2](scalasql/test/src/example/H2Example.scala)
  // * [HsqlExpr](scalasql/test/src/example/HsqlExprExample.scala)
  // * [HikariCP](scalasql/test/src/example/HikariCpExample.scala) (and other connection pools)
  //
  // ### Modeling Your Schema
  //
  // Next, you need to define your data model classes. In ScalaSql, your data model
  // is defined using `case class`es with each field wrapped in the wrapper type
  // parameter `T[_]`. This allows us to re-use the same case class to represent
  // both database values (when `T` is `scalasql.Expr`) as well as Scala values
  // (when `T` is `scalasql.Sc`).
  //
  // Here, we define three classes `Country` `City` and `CountryLanguage`, modeling
  // the database tables we saw above
  //
  case class Country[T[_]](
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

  object Country extends Table[Country]()

  case class City[T[_]](
      id: T[Int],
      name: T[String],
      countryCode: T[String],
      district: T[String],
      population: T[Long]
  )

  object City extends Table[City]()

  case class CountryLanguage[T[_]](
      countryCode: T[String],
      language: T[String],
      isOfficial: T[Boolean],
      percentage: T[Double]
  )

  object CountryLanguage extends Table[CountryLanguage]()
  // -DOCS

  // Shadow uTest's `ArrowAssert` to add lenient SQL string comparisons
  implicit class ArrowAssert(lhs: Any) {
    def ==>[V](rhs: V) = {
      (lhs, rhs) match {
        // Hack to make Arrays compare sanely; at some point we may want some
        // custom, extensible, typesafe equality check but for now this will do
        case (lhs: Array[_], rhs: Array[_]) =>
          Predef.assert(
            lhs.toSeq == rhs.toSeq,
            s"==> assertion failed: ${lhs.toSeq} != ${rhs.toSeq}"
          )
        // Ignore trivial formatting differences when comparing SQL strings
        case (lhs: String, rhs: String) =>
          Predef.assert(
            lhs == rhs.trim.replaceAll("\\s+", " "),
            s"==> assertion failed: $lhs != $rhs"
          )
        case (lhs, rhs) =>
          Predef.assert(lhs == rhs, s"==> assertion failed: $lhs != $rhs")
      }
    }
  }
  def tests = Tests {
    // +DOCS
    // ### Creating Your Database Client
    // Lastly, we need to initialize our `scalasql.DbClient`. This requires
    // passing in a `java.sql.Connection`, a `scalasql.Config` object, and the SQL dialect
    // you are targeting (in this case `H2Dialect`).
    val dbClient = new DbClient.Connection(
      java.sql.DriverManager
        .getConnection("jdbc:h2:mem:testdb" + scala.util.Random.nextInt(), "sa", ""),
      new Config {
        override def nameMapper(v: String) = v.toLowerCase()
      }
    )

    val db = dbClient.getAutoCommitClientConnection
    db.updateRaw(os.read(os.pwd / "scalasql" / "test" / "resources" / "world-schema.sql"))
    db.updateRaw(os.read(os.pwd / "scalasql" / "test" / "resources" / "world-data.sql"))
    // We use `dbClient.getAutoCommitClientConnection` in order to create a client that
    // will automatically run every SQL command in a new transaction and commit it. For
    // the majority of examples in this page, the exact transaction configuration doesn't
    // matter, so using the auto-committing `db` API will help focus on the queries at hand.
    // Note that when using a connection pool or `javax.sql.DataSource`, you will need to
    // explicitly `.close()` the client returned by `getAutoCommitClientConnection` when you
    // are done with it, to avoid leaking connections. Later in this tutorial we will
    // see how to use `.transaction{}` blocks to create explicit transactions that can
    // be rolled back or committed

    //
    // Lastly, we will run the `world.sql` script to initialize the database, and
    // we're ready to begin writing queries!
    // -DOCS

    test("expr") {
      // +DOCS
      // ## Expressions
      // The simplest thing you can query are `scalasql.Expr`s. These represent the SQL
      // expressions that are part of a query, and can be evaluated even without being
      // part of any database table.
      //
      // Here, we construct `Expr`s to represent the SQL query `1 + 3`. We can use
      // `db.renderSql` to see the generated SQL code, and `db.run` to send the
      // query to the database and return the output `4`
      val query = Expr(1) + Expr(3)
      db.renderSql(query) ==> "SELECT (? + ?) AS res"
      db.run(query) ==> 4
      // In general, most primitive types that can be mapped to SQL can be converted
      // to `scalasql.Expr`s: `Int`s and other numeric types, `String`s, `Boolean`s,
      // etc., each returning an `Expr[T]` for the respective type `T`. Each type of
      // `Expr[T]` has a set of operations representing what operations the database
      // supports on that type of expression.
      //
      // You can check out the [ScalaSql Reference](reference.md#exprops) if you want a
      // comprehensive list of built-in operations on various `Expr[T]` types.
      //
      // -DOCS
    }

    test("city") {
      // +DOCS
      // ## Select
      //
      // The next type of query to look at are simple `SELECT`s. Each table
      // that we modelled earlier has `.insert`, `.select`, `.update`, and `.delete`
      // methods to help construct the respective queries. You can run a `Table.select`
      // on its own in order to retrieve all the data in the table:
      val query = City.select
      db.renderSql(query) ==> """
      SELECT
        city0.id AS id,
        city0.name AS name,
        city0.countrycode AS countrycode,
        city0.district AS district,
        city0.population AS population
      FROM city city0
      """

      db.run(query).take(3) ==> Seq(
        City[Sc](1, "Kabul", "AFG", district = "Kabol", population = 1780000),
        City[Sc](2, "Qandahar", "AFG", district = "Qandahar", population = 237500),
        City[Sc](3, "Herat", "AFG", district = "Herat", population = 186800)
      )
      // Notice that `db.run` returns instances of type `City[Sc]`. `Sc` is `scalasql.Sc`,
      // short for the "Scala" type, representing a `City` object containing normal Scala
      // values. The `[Sc]` type parameter must be provided explicitly whenever creating,
      // type-annotating, or otherwise working with these `City` values.
      //
      // In this example, we do `.take(3)` after running the query to show only the first
      // 3 table entries for brevity, but by that point the `City.select` query had already
      // fetched the entire database table into memory. This can be a problem with non-trivial
      // datasets, and for that you
      // -DOCS
    }

    test("filter") {

      test("singleName") {

        // +DOCS
        // ### Filtering
        //
        // To avoid loading the entire database table into your Scala program, you can
        // add filters to the query before running it. Below, we add a filter to only
        // query the city whose name is "Singapore"
        val query = City.select.filter(_.name === "Singapore").single

        db.renderSql(query) ==> """
        SELECT
          city0.id AS id,
          city0.name AS name,
          city0.countrycode AS countrycode,
          city0.district AS district,
          city0.population AS population
        FROM city city0
        WHERE (city0.name = ?)
        """

        db.run(query) ==> City[Sc](3208, "Singapore", "SGP", district = "", population = 4017733)
        // Note that we use `===` rather than `==` for the equality comparison. The
        // function literal passed to `.filter` is given a `City[Expr]` as its parameter,
        // representing a `City` that is part of the database query, in contrast to the
        // `City[Sc]`s that `db.run` returns , and so `_.name` is of type `Expr[String]`
        // rather than just `String` or `Sc[String]`. You can use your IDE's
        // auto-complete to see what operations are available on `Expr[String]`: typically
        // they will represent SQL string functions rather than Scala string functions and
        // take and return `Expr[String]`s rather than plain Scala `String`s. Database
        // value equality is represented by the `===` operator.
        //
        // Note also the `.single` operator. This tells ScalaSql that you expect exactly
        // own result row from this query: not zero rows, and not more than one row. This
        // causes `db.run` to return a `City[Sc]` rather than `Seq[City[Sc]]`, and throw
        // an exception if zero or multiple rows are returned by the query.
        //
        // -DOCS
      }
      test("head") {

        // +DOCS
        // You can also use `.head` rather than `.single`, for cases where you
        // want a single result row and want additional result rows to be ignored
        // rather than causing an exception. `.head` is short for `.take(1).single`
        val query = City.select.filter(_.name === "Singapore").head

        db.renderSql(query) ==> """
        SELECT
          city0.id AS id,
          city0.name AS name,
          city0.countrycode AS countrycode,
          city0.district AS district,
          city0.population AS population
        FROM city city0
        WHERE (city0.name = ?)
        LIMIT ?
        """

        db.run(query) ==> City[Sc](3208, "Singapore", "SGP", district = "", population = 4017733)
        // -DOCS
      }

      test("singleId") {

        // +DOCS
        // Apart from filtering by name, it is also very common to filter by ID,
        // as shown below:
        val query = City.select.filter(_.id === 3208).single
        db.renderSql(query) ==> """
        SELECT
          city0.id AS id,
          city0.name AS name,
          city0.countrycode AS countrycode,
          city0.district AS district,
          city0.population AS population
        FROM city city0
        WHERE (city0.id = ?)
        """

        db.run(query) ==> City[Sc](3208, "Singapore", "SGP", district = "", population = 4017733)
        // -DOCS
      }

      test("multiple") {
        test("combined") {
          // +DOCS
          // You can filter on multiple things, e.g. here we look for cities in China
          // with population more than 5 million:
          val query = City.select.filter(c => c.population > 5000000 && c.countryCode === "CHN")
          db.renderSql(query) ==> """
          SELECT
            city0.id AS id,
            city0.name AS name,
            city0.countrycode AS countrycode,
            city0.district AS district,
            city0.population AS population
          FROM city city0
          WHERE ((city0.population > ?) AND (city0.countrycode = ?))
          """

          db.run(query).take(2) ==> Seq(
            City[Sc](1890, "Shanghai", "CHN", district = "Shanghai", population = 9696300),
            City[Sc](1891, "Peking", "CHN", district = "Peking", population = 7472000)
          )
          // Again, all the operations within the query work on `Expr`s: `c` is a `City[Expr]`,
          // `c.population` is an `Expr[Int]`, `c.countryCode` is an `Expr[String]`, and
          // `===` and `>` and `&&` on `Expr`s all return `Expr[Boolean]`s that represent
          // a SQL expression that can be sent to the Database as part of your query.
          // -DOCS
        }

        test("separate") {
          // +DOCS
          // You can also stack multiple separate filters together, as shown below:
          val query = City.select.filter(_.population > 5000000).filter(_.countryCode === "CHN")
          db.renderSql(query) ==> """
          SELECT
            city0.id AS id,
            city0.name AS name,
            city0.countrycode AS countrycode,
            city0.district AS district,
            city0.population AS population
          FROM city city0
          WHERE (city0.population > ?) AND (city0.countrycode = ?)
          """

          db.run(query).take(2) ==> Seq(
            City[Sc](1890, "Shanghai", "CHN", district = "Shanghai", population = 9696300),
            City[Sc](1891, "Peking", "CHN", district = "Peking", population = 7472000)
          )
          // -DOCS
        }
      }
    }

    test("lifting") {
      test("implicit") {
        // +DOCS
        // ### Lifting
        // Conversion of simple primitive `T`s into `Expr[T]`s happens implicitly. Below,
        // `===` expects both left-hand and right-hand values to be `Expr`s. `_.id` is
        // already an `Expr[Int]`, but `cityId` is a normal `Int` that is "lifted" into
        // a `Expr[Int]` automatically
        def find(cityId: Int) = db.run(City.select.filter(_.id === cityId))

        assert(find(3208) == List(City[Sc](3208, "Singapore", "SGP", "", 4017733)))
        assert(find(3209) == List(City[Sc](3209, "Bratislava", "SVK", "Bratislava", 448292)))
        // Lifting of Scala values into your ScalaSql queries is dependent on there being
        // an implicit `scalasql.TypeMapper[T]` in scope.
        //
        // but you can define `TypeMapper`s
        // for your own types if you want to be able to use them to represent types in the database
        // -DOCS
      }

      test("explicit") {
        // +DOCS
        // This implicit lifting can be done explicitly using the `Expr(...)` syntax
        // as shown below
        def find(cityId: Int) = db.run(City.select.filter(_.id === Expr(cityId)))

        assert(find(3208) == List(City[Sc](3208, "Singapore", "SGP", "", 4017733)))
        assert(find(3209) == List(City[Sc](3209, "Bratislava", "SVK", "Bratislava", 448292)))
        // -DOCS
      }

      test("values") {
        // +DOCS
        // You can also interpolate `Seq[T]`s for any `T: TypeMapper` into your
        // query using `Values(...)`, which translates into a SQL `VALUES` clause
        val query = City.select
          .filter(c => db.values(Seq("Singapore", "Kuala Lumpur", "Jakarta")).contains(c.name))
          .map(_.countryCode)

        db.renderSql(query) ==> """
        SELECT city0.countrycode AS res
        FROM city city0
        WHERE (city0.name IN (VALUES (?), (?), (?)))
        """

        db.run(query) ==> Seq("IDN", "MYS", "SGP")
        // -DOCS
      }
    }

    test("mapping") {
      test("tuple2") {
        // +DOCS
        // ### Mapping
        //
        // You can use `.map` to select exactly what values you want to return from a query.
        // Below, we query the `country` table, but only want the `name` and `continent` of
        // each country, without all the other metadata:
        val query = Country.select.map(c => (c.name, c.continent))
        db.renderSql(query) ==> """
        SELECT country0.name AS res_0, country0.continent AS res_1
        FROM country country0
        """

        db.run(query).take(5) ==> Seq(
          ("Afghanistan", "Asia"),
          ("Netherlands", "Europe"),
          ("Netherlands Antilles", "North America"),
          ("Albania", "Europe"),
          ("Algeria", "Africa")
        )
        // -DOCS
      }

      test("heterogenousTuple") {
        // +DOCS
        // These `.map` calls can contains arbitrarily complex data: below, we query
        // the `city` table to look for `Singapore` and get the entire row as a `City[Sc]`,
        // but also want to fetch the uppercase name and the population-in-millions. As
        // you would expect, you get a tuple of `(City[Sc], String, Int)` back.
        val query = City.select
          .filter(_.name === "Singapore")
          .map(c => (c, c.name.toUpperCase, c.population / 1000000))
          .single

        db.renderSql(query) ==> """
        SELECT
          city0.id AS res_0_id,
          city0.name AS res_0_name,
          city0.countrycode AS res_0_countrycode,
          city0.district AS res_0_district,
          city0.population AS res_0_population,
          UPPER(city0.name) AS res_1,
          (city0.population / ?) AS res_2
        FROM city city0
        WHERE (city0.name = ?)
        """

        db.run(query) ==>
          (
            City[Sc](3208, "Singapore", "SGP", district = "", population = 4017733),
            "SINGAPORE",
            4 // population in millions
          )
        // -DOCS
      }
    }

    test("aggregate") {
      test("sum") {
        // +DOCS
        // ### Aggregates
        //
        // You can perform simple aggregates like `.sum` as below, where we
        // query all cities in China and sum up their populations
        val query = City.select.filter(_.countryCode === "CHN").map(_.population).sum
        db.renderSql(query) ==>
          "SELECT SUM(city0.population) AS res FROM city city0 WHERE (city0.countrycode = ?)"

        db.run(query) ==> 175953614
        // -DOCS
      }
      test("sumBy") {
        // +DOCS
        // Many aggregates have a `By` version, e.g. `.sumBy`, which allows you to
        // customize exactly what you are aggregating:
        val query = City.select.sumBy(_.population)
        db.renderSql(query) ==> "SELECT SUM(city0.population) AS res FROM city city0"

        db.run(query) ==> 1429559884
        // -DOCS
      }
      test("size") {
        // +DOCS
        // `.size` is a commonly used function that translates to the SQL aggregate
        // `COUNT(1)`. Below, we count how many countries in our database have population
        // greater than one million
        val query = Country.select.filter(_.population > 1000000).size
        db.renderSql(query) ==>
          "SELECT COUNT(1) AS res FROM country country0 WHERE (country0.population > ?)"

        db.run(query) ==> 154
        // -DOCS
      }
      test("aggregate") {
        // +DOCS
        // If you want to perform multiple aggregates at once, you can use the `.aggregate`
        // function. Below, we run a single query that returns the minimum, average, and
        // maximum populations across all countries in our dataset
        val query = Country.select
          .aggregate(cs => (cs.minBy(_.population), cs.avgBy(_.population), cs.maxBy(_.population)))
        db.renderSql(query) ==> """
        SELECT
          MIN(country0.population) AS res_0,
          AVG(country0.population) AS res_1,
          MAX(country0.population) AS res_2
        FROM country country0
        """

        db.run(query) ==> (0, 25434098, 1277558000)
        // -DOCS
      }
    }

    test("sortDropTake") {
      // +DOCS
      // ### Sort/Drop/Take
      //
      // You can use `.sortBy` to order the returned rows, and `.drop` and `.take`
      // to select a range of rows within the entire result set:
      val query = City.select
        .sortBy(_.population)
        .desc
        .drop(5)
        .take(5)
        .map(c => (c.name, c.population))

      db.renderSql(query) ==> """
      SELECT city0.name AS res_0, city0.population AS res_1
      FROM city city0
      ORDER BY res_1 DESC
      LIMIT ? OFFSET ?
      """

      db.run(query) ==> Seq(
        ("Karachi", 9269265),
        ("Istanbul", 8787958),
        ("Ciudad de México", 8591309),
        ("Moscow", 8389200),
        ("New York", 8008278)
      )
      // You can also use `.drop` and `.take` without `.sortBy`, but in that case
      // the order of returned rows is arbitrary and may differ between databases
      // and implementations
      //
      // -DOCS
    }
    test("casting") {
      // +DOCS
      // ### Casting
      //
      // You can use `.cast` to generate SQL `CAST` calls between data types. Below,
      // we use it to query Singapore's life expectancy and convert it from a `Double`
      // precision floating point number to an `Int`:
      val query = Country.select
        .filter(_.name === "Singapore")
        .map(_.lifeExpectancy.cast[Int])
        .single

      db.renderSql(query) ==> """
      SELECT CAST(country0.lifeexpectancy AS INTEGER) AS res
      FROM country country0
      WHERE (country0.name = ?)
      """

      db.run(query) ==> 80
      // You can `.cast` to any type with a `TypeMapper[T]` defined, which is the
      // same set of types you can lift into queries.
      //
      //
      // -DOCS
    }

    test("nullable") {
      test("operations") {
        // +DOCS
        // ### Nullable Columns
        //
        // Nullable SQL columns are modeled via `T[Option[V]]` fields in your `case class`,
        // meaning `Expr[Option[V]]` in your query and meaning `Sc[Option[V]]` (or just
        // meaning `Option[V]`) in the returned data. `Expr[Option[V]]` supports a similar
        // set of operations as `Option[V]`: `isDefined`, `isEmpty`, `map`, `flatMap`, `get`,
        // `orElse`, etc., but returning `Expr[V]`s rather than plain `V`s.
        val query = Country.select
          .filter(_.capital.isEmpty)
          .size

        db.renderSql(query) ==> """
        SELECT COUNT(1) AS res
        FROM country country0
        WHERE (country0.capital IS NULL)
        """

        db.run(query) ==> 7
        // -DOCS
      }
      test("equality") {
        // +DOCS
        // ScalaSQL supports two different kinds of equality:
        //
        // ```scala
        // // Scala equality
        // a === b
        // a !== b
        // // SQL equality
        // a `=` b
        // a <> b
        // ```
        //
        // Most of the time these two things are the same, except when `a` or `b`
        // are nullable. In that case:
        //
        // * SQL equality follows SQL rules that `NULL = anything`
        //   is always `false`, and `NULL <> anything` is also always false.
        //
        // * Scala equality follows Scala rules that `None === None` is `true`
        //
        // The difference between these two operations can be seen below, where
        // using SQL equality to compare the `capital` column against a `None`
        // value translates directly into a SQL `=` which always returns false
        // because the right hand value is `None`/`NULL`, thus returning zero
        // countries:

        val myOptionalCityId: Option[Int] = None
        val query = Country.select
          .filter(_.capital `=` myOptionalCityId)
          .size

        db.renderSql(query) ==> """
        SELECT COUNT(1) AS res
        FROM country country0
        WHERE (country0.capital = ?)
        """

        db.run(query) ==> 0
        // Whereas using Scala equality with `===` translates into a more
        // verbose `IS NOT DISTINCT FROM`
        // expression, returning `true` when both left-hand and right-hand values
        // are `None`/`NULL`, thus successfully returning all countries for which
        // the `capital` column is `NULL`
        val query2 = Country.select
          .filter(_.capital === myOptionalCityId)
          .size

        db.renderSql(query2) ==> """
        SELECT COUNT(1) AS res
        FROM country country0
        WHERE (country0.capital IS NOT DISTINCT FROM ?)
        """

        db.run(query2) ==> 7
        // -DOCS
      }
    }
    test("joins") {
      test("inner") {
        // +DOCS
        // ### Joins
        //
        // You can perform SQL inner `JOIN`s between tables via the `.join` or `.join`
        // methods. Below, we use a `JOIN` to look for cities which are in the country
        // named "Liechtenstein":
        val query = City.select
          .join(Country)(_.countryCode === _.code)
          .filter { case (city, country) => country.name === "Liechtenstein" }
          .map { case (city, country) => city.name }

        db.renderSql(query) ==> """
        SELECT city0.name AS res
        FROM city city0
        JOIN country country1 ON (city0.countrycode = country1.code)
        WHERE (country1.name = ?)
        """

        db.run(query) ==> Seq("Schaan", "Vaduz")
        // -DOCS
      }
      test("right") {
        // +DOCS
        // `LEFT JOIN`, `RIGHT JOIN`, and `OUTER JOIN`s are also supported, e.g.
        val query = City.select
          .rightJoin(Country)(_.countryCode === _.code)
          .filter { case (cityOpt, country) => cityOpt.isEmpty(_.id) }
          .map { case (cityOpt, country) => (cityOpt.map(_.name), country.name) }

        db.renderSql(query) ==> """
        SELECT city0.name AS res_0, country1.name AS res_1
        FROM city city0
        RIGHT JOIN country country1 ON (city0.countrycode = country1.code)
        WHERE (city0.id IS NULL)
        """

        db.run(query) ==> Seq(
          (None, "Antarctica"),
          (None, "Bouvet Island"),
          (None, "British Indian Ocean Territory"),
          (None, "South Georgia and the South Sandwich Islands"),
          (None, "Heard Island and McDonald Islands"),
          (None, "French Southern territories"),
          (None, "United States Minor Outlying Islands")
        )
        // Note that when you use a left/right/outer join, the corresponding
        // rows are provided to you as `scalasql.JoinNullable[T]` rather than plain `T`s, e.g.
        // `cityOpt: scalasql.JoinNullable[City[Expr]]` above. `JoinNullable[T]` can be checked
        // for presence/absence using `.isEmpty` and specifying a specific column to check,
        // and can be converted to an `Expr[Option[T]]` by `.map`ing itt to a particular
        // `Expr[T]`.
        //
        // -DOCS
      }

      test("flatMap") {
        // +DOCS
        // ScalaSql also supports performing `JOIN`s via Scala's `for`-comprehension syntax and `.join`.
        // `for`-comprehensions also support `.crossJoin()`  for joins without an `ON` clause, and
        // `.leftJoin()` returning `JoinNullable[T]` for joins where joined table may not have corresponding
        // rows.
        val query = for {
          city <- City.select
          country <- Country.join(city.countryCode === _.code)
          if country.name === "Liechtenstein"
        } yield city.name

        db.renderSql(query) ==> """
      SELECT city0.name AS res
      FROM city city0
      JOIN country country1 ON (city0.countrycode = country1.code)
      WHERE (country1.name = ?)
      """

        db.run(query) ==> Seq("Schaan", "Vaduz")
        // -DOCS
      }
    }

    test("subquery") {
      test("join") {
        // +DOCS
        // ## Subqueries
        //
        // ScalaSql in general allows you to use SQL Subqueries anywhere you would use
        // a table. e.g. you can pass a Subquery to `.join`, as we do in the below
        // query to find language and the name of the top 2 most populous countries:
        val query = CountryLanguage.select
          .join(Country.select.sortBy(_.population).desc.take(2))(_.countryCode === _.code)
          .map { case (language, country) => (language.language, country.name) }
          .sortBy(_._1)

        db.renderSql(query) ==> """
        SELECT countrylanguage0.language AS res_0, subquery1.name AS res_1
        FROM countrylanguage countrylanguage0
        JOIN (SELECT
            country1.code AS code,
            country1.name AS name,
            country1.population AS population
          FROM country country1
          ORDER BY population DESC
          LIMIT ?) subquery1
        ON (countrylanguage0.countrycode = subquery1.code)
        ORDER BY res_0
        """

        db.run(query).take(5) ==> Seq(
          ("Asami", "India"),
          ("Bengali", "India"),
          ("Chinese", "China"),
          ("Dong", "China"),
          ("Gujarati", "India")
        )
        // -DOCS
      }
      test("from") {
        // +DOCS
        // Some operations automatically generate subqueries where necessary, e.g.
        // performing a `.join` after you have done a `.take`:
        val query = Country.select
          .sortBy(_.population)
          .desc
          .take(2)
          .join(CountryLanguage)(_.code === _.countryCode)
          .map { case (country, language) =>
            (language.language, country.name)
          }
          .sortBy(_._1)

        db.renderSql(query) ==> """
        SELECT countrylanguage1.language AS res_0, subquery0.name AS res_1
        FROM (SELECT
            country0.code AS code,
            country0.name AS name,
            country0.population AS population
          FROM country country0
          ORDER BY population DESC
          LIMIT ?) subquery0
        JOIN countrylanguage countrylanguage1
        ON (subquery0.code = countrylanguage1.countrycode)
        ORDER BY res_0
        """

        db.run(query).take(5) ==> List(
          ("Asami", "India"),
          ("Bengali", "India"),
          ("Chinese", "China"),
          ("Dong", "China"),
          ("Gujarati", "India")
        )
        // -DOCS
      }
      test("force") {
        // +DOCS
        // You can force a subquery using `.subquery`, in cases where it would normally
        // be combined into a single query. This can be useful in cases where the
        // database query plan changes based on whether a subquery is present or not
        val query = Country.select.sortBy(_.population).desc.subquery.take(2).map(_.name)

        db.renderSql(query) ==> """
        SELECT subquery0.name AS res
        FROM (SELECT country0.name AS name, country0.population AS population
          FROM country country0
          ORDER BY population DESC) subquery0
        LIMIT ?
        """

        db.run(query) ==> List("China", "India")
        // -DOCS
      }
    }

    test("union") {
      // +DOCS
      // ## Union/Except/Intersect
      //
      // ScalaSql supports `.union`/`.unionAll`/`.except`/`.intersect` operations,
      // generating SQL `UNION`/`UNION ALL`/`EXCEPT`/`INTERSECT` clauses. These
      // also generate subqueries as necessary
      val largestCountries =
        Country.select.sortBy(_.name).sortBy(_.population).desc.take(2).map(_.name)

      val smallestCountries =
        Country.select.sortBy(_.name).sortBy(_.population).asc.take(2).map(_.name)

      val query = smallestCountries.union(largestCountries)

      db.renderSql(query) ==> """
      SELECT subquery0.res AS res
      FROM (SELECT country0.name AS res
        FROM country country0
        ORDER BY country0.population ASC, res
        LIMIT ?) subquery0
      UNION
      SELECT subquery0.res AS res
      FROM (SELECT country0.name AS res
        FROM country country0
        ORDER BY country0.population DESC, res
        LIMIT ?) subquery0
      """

      db.run(query) ==> List("Antarctica", "Bouvet Island", "China", "India")
      // -DOCS
    }
    test("window") {
      test("simple") {
        // +DOCS
        // ## Window Functions
        // ScalaSql supports window functions via the `.over` operator, which
        // enables the `.partitionBy` and `.sortBy` operators on `Expr[T]`. These
        // translate into SQL's `OVER`/`PARTITION BY`/`ORDER BY` clauses
        val query = City.select
          .map(c =>
            (
              c.name,
              c.countryCode,
              c.population,
              db.rank().over.partitionBy(c.countryCode).sortBy(c.population).desc
            )
          )
          .filter { case (name, countryCode, population, rank) =>
            db.values(Seq("San Francisco", "New York", "Kuala Lumpur", "Pinang", "Johor Baharu"))
              .contains(name)
          }

        db.renderSql(query) ==> """
        SELECT
          city0.name AS res_0,
          city0.countrycode AS res_1,
          city0.population AS res_2,
          RANK() OVER (PARTITION BY city0.countrycode ORDER BY city0.population DESC) AS res_3
        FROM city city0
        WHERE (city0.name IN (VALUES (?), (?), (?), (?), (?)))
        """

        db.run(query) ==> Seq(
          ("Kuala Lumpur", "MYS", 1297526L, 1),
          ("Johor Baharu", "MYS", 328436L, 2),
          ("Pinang", "MYS", 219603L, 3),
          ("New York", "USA", 8008278L, 1),
          ("San Francisco", "USA", 776733L, 2)
        )
        // -DOCS
      }
      test("aggregate") {
        // +DOCS
        // You can also perform aggregates as part of your window function by using
        // the `.mapAggregate` function; this provides a `Aggregatable.Proxy[Q]` rather than
        // a `Q`, letting you perform aggregates like `.sumBy` that you can then use
        // as window functions via `.over`. You can reference normal columns by referencing
        // the `.expr` member on each `Aggregatable.Proxy`.
        val query = City.select
          .mapAggregate((c, cs) =>
            (
              c.name,
              c.countryCode,
              c.population,
              cs.sumBy(_.population).over.partitionBy(c.countryCode).sortBy(c.population).desc
            )
          )
          .filter { case (name, countryCode, population, rank) =>
            db.values(Seq("Singapore", "Kuala Lumpur", "Pinang", "Johor Baharu")).contains(name)
          }

        db.renderSql(query) ==> """
        SELECT
          city0.name AS res_0,
          city0.countrycode AS res_1,
          city0.population AS res_2,
          SUM(city0.population) OVER (PARTITION BY city0.countrycode ORDER BY city0.population DESC) AS res_3
        FROM city city0
        WHERE (city0.name IN (VALUES (?), (?), (?), (?)))
        """

        db.run(query).sortBy(t => (t._2, t._4)) ==> Seq(
          ("Kuala Lumpur", "MYS", 1297526L, 1297526L),
          ("Johor Baharu", "MYS", 328436L, 1625962L),
          ("Pinang", "MYS", 219603L, 1845565L),
          ("Singapore", "SGP", 4017733L, 4017733L)
        )
        // -DOCS
      }
    }

    test("realistic") {
      test("languagesByCities") {
        // +DOCS
        //
        // ## Realistic Queries
        //
        // ### Languages Spoken In Most Cities
        // Here's a more complicated query using the techniques we've learned so far:
        // a query fetching the top 10 languages spoken by the largest number of cities
        val query = City.select
          .join(CountryLanguage)(_.countryCode === _.countryCode)
          .map { case (city, language) => (city.id, language.language) }
          .groupBy { case (city, language) => language }(_.size)
          .sortBy { case (language, cityCount) => cityCount }
          .desc
          .take(10)

        db.renderSql(query) ==> """
        SELECT countrylanguage1.language AS res_0, COUNT(1) AS res_1
        FROM city city0
        JOIN countrylanguage countrylanguage1 ON (city0.countrycode = countrylanguage1.countrycode)
        GROUP BY countrylanguage1.language
        ORDER BY res_1 DESC
        LIMIT ?
        """

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
        // -DOCS
      }

      test("weightedLifeExpectancyByContinent") {
        // +DOCS
        // ### Population-Weighted Average Life Expectancy Per Continent
        // Another non-trivia query: listing the population-weighted
        // average life expectancy per continent
        val query = Country.select
          .groupBy(_.continent)(group =>
            group.sumBy(c => c.lifeExpectancy.get * c.population) / group.sumBy(_.population)
          )
          .sortBy(_._2)
          .desc

        db.renderSql(query) ==> """
        SELECT
          country0.continent AS res_0,
          (SUM((country0.lifeexpectancy * country0.population)) / SUM(country0.population)) AS res_1
        FROM country country0
        GROUP BY country0.continent
        ORDER BY res_1 DESC
        """

        db.run(query) ==> Seq(
          ("Oceania", 75.90188415576932),
          ("North America", 74.91544123695004),
          ("Europe", 73.82361172661305),
          ("South America", 67.54433544271905),
          ("Asia", 67.35222776275229),
          ("Africa", 52.031677405178264),
          ("Antarctica", 0.0)
        )
        // -DOCS
      }

      test("largestCityInThreeLargestCountries") {
        // +DOCS
        // ### Most Populous City in each of the Three Most Populous Countries
        // This example uses first gets the three largest Countries, `JOIN`s the
        // Cities, uses a filter with a subquery to pick the city with the largest
        // population in each country, and then returns the name and population of
        // each city/country pair. The "top 3 countries" part of the query before
        // the `JOIN` is automatically converted to a subquery to be compliant
        // with SQL syntax

        val query = Country.select
          .sortBy(_.population)
          .desc
          .take(3)
          .join(City)(_.code === _.countryCode)
          .filter { case (country, city) =>
            city.id ===
              City.select
                .filter(_.countryCode === country.code)
                .sortBy(_.population)
                .desc
                .map(_.id)
                .take(1)
                .toExpr
          }
          .map { case (country, city) =>
            (country.name, country.population, city.name, city.population)
          }

        db.renderSql(query) ==> """
        SELECT
          subquery0.name AS res_0,
          subquery0.population AS res_1,
          city1.name AS res_2,
          city1.population AS res_3
        FROM (SELECT
            country0.code AS code,
            country0.name AS name,
            country0.population AS population
          FROM country country0
          ORDER BY population DESC
          LIMIT ?) subquery0
        JOIN city city1 ON (subquery0.code = city1.countrycode)
        WHERE (city1.id = (SELECT
            city2.id AS res
            FROM city city2
            WHERE (city2.countrycode = subquery0.code)
            ORDER BY city2.population DESC
            LIMIT ?))
        """

        db.run(query) ==> Seq(
          ("China", 1277558000, "Shanghai", 9696300),
          ("India", 1013662000, "Mumbai (Bombay)", 10500000),
          ("United States", 278357000, "New York", 8008278)
        )
        // -DOCS
      }

      test("largestThreeCitiesByCountry") {
        // +DOCS
        // ### Most Populous Three Cities In Each Country
        // This example queries the top 3 cities with the largest population in
        // each country, using ScalaSql's `rank()` function that translates into
        // SQL's `RANK()`. Note that `RANK()` does not work inside the SQL `WHERE`
        // clause, and so we need to use `.subquery` to ensure that the `RANK()` is
        // run in an isolated subquery and does not get executed in the WHERE clause
        val query = City.select
          .map(c => (c, db.rank().over.partitionBy(c.countryCode).sortBy(c.population).desc))
          .subquery
          .filter { case (city, r) => r <= 3 }
          .map { case (city, r) => (city.name, city.population, city.countryCode, r) }
          .join(Country)(_._3 === _.code)
          .sortBy(_._5.population)
          .desc
          .map { case (name, population, countryCode, r, country) =>
            (name, population, countryCode, r)
          }

        db.renderSql(query) ==> """
        SELECT
          subquery0.res_0_name AS res_0,
          subquery0.res_0_population AS res_1,
          subquery0.res_0_countrycode AS res_2,
          subquery0.res_1 AS res_3
        FROM (SELECT
            city0.name AS res_0_name,
            city0.countrycode AS res_0_countrycode,
            city0.population AS res_0_population,
            RANK() OVER (PARTITION BY city0.countrycode ORDER BY city0.population DESC) AS res_1
          FROM city city0) subquery0
        JOIN country country1 ON (subquery0.res_0_countrycode = country1.code)
        WHERE (subquery0.res_1 <= ?)
        ORDER BY country1.population DESC
        """

        db.run(query).take(10) ==> Seq(
          ("Shanghai", 9696300L, "CHN", 1),
          ("Peking", 7472000L, "CHN", 2),
          ("Chongqing", 6351600L, "CHN", 3),
          ("Mumbai (Bombay)", 10500000L, "IND", 1),
          ("Delhi", 7206704L, "IND", 2),
          ("Calcutta [Kolkata]", 4399819L, "IND", 3),
          ("New York", 8008278L, "USA", 1),
          ("Los Angeles", 3694820L, "USA", 2),
          ("Chicago", 2896016L, "USA", 3),
          ("Jakarta", 9604900L, "IDN", 1)
        )
        // -DOCS
      }
    }

    test("insert") {
      test("values") {
        // +DOCS
        // ## Inserts
        // ScalaSql supports SQL `INSERT`s with multiple styles. You can insert
        // a single row via `.insert.values`, passing the columns you want to insert
        // (and leaving out any that the database would auto-populate)
        val query = City.insert.columns( // ID provided by database AUTO_INCREMENT
          _.name := "Sentosa",
          _.countryCode := "SGP",
          _.district := "South",
          _.population := 1337
        )
        db.renderSql(query) ==>
          "INSERT INTO city (name, countrycode, district, population) VALUES (?, ?, ?, ?)"

        db.run(query)

        db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq(
          City[Sc](3208, "Singapore", "SGP", district = "", population = 4017733),
          City[Sc](4080, "Sentosa", "SGP", district = "South", population = 1337)
        )
        // -DOCS
      }

      test("values") {
        // +DOCS
        // You can perform batch inserts via `.insert.batched`, passing in both a set of
        // columns and a list of tuples that provide the data inserted into those columns:
        val query = City.insert.batched(_.name, _.countryCode, _.district, _.population)(
          ("Sentosa", "SGP", "South", 1337), // ID provided by database AUTO_INCREMENT
          ("Loyang", "SGP", "East", 31337),
          ("Jurong", "SGP", "West", 313373)
        )
        db.renderSql(query) ==> """
        INSERT INTO city (name, countrycode, district, population) VALUES
        (?, ?, ?, ?),
        (?, ?, ?, ?),
        (?, ?, ?, ?)
        """

        db.run(query)

        db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq(
          City[Sc](3208, "Singapore", "SGP", district = "", population = 4017733),
          City[Sc](4080, "Sentosa", "SGP", district = "South", population = 1337),
          City[Sc](4081, "Loyang", "SGP", district = "East", population = 31337),
          City[Sc](4082, "Jurong", "SGP", district = "West", population = 313373)
        )
        // -DOCS
      }

      test("select") {
        // +DOCS
        // Or you can provide an entire `SELECT` query via `.insert.select`, allowing
        // you to select arbitrary data from the same table (or another table) to insert:
        val query = City.insert.select(
          c => (c.name, c.countryCode, c.district, c.population),
          City.select
            .filter(_.name === "Singapore")
            .map(c => (Expr("New-") + c.name, c.countryCode, c.district, Expr(0L)))
        )

        db.renderSql(query) ==> """
        INSERT INTO city (name, countrycode, district, population)
        SELECT (? || city0.name) AS res_0, city0.countrycode AS res_1, city0.district AS res_2, ? AS res_3
        FROM city city0
        WHERE (city0.name = ?)
        """

        db.run(query)

        db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq(
          City[Sc](3208, "Singapore", "SGP", district = "", population = 4017733),
          City[Sc](4080, "New-Singapore", "SGP", district = "", population = 0)
        )
        // These three styles of inserts that ScalaSql provides correspond directly to the
        // various `INSERT` syntaxes supported by the underlying database.
        //
        // -DOCS
      }
    }

    test("update") {
      test("simple") {
        // +DOCS
        // ## Updates
        //
        // ScalaSql allows updates via the `.update` syntax, that takes a filter
        // and a list of columns to update:
        val query = City
          .update(_.countryCode === "SGP")
          .set(_.population := 0, _.district := "UNKNOWN")

        db.renderSql(query) ==>
          "UPDATE city SET population = ?, district = ? WHERE (city.countrycode = ?)"

        db.run(query)

        db.run(City.select.filter(_.countryCode === "SGP").single) ==>
          City[Sc](3208, "Singapore", "SGP", district = "UNKNOWN", population = 0)
        // -DOCS
      }

      test("computed") {
        // +DOCS
        // You can perform computed updates by referencing columns as part of the
        // expressions passed to the `.set` call:
        val query = City
          .update(_.countryCode === "SGP")
          .set(c => c.population := (c.population + 1000000))
        db.renderSql(query) ==>
          "UPDATE city SET population = (city.population + ?) WHERE (city.countrycode = ?)"

        db.run(query)

        db.run(City.select.filter(_.countryCode === "SGP").single) ==>
          City[Sc](3208, "Singapore", "SGP", district = "", population = 5017733)
        // -DOCS
      }
      test("all") {
        // +DOCS
        // The filter predicate to `.update` is mandatory, to avoid performing updates across
        // an entire database table accidentally . If you really want to perform an update
        // on every single row, you can pass in `_ => true` as your filter:
        val query = City.update(_ => true).set(_.population := 0)
        db.renderSql(query) ==> "UPDATE city SET population = ?"

        db.run(query)

        db.run(City.select.filter(_.countryCode === "LIE")) ==> Seq(
          City[Sc](2445, "Schaan", "LIE", district = "Schaan", population = 0),
          City[Sc](2446, "Vaduz", "LIE", district = "Vaduz", population = 0)
        )
        // -DOCS
      }
    }

    test("delete") {
      // +DOCS
      // ## Deletes
      // Deletes are performed by the `.delete` method, which takes a predicate
      // letting you specify what rows you want to delete.
      val query = City.delete(_.countryCode === "SGP")
      db.renderSql(query) ==> "DELETE FROM city WHERE (city.countrycode = ?)"
      db.run(query)

      db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq()
      // -DOCS
    }

    test("transactions") {
      test("exception") {
        // +DOCS
        // ## Transactions
        // You can use `.transaction` to perform an explicit database transaction.
        // This transaction is opened when `.transaction` begins, is commited when
        // `.transaction` terminates successfully, and is rolled back if
        // `.transaction` terminates with an error.
        //
        // Below, we can see how `.delete` immediately takes effect within the
        // transaction, but when it fails due to an exception the deletion is rolled
        // back and a subsequent transaction can see the deleted city re-appear
        try {
          dbClient.transaction { implicit db =>
            db.run(City.delete(_.countryCode === "SGP"))

            db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq()

            throw new Exception()
          }
        } catch { case e: Exception => /*do nothing*/ }

        dbClient.transaction { implicit db =>
          db.run(City.select.filter(_.countryCode === "SGP").single) ==>
            City[Sc](3208, "Singapore", "SGP", district = "", population = 4017733)
        }
        // -DOCS
      }
      test("rollback") {
        // +DOCS
        // You can also roll back a transaction explicitly via the `.rollback()` method:
        dbClient.transaction { implicit db =>
          db.run(City.delete(_.countryCode === "SGP"))

          db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq()

          db.rollback()
        }

        dbClient.transaction { implicit db =>
          db.run(City.select.filter(_.countryCode === "SGP").single) ==>
            City[Sc](3208, "Singapore", "SGP", district = "", population = 4017733)
        }
        // -DOCS
      }
    }
    test("savepoint") {
      test("exception") {
        // +DOCS
        // ### Savepoints
        // Most database support Savepoints, which are sort of "nested transactions"
        // allowing you to roll back portions of a transaction without rolling back
        // everything.
        //
        // ScalaSql supports these via the `.savepoint` method, which works similarly
        // to `.transaction`: if the provided block terminates successfully the savepoint
        // is committed ("released"), if it terminates with an exception the savepoint
        // is rolled back and all changes are undoned (as seen below)
        dbClient.transaction { implicit db =>
          try {
            db.savepoint { _ =>
              db.run(City.delete(_.countryCode === "SGP"))

              db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq()
              throw new Exception()
            }
          } catch { case e: Exception => /*do nothing*/ }

          db.run(City.select.filter(_.countryCode === "SGP").single) ==>
            City[Sc](3208, "Singapore", "SGP", district = "", population = 4017733)

        }
        // -DOCS
      }
      test("rollback") {
        // +DOCS
        // Savepoints support an explicit `.rollback()` method, just as transactions do:
        dbClient.transaction { implicit db =>
          db.savepoint { implicit sp =>
            db.run(City.delete(_.countryCode === "SGP"))

            db.run(City.select.filter(_.countryCode === "SGP")) ==> Seq()

            sp.rollback()
          }

          db.run(City.select.filter(_.countryCode === "SGP").single) ==>
            City[Sc](3208, "Singapore", "SGP", district = "", population = 4017733)
        }
        // -DOCS
      }
    }

    test("customExpressions") {
      // +DOCS
      // ## Custom Expressions
      //
      // You can define custom SQL expressions via the `Expr` constructor. This is
      // useful for enclosing ScalaSql when you need to use some operator or syntax
      // that your Database supports but ScalaSql does not have built in. This example
      // shows how to define a custom `rawToHex` Scala function working on `Expr[T]`s,
      // that translates down to the H2 database's `RAWTOHEX` SQL function, and finally
      // using that in a query to return a string.
      import scalasql.core.SqlStr.SqlStringSyntax

      def rawToHex(v: Expr[String]): Expr[String] = Expr { implicit ctx => sql"RAWTOHEX($v)" }

      val query = City.select.filter(_.countryCode === "SGP").map(c => rawToHex(c.name)).single

      db.renderSql(query) ==>
        "SELECT RAWTOHEX(city0.name) AS res FROM city city0 WHERE (city0.countrycode = ?)"

      db.run(query) ==> "00530069006e006700610070006f00720065"
      // Your custom Scala functions can either be standalone functions or extension
      // methods. Most of the operators on `Expr[T]` that ScalaSql comes bundled with
      // are extension methods, with a different set being made available for each database.
      //
      // Different databases have a huge range of functions available. ScalaSql comes
      // with the most commonly-used functions built in, but it is expected that you will
      // need to build up your own library of custom `Expr[T]` functions to to access
      // less commonly used functions that are nonetheless still needed in your application
      // -DOCS
    }
    test("customTypeMapper") {
      // +DOCS
      // ## Custom Type Mappings
      //
      // You can define custom `TypeMapper`s to support reading and writing values to
      // the database which are of a type not supported by ScalaSql. The example below
      // demonstrates how to define a custom `CityId` type, define an implicit `TypeMapper`
      // for it, and then `INSERT` it into the database and `SELECT` it out after.
      //

      case class CityId(value: Int)

      object CityId {
        implicit def tm: TypeMapper[CityId] = new TypeMapper[CityId] {
          def jdbcType: JDBCType = JDBCType.INTEGER

          def get(r: ResultSet, idx: Int): CityId = new CityId(r.getInt(idx))

          def put(r: PreparedStatement, idx: Int, v: CityId): Unit = r.setInt(idx, v.value)
        }
      }

      // -DOCS
      // Note sure why this is required, probably a Scalac bug
      SqlStr.Interp.TypeInterp[CityId](CityId(1337))
      // +DOCS

      case class City[T[_]](
          id: T[CityId],
          name: T[String],
          countryCode: T[String],
          district: T[String],
          population: T[Long]
      )

      object City extends Table[City]() {
        override def tableName: String = "city"
      }
      db.run(
        City.insert.columns(
          _.id := CityId(313373),
          _.name := "test",
          _.countryCode := "XYZ",
          _.district := "district",
          _.population := 1000000
        )
      )

      db.run(City.select.filter(_.id === 313373).single) ==>
        City[Sc](CityId(313373), "test", "XYZ", "district", 1000000)
      // -DOCS

      // You can also use `TypeMapper#bimap` for the common case where you want the
      // new `TypeMapper` to behave the same as an existing `TypeMapper`, just with
      // conversion functions to convert back and forth between the old type and new type:

      case class CityId2(value: Int)

      object CityId2 {
        implicit def tm: TypeMapper[CityId2] = implicitly[TypeMapper[Int]].bimap[CityId2](
          city => city.value,
          int => CityId2(int)
        )
      }

      // -DOCS
      // Note sure why this is required, probably a Scalac bug
      SqlStr.Interp.TypeInterp[CityId2](CityId2(1337))
      // +DOCS
      case class City2[T[_]](
          id: T[CityId2],
          name: T[String],
          countryCode: T[String],
          district: T[String],
          population: T[Long]
      )

      object City2 extends Table[City2]() {
        override def tableName: String = "city"
      }
      db.run(
        City2.insert.columns(
          _.id := CityId2(31337),
          _.name := "test",
          _.countryCode := "XYZ",
          _.district := "district",
          _.population := 1000000
        )
      )

      db.run(City2.select.filter(_.id === 31337).single) ==>
        City2[Sc](CityId2(31337), "test", "XYZ", "district", 1000000)
    }
    test("customTableColumnNames") {
      // +DOCS
      // ## Customizing Table and Column Names
      //
      // ScalaSql allows you to customize the table and column names via overriding
      // `def table` and `def tableColumnNameOverride` om your `Table` object.

      case class CityCustom[T[_]](
          idCustom: T[Int],
          nameCustom: T[String],
          countryCodeCustom: T[String],
          districtCustom: T[String],
          populationCustom: T[Long]
      )

      object CityCustom extends Table[CityCustom]() {

        override def tableName: String = "city"

        override def tableColumnNameOverride(s: String): String = s match {
          case "idCustom" => "id"
          case "nameCustom" => "name"
          case "countryCodeCustom" => "countrycode"
          case "districtCustom" => "district"
          case "populationCustom" => "population"
        }
      }

      val query = CityCustom.select
      db.renderSql(query) ==> """
      SELECT
        city0.id AS idcustom,
        city0.name AS namecustom,
        city0.countrycode AS countrycodecustom,
        city0.district AS districtcustom,
        city0.population AS populationcustom
      FROM city city0
      """

      db.run(query).take(3) ==> Seq(
        CityCustom[Sc](1, "Kabul", "AFG", districtCustom = "Kabol", populationCustom = 1780000),
        CityCustom[Sc](
          2,
          "Qandahar",
          "AFG",
          districtCustom = "Qandahar",
          populationCustom = 237500
        ),
        CityCustom[Sc](3, "Herat", "AFG", districtCustom = "Herat", populationCustom = 186800)
      )
      // -DOCS
    }
  }

}
