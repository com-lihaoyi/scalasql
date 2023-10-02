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
      val res = db.run(City.query).take(5)

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
      val res = db.run(CountryLanguage.query).take(5)
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
        val res = db.run(City.query.filter(_.name === "Singapore"))
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
        val res = db.run(City.query.filter(_.id === 3208))
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
        val res = db.run(City.query.filter(_.population > 9000000)).take(5)
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
          val res = db.run(City.query.filter(c => c.population > 5000000 && c.countryCode === "CHN")).take(5)
          assert(res == expected)
        }

        test("separate") {
          val res = db.run(City.query.filter(_.population > 5000000).filter(_.countryCode === "CHN")).take(5)
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
        val res = db.run(Country.query.map(c => (c.name, c.continent))).take(5)
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
        val res = db.run(Country.query.map(c => (c.name, c.continent, c.population))).take(5)
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
        val res = db.run(Country.query.filter(_.name === "Singapore").map(c => c.population * 2))
        val expected = Seq(7134000)
        assert(res == expected)
      }

      test("interpolateInMap2"){
        val res = db.run(Country.query.filter(_.name === "Singapore").map(c => (c.name, c.population * 2)))
        val expected = Seq(("Singapore", 7134000))
        assert(res == expected)
      }

      test("heterogenousTuple"){
        val res = db.run(City.query.filter(_.name === "Singapore").map(c => (c, c.name, c.population * 2)))
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

    test("joins"){
      test {
        val res = db.run(
          City.query
            .join(Country.query)(_.countryCode === _.code)
            .filter { case (city, country) => country.name === "Aruba" }
        )

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
        val res = db.run(
          City.query
            .join(Country.query)(_.countryCode === _.code)
            .filter { case (city, country) => country.name === "Malaysia" }
            .map{case (city, country) => (city.name, country.name)}
        )

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
  }
}
