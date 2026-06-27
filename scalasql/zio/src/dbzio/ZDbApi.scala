package scalasql.dbzio

import javax.sql.DataSource
import sourcecode.FileName
import sourcecode.Line
import zio.*

import scalasql.DbApi as UnsafeDbApi
import scalasql.DbClient as UnsafeDbClient
import scalasql.DbEntityName
import scalasql.DbLookupException
import scalasql.Queryable
import scalasql.core.DialectConfig
import scalasql.core.SqlStr
import scalasql.core.UseBlock
import scalasql.query.Select

case class DbException(cause: Throwable) extends Exception("DB error caught", cause)

trait AccessCompanion[Service: Tag]:
  def apply[R, E, A](use: Service => ZIO[R, E, A])(using Trace): ZIO[Service & R, E, A] =
    ZIO.service[Service].flatMap(use)

type DbZIO[-R, +E, +A] = ZIO[ZDbTxnApi & R, DbException | E, A]
object DbZIO extends AccessCompanion[ZDbTxnApi]

type DbIO[+E, +A] = ZIO[ZDbTxnApi, DbException | E, A]
type DbRIO[-R, +A] = ZIO[ZDbTxnApi & R, DbException, A]
type DbOp[+A] = ZIO[ZDbTxnApi, DbException, A]
type DbRun[+A] = ZIO[Any, DbException, A]

object DbRun:
  private[dbzio] def apply[A](unsafe: => A)(using location: DbOpLocation): DbRun[A] =
    import location.given
    ZIO.logAnnotate(LogAnnotation("db_operation_at", s"${file.value}:${line.value}")):
      ZIO.attemptBlockingInterrupt(unsafe).mapError(DbException(_))

final class DbOpLocation(using val file: FileName, val line: Line, val trace: Trace)
object DbOpLocation:
  given (using FileName, Line, Trace): DbOpLocation = new DbOpLocation

object DbOp:
  /** Execute a scalasql query, returning the result wrapped in `DbOp`. */
  def run[Q, R](
      query: Q
  )(using qr: Queryable[Q, R], location: DbOpLocation): DbOp[R] =
    DbZIO(_.run[Q, R](query))

  /** Execute a SELECT, returning the first row as `Option`. */
  def runHeadOption[Q, R](
      query: Select[Q, R]
  )(using location: DbOpLocation): DbOp[Option[R]] =
    DbZIO(_.run(query.take(1))).map(_.headOption)

  /**
   * Execute a SELECT expecting exactly one result. Fails with `DbLookupException` on 0 or 2+
   * results.
   */
  def runSingle[Q, R](
      query: Select[Q, R]
  )(using DbOpLocation, DbEntityName[R]): DbIO[DbLookupException[R], R] =
    DbZIO(_.run(query.take(2))).atMostOne.orNotFound

  /** Execute a `sql"..."` string (e.g. DDL) and return the number of affected rows. */
  def updateSql(sql: SqlStr)(using DbOpLocation): DbOp[Int] =
    DbZIO(_.updateSql(sql))

extension [E, A](exit: Exit[E, A])
  private def exceptionOption: Option[Throwable] = exit match
    case Exit.Success(_) => None
    case Exit.Failure(cause) =>
      cause.failureOption
        .map[Throwable]:
          case e: Throwable => e
          case other => new Exception(other.toString)
        .orElse(cause.defects.headOption)

extension [UnsafeApi](useBlock: UseBlock[UnsafeApi])
  private def zScope[SafeApi](wrap: UnsafeApi => SafeApi)(
      using DbOpLocation
  ): ZIO[Scope, DbException, SafeApi] =
    val acquire = DbRun:
      val (resource, release) = useBlock.allocate()
      (wrap(resource), release)
    ZIO
      .acquireReleaseExit(acquire) { case ((_, release), exit) =>
        ZIO.attemptBlocking(release(exit.exceptionOption)).orDieWith(DbException(_))
      }
      .map(_._1)

  private def zLift[SafeApi, R, E, A](wrap: UnsafeApi => SafeApi)(safeUse: SafeApi => ZIO[R, E, A])(
      using DbOpLocation
  ): ZIO[R, DbException | E, A] =
    ZIO.scoped:
      zScope(wrap).flatMap(safeUse)

/**
 * ZIO wrapper around scalasql's `DbApi`. Execute queries as ZIO effects with automatic error
 * wrapping.
 */
