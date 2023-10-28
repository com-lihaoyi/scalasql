package scaalsql.api

import scalasql.{Buyer, Purchase}
import scalasql.utils.ScalaSqlSuite
import utest._

trait TransactionTests extends ScalaSqlSuite{

  def tests = Tests {
    test("test") - {
      checker.db.transaction{ implicit db =>
        db.run(Purchase.select.size) ==> 7

        db.run(Purchase.delete(_ => true)) ==> 7

        db.run(Purchase.select.size) ==> 0

        db.rollBack()

        db.run(Purchase.select.size) ==> 7
      }
    }
  }
}
