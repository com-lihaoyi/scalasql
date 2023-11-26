package scalasql

import geny.Generator
import renderer.{Context, SqlStr}
import upickle.core.Visitor
import scalasql.dialects.DialectConfig
import scalasql.dialects.DialectConfig.dialectCastParams
import scalasql.renderer.SqlStr.{Interp, flatten}
import scalasql.utils.{FlatJson, OptionPickler}

import java.sql.{PreparedStatement, ResultSet, Statement}

/**
 * An interface to the SQL database allowing you to run queries.
 */
trait DbApi extends AutoCloseable {

  /**
   * Converts the given query [[Q]] into a string. Useful for debugging and logging
   */
  def toSqlQuery[Q, R](query: Q, castParams: Boolean = false)(implicit qr: Queryable[Q, R]): String

  /**
   * Runs the given query [[Q]] and returns a value of type [[R]]
   */
  def run[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
      implicit qr: Queryable[Q, R]
  ): R

  /**
   * Runs the given [[SqlStr]] of the form `sql"..."` and returns a value of type [[R]]
   */
  def runSql[R](query: SqlStr, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
      implicit qr: Queryable.Row[_, R]
  ): Seq[R]

  /**
   * Runs the given query [[Q]] and returns a [[Generator]] of values of type [[R]].
   * allow you to process the results in a streaming fashion without materializing
   * the entire list of results in memory
   */
  def stream[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
      implicit qr: Queryable[Q, Seq[R]]
  ): Generator[R]

  /**
   * A combination of [[stream]] and [[runSql]], [[streamSql]] allows you to pass in an
   * arbitrary [[SqlStr]] of the form `sql"..."` and  streams the results back to you
   */
  def streamSql[R](sql: SqlStr, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
      implicit qr: Queryable.Row[_, R]
  ): Generator[R]

  /**
   * Runs a `java.lang.String` (and any interpolated variables) and takes a callback
   * [[block]] allowing you to process the resultant [[ResultSet]]
   */
  def runRaw[R](sql: String,
                variables: Seq[Any] = Nil,
                fetchSize: Int = -1,
                queryTimeoutSeconds: Int = -1)
               (implicit qr: Queryable.Row[_, R]): Seq[R]

  /**
   * Runs an [[SqlStr]] of the form `sql"..."` containing an `UPDATE` or `INSERT` query and returns the
   * number of rows affected
   */
  def updateSql(sql: SqlStr, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1): Int

  /**
   * Runs an `java.lang.String` (and any interpolated variables) containing an
   * `UPDATE` or `INSERT` query and returns the number of rows affected
   */
  def updateRaw(sql: String, variables: Seq[Any] = Nil, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1): Int

}

object DbApi {

  /**
   * An interface to a SQL database *transaction*, allowing you to run queries,
   * create savepoints, or roll back the transaction.
   */
  trait Txn extends DbApi {
    def savepoint[T](block: DbApi.Savepoint => T): T
    def rollback(): Unit
  }

  /**
   * A SQL `SAVEPOINT`, with an ID, name, and the ability to roll back to when it was created
   */
  trait Savepoint {
    def savepointId: Int
    def savepointName: String
    def rollback(): Unit
  }

