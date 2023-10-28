package scaalsql.api

import scalasql.Purchase
import scalasql.utils.ScalaSqlSuite
import utest._

trait TransactionTests extends ScalaSqlSuite{
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  def tests = Tests {
    test("commit") - {
      checker.db.transaction{ implicit db =>
        db.run(Purchase.select.size) ==> 7

        db.run(Purchase.delete(_ => true)) ==> 7

        db.run(Purchase.select.size) ==> 0
      }

      checker.db.autoCommit.run(Purchase.select.size) ==> 0
    }

    test("explicitRollback") - {
      checker.db.transaction{ implicit db =>
        db.run(Purchase.select.size) ==> 7

        db.run(Purchase.delete(_ => true)) ==> 7

        db.run(Purchase.select.size) ==> 0

        db.rollBack()

        db.run(Purchase.select.size) ==> 7
      }

      checker.db.autoCommit.run(Purchase.select.size) ==> 7
    }

    test("throwRollback") - {
      class FooException extends Exception
      try checker.db.transaction{ implicit db =>
        db.run(Purchase.select.size) ==> 7

        db.run(Purchase.delete(_ => true)) ==> 7

        db.run(Purchase.select.size) ==> 0

        throw new FooException()

      }catch{case e: FooException => /*donothing*/}

      checker.db.autoCommit.run(Purchase.select.size) ==> 7
    }
  }
}
