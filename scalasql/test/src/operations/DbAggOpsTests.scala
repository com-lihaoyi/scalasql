package scalasql.operations

import scalasql._
import scalasql.H2Dialect
import utest._
import utils.ScalaSqlSuite

trait ExprAggOpsTests extends ScalaSqlSuite {
  def description = "Operations that can be performed on `Expr[Seq[_]]`"
  def tests = Tests {
    test("size") - checker(
      query = Purchase.select.size,
      sql = "SELECT COUNT(1) AS res FROM purchase purchase0",
      value = 7
    )

    test("sumBy") {
      test("simple") - checker(
        query = Purchase.select.sumBy(_.count),
        sql = "SELECT SUM(purchase0.count) AS res FROM purchase purchase0",
        value = 140
      )

      test("some") - checker(
        query = Purchase.select.sumByOpt(_.count),
        sql = "SELECT SUM(purchase0.count) AS res FROM purchase purchase0",
        value = Option(140)
      )

      test("none") - checker(
        query = Purchase.select.filter(_ => false).sumByOpt(_.count),
        sql = "SELECT SUM(purchase0.count) AS res FROM purchase purchase0 WHERE ?",
        value = Option.empty[Int]
      )
    }

    test("minBy") {
      test("simple") - checker(
        query = Purchase.select.minBy(_.count),
        sql = "SELECT MIN(purchase0.count) AS res FROM purchase purchase0",
        value = 3
      )

      test("some") - checker(
        query = Purchase.select.minByOpt(_.count),
        sql = "SELECT MIN(purchase0.count) AS res FROM purchase purchase0",
        value = Option(3)
      )

      test("none") - checker(
        query = Purchase.select.filter(_ => false).minByOpt(_.count),
        sql = "SELECT MIN(purchase0.count) AS res FROM purchase purchase0 WHERE ?",
        value = Option.empty[Int]
      )
    }

    test("maxBy") {
      test("simple") - checker(
        query = Purchase.select.maxBy(_.count),
        sql = "SELECT MAX(purchase0.count) AS res FROM purchase purchase0",
        value = 100
      )

      test("some") - checker(
        query = Purchase.select.maxByOpt(_.count),
        sql = "SELECT MAX(purchase0.count) AS res FROM purchase purchase0",
        value = Option(100)
      )

      test("none") - checker(
        query = Purchase.select.filter(_ => false).maxByOpt(_.count),
        sql = "SELECT MAX(purchase0.count) AS res FROM purchase purchase0 WHERE ?",
        value = Option.empty[Int]
      )
    }

    test("avgBy") {
      test("simple") - checker(
        query = Purchase.select.avgBy(_.count),
        sql = "SELECT AVG(purchase0.count) AS res FROM purchase purchase0",
        value = 20
      )

      test("some") - checker(
        query = Purchase.select.avgByOpt(_.count),
        sql = "SELECT AVG(purchase0.count) AS res FROM purchase purchase0",
        value = Option(20)
      )

      test("none") - checker(
        query = Purchase.select.filter(_ => false).avgByOpt(_.count),
        sql = "SELECT AVG(purchase0.count) AS res FROM purchase purchase0 WHERE ?",
        value = Option.empty[Int]
      )
    }
    test("mkString") {
      test("simple") - checker(
        query = Buyer.select.map(_.name).mkString(),
        sqls = Seq(
          "SELECT STRING_AGG(buyer0.name || '', '') AS res FROM buyer buyer0",
          "SELECT GROUP_CONCAT(buyer0.name || '', '') AS res FROM buyer buyer0",
          "SELECT LISTAGG(buyer0.name || '', '') AS res FROM buyer buyer0",
          "SELECT GROUP_CONCAT(CONCAT(buyer0.name, '') SEPARATOR '') AS res FROM buyer buyer0"
        ),
        value = "James Bond叉烧包Li Haoyi"
      )

      test("sep") - {
        if (!this.isInstanceOf[H2Dialect])
          checker(
            query = Buyer.select.map(_.name).mkString(", "),
            sqls = Seq(
              "SELECT STRING_AGG(buyer0.name || '', ?) AS res FROM buyer buyer0",
              "SELECT GROUP_CONCAT(buyer0.name || '', ?) AS res FROM buyer buyer0",
              "SELECT GROUP_CONCAT(CONCAT(buyer0.name, '') SEPARATOR ?) AS res FROM buyer buyer0"
            ),
            value = "James Bond, 叉烧包, Li Haoyi"
          )
      }
    }
  }
}
