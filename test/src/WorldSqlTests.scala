package scalasql
import utest._
import dialects.H2Dialect._
import scalasql.dialects.H2Dialect
import scalasql.query.Expr


object WorldSqlTests extends TestSuite {
    
  case class Country[+T[_]](
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

  case class City[+T[_]](
      id: T[Int],
      name: T[String],
      countryCode: T[String],
      district: T[String],
      population: T[Int]
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


  def tests = Tests {
    val dbClient = new DatabaseClient(
      java.sql.DriverManager.getConnection("jdbc:h2:mem:testdb" + scala.util.Random.nextInt(), "sa", ""),
      new Config {
        override def columnNameMapper(v: String) = v.toLowerCase()

        override def tableNameMapper(v: String) = v.toLowerCase()
      },
      H2Dialect
    )
    dbClient.autoCommit.runRawUpdate(os.read(os.pwd / "test" / "resources" / "world.sql"))

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
              district = "",
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
              district = "",
              population = 4017733
            )
          )

          assert(res == expected)
        }
      }

      test("population") {
        dbClient.transaction { implicit db =>
          val query = City.select.filter(_.population > 9980000)
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
              id = 1024,
              name = "Mumbai (Bombay)",
              countryCode = "IND",
              district = "Maharashtra",
              population = 10500000
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
      test("implicit") {

        def find(cityId: Int)(implicit db: scalasql.DbApi) = db.run(City.select.filter(_.id === cityId))
        dbClient.transaction { implicit db =>
          assert(find(3208) == List(City[Id](3208, "Singapore", "SGP", "", 4017733)))
          assert(find(3209) == List(City[Id](3209, "Bratislava", "SVK", "Bratislava", 448292)))
        }
      }

      test("explicit") {

        def find(cityId: Int)(implicit db: scalasql.DbApi) = db.run(City.select.filter(_.id === Expr(cityId)))
        dbClient.transaction { implicit db =>
          assert(find(3208) == List(City[Id](3208, "Singapore", "SGP", "", 4017733)))
          assert(find(3209) == List(City[Id](3209, "Bratislava", "SVK", "Bratislava", 448292)))
        }
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

      test("heterogenousTuple") {
        dbClient.transaction { implicit db =>
          val query = City.select
            .filter(_.name === "Singapore")
            .map(c => (c, c.name, c.population / 1000000))

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
              city0.population / ? as res__2
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
                district = "",
                population = 4017733
              ),
              "Singapore",
              4 // population in millions
            )
          )

          assert(res == expected)
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
            .joinOn(Country.select.sortBy(_.population).desc.take(2))(_.countryCode === _.code)
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
      }

      test("sortLimitSortLimit") {
        dbClient.transaction { implicit db =>
          val query = City.select
            .sortBy(_.population).desc
            .take(20)
            .sortBy(_.population).asc
            .take(10)
            .map(_.name)

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

    test("insert"){
      test("values") {
        dbClient.transaction { implicit db =>
          val query = City.insert.values(
            // ID provided by database AUTO_INCREMENT
            _.name -> "Sentosa",
            _.countryCode -> "SGP",
            _.district -> "South",
            _.population -> 1337
          )
          val sql = db.toSqlQuery(query)
          assert(sql == "INSERT INTO city (name, countrycode, district, population) VALUES (?, ?, ?, ?)")

          db.run(query)

          val query2 = City.select.filter(_.countryCode === "SGP")
          val res = db.run(query2)

          val expected = Seq(
            City[Id](
              id = 3208,
              name = "Singapore",
              countryCode = "SGP",
              district = "",
              population = 4017733
            ),
            City[Id](
              id = 4080,
              name = "Sentosa",
              countryCode = "SGP",
              district = "South",
              population = 1337
            ),
          )

          assert(res == expected)
        }
      }

      test("values") {
        dbClient.transaction { implicit db =>
          val query = City.insert.batched(_.name, _.countryCode, _.district, _.population)(
            // ID provided by database AUTO_INCREMENT
            ("Sentosa", "SGP", "South", 1337),
            ("Loyang", "SGP", "East", 31337),
            ("Jurong", "SGP", "West", 313373),
          )
          val sql = db.toSqlQuery(query)
          assert(
            sql ==
            """
            INSERT INTO city (name, countrycode, district, population) VALUES
            (?, ?, ?, ?),
            (?, ?, ?, ?),
            (?, ?, ?, ?)
            """.trim.replaceAll("\\s+", " ")
          )

          db.run(query)

          val query2 = City.select.filter(_.countryCode === "SGP")
          val res = db.run(query2)

          val expected = Seq(
            City[Id](
              id = 3208,
              name = "Singapore",
              countryCode = "SGP",
              district = "",
              population = 4017733
            ),
            City[Id](
              id = 4080,
              name = "Sentosa",
              countryCode = "SGP",
              district = "South",
              population = 1337
            ),
            City[Id](
              id = 4081,
              name = "Loyang",
              countryCode = "SGP",
              district = "East",
              population = 31337
            ),
            City[Id](
              id = 4082,
              name = "Jurong",
              countryCode = "SGP",
              district = "West",
              population = 313373
            ),
          )

          assert(res == expected)
        }
      }

      test("select") {
        dbClient.transaction { implicit db =>
          val query = City.insert.select(
            c => (c.name, c.countryCode, c.district, c.population),
            City.select.filter(_.name === "Singapore")
              .map(c => (Expr("New-") + c.name, c.countryCode, c.district, Expr(0)))
          )

          val sql = db.toSqlQuery(query)
          assert(
            sql ==
            """
            INSERT INTO city (name, countrycode, district, population)
            SELECT ? || city0.name as res__0, city0.countrycode as res__1, city0.district as res__2, ? as res__3
            FROM city city0 WHERE city0.name = ?
            """.trim.replaceAll("\\s+", " "))

          db.run(query)

          val query2 = City.select.filter(_.countryCode === "SGP")
          val res = db.run(query2)

          val expected = Seq(
            City[Id](
              id = 3208,
              name = "Singapore",
              countryCode = "SGP",
              district = "",
              population = 4017733
            ),
            City[Id](
              id = 4080,
              name = "New-Singapore",
              countryCode = "SGP",
              district = "",
              population = 0
            ),
          )

          assert(res == expected)
        }
      }
    }

    test("update") {
      dbClient.transaction { implicit db =>
        val query = City.update(_.countryCode === "SGP").set(c => c.population -> (c.population + 1000000))
        val sql = db.toSqlQuery(query)
        assert(sql == "UPDATE city SET population = city.population + ? WHERE city.countrycode = ?")

        db.run(query)

        val query2 = City.select.filter(_.countryCode === "SGP")
        val res = db.run(query2)

        val expected = Seq(
          City[Id](
            id = 3208,
            name = "Singapore",
            countryCode = "SGP",
            district = "",
            population = 5017733
          )
        )

        assert(res == expected)
      }
    }

    test("delete") {
      dbClient.transaction { implicit db =>
        val query = City.delete(_.countryCode === "SGP")
        val sql = db.toSqlQuery(query)
        assert(sql == "DELETE FROM city WHERE city.countrycode = ?")

        db.run(query)

        val query2 = City.select.filter(_.countryCode === "SGP")
        val res = db.run(query2)

        val expected = Seq()

        assert(res == expected)
      }
    }

    test("transactions") {
      test("exception") {
        try dbClient.transaction { implicit db =>
          db.run(City.delete(_.countryCode === "SGP"))

          val res = db.run(City.select.filter(_.countryCode === "SGP"))

          assert(res == Seq())
          throw new Exception()
        } catch{case e: Exception => /*do nothing*/}

        dbClient.transaction{implicit db =>
          val res = db.run(City.select.filter(_.countryCode === "SGP"))

          val expected = Seq(
            City[Id](
              id = 3208,
              name = "Singapore",
              countryCode = "SGP",
              district = "",
              population = 4017733
            )
          )

          assert(res == expected)
        }
      }
      test("rollback") {
        dbClient.transaction { implicit db =>
          db.run(City.delete(_.countryCode === "SGP"))

          val res = db.run(City.select.filter(_.countryCode === "SGP"))
          assert(res == Seq())
          db.rollback()
        }

        dbClient.transaction{ implicit db =>
          val res = db.run(City.select.filter(_.countryCode === "SGP"))

          val expected = Seq(
            City[Id](
              id = 3208,
              name = "Singapore",
              countryCode = "SGP",
              district = "",
              population = 4017733
            )
          )

          assert(res == expected)
        }
      }
    }
    test("savepoint") {
      test("exception") {
        dbClient.transaction { implicit db =>
          try db.savepoint { implicit sp =>
            db.run(City.delete(_.countryCode === "SGP"))

            val res = db.run(City.select.filter(_.countryCode === "SGP"))
            assert(res == Seq())
            throw new Exception()
          } catch{case e: Exception => /*do nothing*/}

          val res = db.run(City.select.filter(_.countryCode === "SGP"))

          val expected = Seq(
            City[Id](
              id = 3208,
              name = "Singapore",
              countryCode = "SGP",
              district = "",
              population = 4017733
            )
          )

          assert(res == expected)
        }

      }
      test("rollback") {
        dbClient.transaction { implicit db =>
          db.savepoint { implicit sp =>
            db.run(City.delete(_.countryCode === "SGP"))

            val res = db.run(City.select.filter(_.countryCode === "SGP"))

            assert(res == Seq())
            sp.rollback()
          }

          val res = db.run(City.select.filter(_.countryCode === "SGP"))

          val expected = Seq(
            City[Id](
              id = 3208,
              name = "Singapore",
              countryCode = "SGP",
              district = "",
              population = 4017733
            )
          )

          assert(res == expected)
        }
      }
    }
  }
}
