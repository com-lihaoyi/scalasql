package scalasql
import utest._
import dialects.SqliteDialect._
import scalasql.dialects.SqliteDialect
import scalasql.query.Expr

case class Country[T[_]](
    code: T[String],
    name: T[String],
    continent: T[String],
    region: T[String],
    surfaceArea: T[Int],
    indepYear: T[Option[Int]],
    population: T[Int],
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

case class City[T[_]](
    id: T[Int],
    name: T[String],
    countryCode: T[String],
    district: T[String],
    population: T[Int]
)

object City extends Table[City]() {
  val metadata = initMetadata()
}

case class CountryLanguage[T[_]](
    countryCode: T[String],
    language: T[String],
    isOfficial: T[Boolean],
    percentage: T[Double]
)

object CountryLanguage extends Table[CountryLanguage]() {
  val metadata = initMetadata()
}

object MainTests extends TestSuite {
  val db = new DatabaseClient(
    java.sql.DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", ""),
    new Config{
      override def columnNameMapper(v: String) = v.toLowerCase()
      override def tableNameMapper(v: String) = v.toLowerCase()
    },
    SqliteDialect
  ).autoCommit
  db.runRawUpdate(os.read(os.pwd / "test" / "resources" / "world.sql"))

  def tests = Tests {

    // From https://www.lihaoyi.com/post/WorkingwithDatabasesusingScalaandQuill.html
    test("constant") {
      val query = Expr(1)
      val sql = db.toSqlQuery(query)
      assert(sql == """SELECT ? as res""")

      val res = db.run(query)
      val expected = 1
      assert(res == expected)
    }

    test("city") {
      val query = City.select
      val sql = db.toSqlQuery(query)
      assert(
        sql ==
          """
        SELECT
          city0.id as res__id,
          city0.name as res__name,
          city0.countrycode as res__countrycode,
          city0.district as res__district,
          city0.population as res__population
        FROM city city0
        """.trim.replaceAll("\\s+", " ")
      )

      val res = db.run(query).take(5)

      val expected = Seq[City[Id]](
        City[Id](
          id = 1,
          name = "Kabul",
          countryCode = "AFG",
          district = "Kabol",
          population = 1780000
        ),
        City[Id](
          id = 2,
          name = "Qandahar",
          countryCode = "AFG",
          district = "Qandahar",
          population = 237500
        ),
        City[Id](
          id = 3,
          name = "Herat",
          countryCode = "AFG",
          district = "Herat",
          population = 186800
        ),
        City[Id](
          id = 4,
          name = "Mazar-e-Sharif",
          countryCode = "AFG",
          district = "Balkh",
          population = 127800
        ),
        City[Id](
          id = 5,
          name = "Amsterdam",
          countryCode = "NLD",
          district = "Noord-Holland",
          population = 731200
        )
      )

      assert(res == expected)
    }
    test("country") {
      val query = Country.select
      val sql = db.toSqlQuery(query)

      assert(
        sql ==
          """
        SELECT
          country0.code as res__code,
          country0.name as res__name,
          country0.continent as res__continent,
          country0.region as res__region,
          country0.surfacearea as res__surfacearea,
          country0.indepyear as res__indepyear,
          country0.population as res__population,
          country0.lifeexpectancy as res__lifeexpectancy,
          country0.gnp as res__gnp,
          country0.gnpold as res__gnpold,
          country0.localname as res__localname,
          country0.governmentform as res__governmentform,
          country0.headofstate as res__headofstate,
          country0.capital as res__capital,
          country0.code2 as res__code2
        FROM country country0
        """.trim.replaceAll("\\s+", " ")
      )

      val res = db.run(Country.select).take(2)
      val expected = Seq(
        Country[Id](
          code = "AFG",
          name = "Afghanistan",
          continent = "Asia",
          region = "Southern and Central Asia",
          surfaceArea = 652090,
          indepYear = Some(value = 1919),
          population = 22720000,
          lifeExpectancy = Some(value = 45.900001525878906),
          gnp = Some(value = 5976.00),
          gnpOld = None,
          localName = "Afganistan/Afqanestan",
          governmentForm = "Islamic Emirate",
          headOfState = Some(value = "Mohammad Omar"),
          capital = Some(value = 1),
          code2 = "AF"
        ),
        Country[Id](
          code = "NLD",
          name = "Netherlands",
          continent = "Europe",
          region = "Western Europe",
          surfaceArea = 41526,
          indepYear = Some(value = 1581),
          population = 15864000,
          lifeExpectancy = Some(value = 78.30000305175781),
          gnp = Some(value = 371362.00),
          gnpOld = Some(value = 360478.00),
          localName = "Nederland",
          governmentForm = "Constitutional Monarchy",
          headOfState = Some(value = "Beatrix"),
          capital = Some(value = 5),
          code2 = "NL"
        )
      )

      assert(res == expected)
    }

    test("countryLanguage") {
      val query = CountryLanguage.select
      val sql = db.toSqlQuery(query)
      assert(
        sql ==
          """
        SELECT
          countrylanguage0.countrycode as res__countrycode,
          countrylanguage0.language as res__language,
          countrylanguage0.isofficial as res__isofficial,
          countrylanguage0.percentage as res__percentage
        FROM countrylanguage countrylanguage0
        """.trim.replaceAll("\\s+", " ")
      )

      val res = db.run(query).take(3)
      val expected = Seq(
        CountryLanguage[Id](
          countryCode = "AFG",
          language = "Pashto",
          isOfficial = true,
          percentage = 52.400001525878906
        ),
        CountryLanguage[Id](
          countryCode = "NLD",
          language = "Dutch",
          isOfficial = true,
          percentage = 95.5999984741211
        ),
        CountryLanguage[Id](
          countryCode = "ANT",
          language = "Papiamento",
          isOfficial = true,
          percentage = 86.19999694824219
        ),

      )

      assert(res == expected)
    }

    test("queryFilter") {
      test("singleName") {
        val query = City.select.filter(_.name === "Singapore")
        val sql = db.toSqlQuery(query)
        assert(
          sql ==
            """
          SELECT
            city0.id as res__id,
            city0.name as res__name,
            city0.countrycode as res__countrycode,
            city0.district as res__district,
            city0.population as res__population
          FROM city city0
          WHERE city0.name = ?
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)
        val expected = Seq[City[Id]](
          City[Id](
            id = 3208,
            name = "Singapore",
            countryCode = "SGP",
            district = "",
            population = 4017733
          )
        )

        assert(res == expected)
      }

      test("singleId") {
        val query = City.select.filter(_.id === 3208)
        val sql = db.toSqlQuery(query)
        assert(
          sql ==
            """
          SELECT
            city0.id as res__id,
            city0.name as res__name,
            city0.countrycode as res__countrycode,
            city0.district as res__district,
            city0.population as res__population
          FROM city city0
          WHERE city0.id = ?
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)
        val expected = Seq[City[Id]](
          City[Id](
            id = 3208,
            name = "Singapore",
            countryCode = "SGP",
            district = "",
            population = 4017733
          )
        )

        assert(res == expected)
      }

      test("singlePopulation") {
        val query = City.select.filter(_.population > 9000000)
        val sql = db.toSqlQuery(query)
        assert(
          sql ==
            """
          SELECT
            city0.id as res__id,
            city0.name as res__name,
            city0.countrycode as res__countrycode,
            city0.district as res__district,
            city0.population as res__population
          FROM city city0
          WHERE city0.population > ?
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query).take(5)
        val expected = Seq(
          City[Id](
            id = 206,
            name = "São Paulo",
            countryCode = "BRA",
            district = "São Paulo",
            population = 9968485
          ),
          City[Id](
            id = 939,
            name = "Jakarta",
            countryCode = "IDN",
            district = "Jakarta Raya",
            population = 9604900
          ),
          City[Id](
            id = 1024,
            name = "Mumbai (Bombay)",
            countryCode = "IND",
            district = "Maharashtra",
            population = 10500000
          ),
          City[Id](
            id = 1890,
            name = "Shanghai",
            countryCode = "CHN",
            district = "Shanghai",
            population = 9696300
          ),
          City[Id](
            id = 2331,
            name = "Seoul",
            countryCode = "KOR",
            district = "Seoul",
            population = 9981619
          )
        )

        assert(res == expected)
      }

      test("multiple") {
        val expected = Seq[City[Id]](
          City[Id](
            id = 1890,
            name = "Shanghai",
            countryCode = "CHN",
            district = "Shanghai",
            population = 9696300
          ),
          City[Id](
            id = 1891,
            name = "Peking",
            countryCode = "CHN",
            district = "Peking",
            population = 7472000
          ),
          City[Id](
            id = 1892,
            name = "Chongqing",
            countryCode = "CHN",
            district = "Chongqing",
            population = 6351600
          ),
          City[Id](
            id = 1893,
            name = "Tianjin",
            countryCode = "CHN",
            district = "Tianjin",
            population = 5286800
          )
        )

        test("combined") {
          val query = City.select.filter(c => c.population > 5000000 && c.countryCode === "CHN")
          val sql = db.toSqlQuery(query)
          assert(
            sql ==
              """
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
          )

          val res = db.run(query).take(5)
          assert(res == expected)
        }

        test("separate") {
          val query = City.select.filter(_.population > 5000000).filter(_.countryCode === "CHN")
          val sql = db.toSqlQuery(query)
          assert(
            sql ==
              """
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
          )

          val res = db.run(query).take(5)
          assert(res == expected)
        }
      }
    }

    test("lifting") {
      def find(cityId: Int) = db.run(City.select.filter(_.id === cityId))

      assert(find(3208) == List(City[Id](3208, "Singapore", "SGP", "", 4017733)))
      assert(find(3209) == List(City[Id](3209, "Bratislava", "SVK", "Bratislava", 448292)))
    }

    test("mapping") {
      test("tuple2") {
        val query = Country.select.map(c => (c.name, c.continent))
        val sql = db.toSqlQuery(query)
        assert(
          sql ==
            """
          SELECT
            country0.name as res__0,
            country0.continent as res__1
          FROM country country0
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query).take(5)
        val expected = Seq(
          ("Afghanistan", "Asia"),
          ("Netherlands", "Europe"),
          ("Netherlands Antilles", "North America"),
          ("Albania", "Europe"),
          ("Algeria", "Africa")
        )

        assert(res == expected)
      }

      test("tuple3") {
        val query = Country.select.map(c => (c.name, c.continent, c.population))
        val sql = db.toSqlQuery(query)
        assert(
          sql ==
            """
          SELECT
            country0.name as res__0,
            country0.continent as res__1,
            country0.population as res__2
          FROM country country0
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query).take(5)
        val expected = Seq(
          ("Afghanistan", "Asia", 22720000),
          ("Netherlands", "Europe", 15864000),
          ("Netherlands Antilles", "North America", 217000),
          ("Albania", "Europe", 3401200),
          ("Algeria", "Africa", 31471000)
        )

        assert(res == expected)
      }

      test("interpolateInMap") {
        val query = Country.select.filter(_.name === "Singapore").map(c => c.population * 2)
        val sql = db.toSqlQuery(query)
        assert(
          sql ==
            """
          SELECT
            country0.population * ? as res
          FROM country country0
          WHERE country0.name = ?
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)
        val expected = Seq(7134000)
        assert(res == expected)
      }

      test("interpolateInMap2") {
        val query =
          Country.select.filter(_.name === "Singapore").map(c => (c.name, c.population * 2))
        val sql = db.toSqlQuery(query)
        assert(
          sql ==
            """
          SELECT
            country0.name as res__0,
            country0.population * ? as res__1
          FROM country country0
          WHERE country0.name = ?
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)
        val expected = Seq(("Singapore", 7134000))
        assert(res == expected)
      }

      test("heterogenousTuple") {
        val query =
          City.select.filter(_.name === "Singapore").map(c => (c, c.name, c.population * 2))
        val sql = db.toSqlQuery(query)
        assert(
          sql ==
            """
          SELECT
            city0.id as res__0__id,
            city0.name as res__0__name,
            city0.countrycode as res__0__countrycode,
            city0.district as res__0__district,
            city0.population as res__0__population,
            city0.name as res__1,
            city0.population * ? as res__2
          FROM
            city city0
          WHERE
            city0.name = ?
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)
        val expected = Seq(
          (
            City[Id](
              id = 3208,
              name = "Singapore",
              countryCode = "SGP",
              district = "",
              population = 4017733
            ),
            "Singapore",
            8035466
          )
        )

        assert(res == expected)
      }

      test("filterMap") {
        def findName(cityId: Int) = db.run(City.select.filter(_.id === cityId).map(_.name))

        assert(findName(3208) == List("Singapore"))
        assert(findName(3209) == List("Bratislava"))
      }
    }

    test("aggregate") {
      test("sum") {
        val query = City.select.map(_.population).sum
        val sql = db.toSqlQuery(query)

        assert(sql == """SELECT SUM(city0.population) as res FROM city city0""")

        val res = db.run(query)
        val expected = 1429559884
        assert(res == expected)
      }
      test("sumBy") {
        val query = City.select.sumBy(_.population)
        val sql = db.toSqlQuery(query)

        assert(sql == """SELECT SUM(city0.population) as res FROM city city0""")

        val res = db.run(query)
        val expected = 1429559884
        assert(res == expected)
      }
      test("count") {
        val query = Country.select.size
        val sql = db.toSqlQuery(query)

        assert(sql == """SELECT COUNT(1) as res FROM country country0""")

        val res = db.run(query)
        val expected = 239
        assert(res == expected)
      }
      test("min") {
        val query = City.select.map(_.population).min
        val sql = db.toSqlQuery(query)

        assert(sql == """SELECT MIN(city0.population) as res FROM city city0""")

        val res = db.run(query)
        val expected = 42
        assert(res == expected)
      }
      test("minBy") {
        val query = City.select.minBy(_.population)
        val sql = db.toSqlQuery(query)

        assert(sql == """SELECT MIN(city0.population) as res FROM city city0""")

        val res = db.run(query)
        val expected = 42
        assert(res == expected)
      }
      test("max") {
        val query = City.select.map(_.population).max
        val sql = db.toSqlQuery(query)

        assert(sql == """SELECT MAX(city0.population) as res FROM city city0""")

        val res = db.run(query)
        val expected = 10500000
        assert(res == expected)
      }
      test("maxBy") {
        val query = City.select.maxBy(_.population)
        val sql = db.toSqlQuery(query)

        assert(sql == """SELECT MAX(city0.population) as res FROM city city0""")

        val res = db.run(query)
        val expected = 10500000
        assert(res == expected)
      }
      test("avg") {
        val query = City.select.map(_.population).avg
        val sql = db.toSqlQuery(query)

        assert(sql == """SELECT AVG(city0.population) as res FROM city city0""")

        val res = db.run(query)
        val expected = 350468
        assert(res == expected)
      }
      test("avgBy") {
        val query = City.select.avgBy(_.population)
        val sql = db.toSqlQuery(query)

        assert(sql == """SELECT AVG(city0.population) as res FROM city city0""")

        val res = db.run(query)
        val expected = 350468
        assert(res == expected)
      }
    }

    test("sortLimitOffset") {
      test {
        val query =
          City.select.sortBy(_.population).desc.drop(5).take(5).map(c => (c.name, c.population))
        val sql = db.toSqlQuery(query)

        assert(
          sql ==
            """
        SELECT
          city0.name as res__0,
          city0.population as res__1
        FROM city city0
        ORDER BY res__1 DESC
        LIMIT 5 OFFSET 5
        """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)
        val expected = Seq(
          ("Karachi", 9269265),
          ("Istanbul", 8787958),
          ("Ciudad de México", 8591309),
          ("Moscow", 8389200),
          ("New York", 8008278)
        )

        assert(res == expected)
      }

      test {
        val query = City.select.sortBy(_.population).asc.take(5).map(c => (c.name, c.population))
        val sql = db.toSqlQuery(query)
        assert(
          sql ==
            """
        SELECT
          city0.name as res__0,
          city0.population as res__1
        FROM city city0
        ORDER BY res__1 ASC
        LIMIT 5
        """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)
        val expected = Seq(
          ("Adamstown", 42),
          ("West Island", 167),
          ("Fakaofo", 300),
          ("Città del Vaticano", 455),
          ("Bantam", 503)
        )

        assert(res == expected)
      }
    }

    test("joins") {
      val query = City.select
        .joinOn(Country)(_.countryCode === _.code)
        .filter { case (city, country) => country.name === "Liechtenstein" }
        .map{  case (city, country) => city.name }

      val sql = db.toSqlQuery(query)

      assert(
        sql ==
        """
        SELECT city0.name as res
        FROM city city0
        JOIN country country1 ON city0.countrycode = country1.code
        WHERE country1.name = ?
        """.trim.replaceAll("\\s+", " ")
      )

      val res = db.run(query)

      val expected = Seq("Schaan", "Vaduz")

      assert(res == expected)
    }

    test("flatMap") {
      test {
        val query = for{
          city <- City.select
          country <- Country.select
          if city.countryCode === country.code
          if country.name === "Liechtenstein"
        } yield city.name
        
        val sql = db.toSqlQuery(query)

        assert(
          sql ==
          """
          SELECT city0.name as res
          FROM city city0, country country1
          WHERE city0.countrycode = country1.code AND country1.name = ?
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)

        val expected = Seq("Schaan", "Vaduz")

        assert(res == expected)
      }

    }

    test("subquery") {
      test("join") {
        val query = CountryLanguage.select
          .joinOn(Country.select.sortBy(_.population).desc.take(2))(_.countryCode === _.code)
          .map { case (language, country) => (language.language, country.name) }

        val sql = db.toSqlQuery(query)
        assert(
          sql ==
          """
          SELECT countrylanguage0.language as res__0, subquery1.res__name as res__1
          FROM countrylanguage countrylanguage0
          JOIN (SELECT country0.code as res__code, country0.population as res__population
              FROM country country0
              ORDER BY res__population DESC
              LIMIT 2) subquery1
          ON countrylanguage0.countrycode = subquery1.res__code
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)
        val expected = Seq(
          ("Chinese", "China"),
          ("Dong", "China"),
          ("Hui", "China"),
          ("Mant�u", "China"),
          ("Miao", "China"),
          ("Mongolian", "China"),
          ("Puyi", "China"),
          ("Tibetan", "China"),
          ("Tujia", "China"),
          ("Uighur", "China"),
          ("Yi", "China"),
          ("Zhuang", "China"),
          ("Asami", "India"),
          ("Bengali", "India"),
          ("Gujarati", "India"),
          ("Hindi", "India"),
          ("Kannada", "India"),
          ("Malajalam", "India"),
          ("Marathi", "India"),
          ("Orija", "India"),
          ("Punjabi", "India"),
          ("Tamil", "India"),
          ("Telugu", "India"),
          ("Urdu", "India")
        )

        assert(res == expected)
      }
      test("from") {
        val query = Country.select.sortBy(_.population).desc.take(2)
          .joinOn(CountryLanguage)(_.code === _.countryCode)
          .map { case (country, language) => (language.language, country.name) }

        val sql = db.toSqlQuery(query)
        assert(
          sql ==
          """
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
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)

        val expected = List(
          ("Chinese", "China"),
          ("Dong", "China"),
          ("Hui", "China"),
          ("Mantu", "China"),
          ("Miao", "China"),
          ("Mongolian", "China"),
          ("Puyi", "China"),
          ("Tibetan", "China"),
          ("Tujia", "China"),
          ("Uighur", "China"),
          ("Yi", "China"),
          ("Zhuang", "China"),
          ("Asami", "India"),
          ("Bengali", "India"),
          ("Gujarati", "India"),
          ("Hindi", "India"),
          ("Kannada", "India"),
          ("Malajalam", "India"),
          ("Marathi", "India"),
          ("Orija", "India"),
          ("Punjabi", "India"),
          ("Tamil", "India"),
          ("Telugu", "India"),
          ("Urdu", "India")
        )

        assert(res == expected)
      }
      test("fromAndJoin") {
        val query = Country.select.sortBy(_.population).desc.take(2)
          .joinOn(City.select.sortBy(_.population).desc.take(20))(_.code === _.countryCode)
          .map { case (country, city) => (country.name, city.name) }

        val sql = db.toSqlQuery(query)
        assert(
          sql ==
          """
          SELECT subquery0.res__name as res__0, subquery1.res__name as res__1
          FROM (SELECT
              country0.code as res__code,
              country0.name as res__name,
              country0.population as res__population
            FROM country country0
            ORDER BY res__population DESC
            LIMIT 2) subquery0
          JOIN (SELECT
              city0.countrycode as res__countrycode,
              city0.population as res__population
            FROM city city0
            ORDER BY res__population DESC
            LIMIT 20) subquery1
          ON subquery0.res__code = subquery1.res__countrycode
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)
        val expected = List(
          ("India", "Mumbai (Bombay)"),
          ("China", "Shanghai"),
          ("China", "Peking"),
          ("India", "Delhi"),
          ("China", "Chongqing")
        )

        assert(res == expected)
      }

      test("sortLimitSortLimit") {
        val query = City.select.sortBy(_.population).desc.take(20).sortBy(_.population).asc.take(
          10
        ).map(_.name)

        val sql = db.toSqlQuery(query)
        assert(
          sql ==
          """
          SELECT subquery0.res__name as res
          FROM (SELECT city0.name as res__name, city0.population as res__population
            FROM city city0
            ORDER BY res__population DESC
            LIMIT 20) subquery0
          ORDER BY subquery0.res__population ASC
          LIMIT 10
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)
        val expected = List(
          "Santafé de Bogotá",
          "Bangkok",
          "Chongqing",
          "Lima",
          "Teheran",
          "Cairo",
          "Delhi",
          "London",
          "Peking",
          "Tokyo"
        )

        assert(res == expected)
      }
    }
  }

}
