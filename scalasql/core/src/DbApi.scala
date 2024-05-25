package scalasql.core

import geny.Generator

import java.sql.{PreparedStatement, Statement}

/**
 * An interface to the SQL database allowing you to run queries.
 */
trait DbApi extends AutoCloseable {

  /**
   * Converts the given query [[Q]] into a string. Useful for debugging and logging
   */
  def renderSql[Q, R](query: Q, castParams: Boolean = false)(implicit qr: Queryable[Q, R]): String

  /**
   * Runs the given query [[Q]] and returns a value of type [[R]]
   */
  def run[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
      implicit qr: Queryable[Q, R],
      fileName: sourcecode.FileName,
      lineNum: sourcecode.Line
  ): R

  /**
   * Runs the given [[SqlStr]] of the form `sql"..."` and returns a value of type [[R]]
   */
  def runSql[R](query: SqlStr, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
      implicit qr: Queryable.Row[?, R],
      fileName: sourcecode.FileName,
      lineNum: sourcecode.Line
  ): IndexedSeq[R]

  /**
   * Runs the given query [[Q]] and returns a [[Generator]] of values of type [[R]].
   * allow you to process the results in a streaming fashion without materializing
   * the entire list of results in memory
   */
  def stream[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
      implicit qr: Queryable[Q, Seq[R]],
      fileName: sourcecode.FileName,
      lineNum: sourcecode.Line
  ): Generator[R]

  /**
   * A combination of [[stream]] and [[runSql]], [[streamSql]] allows you to pass in an
   * arbitrary [[SqlStr]] of the form `sql"..."` and  streams the results back to you
   */
  def streamSql[R](sql: SqlStr, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
      implicit qr: Queryable.Row[?, R],
      fileName: sourcecode.FileName,
      lineNum: sourcecode.Line
  ): Generator[R]

  /**
   * Runs a `java.lang.String` (and any interpolated variables) and deserializes
   * the result set into a `Seq` of the given type [[R]]
   */
  def runRaw[R](
      sql: String,
      variables: Seq[Any] = Nil,
      fetchSize: Int = -1,
      queryTimeoutSeconds: Int = -1
  )(
      implicit qr: Queryable.Row[?, R],
      fileName: sourcecode.FileName,
      lineNum: sourcecode.Line
  ): IndexedSeq[R]

  /**
   * Runs a `java.lang.String` (and any interpolated variables) and deserializes
   * the result set into a streaming `Generator` of the given type [[R]]
   */
  def streamRaw[R](
      sql: String,
      variables: Seq[Any] = Nil,
      fetchSize: Int = -1,
      queryTimeoutSeconds: Int = -1
  )(
      implicit qr: Queryable.Row[?, R],
      fileName: sourcecode.FileName,
      lineNum: sourcecode.Line
  ): Generator[R]

  /**
   * Runs an [[SqlStr]] of the form `sql"..."` containing an `UPDATE` or `INSERT` query and returns the
   * number of rows affected
   */
  def updateSql(sql: SqlStr, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
      implicit fileName: sourcecode.FileName,
      lineNum: sourcecode.Line
  ): Int

  /**
   * Runs an `java.lang.String` (and any interpolated variables) containing an
   * `UPDATE` or `INSERT` query and returns the number of rows affected
   */
  def updateRaw(
      sql: String,
      variables: Seq[Any] = Nil,
      fetchSize: Int = -1,
      queryTimeoutSeconds: Int = -1
  )(implicit fileName: sourcecode.FileName, lineNum: sourcecode.Line): Int

  def updateGetGeneratedKeysSql[R](sql: SqlStr, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
      implicit qr: Queryable.Row[?, R],
      fileName: sourcecode.FileName,
      lineNum: sourcecode.Line
  ): IndexedSeq[R]

  def updateGetGeneratedKeysRaw[R](
      sql: String,
      variables: Seq[Any] = Nil,
      fetchSize: Int = -1,
      queryTimeoutSeconds: Int = -1
  )(
      implicit qr: Queryable.Row[?, R],
      fileName: sourcecode.FileName,
      lineNum: sourcecode.Line
  ): IndexedSeq[R]
}

object DbApi {

  def unpackQueryable[R, Q](query: Q, qr: Queryable[Q, R], config: Config) = {
    val ctx = Context.Impl(Map(), Map(), config)
    val flattened = SqlStr.flatten(qr.renderSql(query, ctx))
    flattened
  }

