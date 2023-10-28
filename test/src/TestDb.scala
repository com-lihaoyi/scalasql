package scalasql
import com.github.vertical_blank.sqlformatter.SqlFormatter
import org.testcontainers.containers.{MySQLContainer, PostgreSQLContainer}
import pprint.PPrinter
import scalasql.dialects.DialectConfig
import scalasql.query.{Expr, SubqueryRef}

import java.sql.Connection


class TestDb(
    connection: Connection,
    testSchemaFileName: String,
    testDataFileName: String,
    dialectConfig: DialectConfig
) {

  val db = new DatabaseApi(
    connection,
    dialectConfig = dialectConfig,
    config = TestDb.Config,
  )

  def reset() = {
    db.runRaw(os.read(os.pwd / "test" / "resources" / testSchemaFileName))
    db.runRaw(os.read(os.pwd / "test" / "resources" / testDataFileName))
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
      val sqlResult = db.toSqlQuery(query).stripSuffix(dialectConfig.defaultQueryableSuffix)
      val expectedSql = sql.trim.replaceAll("\\s+", " ")
      assert(sqlResult == expectedSql, pprint.apply(SqlFormatter.format(sqlResult)))
    }
    if (sqls.nonEmpty) {
      val sqlResult = db.toSqlQuery(query).stripSuffix(dialectConfig.defaultQueryableSuffix)
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
  object Config extends Config
  lazy val pg = {
    println("Initializing Postgres")
    val pg: PostgreSQLContainer[_] = new PostgreSQLContainer("postgres:15-alpine")
    pg.start()
    pg
  }

  lazy val mysql = {
    println("Initializing MySql")
    val mysql: MySQLContainer[_] = new MySQLContainer("mysql:8.0.31")
      .withCommand("mysqld", "--character-set-server=utf8mb4", "--collation-server=utf8mb4_bin")
    mysql.start()
    mysql
  }

  lazy val pprinter: PPrinter = PPrinter.Color.copy(additionalHandlers = {
    case v: SubqueryRef[_, _] => pprinter.treeify(v.value, false, true)
    case v: Expr[_] if !v.isInstanceOf[scala.Product] =>
      pprinter.treeify(Expr.getToString(v), false, true)
  })
}
