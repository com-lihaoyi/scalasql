package scalasql.utils

import com.github.vertical_blank.sqlformatter.SqlFormatter
import org.testcontainers.containers.{MySQLContainer, PostgreSQLContainer}
import pprint.PPrinter
import scalasql.dialects.DialectConfig
import scalasql.query.{Expr, SubqueryRef}
import scalasql.{Config, DatabaseClient, Queryable}

import java.sql.Connection

class TestDb(
    val dbClient: DatabaseClient,
    testSchemaFileName: String,
    testDataFileName: String,
    dialectConfig: DialectConfig
) {

  def reset() = {
    dbClient.autoCommit.runRawUpdate(os.read(os.pwd / "test" / "resources" / testSchemaFileName))
    dbClient.autoCommit.runRawUpdate(os.read(os.pwd / "test" / "resources" / testDataFileName))
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
      val sqlResult = dbClient.autoCommit.toSqlQuery(query)
        .stripSuffix(dialectConfig.defaultQueryableSuffix)
      val expectedSql = sql.trim.replaceAll("\\s+", " ")
//      pprint.log(sqlResult)
//      pprint.log(expectedSql)
      assert(sqlResult == expectedSql, pprint.apply(SqlFormatter.format(sqlResult)))
    }
    if (sqls.nonEmpty) {
      val sqlResult = dbClient.autoCommit.toSqlQuery(query)
        .stripSuffix(dialectConfig.defaultQueryableSuffix)

      val simplifiedSqls = sqls.map(_.trim.replaceAll("\\s+", " "))
//      pprint.log(simplifiedSqls)
//      pprint.log(sqlResult)
      assert(simplifiedSqls.contains(sqlResult), pprint.apply(SqlFormatter.format(sqlResult)))

    }

    val result = dbClient.autoCommit.run(query)

    val values = Option(value) ++ moreValues
    val normalized = normalize(result)
    assert(values.exists(value => normalized == value), pprint.apply(normalized))
  }
}

object TestDb {

  lazy val pprinter: PPrinter = PPrinter.Color.copy(additionalHandlers = {
    case v: SubqueryRef[_, _] => pprinter.treeify(v.value, false, true)
    case v: Expr[_] if !v.isInstanceOf[scala.Product] =>
      pprinter.treeify(Expr.getToString(v), false, true)
  })
}
