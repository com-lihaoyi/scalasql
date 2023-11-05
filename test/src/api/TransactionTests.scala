package scalasql.api

import scalasql.Purchase
import scalasql.utils.ScalaSqlSuite
import sourcecode.Text
import utest._

trait TransactionTests extends ScalaSqlSuite {
  def description =
    "Usage of transactions, rollbacks, and savepoints"

  override def utestBeforeEach(path: Seq[String]): Unit = checker.reset()
  class FooException extends Exception

  def tests = Tests {
    test("simple") {
      test("commit") - checker.recorded(Text {
        dbClient.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_ => true)) ==> 7

          db.run(Purchase.select.size) ==> 0
        }

        dbClient.autoCommit.run(Purchase.select.size) ==> 0
      })

      test("rollback") - checker.recorded(Text {
        dbClient.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_ => true)) ==> 7

          db.run(Purchase.select.size) ==> 0

          db.rollback()

          db.run(Purchase.select.size) ==> 7
        }

        dbClient.autoCommit.run(Purchase.select.size) ==> 7
      })

      test("throw") - checker.recorded(Text {

        try {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_ => true)) ==> 7

            db.run(Purchase.select.size) ==> 0

            throw new FooException
          }
        } catch { case e: FooException => /*donothing*/ }

        dbClient.autoCommit.run(Purchase.select.size) ==> 7
      })
    }

    test("savepoint") {
      test("commit") - checker.recorded(Text {
        dbClient.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 3)) ==> 3
          db.run(Purchase.select.size) ==> 4

          db.savepoint { sp =>
            db.run(Purchase.delete(_ => true)) ==> 4
            db.run(Purchase.select.size) ==> 0
          }

          db.run(Purchase.select.size) ==> 0
        }

        dbClient.autoCommit.run(Purchase.select.size) ==> 0
      })

      test("throw") - checker.recorded(Text {
        dbClient.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 3)) ==> 3
          db.run(Purchase.select.size) ==> 4

          try {
            db.savepoint { sp =>
              db.run(Purchase.delete(_ => true)) ==> 4
              db.run(Purchase.select.size) ==> 0
              throw new FooException
            }
          } catch {
            case e: FooException => /*donothing*/
          }

          db.run(Purchase.select.size) ==> 4
        }

        dbClient.autoCommit.run(Purchase.select.size) ==> 4
      })
      test("rollback") - checker.recorded(Text {
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

        dbClient.autoCommit.run(Purchase.select.size) ==> 4
      })

      test("throwDouble") - checker.recorded(Text {
        try {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 3)) ==> 3
            db.run(Purchase.select.size) ==> 4

            try {
              db.savepoint { sp =>
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

        dbClient.autoCommit.run(Purchase.select.size) ==> 7
      })

      test("rollbackDouble") - checker.recorded(Text {
        dbClient.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 3)) ==> 3
          db.run(Purchase.select.size) ==> 4

          db.savepoint { sp =>
            db.run(Purchase.delete(_ => true)) ==> 4
            db.run(Purchase.select.size) ==> 0
            db.rollback()
          }

          db.run(Purchase.select.size) ==> 7
        }

        dbClient.autoCommit.run(Purchase.select.size) ==> 7
      })
    }

    test("doubleSavepoint") {

      test("commit") - checker.recorded(Text {
        dbClient.transaction { implicit db =>
          db.run(Purchase.select.size) ==> 7

          db.run(Purchase.delete(_.id <= 2)) ==> 2
          db.run(Purchase.select.size) ==> 5

          db.savepoint { sp1 =>
            db.run(Purchase.delete(_.id <= 4)) ==> 2
            db.run(Purchase.select.size) ==> 3

            db.savepoint { sp2 =>
              db.run(Purchase.delete(_.id <= 6)) ==> 2
              db.run(Purchase.select.size) ==> 1
            }

            db.run(Purchase.select.size) ==> 1
          }

          db.run(Purchase.select.size) ==> 1
        }

        dbClient.autoCommit.run(Purchase.select.size) ==> 1
      })

      test("throw") {
        test("inner") - checker.recorded(Text {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepoint { sp1 =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              try {
                db.savepoint { sp2 =>
                  db.run(Purchase.delete(_.id <= 6)) ==> 2
                  db.run(Purchase.select.size) ==> 1
                  throw new FooException
                }
              } catch { case e: FooException => /*donothing*/ }

              db.run(Purchase.select.size) ==> 3
            }

            db.run(Purchase.select.size) ==> 3
          }

          dbClient.autoCommit.run(Purchase.select.size) ==> 3
        })

        test("middle") - checker.recorded(Text {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            try {
              db.savepoint { sp1 =>
                db.run(Purchase.delete(_.id <= 4)) ==> 2
                db.run(Purchase.select.size) ==> 3

                db.savepoint { sp2 =>
                  db.run(Purchase.delete(_.id <= 6)) ==> 2
                  db.run(Purchase.select.size) ==> 1
                }

                db.run(Purchase.select.size) ==> 1
                throw new FooException
              }
            } catch { case e: FooException => /*donothing*/ }

            db.run(Purchase.select.size) ==> 5
          }

          dbClient.autoCommit.run(Purchase.select.size) ==> 5
        })

        test("innerMiddle") - checker.recorded(Text {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            try {
              db.savepoint { sp1 =>
                db.run(Purchase.delete(_.id <= 4)) ==> 2
                db.run(Purchase.select.size) ==> 3

                db.savepoint { sp2 =>
                  db.run(Purchase.delete(_.id <= 6)) ==> 2
                  db.run(Purchase.select.size) ==> 1
                  throw new FooException
                }
              }
            } catch { case e: FooException => /*donothing*/ }

            db.run(Purchase.select.size) ==> 5
          }

          dbClient.autoCommit.run(Purchase.select.size) ==> 5
        })

        test("middleOuter") - checker.recorded(Text {
          try {
            dbClient.transaction { implicit db =>
              db.run(Purchase.select.size) ==> 7

              db.run(Purchase.delete(_.id <= 2)) ==> 2
              db.run(Purchase.select.size) ==> 5

              db.savepoint { sp1 =>
                db.run(Purchase.delete(_.id <= 4)) ==> 2
                db.run(Purchase.select.size) ==> 3

                db.savepoint { sp2 =>
                  db.run(Purchase.delete(_.id <= 6)) ==> 2
                  db.run(Purchase.select.size) ==> 1
                }
                db.run(Purchase.select.size) ==> 1
                throw new FooException
              }
            }
          } catch { case e: FooException => /*donothing*/ }

          dbClient.autoCommit.run(Purchase.select.size) ==> 7
        })

        test("innerMiddleOuter") - checker.recorded(Text {
          try {
            dbClient.transaction { implicit db =>
              db.run(Purchase.select.size) ==> 7

              db.run(Purchase.delete(_.id <= 2)) ==> 2
              db.run(Purchase.select.size) ==> 5

              db.savepoint { sp1 =>
                db.run(Purchase.delete(_.id <= 4)) ==> 2
                db.run(Purchase.select.size) ==> 3

                db.savepoint { sp2 =>
                  db.run(Purchase.delete(_.id <= 6)) ==> 2
                  db.run(Purchase.select.size) ==> 1
                  throw new FooException
                }
              }

              db.run(Purchase.select.size) ==> 5
            }
          } catch { case e: FooException => /*donothing*/ }

          dbClient.autoCommit.run(Purchase.select.size) ==> 7
        })
      }

      test("rollback") {
        test("inner") - checker.recorded(Text {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepoint { sp1 =>
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

          dbClient.autoCommit.run(Purchase.select.size) ==> 3
        })

        test("middle") - checker.recorded(Text {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepoint { sp1 =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              db.savepoint { sp2 =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
              }

              db.run(Purchase.select.size) ==> 1
              sp1.rollback()
              db.run(Purchase.select.size) ==> 5
            }

            db.run(Purchase.select.size) ==> 5
          }

          dbClient.autoCommit.run(Purchase.select.size) ==> 5
        })

        test("innerMiddle") - checker.recorded(Text {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepoint { sp1 =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              db.savepoint { sp2 =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
                sp1.rollback()
                db.run(Purchase.select.size) ==> 5
              }
              db.run(Purchase.select.size) ==> 5
            }

            db.run(Purchase.select.size) ==> 5
          }

          dbClient.autoCommit.run(Purchase.select.size) ==> 5
        })

        test("middleOuter") - checker.recorded(Text {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepoint { sp1 =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              db.savepoint { sp2 =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
              }

              db.run(Purchase.select.size) ==> 1
              db.rollback()
              db.run(Purchase.select.size) ==> 7
            }
            db.run(Purchase.select.size) ==> 7
          }

          dbClient.autoCommit.run(Purchase.select.size) ==> 7
        })

        test("innerMiddleOuter") - checker.recorded(Text {
          dbClient.transaction { implicit db =>
            db.run(Purchase.select.size) ==> 7

            db.run(Purchase.delete(_.id <= 2)) ==> 2
            db.run(Purchase.select.size) ==> 5

            db.savepoint { sp1 =>
              db.run(Purchase.delete(_.id <= 4)) ==> 2
              db.run(Purchase.select.size) ==> 3

              db.savepoint { sp2 =>
                db.run(Purchase.delete(_.id <= 6)) ==> 2
                db.run(Purchase.select.size) ==> 1
                db.rollback()
                db.run(Purchase.select.size) ==> 7
              }
              db.run(Purchase.select.size) ==> 7
            }

            db.run(Purchase.select.size) ==> 7
          }

          dbClient.autoCommit.run(Purchase.select.size) ==> 7
        })
      }
    }
  }
}
