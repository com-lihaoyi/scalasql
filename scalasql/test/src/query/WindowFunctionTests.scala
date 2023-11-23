package scalasql.query

import scalasql._
import sourcecode.Text
import utest._
import utils.ScalaSqlSuite

import java.time.LocalDate

trait WindowFunctionTests extends ScalaSqlSuite {
  def description = "Window functions using `OVER`"

  def tests = Tests {
    test("rank") - checker(
      query = Text {
        Purchase.select
          .map(p => (p.shippingInfoId, p.total, rank().over.partitionBy(p.shippingInfoId).sortBy(p.total).asc))
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

  }
}
