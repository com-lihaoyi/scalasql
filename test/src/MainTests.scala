package usql
import utest._
import ExprIntOps._

import Types.Id

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

  def * = Country(code, name, continent, region, surface_area, indep_year)

  implicit def valueReader: upickle.default.Reader[Country[Id]] = upickle.default.macroR
  implicit def queryWriter: upickle.default.Writer[Country[Atomic]] = upickle.default.macroW
  implicit def queryWriter2: upickle.default.Writer[Country[Column]] = upickle.default.macroW
}

object MainTests extends TestSuite {
  Class.forName("org.h2.Driver")
  val db = new DatabaseApi(java.sql.DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", ""))
  db.runRaw(os.read(os.pwd / "test" / "resources" / "world.sql"))

  def tests = Tests {
    test("filter") {
      val query = Query(Country.*).filter(c => c.indep_year === 1965)
      val res = db.run(query)
      val expected = Seq(
        Country[Id](
          code = "GMB",
          name = "Gambia",
          continent = "Africa",
          region = "Western Africa",
          surface_area = 11295,
          indep_year = 1965
        ),
        Country[Id](
          code = "MDV",
          name = "Maldives",
          continent = "Asia",
          region = "Southern and Central Asia",
          surface_area = 298,
          indep_year = 1965
        ),
        Country[Id](
          code = "SGP",
          name = "Singapore",
          continent = "Asia",
          region = "Southeast Asia",
          surface_area = 618,
          indep_year = 1965
        )
      )

      assert(res == expected)
    }
    test("map") {
      val query = Query(Country.*).filter(c => c.indep_year === 1965).map(c => c.copy(surface_area = c.surface_area * 0))
      val res = db.run(query)
      val expected = Seq(
        Country[Id](
          code = "GMB",
          name = "Gambia",
          continent = "Africa",
          region = "Western Africa",
          surface_area = 0,
          indep_year = 1965
        ),
        Country[Id](
          code = "MDV",
          name = "Maldives",
          continent = "Asia",
          region = "Southern and Central Asia",
          surface_area = 0,
          indep_year = 1965
        ),
        Country[Id](
          code = "SGP",
          name = "Singapore",
          continent = "Asia",
          region = "Southeast Asia",
          surface_area = 0,
          indep_year = 1965
        )
      )

      assert(res == expected)
    }
    test("primitive") {
      val query = Query(Country.*).filter(c => c.indep_year === 1965).map(_.name)
      val res = db.run(query)
      val expected = Seq[String]("Gambia", "Maldives", "Singapore")
      assert(res == expected)
    }
  }
}
