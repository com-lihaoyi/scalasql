package scalasql.operations

import scalasql._
import utest._
import utils.ScalaSqlSuite
import sourcecode.Text
import scalasql.datatypes.OptCols

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
      query =
        Text { Purchase.select.groupBy(_.shippingInfoId)(agg => agg.countDistinctBy(_.productId)) },
      sql =
        """SELECT purchase0.shipping_info_id AS res_0, COUNT(DISTINCT purchase0.product_id) AS res_1
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
      query = Text {
        Purchase.select.aggregate(agg =>
          (agg.countBy(_.productId), agg.countDistinctBy(_.productId), agg.sumBy(_.total))
        )
      },
      sql =
        """SELECT COUNT(purchase0.product_id) AS res_0, COUNT(DISTINCT purchase0.product_id) AS res_1, SUM(purchase0.total) AS res_2
              FROM purchase purchase0""",
      value = (7, 6, 12343.2)
    )

    test("countInJoin") - checker(
      query = Text {
        (for {
          p <- Purchase.select
          pr <- Product.join(_.id === p.productId)
        } yield pr).countBy(_.name)
      },
      sql = """SELECT COUNT(product1.name) AS res
              FROM purchase purchase0
              JOIN product product1 ON (product1.id = purchase0.product_id)""",
      value = 7
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
        sql = """SELECT COUNT(((product0.name || ?) || product0.kebab_case_name)) AS res
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
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()

  def tests = Tests {
    // Setup OptCols table with test data
    checker(
      query = Text {
        OptCols.insert.batched(_.myInt, _.myInt2)(
          (None, None),
          (Some(1), Some(2)),
          (Some(3), None),
          (None, Some(4)),
          (Some(1), Some(5)),
          (Some(2), Some(2))
        )
      },
      value = 6
    )(implicitly, utest.framework.TestPath(Nil))

    test("countOptionColumn") - {
      test("countBy") - checker(
        query = OptCols.select.countBy(_.myInt),
        sql = "SELECT COUNT(opt_cols0.my_int) AS res FROM opt_cols opt_cols0",
        value = 4 // NULLs are not counted (4 non-null values)
      )

      test("countDistinctBy") - checker(
        query = OptCols.select.countDistinctBy(_.myInt),
        sql = "SELECT COUNT(DISTINCT opt_cols0.my_int) AS res FROM opt_cols opt_cols0",
        value = 3 // Distinct non-null values: 1, 2, 3
      )
    }

    test("countExprOption") - {
      test("count") - checker(
        query = OptCols.select.map(_.myInt2).count,
        sql = "SELECT COUNT(opt_cols0.my_int2) AS res FROM opt_cols opt_cols0",
        value = 4 // NULLs are not counted
      )

      test("countDistinct") - checker(
        query = OptCols.select.map(_.myInt2).countDistinct,
        sql = "SELECT COUNT(DISTINCT opt_cols0.my_int2) AS res FROM opt_cols opt_cols0",
        value = 3 // Distinct non-null values: 2, 4, 5
      )
    }

    test("countWithOptionFilter") - checker(
      query = OptCols.select
        .filter(_.myInt.map(_ > 1).getOrElse(false))
        .countBy(_.myInt2),
      sqls = Seq(
        """SELECT COUNT(opt_cols0.my_int2) AS res
              FROM opt_cols opt_cols0
              WHERE COALESCE((opt_cols0.my_int > ?), ?)""",
        """SELECT
  COUNT(opt_cols0.my_int2) AS res
FROM
  opt_cols opt_cols0
WHERE
  COALESCE((opt_cols0.my_int > ?), (1 = ?))"""
      ),
      value = 1 // Only one record where myInt > 1 and myInt2 is not null
    )

    test("groupByWithOptionCount") - checker(
      query = Text {
        OptCols.select
          .groupBy(_.myInt)(agg => agg.countBy(_.myInt2))
      },
      sql = """SELECT opt_cols0.my_int AS res_0, COUNT(opt_cols0.my_int2) AS res_1
              FROM opt_cols opt_cols0
              GROUP BY opt_cols0.my_int""",
      value = Seq((None, 1), (Some(1), 2), (Some(2), 1), (Some(3), 0)),
      normalize = (x: Seq[(Option[Int], Int)]) => x.sortBy(_._1.getOrElse(-1))
    )
  }
}
