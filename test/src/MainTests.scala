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

  implicit def rw: upickle.default.ReadWriter[Country[Id]] = upickle.default.macroRW
}

object MainTests extends TestSuite {
  Class.forName("org.h2.Driver")
  val db = new DatabaseApi(java.sql.DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", ""))
  db.runRaw(os.read(os.pwd / "test" / "resources" / "world.sql"))

  def tests = Tests {
    test("simple") {
      val query = Query(Country.*).filter(c => c.indep_year === 1965)
      pprint.log(query.toSqlQuery)
      val res = db.run(query)
      pprint.log(res)
    }
  }
}
