package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

trait UpdateSubQueryTests extends ScalaSqlSuite {
  def description = "`UPDATE` queries that use Subqueries"
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {

    test("setSubquery") - {
      checker(
        query = Text { Product.update(_ => true).set(_.price := Product.select.maxBy(_.price)) },
        sqls = Seq(
          """
            UPDATE product
            SET price = (SELECT MAX(product1.price) AS res FROM product product1)
          """,
          """
            UPDATE product
            SET product.price = (SELECT MAX(product1.price) AS res FROM product product1)
          """
        ),
        value = 6,
        docs = """
          You can use subqueries to compute the values you want to update, using
          aggregates like `.maxBy` to convert the `Select[T]` into an `Expr[T]`
        """
      )

      checker(
        query = Text { Product.select.map(p => (p.id, p.name, p.price)) },
        value = Seq(
          (1, "Face Mask", 1000.0),
          (2, "Guitar", 1000.0),
          (3, "Socks", 1000.0),
          (4, "Skate Board", 1000.0),
          (5, "Camera", 1000.0),
          (6, "Cookie", 1000.0)
        )
      )
    }

    test("whereSubquery") - {
      checker(
        query = Text {
          Product.update(_.price `=` Product.select.maxBy(_.price)).set(_.price := 0)
        },
        sqls = Seq(
          """
            UPDATE product
            SET price = ?
            WHERE (product.price = (SELECT MAX(product1.price) AS res FROM product product1))
          """,
          """
            UPDATE product
            SET product.price = ?
            WHERE (product.price = (SELECT MAX(product1.price) AS res FROM product product1))
          """
        ),
        value = 1,
        docs = """
          Subqueries and aggregates can also be used in the `WHERE` clause, defined by the
          predicate passed to `Table.update
        """
      )

      checker(
        query = Text { Product.select.map(p => (p.id, p.name, p.price)) },
        value = Seq(
          (1, "Face Mask", 8.88),
          (2, "Guitar", 300.0),
          (3, "Socks", 3.14),
          (4, "Skate Board", 123.45),
          (5, "Camera", 0.0),
          (6, "Cookie", 0.1)
        ),
        normalize = (x: Seq[(Int, String, Double)]) => x.sorted
      )

    }
  }
}
