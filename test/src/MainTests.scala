package usql
import utest._
import ExprIntOps._

case class Country[T[_]](code: T[String],
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
                         code2: T[String])

object Country extends Table[Country]() {
  val code = Column[String]()

  val metadata = initMetadata()
}

case class City[T[_]](id: T[Int],
                      name: T[String],
                      countryCode: T[String],
                      district: T[String],
                      population: T[Int])

object City extends Table[City]() {
  val metadata = initMetadata()
}

case class CountryLanguage[T[_]](countryCode: T[String],
                                 language: T[String],
                                 isOfficial: T[Boolean],
                                 percentage: T[Double])

object CountryLanguage extends Table[CountryLanguage]() {
  val metadata = initMetadata()
}

object MainTests extends TestSuite {
  def camelToSnake(s: String) = {
    s.replaceAll("([A-Z])", "#$1").split('#').map(_.toLowerCase).mkString("_").stripPrefix("_")
  }

  def snakeToCamel(s: String) = {
    val out = new StringBuilder()
    val chunks = s.split("_", -1)
    for(i <- Range(0, chunks.length)){
      val chunk = chunks(i)
      if (i == 0) out.append(chunk)
      else{
        out.append(chunk(0).toUpper)
        out.append(chunk.drop(1))
      }
    }
    out.toString()
  }
  val db = new DatabaseApi(
    java.sql.DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", ""),
    tableNameMapper = camelToSnake,
    tableNameUnMapper = snakeToCamel,
    columnNameMapper = camelToSnake,
    columnNameUnMapper = snakeToCamel
  )
  db.runRaw(os.read(os.pwd / "test" / "resources" / "world.sql"))

