package scalasql.dbzio.tests

import zio.*
import zio.test.*

import scalasql.*
import scalasql.dialects.SqliteDialect.*
import scalasql.dbzio.*

object ZDbApiPlainSpec extends ZIOSpec[ZDbClient]:

  import ZDbClient.transaction

  val bootstrap: ZLayer[Any, DbException, ZDbClient] = TestDb.clientLayer

  private def insertUser(email: Email, name: UserName) = DbOp.run:
    User.insert.columns(_.email := email, _.name := name)

  private def insertAccount(userId: User.Id, balance: BigDecimal) = DbOp.run:
    Account.insert.columns(_.userId := userId, _.balance := balance)

  private def getAccount(userId: Long) =
    DbOp
      .run:
        Account.select.filter(_.userId `=` userId).take(1)
      .map(_.headOption)
      .orNotFound

  private def updateAccountBalance(userId: User.Id, balance: BigDecimal) = DbOp.run:
    Account.update(_.userId `=` userId).set(_.balance := balance)

  private def insertAndGetUser(email: Email, name: UserName) =
    insertUser(email, name) *> DbOp
      .run:
        User.select.filter(_.email `=` email).take(1)
      .map(_.headOption)
      .orNotFound

  /** Lock two accounts in a single query, returning (first, second) in argument order. */
  private def lockAccountPair(userId1: User.Id, userId2: User.Id) =
    for
      accounts <- DbOp.run:
        Account.select
          .filter(a => (a.userId `=` userId1) || (a.userId `=` userId2))
          .sortBy(_.userId `=` userId2) // false (0) < true (1), so userId1's row comes first
          .forUpdate
      acc1 <- ZIO.succeed(accounts.headOption).orNotFound
      acc2 <- ZIO.succeed(accounts.lift(1)).orNotFound
    yield (acc1, acc2)

  def spec = suite("ZDbApi — plain")(
    test("run INSERT and SELECT"):
      transaction:
        for
          _ <- insertUser(Email("alice@example.com"), UserName("Alice"))
          rows <- DbOp.run:
            User.select.take(5)
        yield assertTrue(
          rows.size == 1,
          rows.head.name == UserName("Alice"),
          rows.head.email == Email("alice@example.com")
        )
    ,
    test("transaction commits on success"):
      for
        _ <- transaction:
          insertUser(Email("bob@example.com"), UserName("Bob"))
        rows <- transaction(DbOp.run:
          User.select.filter(_.email `=` Email("bob@example.com")))
      yield assertTrue(rows.size == 1, rows.head.name == UserName("Bob"))
    ,
    test("transaction rolls back on failure"):
      for
        _ <- transaction:
          for
            _ <- insertUser(Email("ghost@example.com"), UserName("Ghost"))
            _ <- ZIO.fail(new RuntimeException("boom"))
          yield ()
        .either
        rows <- transaction(DbOp.run:
          User.select.filter(_.email `=` Email("ghost@example.com")))
      yield assertTrue(rows.isEmpty)
    ,
    test("money transfer is atomic"):
      for
        (alice, bob) <- transaction:
          for
            alice <- insertAndGetUser(Email("alice@bank.com"), UserName("Alice"))
            bob <- insertAndGetUser(Email("bob@bank.com"), UserName("Bob"))
            _ <- insertAccount(alice.id, BigDecimal(100))
            _ <- insertAccount(bob.id, BigDecimal(50))
          yield (alice, bob)

        // Transfer $30 from Alice to Bob — locks in id order to prevent deadlocks
        _ <- transaction:
          val amount = BigDecimal(30)
          for
            (from, to) <- lockAccountPair(alice.id, bob.id)
            _ <- updateAccountBalance(alice.id, from.balance - amount)
            _ <- updateAccountBalance(bob.id, to.balance + amount)
          yield ()

        (aliceAcc, bobAcc) <- transaction:
          getAccount(alice.id) <*> getAccount(bob.id)
      yield assertTrue(
        aliceAcc.balance == BigDecimal(70),
        bobAcc.balance == BigDecimal(80)
      )
    ,
    test("failed transfer rolls back both sides"):
      for
        (charlie, dana) <- transaction:
          for
            charlie <- insertAndGetUser(Email("charlie@bank.com"), UserName("Charlie"))
            dana <- insertAndGetUser(Email("dana@bank.com"), UserName("Dana"))
            _ <- insertAccount(charlie.id, BigDecimal(200))
            _ <- insertAccount(dana.id, BigDecimal(100))
          yield (charlie, dana)

        _ <- transaction:
          for
            _ <- updateAccountBalance(charlie.id, BigDecimal(100))
            _ <- ZIO.fail(new RuntimeException("network error"))
            _ <- updateAccountBalance(dana.id, BigDecimal(200))
          yield ()
        .either

        (charlieAcc, danaAcc) <- transaction:
          getAccount(charlie.id) <*> getAccount(dana.id)
      yield assertTrue(
        charlieAcc.balance == BigDecimal(200),
        danaAcc.balance == BigDecimal(100)
      )
    ,
    test("failed transfer is audited via savepoint"):
      for
        (eve, frank) <- transaction:
          for
            eve <- insertAndGetUser(Email("eve@bank.com"), UserName("Eve"))
            frank <- insertAndGetUser(Email("frank@bank.com"), UserName("Frank"))
            _ <- insertAccount(eve.id, BigDecimal(50))
            _ <- insertAccount(frank.id, BigDecimal(50))
          yield (eve, frank)

        // Attempt to transfer $999 — balance updates are rolled back, but audit entries survive
        _ <- transaction:
          for
            // Log the attempt (outside savepoint — persists regardless)
            attempt <- DbOp.run:
              TransferAudit.insert
                .columns(
                  _.fromUserId := eve.id,
                  _.toUserId := frank.id,
                  _.amount := BigDecimal(999),
                  _.event := "attempt"
                )
                .returning(t => (t.fromUserId, t.toUserId, t.amount))
                .single
            // Attempt the actual transfer inside a savepoint
            transferResult <- DbZIO: txn =>
              txn.savepoint: _ =>
                for
                  _ <- DbOp.run:
                    Account
                      .update(_.userId `=` eve.id)
                      .set(_.balance := BigDecimal(50) - attempt._3)
                  _ <- ZIO.fail(new RuntimeException("insufficient funds"))
                yield ()
            .either
            // Log outcome
            _ <- DbOp.run:
              TransferAudit.insert.columns(
                _.fromUserId := attempt._1,
                _.toUserId := attempt._2,
                _.amount := attempt._3,
                _.event := (if transferResult.isRight then "success" else "failure")
              )
          yield ()

        // Both audit entries persist despite transfer rollback
        (audits, eveAcc, frankAcc) <- transaction:
          for
            audits <- DbOp.run:
              TransferAudit.select.filter(_.fromUserId `=` eve.id).sortBy(_.id)
            eveAcc <- getAccount(eve.id)
            frankAcc <- getAccount(frank.id)
          yield (audits, eveAcc, frankAcc)
      yield assertTrue(
        audits.size == 2,
        audits(0).event == "attempt",
        audits(1).event == "failure",
        audits(0).amount == BigDecimal(999),
        // Balances unchanged — transfer was rolled back to savepoint
        eveAcc.balance == BigDecimal(50),
        frankAcc.balance == BigDecimal(50)
      )
  ) @@ TestAspect.sequential