  def renderSql[Q, R](query: Q, config: Config, castParams: Boolean = false)(
      implicit qr: Queryable[Q, R]
  ): String = {
    val flattened = unpackQueryable(query, qr, config)
    flattened.renderSql(castParams)
  }

  /**
   * An interface to a SQL database *transaction*, allowing you to run queries,
   * create savepoints, or roll back the transaction.
   */
  trait Txn extends DbApi {

    /**
     * Creates a SQL Savepoint that is active within the given block; automatically
     * releases the savepoint if the block completes successfully and rolls it back
     * if the block terminates with an exception, and allows you to roll back the
     * savepoint manually via the [[DbApi.Savepoint]] parameter passed to that block
     */
    def savepoint[T](block: DbApi.Savepoint => T): T

    /**
     * Tolls back any active Savepoints and then rolls back this Transaction
     */
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

  // Call hierarchy the various DbApi.Impl methods, both public and private:
  //
  //                                      run
  //                                       |
  //   runRaw       runSql       +---------+---------+
  //     |            |          |                   |
  // streamRaw    streamSql   stream   updateRaw  updateSql
  //     |            |          |         |         |
  //     |          streamFlattened0       |         |
  //     |                  |              |         |
  //     +-----+------------+              +----+----+
  //           |                                |
  //       streamRaw0                      runRawUpdate0
  //           |                                |
  //           +----------------+---------------+
  //                            |
  //                  configureRunCloseStatement

