package scalasql.utils

import com.github.vertical_blank.sqlformatter.SqlFormatter
import org.testcontainers.containers.{MySQLContainer, PostgreSQLContainer}
import pprint.PPrinter
import scalasql.query.SubqueryRef
import scalasql.{Config, DbClient, Queryable, Expr, UtestFramework}

import java.sql.Connection

class TestChecker(
    val dbClient: DbClient,
    testSchemaFileName: String,
    testDataFileName: String,
    suiteName: String,
    suiteLine: Int,
    description: String
) {

  UtestFramework.recordedSuiteDescriptions(suiteName.stripSuffix("Tests$")) = description

  val autoCommitConnection = dbClient.getAutoCommitClientConnection
  def reset() = {
    autoCommitConnection.updateRaw(
      os.read(os.pwd / "scalasql" / "test" / "resources" / testSchemaFileName)
    )
    autoCommitConnection.updateRaw(
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
    if (sys.env.contains("SCALASQL_RUN_BENCHMARK")) {
      for(i <- Range(0, 4)) {
        var iterations = 0
        val multiplier = 10
        val duration = 5000
        val end = System.currentTimeMillis() + duration
        while (System.currentTimeMillis() < end) {
          var i = 0
          while(i < multiplier){
            i += 1
            dbClient.renderSql(query.value)
          }
          iterations += 1
        }
        println(s"${iterations * multiplier} iterations in ${duration}ms")
      }
    }
    val sqlResult = autoCommitConnection
      .renderSql(query.value)

    val allCheckedSqls = Option(sql) ++ sqls
    val matchedSql = allCheckedSqls.find { sql =>
      val expectedSql = sql.trim.replaceAll("\\s+", " ")
      // pprint.log(sqlResult)
      // pprint.log(expectedSql)
      sqlResult == expectedSql
    }

    if (allCheckedSqls.nonEmpty) {
      assert(matchedSql.nonEmpty, pprint.apply(SqlFormatter.format(sqlResult)))
    }

    val result = autoCommitConnection.run(query.value)

    val values = Option(value).map(_.value) ++ moreValues
    val normalized = normalize(result)
    if (values.nonEmpty) {
      assert(values.exists(value => normalized == value), pprint.apply(normalized))
    }

    UtestFramework.recordedTests.append(
      UtestFramework.Record(
        suiteName = suiteName.stripSuffix("Tests$"),
        suiteLine = suiteLine,
        testPath = tp.value,
        docs = docs,
        queryCodeString = query.source,
        sqlString = matchedSql,
        resultCodeString = Option(value).map(_.source)
      )
    )

    ()
  }
}

object TestChecker {

  lazy val pprinter: PPrinter = PPrinter.Color.copy(additionalHandlers = {
    case v: SubqueryRef => pprinter.treeify(v.value, false, true)
    case v: Expr[_] if !v.isInstanceOf[scala.Product] =>
      pprinter.treeify(Expr.toString(v), false, true)
  })
}
