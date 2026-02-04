package scalasql.api

import scalasql.Purchase
import scalasql.utils.{ScalaSqlSuite, SqliteSuite}
import scalasql.DbApi
import sourcecode.Text
import utest._

trait TransactionTests extends ScalaSqlSuite {
  def description =
    "Usage of transactions, rollbacks, and savepoints"

  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  class FooException extends Exception

  class ListenerException(message: String) extends Exception(message)

  class StubTransactionListener(
      throwOnBeforeCommit: Boolean = false,
      throwOnAfterCommit: Boolean = false,
      throwOnBeforeRollback: Boolean = false,
      throwOnAfterRollback: Boolean = false
  ) extends DbApi.TransactionListener {
    var beginCalled = false
    var beforeCommitCalled = false
    var afterCommitCalled = false
    var beforeRollbackCalled = false
    var afterRollbackCalled = false

    override def begin(): Unit = {
      beginCalled = true
    }

    override def beforeCommit(): Unit = {
      beforeCommitCalled = true
      if (throwOnBeforeCommit) throw new ListenerException("beforeCommit")
    }
    override def afterCommit(): Unit = {
      afterCommitCalled = true
      if (throwOnAfterCommit) throw new ListenerException("afterCommit")
    }
    override def beforeRollback(): Unit = {
      beforeRollbackCalled = true
      if (throwOnBeforeRollback) throw new ListenerException("beforeRollback")
    }
    override def afterRollback(): Unit = {
      afterRollbackCalled = true
      if (throwOnAfterRollback) throw new ListenerException("afterRollback")
    }
  }

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
          } catch {
            case e: FooException => /*donothing*/
          }

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
              } catch {
                case e: FooException => /*donothing*/
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
            } catch {
              case e: FooException => /*donothing*/
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
            } catch {
              case e: FooException => /*donothing*/
            }

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
          } catch {
            case e: FooException => /*donothing*/
          }

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
          } catch {
            case e: FooException => /*donothing*/
          }

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

    test("useBlock") - checker.recorded(
      """
      Both `transaction` and `savepoint` accessors are actually returning a special `UseBlock[...]` types.
      When you call `transaction(use)` it desugars into `transaction.apply(use)` that provides a default
      resource-style management - it creates a transaction (or savepoint), runs `use` block immediately, then releases
      (commits or rolls back) the transaction.

      If you need more control over the lifecycle, you may use transaction.allocate() method that returns
      `(resource, releaseFunction)` pair.
      This is especially useful when delaying side-effects with FP libraries like cats-effect or ZIO.
      **Important:** `allocate()` is impure and must be delayed in that case also.
      The `dbClient.transaction` expression itself does not perform any side-effects, it just creates closures.
      """,
      Text {
        val transactionBlock: scalasql.core.UseBlock[DbApi.Txn] = dbClient.transaction

        def createTxWithExitCallback(): (DbApi.Txn, Option[Throwable] => Unit) =
          transactionBlock.allocate()
      }
    )

    test("listener") {
      test("beforeCommit and afterCommit are called under normal circumstances") {
        val listener = new StubTransactionListener()
        dbClient.withTransactionListener(listener).transaction { _ =>
          // do nothing
        }
        listener.beginCalled ==> true
        listener.beforeCommitCalled ==> true
        listener.afterCommitCalled ==> true
        listener.beforeRollbackCalled ==> false
        listener.afterRollbackCalled ==> false
      }

      test("if beforeCommit causes an exception, {before,after}Rollback are called") {
        val listener = new StubTransactionListener(throwOnBeforeCommit = true)
        val e = intercept[ListenerException] {
          dbClient.transaction { implicit txn =>
            txn.addTransactionListener(listener)
          }
        }
        e.getMessage ==> "beforeCommit"
        listener.beforeCommitCalled ==> true
        listener.afterCommitCalled ==> false
        listener.beforeRollbackCalled ==> true
        listener.afterRollbackCalled ==> true
      }

      test("if afterCommit causes an exception, the exception is propagated") {
        val listener = new StubTransactionListener(throwOnAfterCommit = true)
        val e = intercept[ListenerException] {
          dbClient.transaction { implicit txn =>
            txn.addTransactionListener(listener)
          }
        }
        e.getMessage ==> "afterCommit"
        listener.beforeCommitCalled ==> true
        listener.afterCommitCalled ==> true
        listener.beforeRollbackCalled ==> false
        listener.afterRollbackCalled ==> false
      }

      test("if beforeRollback causes an exception, afterRollback is still called") {
        val listener = new StubTransactionListener(throwOnBeforeRollback = true)
        val e = intercept[FooException] {
          dbClient.transaction { implicit txn =>
            txn.addTransactionListener(listener)
            throw new FooException()
          }
        }
        e.getSuppressed.head.getMessage ==> "beforeRollback"
        listener.beforeCommitCalled ==> false
        listener.afterCommitCalled ==> false
        listener.beforeRollbackCalled ==> true
        listener.afterRollbackCalled ==> true
      }

      test("if afterRollback causes an exception, the exception is propagated") {
        val listener = new StubTransactionListener(throwOnAfterRollback = true)
        val e = intercept[FooException] {
          dbClient.transaction { implicit txn =>
            txn.addTransactionListener(listener)
            throw new FooException()
          }
        }
        e.getSuppressed.head.getMessage ==> "afterRollback"
        listener.beforeCommitCalled ==> false
        listener.afterCommitCalled ==> false
        listener.beforeRollbackCalled ==> true
        listener.afterRollbackCalled ==> true
      }
    }
  }
}
