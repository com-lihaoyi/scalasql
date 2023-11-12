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
    suiteName: String,
    suiteLine: Int,
    description: String
) {

  UtestFramework.recordedSuiteDescriptions(suiteName.stripSuffix("Tests$")) = description

  val autoCommitConnection = dbClient.getAutoCommitClientConnection
  def reset() = {
    autoCommitConnection.runRawUpdate(
      os.read(os.pwd / "scalasql" / "test" / "resources" / testSchemaFileName)
    )
    autoCommitConnection.runRawUpdate(
      os.read(os.pwd / "scalasql" / "test" / "resources" / testDataFileName)
    )
  }

  def recorded[T](docs: String, f: sourcecode.Text[T])(implicit tp: utest.framework.TestPath): T = {
    val res = f.value
    UtestFramework.recordedTests.append(
      UtestFramework.Record(
        suiteName = suiteName.stripSuffix("Tests$"),
        suiteLine = suiteLine,
        testPath = tp.value,
        docs = docs,
        queryCodeString = f.source match {
          case s"{$res}" => res
          case res => res
        },
        sqlString = None,
        resultCodeString = None
      )
    )

    res
  }
  def apply[T, V](
      query: sourcecode.Text[T],
      sql: String = null,
      sqls: Seq[String] = Nil,
      value: sourcecode.Text[V] = null,
      moreValues: Seq[V] = Nil,
      normalize: V => V = (x: V) => x,
      docs: String = ""
  )(implicit qr: Queryable[T, V], tp: utest.framework.TestPath) = {
    val sqlResult = autoCommitConnection
      .toSqlQuery(query.value)
      .stripSuffix(dialectConfig.defaultQueryableSuffix)

    val allCheckedSqls = Option(sql) ++ sqls
    val matchedSql = allCheckedSqls.find { sql =>
      val expectedSql = sql.trim.replaceAll("\\s+", " ")
      sqlResult == expectedSql
    }

    if (allCheckedSqls.nonEmpty) {
      assert(matchedSql.nonEmpty, pprint.apply(SqlFormatter.format(sqlResult)))
    }

    val result = autoCommitConnection.run(query.value)

    val values = Option(value.value) ++ moreValues
    val normalized = normalize(result)
    assert(values.exists(value => normalized == value), pprint.apply(normalized))

    UtestFramework.recordedTests.append(
      UtestFramework.Record(
        suiteName = suiteName.stripSuffix("Tests$"),
        suiteLine = suiteLine,
        testPath = tp.value,
        docs = docs,
        queryCodeString = query.source,
        sqlString = matchedSql,
        resultCodeString = Some(value.source)
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