class ZDbApi(unsafe: UnsafeDbApi):

  /** Execute a scalasql query within ZIO, wrapping errors in `DbException`. */
  def run[Q, R](
      query: Q,
      fetchSize: Int = -1,
      queryTimeoutSeconds: Int = -1
  )(using qr: Queryable[Q, R], location: DbOpLocation): DbRun[R] =
    import location.given
    DbRun(unsafe.run(query, fetchSize, queryTimeoutSeconds)).tapErrorCause: cause =>
      ZIO.logErrorCause(s"Error executing query - ${unsafe.renderSql(query)}", cause)

  /** Execute a `sql"..."` string (e.g. DDL) and return the number of affected rows. */
  def updateSql(sql: SqlStr)(using location: DbOpLocation): DbRun[Int] =
    import location.given
    DbRun(unsafe.updateSql(sql))

object ZDbApi:
  def autoCommitDataSource(
      ds: DataSource
  )(using DialectConfig, Trace)(using FileName, Line): DbRun[ZDbApi] = DbRun:
    new ZDbApi(UnsafeDbClient.DataSource(ds).getAutoCommitClientConnection)

/** ZIO wrapper for transactional database access with rollback and savepoint support. */
class ZDbTxnApi(unsafe: UnsafeDbApi.Txn) extends ZDbApi(unsafe):
  /** Roll back the current transaction. */
  def rollback(using DbOpLocation): DbRun[Unit] = DbRun(unsafe.rollback())

  /**
   * Execute a block within a savepoint. The savepoint is released on success and rolled back on
   * failure.
   */
  def savepoint[R, E, A](use: Savepoint => ZIO[R, E, A])(
      using location: DbOpLocation
  ): ZIO[R, DbException | E, A] =
    unsafe.savepoint.zLift(new Savepoint(_))(use)

/** A database savepoint that can be rolled back independently of the enclosing transaction. */
class Savepoint(unsafe: UnsafeDbApi.Savepoint):
  export unsafe.savepointId
  export unsafe.savepointName

  /** Roll back to this savepoint. */
  def rollback(using DbOpLocation): DbRun[Unit] = DbRun(unsafe.rollback())

/** ZIO-native database client. Provides `transaction` and `withAutoCommits` scopes. */
class ZDbClient(unsafe: UnsafeDbClient):
  export unsafe.renderSql

  /** Run a ZIO effect with auto-committing database access. */
  def withAutoCommits[R, E, A](
      use: ZIO[ZDbApi & R, E, A]
  )(using DbOpLocation): ZIO[R, DbException | E, A] =
    DbRun(unsafe.getAutoCommitClientConnection).flatMap: unsafeApi =>
      use.provideSomeEnvironment[R](_.add(new ZDbApi(unsafeApi)))

  /** Run a ZIO effect within a database transaction. Commits on success, rolls back on failure. */
  def transaction[R, E, A](
      use: ZIO[ZDbTxnApi & R, E, A]
  )(using DbOpLocation): ZIO[R, DbException | E, A] =
    unsafe.transaction.zLift(new ZDbTxnApi(_))(client =>
      use.provideSomeEnvironment[R](_.add(client))
    )

  /**
   * Acquire a scoped transaction. Commits when the scope closes successfully, rolls back on
   * failure.
   */
  def transactionScope(using DbOpLocation): ZIO[Scope, DbException, ZDbTxnApi] =
    unsafe.transaction.zScope(new ZDbTxnApi(_))

object ZDbClient extends AccessCompanion[ZDbClient]:
  /** Create a `ZDbClient` from a `DataSource` and scalasql config. */
  def dataSource(ds: DataSource, config: scalasql.Config)(using DialectConfig): ZDbClient =
    new ZDbClient(
      UnsafeDbClient.DataSource(ds, config)
    )

  /** Run a ZIO effect within a transaction, pulling the `ZDbClient` from the environment. */
  def transaction[R, E, A](use: ZIO[ZDbTxnApi & R, E, A])(
      using DbOpLocation
  ): ZIO[ZDbClient & R, DbException | E, A] =
    apply(_.transaction(use))

  /**
   * Run a ZIO effect with auto-committing database access, pulling the `ZDbClient` from the
   * environment.
   */
  def withAutoCommits[R, E, A](use: ZIO[ZDbApi & R, E, A])(
      using DbOpLocation
  ): ZIO[ZDbClient & R, DbException | E, A] =
    apply(_.withAutoCommits(use))
