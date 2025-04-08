package scalasql.query

import scalasql._
import scalasql.core.JoinNullable
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate
import scalasql.core.Config
import scalasql.dialects.ReturningDialect

trait EscapedTableNameWithReturningTests extends ScalaSqlSuite {
  this: ReturningDialect =>

  def description = """
    If your table name is a reserved sql world, e.g. `order`, you can specify this in your table definition with
    `override def escape = true`
  """

  def tests = Tests {
    val tableNameEscaped = dialectSelf.escape(Config.camelToSnake(Table.name(Select)))

    test("insert with returning") {
      checker(
        query = Text {
          Select.insert
            .values(
              Select[Sc](
                id = 0,
                name = "hello"
              )
            )
            .returning(_.id)
        },
        sql =
          s"INSERT INTO $tableNameEscaped (id, name) VALUES (?, ?) RETURNING $tableNameEscaped.id AS res",
        value = Seq(0),
        docs = ""
      )
    }
  }
}
