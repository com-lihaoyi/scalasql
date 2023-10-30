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

      test("rollback") - {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_ => true)) ==> 7

          db.run(Purchase.select.size) ==> 0

          db.rollback()

          db.run(Purchase.select.size) ==> 7
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 7
      }

      test("throw") - {

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

    test("savepoint") {
      test("commit") {
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

      test("throw") {
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
      test("rollback") {
        checker.db.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 3)) ==> 3
          db.run(Purchase.select.size) ==> 4

          db.savepointTransaction { sp =>
            db.run(Purchase.delete(_ => true)) ==> 4
            db.run(Purchase.select.size) ==> 0
            sp.rollback()
            db.run(Purchase.select.size) ==> 4
          }

          db.run(Purchase.select.size) ==> 4
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 4
      }

      test("throwDouble") {
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

      test("rollbackDouble") {
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

    test("doubleSavepoint") {

      test("commit") {
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
          }

          db.run(Purchase.select.size) ==> 1
        }

        checker.db.autoCommit.run(Purchase.select.size) ==> 1
      }

      test("throw") {
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

        test("innerMiddle") {
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

        test("middleOuter") {
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

        test("innerMiddleOuter") {
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

      test("rollback") {
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

        test("innerMiddle") {
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

        test("middleOuter") {
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

        test("innerMiddleOuter") {
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
}
