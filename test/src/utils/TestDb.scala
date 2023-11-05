package scalasql.utils

import com.github.vertical_blank.sqlformatter.SqlFormatter
import org.testcontainers.containers.{MySQLContainer, PostgreSQLContainer}
import pprint.PPrinter
import scalasql.dialects.DialectConfig
import scalasql.query.{Expr, SubqueryRef}
import scalasql.{Config, DatabaseClient, Queryable, UtestFramework}

import java.sql.Connection

class TestDb(
    val dbClient: DatabaseClient,
    testSchemaFileName: String,
    testDataFileName: String,
    dialectConfig: DialectConfig,
    suiteName: String
) {

  def reset() = {
    dbClient.autoCommit.runRawUpdate(os.read(os.pwd / "test" / "resources" / testSchemaFileName))
    dbClient.autoCommit.runRawUpdate(os.read(os.pwd / "test" / "resources" / testDataFileName))
  }

  def apply[T, V](
      query: sourcecode.Text[T],
      sql: String = null,
      sqls: Seq[String] = Nil,
      value: sourcecode.Text[V] = null,
      moreValues: Seq[V] = Nil,
      normalize: V => V = (x: V) => x
  )(implicit qr: Queryable[T, V], tp: utest.framework.TestPath) = {
    val sqlResult = dbClient.autoCommit.toSqlQuery(query.value)
      .stripSuffix(dialectConfig.defaultQueryableSuffix)

    val matchedSql = (Option(sql) ++ sqls).find{ sql =>

      val expectedSql = sql.trim.replaceAll("\\s+", " ")
      sqlResult == expectedSql
    }

    if (sql != null) {
      assert(matchedSql.nonEmpty, pprint.apply(SqlFormatter.format(sqlResult)))
    }

    val result = dbClient.autoCommit.run(query.value)

    val values = Option(value.value) ++ moreValues
    val normalized = normalize(result)
    assert(values.exists(value => normalized == value), pprint.apply(normalized))

    UtestFramework.recorded.append(
      UtestFramework.Record(
        suiteName = suiteName.stripSuffix("$"),
        testPath = tp.value,
        queryCodeString = query.source,
        sqlString = matchedSql.orNull,
        resultCodeString = value.source
      )
    )

    ()
  }
}

object TestDb {

  lazy val pprinter: PPrinter = PPrinter.Color.copy(additionalHandlers = {
    case v: SubqueryRef[_, _] => pprinter.treeify(v.value, false, true)
    case v: Expr[_] if !v.isInstanceOf[scala.Product] =>
      pprinter.treeify(Expr.getToString(v), false, true)
  })
}
