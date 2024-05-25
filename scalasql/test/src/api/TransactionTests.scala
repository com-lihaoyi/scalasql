package scalasql.api

import scalasql.Purchase
import scalasql.utils.{ScalaSqlSuite, SqliteSuite}
import sourcecode.Text
import utest._

trait TransactionTests extends ScalaSqlSuite {
  def description =
    "Usage of transactions, rollbacks, and savepoints"

  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  class FooException extends Exception

  def tests = Tests {
    test("simple") {
      test("commit") - checker.recorded(
        """
        Common workflow to create a transaction and run a `delete` inside of it. The effect
        of the `delete` is visible both inside the transaction and outside after the
        transaction completes successfully and commits
        """,
        Text {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_ => true)) ==> 7

            db.run(Purchase.select.size) ==> 0
          }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 0
        }
      )
      test("isolation") - {
        // Sqlite doesn't support configuring transaction isolation levels
        if (!this.isInstanceOf[SqliteSuite]) {
          checker.recorded(
            """
            You can use `.updateRaw` to perform `SET TRANSACTION ISOLATION LEVEL` commands,
            allowing you to configure the isolation and performance characteristics of
            concurrent transactions in your database
            """,
            Text {
              dbClient.transaction { implicit db =>
                db.updateRaw("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE")

                db.run(Purchase.select.size) ==> 7

                db.run(Purchase.delete(_ => true)) ==> 7

                db.run(Purchase.select.size) ==> 0
              }

              dbClient.transaction(_.run(Purchase.select.size)) ==> 0
            }
          )
        }
      }

      test("rollback") - checker.recorded(
        """
        Example of explicitly rolling back a transaction using the `db.rollback()` method.
        After rollback, the effects of the `delete` query are undone, and subsequent `select`
        queries can see the previously-deleted entries both inside and outside the transaction
        """,
        Text {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_ => true)) ==> 7

            db.run(Purchase.select.size) ==> 0

            db.rollback()

            db.run(Purchase.select.size) ==> 7
          }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 7
        }
      )

      test("throw") - checker.recorded(
        """
        Transactions are also rolled back if they terminate with an uncaught exception
        """,
        Text {

          try {
            dbClient.transaction { implicit db =>
              db.run(Purchase.select.size) ==> 7

              db.run(Purchase.delete(_ => true)) ==> 7

              db.run(Purchase.select.size) ==> 0

              throw new FooException
            }
          } catch { case e: FooException => /*donothing*/ }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 7
        }
      )
    }

    test("savepoint") {
      test("commit") - checker.recorded(
        """
        Savepoints are like "sub" transactions: they let you declare a savepoint
        and roll back any changes to the savepoint later. If a savepoint block
        completes successfully, the savepoint changes are committed ("released")
        and remain visible later in the transaction and outside of it
        """,
        Text {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 3)) ==> 3
            db.run(Purchase.select.size) ==> 4

            db.savepoint { _ =>
              db.run(Purchase.delete(_ => true)) ==> 4
              db.run(Purchase.select.size) ==> 0
            }

            db.run(Purchase.select.size) ==> 0
          }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 0
        }
      )
      test("rollback") - checker.recorded(
        """
        Like transactions, savepoints support the `.rollback()` method, to undo any
        changes since the start of the savepoint block.
        """,
        Text {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 3)) ==> 3
            db.run(Purchase.select.size) ==> 4

            db.savepoint { sp =>
              db.run(Purchase.delete(_ => true)) ==> 4
              db.run(Purchase.select.size) ==> 0
              sp.rollback()
              db.run(Purchase.select.size) ==> 4
            }

            db.run(Purchase.select.size) ==> 4
          }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 4
        }
      )

      test("throw") - checker.recorded(
        """
        Savepoints also roll back their enclosed changes automatically if they
        terminate with an uncaught exception
        """,
        Text {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 3)) ==> 3
            db.run(Purchase.select.size) ==> 4

            try {
              db.savepoint { _ =>
                db.run(Purchase.delete(_ => true)) ==> 4
                db.run(Purchase.select.size) ==> 0
                throw new FooException
              }
            } catch {
              case e: FooException => /*donothing*/
            }

            db.run(Purchase.select.size) ==> 4
          }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 4
        }
      )

      test("throwDouble") {
        try {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 3)) ==> 3
            db.run(Purchase.select.size) ==> 4

            try {
              db.savepoint { _ =>
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

        dbClient.transaction(_.run(Purchase.select.size)) ==> 7
      }

      test("rollbackDouble") {
        dbClient.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 3)) ==> 3
          db.run(Purchase.select.size) ==> 4

          db.savepoint { _ =>
            db.run(Purchase.delete(_ => true)) ==> 4
            db.run(Purchase.select.size) ==> 0
            db.rollback()
          }

          db.run(Purchase.select.size) ==> 7
        }

        dbClient.transaction(_.run(Purchase.select.size)) ==> 7
      }
    }

    test("doubleSavepoint") {

      test("commit") - checker.recorded(
        """
        Only one transaction can be present at a time, but savepoints can be arbitrarily nested.
        Uncaught exceptions or explicit `.rollback()` calls would roll back changes done during
        the inner savepoint/transaction blocks, while leaving changes applied during outer
        savepoint/transaction blocks in-place
        """,
        Text {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepoint { _ =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              db.savepoint { _ =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
              }

              db.run(Purchase.select.size) ==> 1
            }

            db.run(Purchase.select.size) ==> 1
          }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 1
        }
      )

      test("throw") {
        test("inner") {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepoint { _ =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              try {
                db.savepoint { _ =>
                  db.run(Purchase.delete(_.id <= 6)) ==> 2
                  db.run(Purchase.select.size) ==> 1
                  throw new FooException
                }
              } catch { case e: FooException => /*donothing*/ }

              db.run(Purchase.select.size) ==> 3
            }

            db.run(Purchase.select.size) ==> 3
          }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 3
        }

        test("middle") {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            try {
              db.savepoint { _ =>
                db.run(Purchase.delete(_.id <= 4)) ==> 2
                db.run(Purchase.select.size) ==> 3

                db.savepoint { _ =>
                  db.run(Purchase.delete(_.id <= 6)) ==> 2
                  db.run(Purchase.select.size) ==> 1
                }

                db.run(Purchase.select.size) ==> 1
                throw new FooException
              }
            } catch { case e: FooException => /*donothing*/ }

            db.run(Purchase.select.size) ==> 5
          }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 5
        }

        test("innerMiddle") {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            try {
              db.savepoint { _ =>
                db.run(Purchase.delete(_.id <= 4)) ==> 2
                db.run(Purchase.select.size) ==> 3

                db.savepoint { _ =>
                  db.run(Purchase.delete(_.id <= 6)) ==> 2
                  db.run(Purchase.select.size) ==> 1
                  throw new FooException
                }
              }
            } catch { case e: FooException => /*donothing*/ }

            db.run(Purchase.select.size) ==> 5
          }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 5
        }

        test("middleOuter") {
          try {
            dbClient.transaction { implicit db =>
              db.run(Purchase.select.size) ==> 7

              db.run(Purchase.delete(_.id <= 2)) ==> 2
              db.run(Purchase.select.size) ==> 5

              db.savepoint { _ =>
                db.run(Purchase.delete(_.id <= 4)) ==> 2
                db.run(Purchase.select.size) ==> 3

                db.savepoint { _ =>
                  db.run(Purchase.delete(_.id <= 6)) ==> 2
                  db.run(Purchase.select.size) ==> 1
                }
                db.run(Purchase.select.size) ==> 1
                throw new FooException
              }
            }
          } catch { case e: FooException => /*donothing*/ }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 7
        }

        test("innerMiddleOuter") {
          try {
            dbClient.transaction { implicit db =>
              db.run(Purchase.select.size) ==> 7

              db.run(Purchase.delete(_.id <= 2)) ==> 2
              db.run(Purchase.select.size) ==> 5

              db.savepoint { _ =>
                db.run(Purchase.delete(_.id <= 4)) ==> 2
                db.run(Purchase.select.size) ==> 3

                db.savepoint { _ =>
                  db.run(Purchase.delete(_.id <= 6)) ==> 2
                  db.run(Purchase.select.size) ==> 1
                  throw new FooException
                }
              }

              db.run(Purchase.select.size) ==> 5
            }
          } catch { case e: FooException => /*donothing*/ }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 7
        }
      }

      test("rollback") {
        test("inner") {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepoint { _ =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              db.savepoint { sp2 =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
                sp2.rollback()
              }

              db.run(Purchase.select.size) ==> 3
            }

            db.run(Purchase.select.size) ==> 3
          }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 3
        }

        test("middle") {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepoint { sp1 =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              db.savepoint { _ =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
              }

              db.run(Purchase.select.size) ==> 1
              sp1.rollback()
              db.run(Purchase.select.size) ==> 5
            }

            db.run(Purchase.select.size) ==> 5
          }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 5
        }

        test("innerMiddle") {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepoint { sp1 =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              db.savepoint { _ =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
                sp1.rollback()
                db.run(Purchase.select.size) ==> 5
              }
              db.run(Purchase.select.size) ==> 5
            }

            db.run(Purchase.select.size) ==> 5
          }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 5
        }

        test("middleOuter") {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepoint { _ =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              db.savepoint { _ =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
              }

              db.run(Purchase.select.size) ==> 1
              db.rollback()
              db.run(Purchase.select.size) ==> 7
            }
            db.run(Purchase.select.size) ==> 7
          }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 7
        }

        test("innerMiddleOuter") {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepoint { _ =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              db.savepoint { _ =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
                db.rollback()
                db.run(Purchase.select.size) ==> 7
              }
              db.run(Purchase.select.size) ==> 7
            }

            db.run(Purchase.select.size) ==> 7
          }

          dbClient.transaction(_.run(Purchase.select.size)) ==> 7
        }
      }
    }
  }
}
