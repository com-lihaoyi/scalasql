package scalasql.query

import scalasql._
import scalasql.core.JoinNullable
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait SchemaTests extends ScalaSqlSuite {
  def description = "Additional tests to ensure schema mapping produces valid SQL"

  def tests = Tests {
    test("schema") {
      test("select") {
        checker(
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
      test("insert.columns") {
        checker(
          query = Invoice.insert.columns(
            _.total := 200.3,
            _.vendor_name := "Huawei"
          ),
          sql = "INSERT INTO otherschema.invoice (total, vendor_name) VALUES (?, ?)",
          value = 1,
          docs = """
            If your table belongs to a schema other than the default schema of your database,
            you can specify this in your table definition with table.schemaName
          """
        )
      }
      test("insert.values") {
        checker(
          query = Invoice.insert
            .values(
              Invoice[Sc](
                id = 0,
                total = 200.3,
                vendor_name = "Huawei"
              )
            )
            .skipColumns(_.id),
          sql = "INSERT INTO otherschema.invoice (total, vendor_name) VALUES (?, ?)",
          value = 1,
          docs = """
            If your table belongs to a schema other than the default schema of your database,
            you can specify this in your table definition with table.schemaName
          """
        )
      }
      test("update") {
        checker(
          query = Invoice
            .update(_.id === 1)
            .set(
              _.total := 200.3,
              _.vendor_name := "Huawei"
            ),
          sql = """UPDATE otherschema.invoice
            SET
              total = ?,
              vendor_name = ?
            WHERE
              (invoice.id = ?)""",
          value = 1,
          docs = """
            If your table belongs to a schema other than the default schema of your database,
            you can specify this in your table definition with table.schemaName
          """
        )
      }
      test("delete") {
        checker(
          query = Invoice.delete(_.id === 1),
          sql = "DELETE FROM otherschema.invoice WHERE (invoice.id = ?)",
          value = 1,
          docs = """
            If your table belongs to a schema other than the default schema of your database,
            you can specify this in your table definition with table.schemaName
          """
        )
      }
      test("insert into") {
        checker(
          query = Invoice.insert.select(
            i => (i.total, i.vendor_name),
            Invoice.select.map(i => (i.total, i.vendor_name))
          ),
          sql = """INSERT INTO
              otherschema.invoice (total, vendor_name)
            SELECT
              invoice0.total AS res_0,
              invoice0.vendor_name AS res_1
            FROM
              otherschema.invoice invoice0""",
          value = 4,
          docs = """
            If your table belongs to a schema other than the default schema of your database,
            you can specify this in your table definition with table.schemaName
          """
        )
      }
      test("join") {
        checker(
          query = Text {
            Invoice.select.join(Invoice)(_.id `=` _.id).map(_._1.id)
          },
          sql = """SELECT
              invoice0.id AS res
            FROM
              otherschema.invoice invoice0
            JOIN otherschema.invoice invoice1 ON (invoice0.id = invoice1.id)""",
          value = Seq(2, 3, 4, 5, 6, 7, 8, 9),
          docs = """
            If your table belongs to a schema other than the default schema of your database,
            you can specify this in your table definition with table.schemaName
          """
        )
      }
    }
  }
}