  def tests = Tests {

    // From https://www.lihaoyi.com/post/WorkingwithDatabasesusingScalaandQuill.html
    test("city") {
      val query = City.query
      val sql = db.toSqlQuery(query)
      assert(
        sql ==
        """
        SELECT
          city0.id as res__id,
          city0.name as res__name,
          city0.country_code as res__country_code,
          city0.district as res__district,
          city0.population as res__population
        FROM city city0
        """.trim.replaceAll("\\s+", " ")
      )

      val res = db.run(query).take(5)


      val expected = Seq[City[Val]](
        City(
          id = 1,
          name = "Kabul",
          countryCode = "AFG",
          district = "Kabol",
          population = 1780000
        ),
        City(
          id = 2,
          name = "Qandahar",
          countryCode = "AFG",
          district = "Qandahar",
          population = 237500
        ),
        City(
          id = 3,
          name = "Herat",
          countryCode = "AFG",
          district = "Herat",
          population = 186800
        ),
        City(
          id = 4,
          name = "Mazar-e-Sharif",
          countryCode = "AFG",
          district = "Balkh",
          population = 127800
        ),
        City(
          id = 5,
          name = "Amsterdam",
          countryCode = "NLD",
          district = "Noord-Holland",
          population = 731200
        ),
      )

      assert(res == expected)
    }
    test("country") {
      val query = Country.query
      val sql = db.toSqlQuery(query)

      assert(
        sql ==
        """
        SELECT
          country0.code as res__code,
          country0.name as res__name,
          country0.continent as res__continent,
          country0.region as res__region,
          country0.surface_area as res__surface_area,
          country0.indep_year as res__indep_year,
          country0.population as res__population,
          country0.life_expectancy as res__life_expectancy,
          country0.gnp as res__gnp,
          country0.gnp_old as res__gnp_old,
          country0.local_name as res__local_name,
          country0.government_form as res__government_form,
          country0.head_of_state as res__head_of_state,
          country0.capital as res__capital,
          country0.code2 as res__code2
        FROM country country0
        """.trim.replaceAll("\\s+", " ")
      )

      val res = db.run(Country.query).take(2)
      val expected = Seq[Country[Val]](
        Country(
          code = "ABW",
          name = "Aruba",
          continent = "North America",
          region = "Caribbean",
          surfaceArea = 193,
          indepYear = None,
          population = 103000,
          lifeExpectancy = Some(78.4),
          gnp = Some(BigDecimal(828.0)),
          gnpOld = Some(BigDecimal(793.0)),
          localName = "Aruba",
          governmentForm = "Nonmetropolitan Territory of The Netherlands",
          headOfState = Some("Beatrix"),
          capital = Some(129),
          code2 = "AW"
        ),
        Country(
          code = "AFG",
          name = "Afghanistan",
          continent = "Asia",
          region = "Southern and Central Asia",
          surfaceArea = 652090,
          indepYear = Some(1919),
          population = 22720000,
          lifeExpectancy = Some(45.9),
          gnp = Some(BigDecimal(5976.0)),
          gnpOld = None,
          localName = "Afganistan/Afqanestan",
          governmentForm = "Islamic Emirate",
          headOfState = Some("Mohammad Omar"),
          capital = Some(1),
          code2 = "AF"
        )
      )

      assert(res == expected)
    }

    test("countryLanguage") {
      val query = CountryLanguage.query
      val sql = db.toSqlQuery(query)
      assert(
        sql ==
        """
        SELECT
          country_language0.country_code as res__country_code,
          country_language0.language as res__language,
          country_language0.is_official as res__is_official,
          country_language0.percentage as res__percentage
        FROM country_language country_language0
        """.trim.replaceAll("\\s+", " ")
      )

      val res = db.run(query).take(5)
      val expected = Seq[CountryLanguage[Val]](
        CountryLanguage(
          countryCode = "ABW",
          language = "Dutch",
          isOfficial = true,
          percentage = 5.3,
        ),
        CountryLanguage(
          countryCode = "ABW",
          language = "English",
          isOfficial = false,
          percentage = 9.5,
        ),
        CountryLanguage(
          countryCode = "ABW",
          language = "Papiamento",
          isOfficial = false,
          percentage = 76.7,
        ),
        CountryLanguage(
          countryCode = "ABW",
          language = "Spanish",
          isOfficial = false,
          percentage = 7.4,
        ),
        CountryLanguage(
          countryCode = "AFG",
          language = "Balochi",
          isOfficial = false,
          percentage = 0.9,
        )
      )

      assert(res == expected)
    }

    test("queryFilter") {
      test("singleName") {
        val query = City.query.filter(_.name === "Singapore")
        val sql = db.toSqlQuery(query)
        assert(
          sql ==
          """
          SELECT
            city0.id as res__id,
            city0.name as res__name,
            city0.country_code as res__country_code,
            city0.district as res__district,
            city0.population as res__population
          FROM city city0
          WHERE city0.name = ?
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)
        val expected = Seq[City[Val]](
          City(
            id = 3208,
            name = "Singapore",
            countryCode = "SGP",
            district = "�",
            population = 4017733,
          )
        )

        assert(res == expected)
      }

      test("singleId") {
        val query = City.query.filter(_.id === 3208)
        val sql = db.toSqlQuery(query)
        assert(
          sql ==
          """
          SELECT
            city0.id as res__id,
            city0.name as res__name,
            city0.country_code as res__country_code,
            city0.district as res__district,
            city0.population as res__population
          FROM city city0
          WHERE city0.id = ?
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)
        val expected = Seq[City[Val]](
          City(
            id = 3208,
            name = "Singapore",
            countryCode = "SGP",
            district = "�",
            population = 4017733,
          )
        )

        assert(res == expected)
      }

      test("singlePopulation") {
        val query = City.query.filter(_.population > 9000000)
        val sql = db.toSqlQuery(query)
        assert(
          sql ==
          """
          SELECT
            city0.id as res__id,
            city0.name as res__name,
            city0.country_code as res__country_code,
            city0.district as res__district,
            city0.population as res__population
          FROM city city0
          WHERE city0.population > ?
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query).take(5)
        val expected = Seq[City[Val]](
          City(
            id = 206,
            name = "S�o Paulo",
            countryCode = "BRA",
            district = "S�o Paulo",
            population = 9968485,
          ),
          City(
            id = 939,
            name = "Jakarta",
            countryCode = "IDN",
            district = "Jakarta Raya",
            population = 9604900,
          ),
          City(
            id = 1024,
            name = "Mumbai (Bombay)",
            countryCode = "IND",
            district = "Maharashtra",
            population = 10500000,
          ),
          City(
            id = 1890,
            name = "Shanghai",
            countryCode = "CHN",
            district = "Shanghai",
            population = 9696300,
          ),
          City(
            id = 2331,
            name = "Seoul",
            countryCode = "KOR",
            district = "Seoul",
            population = 9981619,
          )
        )
        assert(res == expected)
      }

      test("multiple") {
        val expected = Seq[City[Val]](
          City(
            id = 1890,
            name = "Shanghai",
            countryCode = "CHN",
            district = "Shanghai",
            population = 9696300,
          ),
          City(
            id = 1891,
            name = "Peking",
            countryCode = "CHN",
            district = "Peking",
            population = 7472000,
          ),
          City(
            id = 1892,
            name = "Chongqing",
            countryCode = "CHN",
            district = "Chongqing",
            population = 6351600,
          ),
          City(
            id = 1893,
            name = "Tianjin",
            countryCode = "CHN",
            district = "Tianjin",
            population = 5286800,
          )
        )

        test("combined") {
          val query = City.query.filter(c => c.population > 5000000 && c.countryCode === "CHN")
          val sql = db.toSqlQuery(query)
          assert(
            sql ==
            """
            SELECT
              city0.id as res__id,
              city0.name as res__name,
              city0.country_code as res__country_code,
              city0.district as res__district,
              city0.population as res__population
            FROM city city0
            WHERE
              city0.population > ?
              AND city0.country_code = ?
            """.trim.replaceAll("\\s+", " ")
          )

          val res = db.run(query).take(5)
          assert(res == expected)
        }

        test("separate") {
          val query = City.query.filter(_.population > 5000000).filter(_.countryCode === "CHN")
          val sql = db.toSqlQuery(query)
          assert(
            sql ==
              """
            SELECT
              city0.id as res__id,
              city0.name as res__name,
              city0.country_code as res__country_code,
              city0.district as res__district,
              city0.population as res__population
            FROM city city0
            WHERE
              city0.population > ?
              AND city0.country_code = ?
            """.trim.replaceAll("\\s+", " ")
          )

          val res = db.run(query).take(5)
          assert(res == expected)
        }
      }
    }

    test("lifting"){
      def find(cityId: Int) = db.run(City.query.filter(_.id === cityId))

      assert(find(3208) == List(City[Val](3208, "Singapore", "SGP", "�", 4017733)))
      assert(find(3209) == List(City[Val](3209, "Bratislava", "SVK", "Bratislava", 448292)))
    }

    test("mapping"){
      test("tuple2") {
        val query = Country.query.map(c => (c.name, c.continent))
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
          ("Aruba", "North America"),
          ("Afghanistan", "Asia"),
          ("Angola", "Africa"),
          ("Anguilla", "North America"),
          ("Albania", "Europe")
        )

        assert(res == expected)
      }

      test("tuple3"){
        val query = Country.query.map(c => (c.name, c.continent, c.population))
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
          ("Aruba", "North America", 103000),
          ("Afghanistan", "Asia", 22720000),
          ("Angola", "Africa", 12878000),
          ("Anguilla", "North America", 8000),
          ("Albania", "Europe", 3401200)
        )

        assert(res == expected)
      }

      test("interpolateInMap"){
        val query = Country.query.filter(_.name === "Singapore").map(c => c.population * 2)
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

      test("interpolateInMap2"){
        val query = Country.query.filter(_.name === "Singapore").map(c => (c.name, c.population * 2))
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

      test("heterogenousTuple"){
        val query = City.query.filter(_.name === "Singapore").map(c => (c, c.name, c.population * 2))
        val sql = db.toSqlQuery(query)
        assert(
          sql ==
          """
          SELECT
            city0.id as res__0__id,
            city0.name as res__0__name,
            city0.country_code as res__0__country_code,
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
            City[Val](
              id = 3208,
              name = "Singapore",
              countryCode = "SGP",
              district = "�",
              population = 4017733
            ),
            "Singapore",
            8035466
          )
        )
        assert(res == expected)
      }

      test("filterMap"){
        def findName(cityId: Int) = db.run(City.query.filter(_.id === cityId).map(_.name))

        assert(findName(3208) == List("Singapore"))
        assert(findName(3209) == List("Bratislava"))
      }
    }

