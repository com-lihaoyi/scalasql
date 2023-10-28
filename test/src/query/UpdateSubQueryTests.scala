package scalasql.query

import scalasql._
import utest._

import java.sql.Date

/**
 * Tests for basic update operations
 */
trait UpdateSubQueryTests extends ScalaSqlSuite {
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {

    test("setSubquery") - {
      checker(
        query = Product.update.set(_.price -> Product.select.maxBy(_.price)),
        sqls = Seq(
          """
            UPDATE product
            SET price = (SELECT MAX(product0.price) as res FROM product product0)
          """,
          """
            UPDATE product
            SET product.price = (SELECT MAX(product0.price) as res FROM product product0)
          """
        ),
        value = 6
      )

      checker(
        query = Product.select.map(p => (p.id, p.name, p.price)),
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
        query = Product.update.filter(_.price === Product.select.maxBy(_.price)).set(_.price -> 0),
        sqls = Seq(
          """
            UPDATE product
            SET price = ?
            WHERE product.price = (SELECT MAX(product0.price) as res FROM product product0)
          """,
          """
            UPDATE product
            SET product.price = ?
            WHERE product.price = (SELECT MAX(product0.price) as res FROM product product0)
          """
        ),
        value = 1
      )

      checker(
        query = Product.select.map(p => (p.id, p.name, p.price)),
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
