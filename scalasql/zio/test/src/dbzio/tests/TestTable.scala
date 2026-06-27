package scalasql
package dbzio.tests

import javax.sql.DataSource
import org.sqlite.SQLiteDataSource
import zio.*

import scalasql.*
import scalasql.core.SqlStr.SqlStringSyntax
import scalasql.core.TypeMapper
import scalasql.dialects.SqliteDialect
import scalasql.query.Table as QTable

import scalasql.dbzio.*

given scalasql.dialects.ReturningDialect = SqliteDialect
import SqliteDialect.{dialectSelf as _, *}

opaque type Email = String
object Email:
  def apply(s: String): Email = s
  given TypeMapper[Email] = summon[TypeMapper[String]]

opaque type UserName = String
object UserName:
  def apply(s: String): UserName = s
  given TypeMapper[UserName] = summon[TypeMapper[String]]

case class User[+F[_]](
    id: F[User.Id],
    email: F[Email],
    name: F[UserName]
)

object User extends QTable[User]:
  type Id = PrimaryKey[User, Long]

case class Account[+F[_]](
    id: F[Account.Id],
    userId: F[User.Id],
    balance: F[BigDecimal]
)

object Account extends QTable[Account]:
  type Id = PrimaryKey[Account, Long]

case class TransferAudit[+F[_]](
    id: F[TransferAudit.Id],
    fromUserId: F[User.Id],
    toUserId: F[User.Id],
    amount: F[BigDecimal],
    event: F[String]
)

object TransferAudit extends QTable[TransferAudit]:
  type Id = PrimaryKey[TransferAudit, Long]

object TestDb:
  private val freshDataSource: Task[DataSource] = ZIO.attemptBlocking:
    val tmpFile = java.io.File.createTempFile("scalasqlzio-test-", ".db")
    tmpFile.deleteOnExit()
    val ds = new SQLiteDataSource()
    ds.setUrl(s"jdbc:sqlite:${tmpFile.getAbsolutePath}")
    ds

  def clientLayer: ZLayer[Any, DbException, ZDbClient] = ZLayer.fromZIO:
    for
      ds <- freshDataSource.mapError(DbException(_))
      client = ZDbClient.dataSource(ds, new scalasql.Config {})
      _ <- client.withAutoCommits:
        ZIO.serviceWithZIO[ZDbApi]: db =>
          for
            _ <- db.updateSql(sql"""CREATE TABLE user(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  email TEXT NOT NULL,
                  name TEXT NOT NULL
                )""")
            _ <- db.updateSql(sql"""CREATE TABLE account(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  user_id INTEGER NOT NULL REFERENCES user(id),
                  balance DECIMAL(12,2) NOT NULL DEFAULT 0
                )""")
            _ <- db.updateSql(sql"""CREATE TABLE transfer_audit(
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  from_user_id INTEGER NOT NULL REFERENCES user(id),
                  to_user_id INTEGER NOT NULL REFERENCES user(id),
                  amount DECIMAL(12,2) NOT NULL,
                  event TEXT NOT NULL
                )""")
          yield ()
    yield client