  class Impl(
      connection: java.sql.Connection,
      config: Config,
      dialect: DialectConfig,
      autoCommit: Boolean,
      rollBack0: () => Unit
  ) extends DbApi.Txn {
    def run[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
        implicit qr: Queryable[Q, R],
        fileName: sourcecode.FileName,
        lineNum: sourcecode.Line
    ): R = {

      val flattened = unpackQueryable(query, qr, config)
      if (qr.isGetGeneratedKeys(query).nonEmpty)
        updateGetGeneratedKeysSql(flattened)(qr.isGetGeneratedKeys(query).get, fileName, lineNum)
          .asInstanceOf[R]
      else if (qr.isExecuteUpdate(query)) updateSql(flattened).asInstanceOf[R]
      else {
        val res = stream(query, fetchSize, queryTimeoutSeconds)(
          qr.asInstanceOf[Queryable[Q, Seq[?]]],
          fileName,
          lineNum
        )
        if (qr.isSingleRow(query)) {
          val results = res.take(2).toVector
          assert(
            results.size == 1,
            s"Single row query must return 1 result, not ${results.size}"
          )
          results.head.asInstanceOf[R]
        } else {
          res.toVector.asInstanceOf[R]
        }
      }
    }

    def stream[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
        implicit qr: Queryable[Q, Seq[R]],
        fileName: sourcecode.FileName,
        lineNum: sourcecode.Line
    ): Generator[R] = {
      val flattened = unpackQueryable(query, qr, config)
      streamFlattened0(
        r => {
          qr.asInstanceOf[Queryable[Q, R]].construct(query, r) match {
            case s: Seq[R] => s.head
            case r: R => r
          }
        },
        flattened,
        fetchSize,
        queryTimeoutSeconds,
        fileName,
        lineNum
      )
    }

    def runSql[R](
        sql: SqlStr,
        fetchSize: Int = -1,
        queryTimeoutSeconds: Int = -1
    )(
        implicit qr: Queryable.Row[?, R],
        fileName: sourcecode.FileName,
        lineNum: sourcecode.Line
    ): IndexedSeq[R] = streamSql(sql, fetchSize, queryTimeoutSeconds).toVector

    def streamSql[R](
        sql: SqlStr,
        fetchSize: Int = -1,
        queryTimeoutSeconds: Int = -1
    )(
        implicit qr: Queryable.Row[?, R],
        fileName: sourcecode.FileName,
        lineNum: sourcecode.Line
    ): Generator[R] = {
      streamFlattened0(
        qr.construct,
        SqlStr.flatten(sql),
        fetchSize,
        queryTimeoutSeconds,
        fileName,
        lineNum
      )
    }

    def updateSql(sql: SqlStr, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
        implicit fileName: sourcecode.FileName,
        lineNum: sourcecode.Line
    ): Int = {
      val flattened = SqlStr.flatten(sql)
      runRawUpdate0(
        flattened.renderSql(DialectConfig.castParams(dialect)),
        flattenParamPuts(flattened),
        fetchSize,
        queryTimeoutSeconds,
        fileName,
        lineNum
      )
    }

    def updateGetGeneratedKeysSql[R](
        sql: SqlStr,
        fetchSize: Int = -1,
        queryTimeoutSeconds: Int = -1
    )(
        implicit qr: Queryable.Row[?, R],
        fileName: sourcecode.FileName,
        lineNum: sourcecode.Line
    ): IndexedSeq[R] = {
      val flattened = SqlStr.flatten(sql)
      runRawUpdateGetGeneratedKeys0(
        flattened.renderSql(DialectConfig.castParams(dialect)),
        flattenParamPuts(flattened),
        fetchSize,
        queryTimeoutSeconds,
        fileName,
        lineNum,
        qr
      )
    }

    def runRaw[R](
        sql: String,
        variables: Seq[Any] = Nil,
        fetchSize: Int = -1,
        queryTimeoutSeconds: Int = -1
    )(
        implicit qr: Queryable.Row[?, R],
        fileName: sourcecode.FileName,
        lineNum: sourcecode.Line
    ): IndexedSeq[R] = {
      streamRaw(sql, variables, fetchSize, queryTimeoutSeconds).toVector
    }

    def streamRaw[R](
        sql: String,
        variables: Seq[Any] = Nil,
        fetchSize: Int = -1,
        queryTimeoutSeconds: Int = -1
    )(
        implicit qr: Queryable.Row[?, R],
        fileName: sourcecode.FileName,
        lineNum: sourcecode.Line
    ): Generator[R] = {
      streamRaw0[R](
        construct = qr.construct,
        sql,
        anySeqPuts(variables),
        fetchSize,
        queryTimeoutSeconds,
        fileName,
        lineNum
      )
    }

    def updateRaw(
        sql: String,
        variables: Seq[Any] = Nil,
        fetchSize: Int = -1,
        queryTimeoutSeconds: Int = -1
    )(implicit fileName: sourcecode.FileName, lineNum: sourcecode.Line): Int = runRawUpdate0(
      sql,
      anySeqPuts(variables),
      fetchSize,
      queryTimeoutSeconds,
      fileName,
      lineNum
    )

    def updateGetGeneratedKeysRaw[R](
        sql: String,
        variables: Seq[Any] = Nil,
        fetchSize: Int = -1,
        queryTimeoutSeconds: Int = -1
    )(
        implicit qr: Queryable.Row[?, R],
        fileName: sourcecode.FileName,
        lineNum: sourcecode.Line
    ): IndexedSeq[R] = runRawUpdateGetGeneratedKeys0(
      sql,
      anySeqPuts(variables),
      fetchSize,
      queryTimeoutSeconds,
      fileName,
      lineNum,
      qr
    )

    def streamFlattened0[R](
        construct: Queryable.ResultSetIterator => R,
        flattened: SqlStr.Flattened,
        fetchSize: Int,
        queryTimeoutSeconds: Int,
        fileName: sourcecode.FileName,
        lineNum: sourcecode.Line
    ) = streamRaw0(
      construct,
      flattened.renderSql(DialectConfig.castParams(dialect)),
      flattenParamPuts(flattened),
      fetchSize,
      queryTimeoutSeconds,
      fileName,
      lineNum
    )

    def streamRaw0[R](
        construct: Queryable.ResultSetIterator => R,
        sql: String,
        variables: Seq[(PreparedStatement, Int) => Unit],
        fetchSize: Int,
        queryTimeoutSeconds: Int,
        fileName: sourcecode.FileName,
        lineNum: sourcecode.Line
    ) = new Generator[R] {
      def generate(handleItem: R => Generator.Action): Generator.Action = {
        val statement = connection.prepareStatement(sql)
        for ((setVariable, i) <- variables.iterator.zipWithIndex) setVariable(statement, i + 1)

        configureRunCloseStatement(
          statement,
          fetchSize,
          queryTimeoutSeconds,
          sql,
          fileName,
          lineNum
        ) { stmt =>
          val resultSet = stmt.executeQuery()
          var action: Generator.Action = Generator.Continue
          while (resultSet.next() && action == Generator.Continue) {
            val rowRes = construct(new Queryable.ResultSetIterator(resultSet))
            action = handleItem(rowRes)
          }
          action
        }
      }
    }

    def runRawUpdate0(
        sql: String,
        variables: Seq[(PreparedStatement, Int) => Unit],
        fetchSize: Int,
        queryTimeoutSeconds: Int,
        fileName: sourcecode.FileName,
        lineNum: sourcecode.Line
    ): Int = {
      // Sqlite and HsqlExpr for some reason blow up if you try to do DDL
      // like DROP TABLE or CREATE TABLE inside a prepared statement, so
      // fall back to vanilla `createStatement`
      if (variables.isEmpty) {
        val statement = connection.createStatement()
        configureRunCloseStatement(
          statement,
          fetchSize,
          queryTimeoutSeconds,
          sql,
          fileName,
          lineNum
        )(_.executeUpdate(sql))
      } else {
        val statement = connection.prepareStatement(sql)
        for ((v, i) <- variables.iterator.zipWithIndex) v(statement, i + 1)
        configureRunCloseStatement(
          statement,
          fetchSize,
          queryTimeoutSeconds,
          sql,
          fileName,
          lineNum
        )(_.executeUpdate())
      }
    }

    def runRawUpdateGetGeneratedKeys0[R](
        sql: String,
        variables: Seq[(PreparedStatement, Int) => Unit],
        fetchSize: Int,
        queryTimeoutSeconds: Int,
        fileName: sourcecode.FileName,
        lineNum: sourcecode.Line,
        qr: Queryable.Row[?, R]
    ): IndexedSeq[R] = {
      val statement = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)
      for ((v, i) <- variables.iterator.zipWithIndex) v(statement, i + 1)
      configureRunCloseStatement(
        statement,
        fetchSize,
        queryTimeoutSeconds,
        sql,
        fileName,
        lineNum
      ) { stmt =>
        stmt.executeUpdate()
        val resultSet = stmt.getGeneratedKeys
        val output = Vector.newBuilder[R]
        while (resultSet.next()) {
          val rowRes = qr.construct(new Queryable.ResultSetIterator(resultSet))
          output.addOne(rowRes)
        }
        output.result()
      }
    }

