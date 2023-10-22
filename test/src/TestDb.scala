package usql
import com.github.vertical_blank.sqlformatter.SqlFormatter
import org.testcontainers.containers.{MySQLContainer, PostgreSQLContainer}
import pprint.PPrinter
import usql.query.{Expr, SubqueryRef}
import utest.TestSuite

import java.sql.{Connection, DriverManager}
trait UsqlTestSuite extends TestSuite with Dialect {
  val checker: TestDb
}
trait SqliteSuite extends TestSuite with SqliteDialect {
  val checker = new TestDb(
    DriverManager.getConnection("jdbc:sqlite::memory:"),
    "sqlite-test-data.sql",
    mySqlUpdateJoinSyntax = false
  )

  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
}
trait PostgresSuite extends TestSuite with PostgresDialect {
  val checker = new TestDb(
    DriverManager.getConnection(
      s"${TestDb.pg.getJdbcUrl}&user=${TestDb.pg.getUsername}&password=${TestDb.pg.getPassword}"
    ),
    "postgres-test-data.sql",
    mySqlUpdateJoinSyntax = false
  )

  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
}
trait MySqlSuite extends TestSuite with MySqlDialect {
  val checker = new TestDb(
    DriverManager.getConnection(
      TestDb.mysql.getJdbcUrl + "?allowMultiQueries=true",
      TestDb.mysql.getUsername,
      TestDb.mysql.getPassword
    ),
    "mysql-test-data.sql",
    mySqlUpdateJoinSyntax = true
  )

  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
}
class TestDb(connection: Connection,
             testDataFileName: String,
             mySqlUpdateJoinSyntax: Boolean) {

  val db = new DatabaseApi(
    connection,
    tableNameMapper = camelToSnake,
    tableNameUnMapper = snakeToCamel,
    columnNameMapper = camelToSnake,
    columnNameUnMapper = snakeToCamel,
    mySqlUpdateJoinSyntax = mySqlUpdateJoinSyntax
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

  def apply[T, V](query: T, sql: String = null, sqls: Seq[String] = null, value: V = null.asInstanceOf[V], normalize: V => V = null)
                 (implicit qr: Queryable[T, V]) = {
    if (sql != null) {
      val sqlResult = db.toSqlQuery(query)
      val expectedSql = sql.trim.replaceAll("\\s+", " ")
      assert(sqlResult == expectedSql, pprint.apply(SqlFormatter.format(sqlResult)))
    }
    if (sqls != null){
      val sqlResult = db.toSqlQuery(query)
//      pprint.log(sqlResult)
//      pprint.log(sqls.map(_.trim.replaceAll("\\s+", " ")))
      assert(
        sqls.exists(_.trim.replaceAll("\\s+", " ") == sqlResult),
        pprint.apply(SqlFormatter.format(sqlResult))
      )

    }

    val result = db.run(query)
    if (value != null) {
      val normalized = if (normalize == null) result else normalize(result)
      assert(normalized == value, pprint.apply(normalized))
    }
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
        "mysqld", "--character-set-server=utf8mb4", "--collation-server=utf8mb4_bin"
      )
    mysql.start()
    mysql
  }

  lazy val pprinter: PPrinter = PPrinter.Color.copy(
    additionalHandlers = {
      case v: Val[_] => pprinter.treeify(v.apply(), false, true)
      case v: SubqueryRef[_] => pprinter.treeify(v.value, false, true)
      case v: Expr[_] if !v.isInstanceOf[scala.Product] =>
        pprinter.treeify(v.exprToString, false, true)
    }
  )
}
