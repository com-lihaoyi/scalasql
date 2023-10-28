package scaalsql.api

import scalasql.Purchase
import scalasql.utils.ScalaSqlSuite
import utest._

trait TransactionTests extends ScalaSqlSuite{
  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  class FooException extends Exception

  def tests = Tests {
    test("simple") {
      test("commit") - {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_ => true)) ==> 7

          db.run(Purchase.select.size) ==> 0
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 0
      }

      test("explicitRollback") - {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_ => true)) ==> 7

          db.run(Purchase.select.size) ==> 0

          db.rollBack()

          db.run(Purchase.select.size) ==> 7
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 7
      }

      test("throwRollback") - {

        try checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_ => true)) ==> 7

          db.run(Purchase.select.size) ==> 0

          throw new FooException
        } catch {
          case e: FooException => /*donothing*/
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 7
      }
    }

    test("subtransactions") {
      test("commit") {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 3)) ==> 3
          db.run(Purchase.select.size) ==> 4

          db.transaction {
            db.run(Purchase.delete(_ => true)) ==> 4
            db.run(Purchase.select.size) ==> 0
          }

          db.run(Purchase.select.size) ==> 0
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 0
      }

      test("throwRollback") {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 3)) ==> 3
          db.run(Purchase.select.size) ==> 4

          try db.transaction {
            db.run(Purchase.delete(_ => true)) ==> 4
            db.run(Purchase.select.size) ==> 0
            throw new FooException
          } catch {
            case e: FooException => /*donothing*/
          }

          db.run(Purchase.select.size) ==> 4
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 4
      }

      test("throwDoubleRollback") {
        try checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 3)) ==> 3
          db.run(Purchase.select.size) ==> 4

          try db.transaction {
            db.run(Purchase.delete(_ => true)) ==> 4
            db.run(Purchase.select.size) ==> 0
            throw new FooException
          } catch {
            case e: FooException =>
              db.run(Purchase.select.size) ==> 4
              throw e
          }

          db.run(Purchase.select.size) ==> 4
        } catch {
          case e: FooException => /*donothing*/
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 7
      }
    }
  }
}
