package scalasql

import geny.Generator
import renderer.{Context, SqlStr}
import upickle.core.Visitor
import scalasql.dialects.DialectConfig
import scalasql.dialects.DialectConfig.dialectCastParams
import scalasql.renderer.SqlStr.Interp
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
   * Runs the given query [[Q]] and returns a value of type [[R]]
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
   * Runs a [[SqlStr]] and takes a callback [[block]] allowing you to process the
   * resultant [[ResultSet]]
   */
  def runQuery[T](sql: SqlStr)(block: ResultSet => T): T

  def streamSql[R](sql: SqlStr, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
      implicit qr: Queryable.Row[_, R]
  ): Generator[R]

  /**
   * Runs a `java.lang.String` (and any interpolated variables) and takes a callback
   * [[block]] allowing you to process the resultant [[ResultSet]]
   */
  def runRawQuery[T](sql: String, variables: Any*)(block: ResultSet => T): T

  /**
   * Runs an [[SqlStr]] containing an `UPDATE` or `INSERT` query and returns the
   * number of rows affected
   */
  def runUpdate(sql: SqlStr): Int

  /**
   * Runs an `java.lang.String` (and any interpolated variables) containing an
   * `UPDATE` or `INSERT` query and returns the number of rows affected
   */
  def runRawUpdate(sql: String, variables: Any*): Int

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

    def runRawQueryFlattened[T](flattened: SqlStr.Flattened)(block: ResultSet => T): T = {
      runRawQuery0[T](combineQueryString(flattened), flattenParamPuts(flattened))(block)
    }

    def runRawQuery[T](sql: String, variables: Any*)(block: ResultSet => T): T = {
      runRawQuery0[T](sql, anySeqPuts(variables))(block)
    }

    private def flattenParamPuts[T](flattened: SqlStr.Flattened) = {
      flattened.params.map(v => (s, n) => v.mappedType.put(s, n, cast(v.value))).toSeq
    }

    private def anySeqPuts(variables: Seq[Any]) = {
      variables.map(v => (s: PreparedStatement, n: Int) => s.setObject(n, v))
    }

    def runRawQuery0[T](sql: String, variables: Seq[(PreparedStatement, Int) => Unit])(
        block: ResultSet => T
    ): T = {
      if (autoCommit) connection.setAutoCommit(true)
      val statement = connection.prepareStatement(sql)
      for ((variable, i) <- variables.iterator.zipWithIndex) variable(statement, i + 1)

      try block(statement.executeQuery())
      finally statement.close()
    }

    def runSql[R](
        sql: SqlStr,
        fetchSize: Int = -1,
        queryTimeoutSeconds: Int = -1
    )(implicit qr: Queryable.Row[_, R]): IndexedSeq[R] =
      streamSql(sql, fetchSize, queryTimeoutSeconds).toVector

    def runQuery[T](sql: SqlStr)(block: ResultSet => T): T = {
      if (autoCommit) connection.setAutoCommit(true)
      val flattened = SqlStr.flatten(sql)
      runRawQueryFlattened(flattened)(block)
    }

    def runRawUpdateFlattened(flattened: SqlStr.Flattened): Int = {
      runRawUpdate0(combineQueryString(flattened), flattenParamPuts(flattened))
    }

    def runRawUpdate0(sql: String, variables: Seq[(PreparedStatement, Int) => Unit]): Int = {
      if (autoCommit) connection.setAutoCommit(true)

      // Sqlite and HsqlDb for some reason blow up if you try to do DDL
      // like DROP TABLE or CREATE TABLE inside a prepared statement, so
      // fall back to vanilla `createStatement`
      if (variables.isEmpty) {
        val statement = connection.createStatement()
        try statement.executeUpdate(sql)
        finally statement.close()
      } else {
        val statement = connection.prepareStatement(sql)
        for ((v, i) <- variables.iterator.zipWithIndex) v(statement, i + 1)
        try statement.executeUpdate()
        finally statement.close()
      }
    }

    def runRawUpdate(sql: String, variables: Any*): Int = runRawUpdate0(sql, anySeqPuts(variables))

    def runUpdate(sql: SqlStr): Int = runRawUpdateFlattened(SqlStr.flatten(sql))

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

      if (qr.isExecuteUpdate(query)) {
        val (typeMappers, statement) = prepareStatement(query, fetchSize, queryTimeoutSeconds)

        statement.executeUpdate().asInstanceOf[R]
      } else {
        try {
          if (qr.singleRow(query)) {
            val columnUnMapper = prepareColumnUnmapper(query, qr)
            val (typeMappers, statement) =
              prepareStatement(query, fetchSize, queryTimeoutSeconds)

            val resultSet: ResultSet = statement.executeQuery()
            try {
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
            } finally {
              resultSet.close()
              statement.close()
            }
          } else {
            stream(query, fetchSize, queryTimeoutSeconds)(
              qr.asInstanceOf[Queryable[Q, Seq[_]]]
            ).toVector.asInstanceOf[R]
          }
        }
      }
    }

    private def prepareStatement[Q, R](query: Q, fetchSize: Int, queryTimeoutSeconds: Int)(
        implicit qr: Queryable[Q, R]
    ) = {

      val (flattened, typeMappers) = prepareMetadata(query, qr)

      if (autoCommit) connection.setAutoCommit(true)

      val statement = connection.prepareStatement(combineQueryString(flattened))

      Seq(fetchSize, config.defaultFetchSize).find(_ != -1).foreach(statement.setFetchSize)
      Seq(queryTimeoutSeconds, config.defaultQueryTimeoutSeconds)
        .find(_ != -1)
        .foreach(statement.setQueryTimeout)

      for ((p, n) <- flattened.params.iterator.zipWithIndex) {
        p.mappedType.asInstanceOf[TypeMapper[Any]].put(statement, n + 1, p.value)
      }

      (typeMappers, statement)
    }

    private def prepareMetadata[Q, R](query: Q, qr: Queryable[Q, R]) = {

      val (typeMappers, flattened) = unpackQueryable(query, qr)
      (flattened, typeMappers)
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
    )(implicit qr: Queryable.Row[_, R]): Generator[R] = new Generator[R] {
      def generate(handleItem: R => Generator.Action): Generator.Action = {
        if (autoCommit) connection.setAutoCommit(true)
        val valueReader: OptionPickler.Reader[R] = qr.valueReader()
        val typeMappers: Seq[TypeMapper[_]] = qr.toTypeMappers()
        val columnNameUnMapper = Right(qr.walkLabels().map(_.toIndexedSeq).toIndexedSeq)
        val flattened = SqlStr.flatten(sql)

        stream0(handleItem, valueReader, typeMappers, columnNameUnMapper, flattened)
      }
    }

    def stream0[R](
        handleItem: R => Generator.Action,
        valueReader: OptionPickler.Reader[R],
        typeMappers: Seq[TypeMapper[_]],
        columnNameUnMapper: Either[Map[String, String], IndexedSeq[IndexedSeq[String]]],
        flattened: SqlStr.Flattened
    ) = {
      runRawQueryFlattened(flattened) { resultSet =>
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

    def stream[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
        implicit qr: Queryable[Q, Seq[R]]
    ): Generator[R] = new Generator[R] {
      def generate(handleItem: R => Generator.Action): Generator.Action = {

        val (flattened, typeMappers) = prepareMetadata(query, qr)
        val columnUnMapper = prepareColumnUnmapper(query, qr)

        stream0(
          handleItem,
          qr.valueReader(query).asInstanceOf[OptionPickler.SeqLikeReader2[Seq, R]].r,
          typeMappers,
          Left(columnUnMapper),
          flattened
        )
      }
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
