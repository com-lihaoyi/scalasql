package scalasql.operations

import scalasql._
import utest._
import utils.ScalaSqlSuite
import sourcecode.Text

trait DbCountOpsTests extends ScalaSqlSuite {
  def description = "COUNT and COUNT(DISTINCT) aggregations"
  def tests = Tests {
    test("countBy") - checker(
      query = Purchase.select.countBy(_.productId),
      sql = "SELECT COUNT(purchase0.product_id) AS res FROM purchase purchase0",
      value = 7
    )

    test("countDistinctBy") - checker(
      query = Purchase.select.countDistinctBy(_.productId),
      sql = "SELECT COUNT(DISTINCT purchase0.product_id) AS res FROM purchase purchase0",
      value = 6
    )

    test("countExpr") - checker(
      query = Purchase.select.map(_.productId).count,
      sql = "SELECT COUNT(purchase0.product_id) AS res FROM purchase purchase0",
      value = 7
    )

    test("countDistinctExpr") - checker(
      query = Purchase.select.map(_.productId).countDistinct,
      sql = "SELECT COUNT(DISTINCT purchase0.product_id) AS res FROM purchase purchase0",
      value = 6
    )

    test("countWithGroupBy") - checker(
      query = Text { Purchase.select.groupBy(_.shippingInfoId)(agg => agg.countBy(_.productId)) },
      sql = """SELECT purchase0.shipping_info_id AS res_0, COUNT(purchase0.product_id) AS res_1
              FROM purchase purchase0
              GROUP BY purchase0.shipping_info_id""",
      value = Seq((1, 3), (2, 2), (3, 2)),
      normalize = (x: Seq[(Int, Int)]) => x.sorted
    )

    test("countDistinctWithGroupBy") - checker(
      query = Text { Purchase.select.groupBy(_.shippingInfoId)(agg => agg.countDistinctBy(_.productId)) },
      sql = """SELECT purchase0.shipping_info_id AS res_0, COUNT(DISTINCT purchase0.product_id) AS res_1
              FROM purchase purchase0
              GROUP BY purchase0.shipping_info_id""",
      value = Seq((1, 3), (2, 2), (3, 2)),
      normalize = (x: Seq[(Int, Int)]) => x.sorted
    )

    test("countWithFilter") - checker(
      query = Purchase.select.filter(_.total > 100).countBy(_.productId),
      sql = """SELECT COUNT(purchase0.product_id) AS res
              FROM purchase purchase0
              WHERE (purchase0.total > ?)""",
      value = 4
    )

    test("countDistinctWithFilter") - checker(
      query = Purchase.select.filter(_.total > 100).countDistinctBy(_.productId),
      sql = """SELECT COUNT(DISTINCT purchase0.product_id) AS res
              FROM purchase purchase0
              WHERE (purchase0.total > ?)""",
      value = 4
    )

    test("multipleAggregatesWithCount") - checker(
      query = Text { Purchase.select.aggregate(agg =>
        (agg.countBy(_.productId), agg.countDistinctBy(_.productId), agg.sumBy(_.total))
      ) },
      sql = """SELECT COUNT(purchase0.product_id) AS res_0, COUNT(DISTINCT purchase0.product_id) AS res_1, SUM(purchase0.total) AS res_2
              FROM purchase purchase0""",
      value = (7, 6, 12343.2)
    )

    test("countInJoin") - checker(
      query = Text { for {
        p <- Purchase.select
        pr <- Product.join(_.id === p.productId)
      } yield pr.name },
      sql = """SELECT product1.name AS res
              FROM purchase purchase0
              JOIN product product1 ON (product1.id = purchase0.product_id)""",
      value = Seq("Face Mask", "Guitar", "Socks", "Skate Board", "Camera", "Face Mask", "Cookie"),
      normalize = (x: Seq[String]) => x.sorted
    )

    test("countWithComplexExpressions") - {
      test("arithmetic") - checker(
        query = Text { Purchase.select.map(_.total * 2).count },
        sql = """SELECT COUNT((purchase0.total * ?)) AS res
                FROM purchase purchase0""",
        value = 7
      )

      test("stringConcat") - checker(
        query = Text { Product.select.map(p => p.name + " - " + p.kebabCaseName).count },
        sql = """SELECT COUNT(CONCAT(product0.name, ?, product0.kebab_case_name)) AS res
                FROM product product0""",
        value = 6
      )
    }

    test("countDistinctWithComplexExpressions") - {
      test("arithmetic") - checker(
        query = Text { Purchase.select.map(p => p.productId + 100).countDistinct },
        sql = """SELECT COUNT(DISTINCT (purchase0.product_id + ?)) AS res
                FROM purchase purchase0""",
        value = 6
      )
    }
  }
}