  class Impl(
      connection: java.sql.Connection,
      config: Config,
      dialectConfig: DialectConfig,
      autoCommit: Boolean,
      rollBack0: () => Unit
  ) extends DbApi.Txn {
    val savepointStack = collection.mutable.ArrayDeque.empty[java.sql.Savepoint]

    def savepoint[T](block: DbApi.Savepoint => T): T = {
      val savepoint = connection.setSavepoint()
      savepointStack.append(savepoint)

      try {
        val res = block(new DbApi.SavepointApiImpl(savepoint, () => rollbackSavepoint(savepoint)))
        if (savepointStack.lastOption.exists(_ eq savepoint)) {
          // Only release if this savepoint has not been rolled back,
          // directly or indirectly
          connection.releaseSavepoint(savepoint)
        }
        res
      } catch {
        case e: Throwable =>
          rollbackSavepoint(savepoint)
          throw e
      }
    }

    // Make sure we keep track of what savepoints are active on the stack, so we do
    // not release or rollback the same savepoint multiple times even in the case of
    // exceptions or explicit rollbacks
    def rollbackSavepoint(savepoint: java.sql.Savepoint) = {
      savepointStack.indexOf(savepoint) match {
        case -1 => // do nothing
        case savepointIndex =>
          connection.rollback(savepointStack(savepointIndex))
          savepointStack.takeInPlace(savepointIndex)
      }
    }

    def rollback() = {
      savepointStack.clear()
      rollBack0()
    }

    private def cast[T](t: Any): T = t.asInstanceOf[T]

    def runRawQueryFlattened[T](flattened: SqlStr.Flattened,
                                fetchSize: Int,
                                queryTimeoutSeconds: Int)(block: ResultSet => T): T = {
      runRawQuery0[T](combineQueryString(flattened), flattenParamPuts(flattened), fetchSize, queryTimeoutSeconds)(block)
    }

    def runRaw[R](sql: String,
                  variables: Seq[Any] = Nil,
                  fetchSize: Int = -1,
                  queryTimeoutSeconds: Int = -1)
                 (implicit qr: Queryable.Row[_, R]): Seq[R] = {
      streamRaw0[R](
        valueReader = qr.valueReader(),
        typeMappers = qr.toTypeMappers(),
        columnNameUnMapper = Right(qr.walkLabels().map(_.toIndexedSeq).toIndexedSeq),
        sql,
        anySeqPuts(variables),
        fetchSize,
        queryTimeoutSeconds
      ).toSeq
    }

    private def flattenParamPuts[T](flattened: SqlStr.Flattened) = {
      flattened.params.map(v => (s, n) => v.mappedType.put(s, n, cast(v.value))).toSeq
    }

    private def anySeqPuts(variables: Seq[Any]) = {
      variables.map(v => (s: PreparedStatement, n: Int) => s.setObject(n, v))
    }

    def runRawQuery0[T](sql: String,
                        variables: Seq[(PreparedStatement, Int) => Unit],
                        fetchSize: Int,
                        queryTimeoutSeconds: Int)(
        block: ResultSet => T
    ): T = {
      val statement = connection.prepareStatement(sql)
      for ((variable, i) <- variables.iterator.zipWithIndex) variable(statement, i + 1)
      configureRunCloseStatement(statement, fetchSize, queryTimeoutSeconds)(s => block(s.executeQuery()))
    }

    def runSql[R](
        sql: SqlStr,
        fetchSize: Int = -1,
        queryTimeoutSeconds: Int = -1
    )(implicit qr: Queryable.Row[_, R]): IndexedSeq[R] =
      streamSql(sql, fetchSize, queryTimeoutSeconds).toVector

    def runRawUpdate0(sql: String,
                      variables: Seq[(PreparedStatement, Int) => Unit],
                      fetchSize: Int,
                      queryTimeoutSeconds: Int): Int = {
      // Sqlite and HsqlDb for some reason blow up if you try to do DDL
      // like DROP TABLE or CREATE TABLE inside a prepared statement, so
      // fall back to vanilla `createStatement`
      if (variables.isEmpty) {
        val statement = connection.createStatement()
        configureRunCloseStatement(statement, fetchSize, queryTimeoutSeconds)(_.executeUpdate(sql))
      } else {
        val statement = connection.prepareStatement(sql)
        for ((v, i) <- variables.iterator.zipWithIndex) v(statement, i + 1)
        configureRunCloseStatement(statement, fetchSize, queryTimeoutSeconds)(_.executeUpdate())
      }
    }

    def configureRunCloseStatement[P <: Statement, T](statement: P,
                           fetchSize: Int,
                           queryTimeoutSeconds: Int)(
                              f: P => T): T = {
      if (autoCommit) connection.setAutoCommit(true)
      Seq(fetchSize, config.defaultFetchSize).find(_ != -1).foreach(statement.setFetchSize)
      Seq(queryTimeoutSeconds, config.defaultQueryTimeoutSeconds)
        .find(_ != -1)
        .foreach(statement.setQueryTimeout)

      try f(statement)
      finally statement.close()
    }

    def updateRaw(sql: String, variables: Seq[Any] = Nil,
                  fetchSize: Int = -1,
                  queryTimeoutSeconds: Int = -1): Int = runRawUpdate0(sql, anySeqPuts(variables), fetchSize, queryTimeoutSeconds)

    def updateSql(sql: SqlStr,
                  fetchSize: Int = -1,
                  queryTimeoutSeconds: Int = -1): Int = {
      val flattened = SqlStr.flatten(sql)
      runRawUpdate0(combineQueryString(flattened), flattenParamPuts(flattened), fetchSize, queryTimeoutSeconds)
    }

    def toSqlQuery[Q, R](query: Q, castParams: Boolean = false)(
        implicit qr: Queryable[Q, R]
    ): String = {
      val (str, params, mappedTypes) = toSqlQuery0(query, castParams)
      str
    }

    def toSqlQuery0[Q, R](query: Q, castParams: Boolean)(
        implicit qr: Queryable[Q, R]
    ): (String, collection.Seq[SqlStr.Interp.TypeInterp[_]], Seq[TypeMapper[_]]) = {
      val (mappedTypes, flattened) = unpackQueryable(query, qr)
      (combineQueryString(flattened, castParams), flattened.params, mappedTypes)
    }

    private def combineQueryString(
        flattened: SqlStr.Flattened,
        castParams: Boolean = dialectCastParams(dialectConfig)
    ) = {
      val queryStr = flattened.queryParts.iterator
        .zipAll(flattened.params, "", null)
        .map {
          case (part, null) => part
          case (part, param) =>
            val jdbcTypeString = param.mappedType.typeString
            if (castParams) part + s"CAST(? AS $jdbcTypeString)" else part + "?"
        }
        .mkString

      queryStr
    }

    private def unpackQueryable[R, Q](query: Q, qr: Queryable[Q, R]) = {
      val ctx = Context.Impl(Map(), Map(), config)
      val sqlStr = qr.toSqlStr(query, ctx)
      val mappedTypes = qr.toTypeMappers(query)
      val flattened = SqlStr.flatten(sqlStr)
      (mappedTypes, flattened)
    }

    def run[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
        implicit qr: Queryable[Q, R]
    ): R = {

      val (typeMappers, flattened) = unpackQueryable(query, qr)
      if (qr.isExecuteUpdate(query)) {
        updateSql(flattened).asInstanceOf[R]
      } else {
        try {
          if (qr.singleRow(query)) {
            val columnUnMapper = prepareColumnUnmapper(query, qr)
            runRawQueryFlattened(flattened, fetchSize, queryTimeoutSeconds){resultSet =>
              assert(resultSet.next())
              val res =
                handleResultRow(
                  resultSet,
                  qr.valueReader(query),
                  typeMappers,
                  config,
                  Left(columnUnMapper)
                )
              assert(!resultSet.next())
              res
            }
          } else {
            stream(query, fetchSize, queryTimeoutSeconds)(
              qr.asInstanceOf[Queryable[Q, Seq[_]]]
            ).toVector.asInstanceOf[R]
          }
        }
      }
    }

    private def prepareColumnUnmapper[Q, R](query: Q, qr: Queryable[Q, R]) = {
      val walked = qr.walkLabels(query)

      val columnUnMapper = walked.map { namesChunks =>
        Config.joinName(namesChunks.map(config.columnNameMapper), config) ->
          Config.joinName(namesChunks, config)
      }.toMap

      columnUnMapper
    }

    def streamSql[R](
        sql: SqlStr,
        fetchSize: Int = -1,
        queryTimeoutSeconds: Int = -1
    )(implicit qr: Queryable.Row[_, R]): Generator[R] = {
      val valueReader: OptionPickler.Reader[R] = qr.valueReader()
      val typeMappers: Seq[TypeMapper[_]] = qr.toTypeMappers()
      val columnNameUnMapper = Right(qr.walkLabels().map(_.toIndexedSeq).toIndexedSeq)
      val flattened = SqlStr.flatten(sql)

      streamFlattened(valueReader, typeMappers, columnNameUnMapper, flattened, fetchSize, queryTimeoutSeconds)
    }

    def streamFlattened[R](
        valueReader: OptionPickler.Reader[R],
        typeMappers: Seq[TypeMapper[_]],
        columnNameUnMapper: Either[Map[String, String], IndexedSeq[IndexedSeq[String]]],
        flattened: SqlStr.Flattened,
        fetchSize: Int,
        queryTimeoutSeconds: Int
    ) = streamRaw0(
      valueReader,
      typeMappers,
      columnNameUnMapper,
      combineQueryString(flattened),
      flattenParamPuts(flattened),
      fetchSize,
      queryTimeoutSeconds,
    )

    def streamRaw0[R](
        valueReader: OptionPickler.Reader[R],
        typeMappers: Seq[TypeMapper[_]],
        columnNameUnMapper: Either[Map[String, String], IndexedSeq[IndexedSeq[String]]],
        sql: String,
        variables: Seq[(PreparedStatement, Int) => Unit],
        fetchSize: Int,
        queryTimeoutSeconds: Int
    ) = new Generator[R] {
      def generate(handleItem: R => Generator.Action): Generator.Action = {

        runRawQuery0(
          sql,
          variables,
          fetchSize,
          queryTimeoutSeconds) { resultSet =>
          var action: Generator.Action = Generator.Continue
          while (resultSet.next() && action == Generator.Continue) {
            val rowRes = handleResultRow(
              resultSet,
              valueReader,
              typeMappers,
              config,
              columnNameUnMapper
            )
            action = handleItem(rowRes)
          }
          action
        }
      }
    }

    def stream[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
        implicit qr: Queryable[Q, Seq[R]]
    ): Generator[R] = {
      val (typeMappers, flattened) = unpackQueryable(query, qr)
      val columnUnMapper = prepareColumnUnmapper(query, qr)
      streamFlattened(
        qr.valueReader(query).asInstanceOf[OptionPickler.SeqLikeReader2[Seq, R]].r,
        typeMappers,
        Left(columnUnMapper),
        flattened,
        fetchSize,
        queryTimeoutSeconds
      )

    }

    def close() = connection.close()
  }

