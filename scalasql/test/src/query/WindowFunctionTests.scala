package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait WindowFunctionTests extends ScalaSqlSuite {
  def description = "Window functions using `OVER`"

  def tests = Tests {
    test("simple"){
      test("rank") - checker(
        query = Text {
          Purchase.select.map(p =>
            (
              p.shippingInfoId,
              p.total,
              rank().over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
            )
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            RANK() OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Int)](
          (1, 15.7, 1),
          (1, 888.0, 2),
          (1, 900.0, 3),
          (2, 493.8, 1),
          (2, 10000.0, 2),
          (3, 1.3, 1),
          (3, 44.4, 2)
        ),
        docs = """
          Window functions like `rank()` are supported. You can use the `.over`, `.partitionBy`,
          and `.sortBy`
        """,
        normalize = (x: Seq[(Int, Double, Int)]) => x.sortBy(t => (t._1, t._3))
      )

      test("rowNumber") - checker(
        query = Text {
          Purchase.select.map(p =>
            (
              p.shippingInfoId,
              p.total,
              rowNumber().over.partitionBy(p.shippingInfoId).sortBy(p.total).asc)
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            ROW_NUMBER() OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Int)](
          (1, 15.7, 1),
          (1, 888.0, 2),
          (1, 900.0, 3),
          (2, 493.8, 1),
          (2, 10000.0, 2),
          (3, 1.3, 1),
          (3, 44.4, 2)
        ),
        normalize = (x: Seq[(Int, Double, Int)]) => x.sortBy(t => (t._1, t._3))
      )

      test("denseRank") - checker(
        query = Text {
          Purchase.select.map(p =>
            (
              p.shippingInfoId,
              p.total,
              denseRank().over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
            )
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            DENSE_RANK() OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Int)](
          (1, 15.7, 1),
          (1, 888.0, 2),
          (1, 900.0, 3),
          (2, 493.8, 1),
          (2, 10000.0, 2),
          (3, 1.3, 1),
          (3, 44.4, 2)
        ),
        normalize = (x: Seq[(Int, Double, Int)]) => x.sortBy(t => (t._1, t._3))
      )

      test("denseRank") - checker(
        query = Text {
          Purchase.select.map(p =>
            (
              p.shippingInfoId,
              p.total,
              denseRank().over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
            )
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            DENSE_RANK() OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Int)](
          (1, 15.7, 1),
          (1, 888.0, 2),
          (1, 900.0, 3),
          (2, 493.8, 1),
          (2, 10000.0, 2),
          (3, 1.3, 1),
          (3, 44.4, 2)
        ),
        normalize = (x: Seq[(Int, Double, Int)]) => x.sortBy(t => (t._1, t._3))
      )

      test("percentRank") - checker(
        query = Text {
          Purchase.select.map(p =>
            (
              p.shippingInfoId,
              p.total,
              percentRank().over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
            )
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            PERCENT_RANK() OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Double)](
          (1, 15.7, 0.0),
          (1, 888.0, 0.5),
          (1, 900.0, 1.0),
          (2, 493.8, 0.0),
          (2, 10000.0, 1.0),
          (3, 1.3, 0.0),
          (3, 44.4, 1.0)
        ),
        normalize = (x: Seq[(Int, Double, Double)]) => x.sortBy(t => (t._1, t._3))
      )

      test("cumeDist") - checker(
        query = Text {
          Purchase.select.map(p =>
            (
              p.shippingInfoId,
              p.total,
              cumeDist().over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
            )
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            CUME_DIST() OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Double)](
          (1, 15.7, 0.3333333333333333),
          (1, 888.0, 0.6666666666666666),
          (1, 900.0, 1.0),
          (2, 493.8, 0.5),
          (2, 10000.0, 1.0),
          (3, 1.3, 0.5),
          (3, 44.4, 1.0)
        ),
        normalize = (x: Seq[(Int, Double, Double)]) => x.sortBy(t => (t._1, t._3))
      )

      test("ntile") - checker(
        query = Text {
          Purchase.select.map(p =>
            (
              p.shippingInfoId,
              p.total,
              ntile(3).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
            )
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            NTILE(?) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Int)](
          (1, 15.7, 1),
          (1, 888.0, 2),
          (1, 900.0, 3),
          (2, 493.8, 1),
          (2, 10000.0, 2),
          (3, 1.3, 1),
          (3, 44.4, 2)
        ),
        normalize = (x: Seq[(Int, Double, Int)]) => x.sortBy(t => (t._1, t._3))
      )

      test("lag") - checker(
        query = Text {
          Purchase.select.map(p =>
            (
              p.shippingInfoId,
              p.total,
              lag(p.total, 1, -1.0).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
            )
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            LAG(purchase0.total, ?, ?) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Double)](
          (1, 15.7, -1.0),
          (1, 888.0, 15.7),
          (1, 900.0, 888.0),
          (2, 493.8, -1.0),
          (2, 10000.0, 493.8),
          (3, 1.3, -1.0),
          (3, 44.4, 1.3)
        ),
        normalize = (x: Seq[(Int, Double, Double)]) => x.sorted
      )


      test("lead") - checker(
        query = Text {
          Purchase.select.map(p =>
            (
              p.shippingInfoId,
              p.total,
              lead(p.total, 1, -1.0).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
            )
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            LEAD(purchase0.total, ?, ?) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Double)](
          (1, 15.7, 888.0),
          (1, 888.0, 900.0),
          (1, 900.0, -1.0),
          (2, 493.8, 10000.0),
          (2, 10000.0, -1.0),
          (3, 1.3, 44.4),
          (3, 44.4, -1.0)
        ),
        normalize = (x: Seq[(Int, Double, Double)]) => x.sorted
      )


      test("firstValue") - checker(
        query = Text {
          Purchase.select.map(p =>
            (
              p.shippingInfoId,
              p.total,
              firstValue(p.total).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
            )
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            FIRST_VALUE(purchase0.total) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Double)](
          (1, 15.7, 15.7),
          (1, 888.0, 15.7),
          (1, 900.0, 15.7),
          (2, 493.8, 493.8),
          (2, 10000.0, 493.8),
          (3, 1.3, 1.3),
          (3, 44.4, 1.3)
        ),
        normalize = (x: Seq[(Int, Double, Double)]) => x.sorted
      )

      test("lastValue") - checker(
        query = Text {
          Purchase.select.map(p =>
            (
              p.shippingInfoId,
              p.total,
              lastValue(p.total).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
            )
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            LAST_VALUE(purchase0.total) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Double)](
          (1, 15.7, 15.7),
          (1, 888.0, 888.0),
          (1, 900.0, 900.0),
          (2, 493.8, 493.8),
          (2, 10000.0, 10000.0),
          (3, 1.3, 1.3),
          (3, 44.4, 44.4)
        ),
        normalize = (x: Seq[(Int, Double, Double)]) => x.sorted
      )

      test("nthValue") - checker(
        query = Text {
          Purchase.select.map(p =>
            (
              p.shippingInfoId,
              p.total,
              nthValue(p.total, 2).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
            )
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            NTH_VALUE(purchase0.total, ?) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Double)](
          (1, 15.7, 0.0),
          (1, 888.0, 888.0),
          (1, 900.0, 888.0),
          (2, 493.8, 0.0),
          (2, 10000.0, 10000.0),
          (3, 1.3, 0.0),
          (3, 44.4, 44.4)
        ),
        normalize = (x: Seq[(Int, Double, Double)]) => x.sorted
      )
    }
    test("aggregate"){

      test("sumBy") - checker(
        query = Text {
          Purchase.select.mapAggregate((p, ps) =>
            (
              p.shippingInfoId,
              p.total,
              ps.sumBy(_.total).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
            )
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            SUM(purchase0.total) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Double)](
          (1, 15.7, 15.7),
          (1, 888.0, 903.7),
          (1, 900.0, 1803.7),
          (2, 493.8, 493.8),
          (2, 10000.0, 10493.8),
          (3, 1.3, 1.3),
          (3, 44.4, 45.699999999999996)
        ),
        docs = """
          You can use `.mapAggregate` to use aggregate functions as window function
        """,
        normalize = (x: Seq[(Int, Double, Double)]) => x.sortBy(t => (t._1, t._2))
      )

      test("avgBy") - checker(
        query = Text {
          Purchase.select.mapAggregate((p, ps) =>
            (
              p.shippingInfoId,
              p.total,
              ps.avgBy(_.total).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
            )
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            AVG(purchase0.total) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Double)](
          (1, 15.7, 15.7),
          (1, 888.0, 451.85),
          (1, 900.0, 601.2333333333333),
          (2, 493.8, 493.8),
          (2, 10000.0, 5246.9),
          (3, 1.3, 1.3),
          (3, 44.4, 22.849999999999998)
        ),
        docs = """
          Window functions like `rank()` are supported. You can use the `.over`, `.partitionBy`,
          and `.sortBy`
        """,
        normalize = (x: Seq[(Int, Double, Double)]) => x.sortBy(t => (t._1, t._2))
      )
    }
    test("frames"){
      test("sumBy") - checker(
        query = Text {
          Purchase.select.mapAggregate((p, ps) =>
            (
              p.shippingInfoId,
              p.total,
              ps.sumBy(_.total).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc.frameStart.preceding().frameEnd.following().frameExclusion.currentRow
            )
          )
        },
        sql = """
          SELECT
            purchase0.shipping_info_id AS res__0,
            purchase0.total AS res__1,
            SUM(purchase0.total)
            OVER (PARTITION BY purchase0.shipping_info_id
              ORDER BY purchase0.total ASC
              ROWS BETWEEN UNBOUNDED PRECEDING
              AND UNBOUNDED FOLLOWING EXCLUDE CURRENT ROW) AS res__2
          FROM purchase purchase0
        """,
        value = Seq[(Int, Double, Double)](
          (1, 15.7, 1788.0),
          (1, 888.0, 915.7),
          (1, 900.0, 903.7),
          (2, 493.8, 10000.0),
          (2, 10000.0, 493.8),
          (3, 1.3, 44.4),
          (3, 44.4, 1.3)
        ),
        normalize = (x: Seq[(Int, Double, Double)]) => x.sortBy(t => (t._1, t._2))
      )

    }
  }

}
