package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait SchemaTests extends ScalaSqlSuite {
  def description = "Additional tests to ensure schema mapping produces valid SQL"

  def tests = Tests {
    test("schema") - checker(
      query = Text {
        Invoice.select
      },
      sql = """
        SELECT invoice0.id AS id, invoice0.total AS total, invoice0.vendor_name AS vendor_name
        FROM otherschema.invoice invoice0
      """,
      value = Seq(
        Invoice[Sc](id = 1, total = 150.4, vendor_name = "Siemens"),
        Invoice[Sc](id = 2, total = 213.3, vendor_name = "Samsung"),
        Invoice[Sc](id = 3, total = 407.2, vendor_name = "Shell")
      ),
      docs = """
        If your table belongs to a schema other than the default schema of your database,
        you can specify this in your table definition with table.schemaName
      """
    )
  }
}