    def configureRunCloseStatement[P <: Statement, T](
        statement: P,
        fetchSize: Int,
        queryTimeoutSeconds: Int,
        sql: String,
        fileName: sourcecode.FileName,
        lineNum: sourcecode.Line
    )(f: P => T): T = {
      if (autoCommit) connection.setAutoCommit(true)
      Seq(fetchSize, config.defaultFetchSize).find(_ != -1).foreach(statement.setFetchSize)
      Seq(queryTimeoutSeconds, config.defaultQueryTimeoutSeconds)
        .find(_ != -1)
        .foreach(statement.setQueryTimeout)
      config.logSql(sql, fileName.value, lineNum.value)
      try f(statement)
      finally statement.close()
    }

    def renderSql[Q, R](query: Q, castParams: Boolean = false)(
        implicit qr: Queryable[Q, R]
    ): String = {
      DbApi.renderSql(query, config, castParams)
    }

    val savepointStack = collection.mutable.ArrayDeque.empty[java.sql.Savepoint]

    def savepoint[T](block: DbApi.Savepoint => T): T = {
      val savepoint = connection.setSavepoint()
      savepointStack.append(savepoint)

      try {
        val res = block(new DbApi.SavepointImpl(savepoint, () => rollbackSavepoint(savepoint)))
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

    private def flattenParamPuts[T](flattened: SqlStr.Flattened) = {
      flattened.interpsIterator.map(v => (s, n) => v.mappedType.put(s, n, cast(v.value))).toSeq
    }

    private def anySeqPuts(variables: Seq[Any]) = {
      variables.map(v => (s: PreparedStatement, n: Int) => s.setObject(n, v))
    }

    def close() = connection.close()
  }

  class SavepointImpl(savepoint: java.sql.Savepoint, rollback0: () => Unit) extends Savepoint {
    def savepointId = savepoint.getSavepointId
    def savepointName = savepoint.getSavepointName
    def rollback() = rollback0()
  }
}
