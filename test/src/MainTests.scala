package usql
import utest._
import ExprIntOps._

case class Country[T[_]](code: T[String],
                         name: T[String],
                         continent: T[String],
                         region: T[String],
                         surface_area: T[Int],
                         indep_year: T[Int])

object Country extends Table[Country]() {
  val code = Column[String]()
  val name = Column[String]()
  val continent = Column[String]()
  val region = Column[String]()
  val surface_area = Column[Int]()
  val indep_year = Column[Int]()

  def * = Query(Country(code, name, continent, region, surface_area, indep_year))

  implicit def valueReader: upickle.default.Reader[Country[Val]] = upickle.default.macroR
  implicit def queryWriter: upickle.default.Writer[Country[Atomic]] = upickle.default.macroW
  implicit def queryWriter2: upickle.default.Writer[Country[Column]] = upickle.default.macroW
}

object MainTests extends TestSuite {

  val db = new DatabaseApi(java.sql.DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", ""))
  db.runRaw(os.read(os.pwd / "test" / "resources" / "world.sql"))

  val expected0 = Seq[Country[Val]](
    Country(
      code = "GMB",
      name = "Gambia",
      continent = "Africa",
      region = "Western Africa",
      surface_area = 11295,
      indep_year = 1965
    ),
    Country(
      code = "MDV",
      name = "Maldives",
      continent = "Asia",
      region = "Southern and Central Asia",
      surface_area = 298,
      indep_year = 1965
    ),
    Country(
      code = "SGP",
      name = "Singapore",
      continent = "Asia",
      region = "Southeast Asia",
      surface_area = 618,
      indep_year = 1965
    )
  )

  def tests = Tests {
    test("filter") {
      val res = db.run(Country.*.filter(c => c.indep_year === 1965))
      val expected = expected0
      assert(res == expected)
    }

    test("map") {
      val res = db.run(
        Country.*
          .filter(c => c.indep_year === 1965)
          .map(c => c.copy(surface_area = c.surface_area * 2))
      )

      val expected = expected0.map(c => c.copy[Val](surface_area = c.surface_area() * 2))
      assert(res == expected)
    }

    test("primitive") {
      val res = db.run(
        Country.*
          .filter(c => c.indep_year === 1965)
          .map(_.name)
      )

      val expected = expected0.map(_.name)
      assert(res == expected)
    }
  }
}