// Additional test suite specifically for Option types
trait DbCountOpsOptionTests extends ScalaSqlSuite {
  def description = "COUNT operations with Option types"

  // Table with optional columns for testing
  case class OptionalPurchase[T[_]](
    id: T[Int],
    productId: T[Option[Int]],
    buyerId: T[Option[Int]],
    total: T[Option[Double]]
  )
  object OptionalPurchase extends Table[OptionalPurchase]

  def tests = Tests {
    test("setup") - checker(
      query = OptionalPurchase.insert.batched(_.id, _.productId, _.buyerId, _.total)(
        (1, Some(1), Some(1), Some(100.0)),
        (2, Some(1), None, Some(200.0)),
        (3, Some(2), Some(2), None),
        (4, None, Some(1), Some(300.0)),
        (5, Some(3), None, None),
        (6, Some(2), Some(3), Some(150.0)),
        (7, None, None, None)
      ),
      value = 7
    )

    test("countOptionColumn") - {
      test("countBy") - checker(
        query = OptionalPurchase.select.countBy(_.productId),
        sql = "SELECT COUNT(optional_purchase0.product_id) AS res FROM optional_purchase optional_purchase0",
        value = 5  // NULLs are not counted
      )

      test("countDistinctBy") - checker(
        query = OptionalPurchase.select.countDistinctBy(_.productId),
        sql = "SELECT COUNT(DISTINCT optional_purchase0.product_id) AS res FROM optional_purchase optional_purchase0",
        value = 3  // Distinct non-null values: 1, 2, 3
      )
    }

    test("countExprOption") - {
      test("count") - checker(
        query = OptionalPurchase.select.map(_.buyerId).count,
        sql = "SELECT COUNT(optional_purchase0.buyer_id) AS res FROM optional_purchase optional_purchase0",
        value = 4  // NULLs are not counted
      )

      test("countDistinct") - checker(
        query = OptionalPurchase.select.map(_.buyerId).countDistinct,
        sql = "SELECT COUNT(DISTINCT optional_purchase0.buyer_id) AS res FROM optional_purchase optional_purchase0",
        value = 3  // Distinct non-null values: 1, 2, 3
      )
    }

    test("countWithOptionFilter") - checker(
      query = OptionalPurchase.select
        .filter(_.total.map(_ > 100).getOrElse(false))
        .countBy(_.productId),
      sql = """SELECT COUNT(optional_purchase0.product_id) AS res
              FROM optional_purchase optional_purchase0
              WHERE COALESCE((optional_purchase0.total > ?), ?)""",
      value = 3
    )

    test("groupByWithOptionCount") - checker(
      query = Text { OptionalPurchase.select
        .groupBy(_.productId)(agg => agg.countBy(_.buyerId)) },
      sql = """SELECT optional_purchase0.product_id AS res_0, COUNT(optional_purchase0.buyer_id) AS res_1
              FROM optional_purchase optional_purchase0
              GROUP BY optional_purchase0.product_id""",
      value = Seq((Some(1), 1), (Some(2), 2), (Some(3), 0), (None, 1)),
      normalize = (x: Seq[(Option[Int], Int)]) => x.sortBy(_._1.getOrElse(-1))
    )
  }
}

