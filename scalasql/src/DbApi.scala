package scalasql

import geny.Generator
import renderer.{Context, SqlStr}
import upickle.core.Visitor
import scalasql.dialects.DialectConfig
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

  /**
   * Runs a batch of update queries, as `java.lang.String`s, and returns
   * the number of rows affected by each one
   */
  def runRawUpdateBatch(sql: Seq[String]): Seq[Int]

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

    def runRawQuery[T](sql: String, variables: Any*)(block: ResultSet => T): T = {
      if (autoCommit) connection.setAutoCommit(true)
      val statement = connection.prepareStatement(sql)
      for ((variable, i) <- variables.zipWithIndex) statement.setObject(i + 1, variable)

      try block(statement.executeQuery())
      finally statement.close()
    }

    def runQuery[T](sql: SqlStr)(block: ResultSet => T): T = {
      if (autoCommit) connection.setAutoCommit(true)
      val flattened = SqlStr.flatten(sql)
      runRawQuery(flattened.queryParts.mkString("?"), flattened.params.map(_.value): _*)(block)
    }

    def runRawUpdate(sql: String, variables: Any*): Int = {
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
        for ((variable, i) <- variables.zipWithIndex) statement.setObject(i + 1, variable)
        try statement.executeUpdate()
        finally statement.close()
      }
    }

    def runUpdate(sql: SqlStr): Int = {
      if (autoCommit) connection.setAutoCommit(true)
      val flattened = SqlStr.flatten(sql)
      runRawUpdate(flattened.queryParts.mkString("?"), flattened.params.map(_.value): _*)
    }

    def runRawUpdateBatch(sqls: Seq[String]) = {

      if (autoCommit) connection.setAutoCommit(true)
      val statement: Statement = connection.createStatement()

      try {
        sqls.foreach(statement.addBatch)
        statement.executeBatch()
      } finally statement.close()
    }

    def toSqlQuery[Q, R](query: Q, castParams: Boolean = false)(
        implicit qr: Queryable[Q, R]
    ): String = {
      val (str, params, mappedTypes) = toSqlQuery0(query)
      str
    }

    def toSqlQuery0[Q, R](query: Q, castParams: Boolean = false)(
        implicit qr: Queryable[Q, R]
    ): (String, Seq[SqlStr.Interp.TypeInterp[_]], Seq[TypeMapper[_]]) = {
      val ctx = Context.Impl(Map(), Map(), config, dialectConfig.defaultQueryableSuffix)
      val sqlStr = qr.toSqlStr(query, ctx)
      val mappedTypes = qr.toTypeMappers(query)
      val flattened = SqlStr.flatten(sqlStr)
      val queryStr = flattened.queryParts
        .zipAll(flattened.params, "", null)
        .map {
          case (part, null) => part
          case (part, param) =>
            val jdbcTypeString = param.mappedType.typeString
            if (castParams) part + s"CAST(? AS $jdbcTypeString)" else part + "?"
        }
        .mkString

      (queryStr, flattened.params, mappedTypes)
    }

    def run[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
        implicit qr: Queryable[Q, R]
    ): R = {
      val (exprs, statement, columnUnMapper) = prepareRun(query, fetchSize, queryTimeoutSeconds)

      if (qr.isExecuteUpdate(query)) statement.executeUpdate().asInstanceOf[R]
      else {
        val resultSet: ResultSet = statement.executeQuery()

        try {
          if (qr.singleRow(query)) {
            assert(resultSet.next())
            val res =
              handleResultRow(resultSet, qr.valueReader(query), exprs, config, columnUnMapper)
            assert(!resultSet.next())
            res
          } else {
            val arrVisitor = qr.valueReader(query).visitArray(-1, -1)
            while (resultSet.next()) {
              val rowRes =
                handleResultRow(resultSet, arrVisitor.subVisitor, exprs, config, columnUnMapper)
              arrVisitor.visitValue(rowRes, -1)
            }
            arrVisitor.visitEnd(-1)
          }
        } finally {
          resultSet.close()
          statement.close()
        }
      }
    }

    private def prepareRun[Q, R](query: Q, fetchSize: Int, queryTimeoutSeconds: Int)(
        implicit qr: Queryable[Q, R]
    ) = {

      if (autoCommit) connection.setAutoCommit(true)
      val (str, params, exprs) = toSqlQuery0(query, dialectConfig.castParams)
      val statement = connection.prepareStatement(str)

      Seq(fetchSize, config.defaultFetchSize).find(_ != -1).foreach(statement.setFetchSize)
      Seq(queryTimeoutSeconds, config.defaultQueryTimeoutSeconds)
        .find(_ != -1)
        .foreach(statement.setQueryTimeout)

      for ((p, n) <- params.zipWithIndex) {
        p.mappedType.asInstanceOf[TypeMapper[Any]].put(statement, n + 1, p.value)
      }

      val walked = qr.walk(query)

      val columnUnMapper = walked.map { case (namesChunks, exprs) =>
        Config.joinName(namesChunks.map(config.columnNameMapper), config) ->
          Config.joinName(namesChunks, config)
      }.toMap

      (exprs, statement, columnUnMapper)
    }

    def stream[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
        implicit qr: Queryable[Q, Seq[R]]
    ): Generator[R] = new Generator[R] {
      def generate(handleItem: R => Generator.Action): Generator.Action = {
        val (exprs, statement, columnUnMapper) = prepareRun(query, fetchSize, queryTimeoutSeconds)

        val resultSet: ResultSet = statement.executeQuery()
        try {
          val visitor = qr.valueReader(query).asInstanceOf[OptionPickler.SeqLikeReader2[Seq, R]].r
          var action: Generator.Action = Generator.Continue
          while (resultSet.next() && action == Generator.Continue) {
            val rowRes = handleResultRow(resultSet, visitor, exprs, config, columnUnMapper)
            action = handleItem(rowRes)
          }

          action
        } finally {
          resultSet.close()
          statement.close()
        }
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
      columnNameUnMapper: Map[String, String]
  ): V = {

    val keys = Array.newBuilder[IndexedSeq[String]]
    val values = Array.newBuilder[Object]
    val nulls = Array.newBuilder[Boolean]
    val metadata = resultSet.getMetaData

    for (i <- Range(0, metadata.getColumnCount)) {
      val k = FlatJson
        .fastSplitNonRegex(
          columnNameUnMapper(metadata.getColumnLabel(i + 1).toLowerCase),
          config.columnLabelDelimiter
        )
        .drop(1)

      val v = exprs(i).get(resultSet, i + 1).asInstanceOf[Object]
      val isNull = resultSet.getObject(i + 1) == null

      keys.addOne(k)
      values.addOne(v)
      nulls.addOne(isNull)
    }

    FlatJson.unflatten[V](keys.result(), values.result(), nulls.result(), rowVisitor)
  }
}
