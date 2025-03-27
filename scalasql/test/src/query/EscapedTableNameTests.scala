package scalasql.query

import scalasql._
import scalasql.core.JoinNullable
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate
import scalasql.core.Config

trait EscapedTableNameTests extends ScalaSqlSuite {
  def description = """
    If your table name is a reserved sql world, e.g. `order`, you can specify this in your table definition with
    `override def escape = true`
  """

  def tests = Tests {
    test("escape table name") {
      val tableNameEscaped = dialectSelf.escape(Config.camelToSnake(Table.name(Select)))
      test("select") {
        checker(
          query = Text {
            Select.select
          },
          sql = s"""
            SELECT select0.id AS id, select0.name AS name
            FROM $tableNameEscaped select0
          """,
          value = Seq.empty[Select[Sc]],
          docs = ""
        )
      }
      test("delete") {
        checker(
          query = Text {
            Select.delete(_ => true)
          },
          sql = s"DELETE FROM $tableNameEscaped WHERE ?",
          value = 0,
          docs = ""
        )
      }
      test("join") {
        checker(
          query = Text {
            Select.select.join(Select)(_.id `=` _.id)
          },
          sql = s"""
            SELECT
              select0.id AS res_0_id,
              select0.name AS res_0_name,
              select1.id AS res_1_id,
              select1.name AS res_1_name
            FROM
              $tableNameEscaped select0
              JOIN $tableNameEscaped select1 ON (select0.id = select1.id)
          """,
          value = Seq.empty[(Select[Sc], Select[Sc])],
          docs = ""
        )
      }
      test("update") {
        checker(
          query = Text {
            Select.update(_ => true).set(_.name := "hello")
          },
          sqls = Seq(
            s"UPDATE $tableNameEscaped SET $tableNameEscaped.name = ?",
            s"UPDATE $tableNameEscaped SET name = ?"
          ),
          value = 0,
          docs = ""
        )
      }
      test("insert") {
        checker(
          query = Text {
            Select.insert.values(
              Select[Sc](
                id = 0,
                name = "hello"
              )
            )
          },
          sql = s"INSERT INTO $tableNameEscaped (id, name) VALUES (?, ?)",
          value = 1,
          docs = ""
        )
      }
    }
  }
}
