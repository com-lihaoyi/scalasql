package scalasql
import com.github.vertical_blank.sqlformatter.SqlFormatter
import org.testcontainers.containers.{MySQLContainer, PostgreSQLContainer}
import pprint.PPrinter
import scalasql.query.{Expr, SubqueryRef}
import scalasql.renderer.SqlStr
import utest.TestSuite

import java.sql.{Connection, DriverManager}


class TestDb(connection: Connection,
             testDataFileName: String,
             defaultQueryableSuffix: String,
             castParams: Boolean) {

  val db = new DatabaseApi(
    connection,
    tableNameMapper = camelToSnake,
    tableNameUnMapper = snakeToCamel,
    columnNameMapper = camelToSnake,
    columnNameUnMapper = snakeToCamel,
    defaultQueryableSuffix = defaultQueryableSuffix,
    castParams = castParams
  )

  def reset() = {
    db.runRaw(os.read(os.pwd / "test" / "resources" / testDataFileName))
  }

  def camelToSnake(s: String) = {
    s.replaceAll("([A-Z])", "#$1").split('#').map(_.toLowerCase).mkString("_").stripPrefix("_")
  }

  def snakeToCamel(s: String) = {
    val out = new StringBuilder()
    val chunks = s.split("_", -1)
    for (i <- Range(0, chunks.length)) {
      val chunk = chunks(i)
      if (i == 0) out.append(chunk)
      else {
        out.append(chunk(0).toUpper)
        out.append(chunk.drop(1))
      }
    }
    out.toString()
  }

  def apply[T, V](
      query: T,
      sql: String = null,
      sqls: Seq[String] = Nil,
      value: V = null,
      moreValues: Seq[V] = Nil,
      normalize: V => V = (x: V) => x
  )(implicit qr: Queryable[T, V]) = {
    if (sql != null) {
      val sqlResult = db.toSqlQuery(query).stripSuffix(defaultQueryableSuffix)
      val expectedSql = sql.trim.replaceAll("\\s+", " ")
//      pprint.log(sqlResult)
//      pprint.log(expectedSql)
      assert(sqlResult == expectedSql, pprint.apply(SqlFormatter.format(sqlResult)))
    }
    if (sqls.nonEmpty) {
      val sqlResult = db.toSqlQuery(query).stripSuffix(defaultQueryableSuffix)
//      pprint.log(sqlResult)
//      pprint.log(sqls.map(_.trim.replaceAll("\\s+", " ")))
      assert(
        sqls.exists(_.trim.replaceAll("\\s+", " ") == sqlResult),
        pprint.apply(SqlFormatter.format(sqlResult))
      )

    }

    val result = db.run(query)

    val values = Option(value) ++ moreValues
    val normalized = normalize(result)
    assert(values.exists(value => normalized == value), pprint.apply(normalized))
  }
}

object TestDb {
  lazy val pg = {
    println("Initializing Postgres")
    val pg: PostgreSQLContainer[_] = new PostgreSQLContainer("postgres:15-alpine")
    pg.start()
    pg
  }

  lazy val mysql = {
    println("Initializing MySql")
    val mysql: MySQLContainer[_] = new MySQLContainer("mysql:8.0.31")
      .withCommand(
        "mysqld",
        "--character-set-server=utf8mb4",
        "--collation-server=utf8mb4_bin"
      )
    mysql.start()
    mysql
  }

  lazy val pprinter: PPrinter = PPrinter.Color.copy(
    additionalHandlers = {
      case v: Val[_] => pprinter.treeify(v.apply(), false, true)
      case v: SubqueryRef[_, _] => pprinter.treeify(v.value, false, true)
      case v: Expr[_] if !v.isInstanceOf[scala.Product] =>
        pprinter.treeify(v.exprToString, false, true)
    }
  )
}