    test("sortLimitOffset"){
      val query = City.query.sortBy(_.population).desc.drop(5).take(5).map(c => (c.name, c.population))
      val sql = db.toSqlQuery(query)

      assert(
        sql ==
        """
        SELECT
          city0.name as res__0,
          city0.population as res__1
        FROM city city0
        ORDER BY city0.population DESC
        LIMIT 5 OFFSET 5
        """.trim.replaceAll("\\s+", " ")
      )

      val res = db.run(query)
      val expected = Seq(
        ("Karachi", 9269265),
        ("Istanbul", 8787958),
        ("Ciudad de M�xico", 8591309),
        ("Moscow", 8389200),
        ("New York", 8008278)
      )
      assert(res == expected)
    }

    test("sortLimitOffset2"){
      val query = City.query.sortBy(_.population).asc.take(5).map(c => (c.name, c.population))
      val sql = db.toSqlQuery(query)
      assert(
        sql ==
        """
        SELECT
          city0.name as res__0,
          city0.population as res__1
        FROM city city0
        ORDER BY city0.population ASC
        LIMIT 5
        """.trim.replaceAll("\\s+", " ")
      )

      val res = db.run(query)
      val expected = Seq(
        ("Adamstown", 42),
        ("West Island", 167),
        ("Fakaofo", 300),
        ("Citt� del Vaticano", 455),
        ("Bantam", 503)
      )
      assert(res == expected)
    }