  class SavepointApiImpl(savepoint: java.sql.Savepoint, rollback0: () => Unit) extends Savepoint {
    def savepointId = savepoint.getSavepointId
    def savepointName = savepoint.getSavepointName
    def rollback() = rollback0()
  }

  def handleResultRow[V](
      resultSet: ResultSet,
      rowVisitor: Visitor[_, V],
      exprs: Seq[TypeMapper[_]],
      config: Config,
      columnNameUnMapper0: Either[Map[String, String], IndexedSeq[IndexedSeq[String]]]
  ): V = {

    val keys = Array.newBuilder[IndexedSeq[String]]
    val values = Array.newBuilder[Object]
    val nulls = Array.newBuilder[Boolean]
    val metadata = resultSet.getMetaData

    for (i <- Range(0, metadata.getColumnCount)) {
      val k = columnNameUnMapper0 match {
        case Left(columnNameUnMapper) =>
          metadata.getColumnLabel(i + 1).toLowerCase match {
            // Hack to support top-level `VALUES` clause; most databases do not
            // let you rename their columns
            case /*h2*/ "c1" | /*postgres/sqlite*/ "column1" | /*mysql*/ "column_0" => IndexedSeq()
            case label =>
              FlatJson
                .fastSplitNonRegex(
                  columnNameUnMapper(label),
                  config.columnLabelDelimiter
                )
                .drop(1)
          }
        case Right(columnNameUnMapper) =>
          columnNameUnMapper(i)
      }
      val v = exprs(i).get(resultSet, i + 1).asInstanceOf[Object]
      val isNull = resultSet.getObject(i + 1) == null

      keys.addOne(k)
      values.addOne(v)
      nulls.addOne(isNull)
    }

    FlatJson.unflatten[V](keys.result(), values.result(), nulls.result(), rowVisitor)
  }
}
