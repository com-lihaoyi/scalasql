package scalasql.query

import scalasql._
import utest._
import utils.ScalaSqlSuite

trait DeleteTests extends ScalaSqlSuite {
  def description = "Basic `DELETE` operations"
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {
    test("single") {
      checker(
        query = Purchase.delete(_.id `=` 2),
        sql = "DELETE FROM purchase WHERE (purchase.id = ?)",
        value = 1,
        docs = """
          `Table.delete` takes a mandatory predicate specifying what rows you want to delete.
          The most common case is to specify the ID of the row you want to delete
        """
      )

      checker(
        query = Purchase.select,
        value = Seq(
          Purchase[Sc](id = 1, shippingInfoId = 1, productId = 1, count = 100, total = 888.0),
          // id==2 got deleted
          Purchase[Sc](id = 3, shippingInfoId = 1, productId = 3, count = 5, total = 15.7),
          Purchase[Sc](id = 4, shippingInfoId = 2, productId = 4, count = 4, total = 493.8),
          Purchase[Sc](id = 5, shippingInfoId = 2, productId = 5, count = 10, total = 10000.0),
          Purchase[Sc](id = 6, shippingInfoId = 3, productId = 1, count = 5, total = 44.4),
          Purchase[Sc](id = 7, shippingInfoId = 3, productId = 6, count = 13, total = 1.3)
        )
      )
    }
    test("multiple") {
      checker(
        query = Purchase.delete(_.id <> 2),
        sql = "DELETE FROM purchase WHERE (purchase.id <> ?)",
        value = 6,
        docs = """
          Although specifying a single ID to delete is the most common case, you can pass
          in arbitrary predicates, e.g. in this example deleting all rows _except_ for the
          one with a particular ID
        """
      )

      checker(
        query = Purchase.select,
        value =
          Seq(Purchase[Sc](id = 2, shippingInfoId = 1, productId = 2, count = 3, total = 900.0))
      )
    }
    test("all") {
      checker(
        query = Purchase.delete(_ => true),
        sql = "DELETE FROM purchase WHERE ?",
        value = 7,
        docs = """
          If you actually want to delete all rows in the table, you can explicitly
          pass in the predicate `_ => true`
        """
      )

      checker(
        query = Purchase.select,
        value = Seq[Purchase[Sc]](
          // all Deleted
        )
      )
    }
  }
}
