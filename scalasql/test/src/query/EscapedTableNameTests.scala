package scalasql.query

import scalasql._
import scalasql.core.JoinNullable
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate
import scalasql.core.Config

trait EscapedTableNameTests extends ScalaSqlSuite {
  def description = "Additional tests to ensure schema mapping produces valid SQL"

  def tests = Tests {
    test("escape table name") {
      test("select") {
        val tableNameEscaped = dialectSelf.escape(Config.camelToSnake(Table.name(Select)))
        checker(
          query = Text {
            Select.select
          },
          sql = s"""
          SELECT select0.id AS id, select0.name AS name
          FROM ${tableNameEscaped} select0
        """,
          value = Seq.empty[Select[Sc]],
          docs = """
          
        """
        )
      }
    }
  }
}
