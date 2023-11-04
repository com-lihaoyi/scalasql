package scalasql
import utest._
import dialects.H2Dialect._
import scalasql.dialects.H2Dialect
import scalasql.query.Expr


object WorldSqlTests extends TestSuite {
    
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

  val dbClient = new DatabaseClient(
    java.sql.DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", ""),
    new Config{
      override def columnNameMapper(v: String) = v.toLowerCase()
      override def tableNameMapper(v: String) = v.toLowerCase()
    },
    H2Dialect
  )
  dbClient.autoCommit.runRawUpdate(os.read(os.pwd / "test" / "resources" / "world.sql"))

  def tests = Tests {

    // From https://www.lihaoyi.com/post/WorkingwithDatabasesusingScalaandQuill.html
    test("expr") {
      dbClient.transaction { implicit db =>
        val query = Expr(1) + Expr(3)
        val sql = db.toSqlQuery(query)
        assert(sql == """SELECT ? + ? as res""")

        val res = db.run(query)
        val expected = 4
        assert(res == expected)
      }
    }

    test("city") {
      dbClient.transaction { implicit db =>
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

        val res = db.run(query).take(3)

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
          )
        )

        assert(res == expected)
      }
    }

    test("queryFilter") {
      test("singleName") {
        dbClient.transaction { implicit db =>
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
      }

      test("singleId") {
        dbClient.transaction { implicit db =>
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
      }

      test("singlePopulation") {
        dbClient.transaction { implicit db =>
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
      }

      test("multiple") {
        test("combined") {
          dbClient.transaction { implicit db =>
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

            val res = db.run(query).take(2)
            val expected = Seq(
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
              )
            )
            assert(res == expected)
          }
        }

        test("separate") {
          dbClient.transaction { implicit db =>
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

            val res = db.run(query).take(2)
            val expected = Seq(
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
              )
            )
            assert(res == expected)
          }
        }
      }
    }

    test("lifting") {

      def find(cityId: Int)(implicit db: scalasql.DbApi) = db.run(City.select.filter(_.id === cityId))
      dbClient.transaction { implicit db =>
        assert(find(3208) == List(City[Id](3208, "Singapore", "SGP", "", 4017733)))
        assert(find(3209) == List(City[Id](3209, "Bratislava", "SVK", "Bratislava", 448292)))
      }
    }

    test("mapping") {
      test("tuple2") {
        dbClient.transaction { implicit db =>
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
      }

      test("tuple3") {
        dbClient.transaction { implicit db =>
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
      }

      test("interpolateInMap") {
        dbClient.transaction { implicit db =>
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
      }

      test("interpolateInMap2") {
        dbClient.transaction { implicit db =>
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
      }

      test("heterogenousTuple") {
        dbClient.transaction { implicit db =>
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
      }

      test("filterMap") {

        def findName(cityId: Int)(implicit db: DbApi) = db.run(City.select.filter(_.id === cityId).map(_.name))

        dbClient.transaction { implicit db =>
          assert(findName(3208) == List("Singapore"))
          assert(findName(3209) == List("Bratislava"))
        }
      }
    }

    test("aggregate") {
      test("sum") {
        dbClient.transaction { implicit db =>
          val query = City.select.map(_.population).sum
          val sql = db.toSqlQuery(query)

          assert(sql == """SELECT SUM(city0.population) as res FROM city city0""")

          val res = db.run(query)
          val expected = 1429559884
          assert(res == expected)
        }
      }
      test("sumBy") {
        dbClient.transaction { implicit db =>
          val query = City.select.sumBy(_.population)
          val sql = db.toSqlQuery(query)

          assert(sql == """SELECT SUM(city0.population) as res FROM city city0""")

          val res = db.run(query)
          val expected = 1429559884
          assert(res == expected)
        }
      }
      test("count") {
        dbClient.transaction { implicit db =>
          val query = Country.select.size
          val sql = db.toSqlQuery(query)

          assert(sql == """SELECT COUNT(1) as res FROM country country0""")

          val res = db.run(query)
          val expected = 239
          assert(res == expected)
        }
      }
      test("min") {
        dbClient.transaction { implicit db =>
          val query = City.select.map(_.population).min
          val sql = db.toSqlQuery(query)

          assert(sql == """SELECT MIN(city0.population) as res FROM city city0""")

          val res = db.run(query)
          val expected = 42
          assert(res == expected)
        }
      }
      test("minBy") {
        dbClient.transaction { implicit db =>
          val query = City.select.minBy(_.population)
          val sql = db.toSqlQuery(query)

          assert(sql == """SELECT MIN(city0.population) as res FROM city city0""")

          val res = db.run(query)
          val expected = 42
          assert(res == expected)
        }
      }
      test("max") {
        dbClient.transaction { implicit db =>
          val query = City.select.map(_.population).max
          val sql = db.toSqlQuery(query)

          assert(sql == """SELECT MAX(city0.population) as res FROM city city0""")

          val res = db.run(query)
          val expected = 10500000
          assert(res == expected)
        }
      }
      test("maxBy") {
        dbClient.transaction { implicit db =>
          val query = City.select.maxBy(_.population)
          val sql = db.toSqlQuery(query)

          assert(sql == """SELECT MAX(city0.population) as res FROM city city0""")

          val res = db.run(query)
          val expected = 10500000
          assert(res == expected)
        }
      }
      test("avg") {
        dbClient.transaction { implicit db =>
          val query = City.select.map(_.population).avg
          val sql = db.toSqlQuery(query)

          assert(sql == """SELECT AVG(city0.population) as res FROM city city0""")

          val res = db.run(query)
          val expected = 350468
          assert(res == expected)
        }
      }
      test("avgBy") {
        dbClient.transaction { implicit db =>
          val query = City.select.avgBy(_.population)
          val sql = db.toSqlQuery(query)

          assert(sql == """SELECT AVG(city0.population) as res FROM city city0""")

          val res = db.run(query)
          val expected = 350468
          assert(res == expected)
        }
      }
    }

    test("sortLimitOffset") {
      dbClient.transaction { implicit db =>
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
    }

    test("joins") {
      dbClient.transaction { implicit db =>
        val query = City.select
          .joinOn(Country)(_.countryCode === _.code)
          .filter { case (city, country) => country.name === "Liechtenstein" }
          .map { case (city, country) => city.name }

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
    }

    test("flatMap") {
      dbClient.transaction { implicit db =>
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
        dbClient.transaction { implicit db =>
          val query = CountryLanguage.select
            .joinOn(Country.select.sortBy(_.population).asc.take(2))(_.countryCode === _.code)
            .map { case (language, country) => (language.language, country.name) }

          val sql = db.toSqlQuery(query)
          assert(
            sql ==
              """
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
          """.trim.replaceAll("\\s+", " ")
          )

          val res = db.run(query)
          val expected = Seq(
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
      }
      test("from") {
        dbClient.transaction { implicit db =>
          val query = Country.select.sortBy(_.population).asc.take(2)
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
      }

      test("sortLimitSortLimit") {
        dbClient.transaction { implicit db =>
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

}