    test("joins"){
      test {
        val query = City.query
          .joinOn(Country.query)(_.countryCode === _.code)
          .filter { case (city, country) => country.name === "Aruba" }
        val sql = db.toSqlQuery(query)
        assert(
          sql ==
          """
          SELECT
            city0.id as res__0__id,
            city0.name as res__0__name,
            city0.country_code as res__0__country_code,
            city0.district as res__0__district,
            city0.population as res__0__population,
            country1.code as res__1__code,
            country1.name as res__1__name,
            country1.continent as res__1__continent,
            country1.region as res__1__region,
            country1.surface_area as res__1__surface_area,
            country1.indep_year as res__1__indep_year,
            country1.population as res__1__population,
            country1.life_expectancy as res__1__life_expectancy,
            country1.gnp as res__1__gnp,
            country1.gnp_old as res__1__gnp_old,
            country1.local_name as res__1__local_name,
            country1.government_form as res__1__government_form,
            country1.head_of_state as res__1__head_of_state,
            country1.capital as res__1__capital,
            country1.code2 as res__1__code2
          FROM
            city city0
            JOIN country country1 ON city0.country_code = country1.code
          WHERE
            country1.name = ?
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)

        val expected = Seq[(City[Val], Country[Val])](
          (
            City(
              id = 129,
              name = "Oranjestad",
              countryCode = "ABW",
              district = "�",
              population = 29034
            ),
            Country(
              code = "ABW",
              name = "Aruba",
              continent = "North America",
              region = "Caribbean",
              surfaceArea = 193,
              indepYear = None,
              population = 103000,
              lifeExpectancy = Some(78.4),
              gnp = Some(BigDecimal(828.0)),
              gnpOld = Some(BigDecimal(793.0)),
              localName = "Aruba",
              governmentForm = "Nonmetropolitan Territory of The Netherlands",
              headOfState = Some("Beatrix"),
              capital = Some(129),
              code2 = "AW"
            )
          )
        )

        assert(res == expected)
      }

      test{
        val query = City.query
          .joinOn(Country.query)(_.countryCode === _.code)
          .filter { case (city, country) => country.name === "Malaysia" }
          .map { case (city, country) => (city.name, country.name) }

        val sql = db.toSqlQuery(query)
        assert(
          sql ==
          """
          SELECT
            city0.name as res__0,
            country1.name as res__1
          FROM city city0
          JOIN country country1 ON city0.country_code = country1.code
          WHERE country1.name = ?
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)

        val expected = Seq(
          ("Kuala Lumpur", "Malaysia"),
          ("Ipoh", "Malaysia"),
          ("Johor Baharu", "Malaysia"),
          ("Petaling Jaya", "Malaysia"),
          ("Kelang", "Malaysia"),
          ("Kuala Terengganu", "Malaysia"),
          ("Pinang", "Malaysia"),
          ("Kota Bharu", "Malaysia"),
          ("Kuantan", "Malaysia"),
          ("Taiping", "Malaysia"),
          ("Seremban", "Malaysia"),
          ("Kuching", "Malaysia"),
          ("Sibu", "Malaysia"),
          ("Sandakan", "Malaysia"),
          ("Alor Setar", "Malaysia"),
          ("Selayang Baru", "Malaysia"),
          ("Sungai Petani", "Malaysia"),
          ("Shah Alam", "Malaysia")
        )

        assert(res == expected)
      }
    }
    test("flatMap"){
      test {
        val query = City.query
          .flatMap(city => Country.query.map(country => (city.countryCode, country.code, country.name, city.name)))
          .filter { case (cityCountryCode, countryCode, countryName, cityName) =>
            cityCountryCode === countryCode && countryName === "Aruba"
          }
          .map { case (cityCountryCode, countryCode, countryName, cityName) => (cityName, countryCode) }

        val sql = db.toSqlQuery(query)

        assert(
          sql ==
          """
          SELECT
            city0.name as res__0,
            country1.code as res__1
          FROM
            city city0,
            country country1
          WHERE city0.country_code = country1.code
          AND country1.name = ?
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)

        val expected = Seq(
          ("Oranjestad", "ABW"),
        )

        assert(res == expected)
      }

      test{
        val query = City.query
          .flatMap(city => Country.query.map(country => (city.countryCode, country.code, country.name, city.name)))
          .filter { case (cityCountryCode, countryCode, countryName, cityName) =>
            cityCountryCode === countryCode && countryName === "Malaysia"
          }
          .map { case (cityCountryCode, countryCode, countryName, cityName) => (countryCode, cityName) }

        val sql = db.toSqlQuery(query)
        assert(
          sql ==
          """
          SELECT
            country1.code as res__0,
            city0.name as res__1
          FROM city city0, country country1
          WHERE city0.country_code = country1.code
          AND country1.name = ?
          """.trim.replaceAll("\\s+", " ")
        )

        val res = db.run(query)

        val expected = Seq(
          ("MYS", "Kuala Lumpur"),
          ("MYS", "Ipoh"),
          ("MYS", "Johor Baharu"),
          ("MYS", "Petaling Jaya"),
          ("MYS", "Kelang"),
          ("MYS", "Kuala Terengganu"),
          ("MYS", "Pinang"),
          ("MYS", "Kota Bharu"),
          ("MYS", "Kuantan"),
          ("MYS", "Taiping"),
          ("MYS", "Seremban"),
          ("MYS", "Kuching"),
          ("MYS", "Sibu"),
          ("MYS", "Sandakan"),
          ("MYS", "Alor Setar"),
          ("MYS", "Selayang Baru"),
          ("MYS", "Sungai Petani"),
          ("MYS", "Shah Alam")
        )

        assert(res == expected)
      }
    }

    test("subquery") {
      val query = CountryLanguage.query
        .joinOn(Country.query.sortBy(_.population).desc.take(2))(_.countryCode === _.code)
        .map { case (language, country) => (language.language, country.name) }

      val sql = db.toSqlQuery(query)
      assert(
        sql ==
        """
        SELECT
          country_language0.language as res__0,
          subquery1.res__name as res__1
        FROM
          country_language country_language0
        JOIN (SELECT
            country0.code as res__code,
            country0.name as res__name,
            country0.continent as res__continent,
            country0.region as res__region,
            country0.surface_area as res__surface_area,
            country0.indep_year as res__indep_year,
            country0.population as res__population,
            country0.life_expectancy as res__life_expectancy,
            country0.gnp as res__gnp,
            country0.gnp_old as res__gnp_old,
            country0.local_name as res__local_name,
            country0.government_form as res__government_form,
            country0.head_of_state as res__head_of_state,
            country0.capital as res__capital,
            country0.code2 as res__code2
          FROM
            country country0
          ORDER BY
            country0.population DESC
          LIMIT
            2) subquery1
        ON country_language0.country_code = subquery1.res__code
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
  }
}
