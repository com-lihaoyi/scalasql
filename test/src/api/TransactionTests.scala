package scaalsql.api

import scalasql.Purchase
import scalasql.utils.ScalaSqlSuite
import utest._

trait TransactionTests extends ScalaSqlSuite {
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

      test("explicitrollback") - {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_ => true)) ==> 7

          db.run(Purchase.select.size) ==> 0

          db.rollback()

          db.run(Purchase.select.size) ==> 7
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 7
      }

      test("throwrollback") - {

        try {
          checker.db.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_ => true)) ==> 7

            db.run(Purchase.select.size) ==> 0

            throw new FooException
          }
        } catch { case e: FooException => /*donothing*/ }

        checker.db.autoCommit.run(Purchase.select.size) ==> 7
      }
    }

    test("subtransaction") {
      test("commitSubtransaction") {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 3)) ==> 3
          db.run(Purchase.select.size) ==> 4

          db.savepointTransaction { sp =>
            db.run(Purchase.delete(_ => true)) ==> 4
            db.run(Purchase.select.size) ==> 0
          }

          db.run(Purchase.select.size) ==> 0
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 0
      }

      test("throwSubtransactionrollback") {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 3)) ==> 3
          db.run(Purchase.select.size) ==> 4

          try {
            db.savepointTransaction { sp =>
              db.run(Purchase.delete(_ => true)) ==> 4
              db.run(Purchase.select.size) ==> 0
              throw new FooException
            }
          } catch {
            case e: FooException => /*donothing*/
          }

          db.run(Purchase.select.size) ==> 4
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 4
      }

      test("throwDoublerollback") {
        try {
          checker.db.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 3)) ==> 3
            db.run(Purchase.select.size) ==> 4

            try {
              db.savepointTransaction { sp =>
                db.run(Purchase.delete(_ => true)) ==> 4
                db.run(Purchase.select.size) ==> 0
                throw new FooException
              }
            } catch {
              case e: FooException =>
                db.run(Purchase.select.size) ==> 4
                throw e
            }

            db.run(Purchase.select.size) ==> 4
          }
        } catch {
          case e: FooException => /*donothing*/
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 7
      }

      test("explicitDoublerollback") {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 3)) ==> 3
          db.run(Purchase.select.size) ==> 4

          db.savepointTransaction { sp =>
            db.run(Purchase.delete(_ => true)) ==> 4
            db.run(Purchase.select.size) ==> 0
            db.rollback()
          }

          db.run(Purchase.select.size) ==> 7
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 7
      }
    }

    test("doubleSubtransactionThrowrollback") {
      test("inner") {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 2)) ==> 2
          db.run(Purchase.select.size) ==> 5

          db.savepointTransaction { sp1 =>
            db.run(Purchase.delete(_.id <= 4)) ==> 2
            db.run(Purchase.select.size) ==> 3

            try {
              db.savepointTransaction { sp2 =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
                throw new FooException
              }
            } catch { case e: FooException => /*donothing*/ }

            db.run(Purchase.select.size) ==> 3
          }

          db.run(Purchase.select.size) ==> 3
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 3
      }

      test("middle") {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 2)) ==> 2
          db.run(Purchase.select.size) ==> 5

          try {
            db.savepointTransaction { sp1 =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              db.savepointTransaction { sp2 =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
              }

              db.run(Purchase.select.size) ==> 1
              throw new FooException
            }
          } catch { case e: FooException => /*donothing*/ }

          db.run(Purchase.select.size) ==> 5
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 5
      }

      test("innerAndMiddle") {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 2)) ==> 2
          db.run(Purchase.select.size) ==> 5

          try {
            db.savepointTransaction { sp1 =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              db.savepointTransaction { sp2 =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
                throw new FooException
              }
            }
          } catch { case e: FooException => /*donothing*/ }

          db.run(Purchase.select.size) ==> 5
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 5
      }

      test("middleAndOuter") {
        try {
          checker.db.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepointTransaction { sp1 =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              db.savepointTransaction { sp2 =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
              }
              db.run(Purchase.select.size) ==> 1
              throw new FooException
            }
          }
        } catch { case e: FooException => /*donothing*/ }

        checker.db.autoCommit.run(Purchase.select.size) ==> 7
      }

      test("innerAndMiddleAndOuter") {
        try {
          checker.db.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepointTransaction { sp1 =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              db.savepointTransaction { sp2 =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
                throw new FooException
              }
            }

            db.run(Purchase.select.size) ==> 5
          }
        } catch { case e: FooException => /*donothing*/ }

        checker.db.autoCommit.run(Purchase.select.size) ==> 7
      }
    }
    test("doubleSubtransactionExplicitrollback") {
      test("inner") {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 2)) ==> 2
          db.run(Purchase.select.size) ==> 5

          db.savepointTransaction { sp1 =>
            db.run(Purchase.delete(_.id <= 4)) ==> 2
            db.run(Purchase.select.size) ==> 3

            db.savepointTransaction { sp2 =>
              db.run(Purchase.delete(_.id <= 6)) ==> 2
              db.run(Purchase.select.size) ==> 1
              sp2.rollback()
            }

            db.run(Purchase.select.size) ==> 3
          }

          db.run(Purchase.select.size) ==> 3
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 3
      }

      test("middle") {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 2)) ==> 2
          db.run(Purchase.select.size) ==> 5

          db.savepointTransaction { sp1 =>
            db.run(Purchase.delete(_.id <= 4)) ==> 2
            db.run(Purchase.select.size) ==> 3

            db.savepointTransaction { sp2 =>
              db.run(Purchase.delete(_.id <= 6)) ==> 2
              db.run(Purchase.select.size) ==> 1
            }

            db.run(Purchase.select.size) ==> 1
            sp1.rollback()
            db.run(Purchase.select.size) ==> 5
          }

          db.run(Purchase.select.size) ==> 5
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 5
      }

      test("innerAndMiddle") {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 2)) ==> 2
          db.run(Purchase.select.size) ==> 5

          db.savepointTransaction { sp1 =>
            db.run(Purchase.delete(_.id <= 4)) ==> 2
            db.run(Purchase.select.size) ==> 3

            db.savepointTransaction { sp2 =>
              db.run(Purchase.delete(_.id <= 6)) ==> 2
              db.run(Purchase.select.size) ==> 1
              sp1.rollback()
              db.run(Purchase.select.size) ==> 5
            }
            db.run(Purchase.select.size) ==> 5
          }

          db.run(Purchase.select.size) ==> 5
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 5
      }

      test("middleAndOuter") {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 2)) ==> 2
          db.run(Purchase.select.size) ==> 5

          db.savepointTransaction { sp1 =>
            db.run(Purchase.delete(_.id <= 4)) ==> 2
            db.run(Purchase.select.size) ==> 3

            db.savepointTransaction { sp2 =>
              db.run(Purchase.delete(_.id <= 6)) ==> 2
              db.run(Purchase.select.size) ==> 1
            }

            db.run(Purchase.select.size) ==> 1
            db.rollback()
            db.run(Purchase.select.size) ==> 7
          }
          db.run(Purchase.select.size) ==> 7
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 7
      }

      test("innerAndMiddleAndOuter") {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 2)) ==> 2
          db.run(Purchase.select.size) ==> 5

          db.savepointTransaction { sp1 =>
            db.run(Purchase.delete(_.id <= 4)) ==> 2
            db.run(Purchase.select.size) ==> 3

            db.savepointTransaction { sp2 =>
              db.run(Purchase.delete(_.id <= 6)) ==> 2
              db.run(Purchase.select.size) ==> 1
              db.rollback()
              db.run(Purchase.select.size) ==> 7
            }
            db.run(Purchase.select.size) ==> 7
          }

          db.run(Purchase.select.size) ==> 7
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 7
      }
    }
  }
}
