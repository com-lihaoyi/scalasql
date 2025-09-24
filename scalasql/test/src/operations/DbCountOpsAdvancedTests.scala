package scalasql.operations

import scalasql._
import utest._
import utils.ScalaSqlSuite
import sourcecode.Text
import scalasql.datatypes.OptCols

trait DbCountOpsAdvancedTests extends ScalaSqlSuite {
  def description = "Advanced COUNT operations with edge cases and expressions"

  def tests = Tests {
    test("setup") - checker(
      query = Text {
        OptCols.insert.batched(_.myInt, _.myInt2)(
          (Some(1), Some(1)),
          (Some(2), None),
          (Some(3), Some(3)),
          (None, Some(4)),
          (Some(5), Some(5))
        )
      },
      value = 5
    )

    test("countWithNulls") - {
      test("nonNullCount") - checker(
        query = Text { OptCols.select.countBy(_.myInt) },
        sql = "SELECT COUNT(opt_cols0.my_int) AS res FROM opt_cols opt_cols0",
        value = 4 // NULLs are not counted
      )

      test("nonNullCountDistinct") - checker(
        query = Text { OptCols.select.countDistinctBy(_.myInt) },
        sql = "SELECT COUNT(DISTINCT opt_cols0.my_int) AS res FROM opt_cols opt_cols0",
        value = 4 // All non-NULL values are unique
      )

      test("secondColumnCount") - checker(
        query = Text { OptCols.select.countBy(_.myInt2) },
        sql = "SELECT COUNT(opt_cols0.my_int2) AS res FROM opt_cols opt_cols0",
        value = 4 // NULLs are not counted
      )

      test("secondColumnCountDistinct") - checker(
        query = Text { OptCols.select.countDistinctBy(_.myInt2) },
        sql = "SELECT COUNT(DISTINCT opt_cols0.my_int2) AS res FROM opt_cols opt_cols0",
        value = 4 // All non-NULL values are unique
      )
    }

    test("countWithExpressions") - {
      test("countArithmeticExpressions") - checker(
        query = Text { Purchase.select.map(p => p.productId * 2).count },
        sql = "SELECT COUNT((purchase0.product_id * ?)) AS res FROM purchase purchase0",
        value = 7 // All rows have non-NULL productId
      )

      test("countDistinctArithmeticExpressions") - checker(
        query = Text { Purchase.select.map(p => p.productId + p.shippingInfoId).countDistinct },
        sql =
          "SELECT COUNT(DISTINCT (purchase0.product_id + purchase0.shipping_info_id)) AS res FROM purchase purchase0",
        value = 6 // All combinations are unique
      )
    }

    test("countWithModuloOperations") - {
      test("moduloCount") - checker(
        query = Text { Purchase.select.map(p => p.productId % 2).countDistinct },
        sqls = Seq(
          "SELECT COUNT(DISTINCT purchase0.product_id % ?) AS res FROM purchase purchase0",
          "SELECT COUNT(DISTINCT MOD(purchase0.product_id, ?)) AS res FROM purchase purchase0"
        ),
        value = 2 // 0 and 1 (even and odd)
      )

      test("moduloWithFilter") - checker(
        query =
          Text { Purchase.select.filter(_.productId > 2).map(p => p.productId % 3).countDistinct },
        sqls = Seq(
          "SELECT COUNT(DISTINCT purchase0.product_id % ?) AS res FROM purchase purchase0 WHERE (purchase0.product_id > ?)",
          "SELECT COUNT(DISTINCT MOD(purchase0.product_id, ?)) AS res FROM purchase purchase0 WHERE (purchase0.product_id > ?)"
        ),
        value = 3 // Different modulo values for productId > 2
      )
    }

    test("countWithGroupBy") - {
      test("groupByWithCount") - checker(
        query = Text { Purchase.select.groupBy(_.shippingInfoId)(agg => agg.countBy(_.productId)) },
        sql = """SELECT purchase0.shipping_info_id AS res_0, COUNT(purchase0.product_id) AS res_1
                FROM purchase purchase0
                GROUP BY purchase0.shipping_info_id""",
        value = Seq((1, 3), (2, 2), (3, 2)),
        normalize = (x: Seq[(Int, Int)]) => x.sortBy(_._1)
      )

      test("groupByWithCountDistinct") - checker(
        query = Text {
          Purchase.select.groupBy(_.shippingInfoId)(agg => agg.countDistinctBy(_.productId))
        },
        sql =
          """SELECT purchase0.shipping_info_id AS res_0, COUNT(DISTINCT purchase0.product_id) AS res_1
                FROM purchase purchase0
                GROUP BY purchase0.shipping_info_id""",
        value = Seq((1, 3), (2, 2), (3, 2)),
        normalize = (x: Seq[(Int, Int)]) => x.sortBy(_._1)
      )
    }

    test("countWithComplexFilters") - {
      test("countWithRangeFilter") - checker(
        query = Text {
          Purchase.select
            .filter(p => p.productId >= 2 && p.productId <= 4)
            .countBy(_.total)
        },
        sql = """SELECT COUNT(purchase0.total) AS res
                FROM purchase purchase0
                WHERE ((purchase0.product_id >= ?) AND (purchase0.product_id <= ?))""",
        value = 3 // Purchases with productId in [2,4]
      )

      test("countWithDecimalFilter") - checker(
        query = Text {
          Purchase.select
            .filter(_.total > 100)
            .countDistinctBy(_.productId)
        },
        sql = """SELECT COUNT(DISTINCT purchase0.product_id) AS res
                FROM purchase purchase0
                WHERE (purchase0.total > ?)""",
        value = 4 // Distinct productIds for purchases > 100
      )
    }

    test("countWithAdvancedPredicates") - {
      test("countWithComplexFilter") - checker(
        query = Text {
          Purchase.select
            .filter(p => p.productId > 1 && p.shippingInfoId <= 2)
            .countDistinctBy(_.productId)
        },
        sql = """SELECT COUNT(DISTINCT purchase0.product_id) AS res
                FROM purchase purchase0
                WHERE ((purchase0.product_id > ?) AND (purchase0.shipping_info_id <= ?))""",
        value = 4 // Distinct productIds matching filter criteria
      )
    }
  }
}
