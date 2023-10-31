package scalasql

import geny.Generator
import renderer.{Context, SqlStr}
import upickle.core.Visitor
import scalasql.dialects.DialectConfig
import scalasql.utils.{FlatJson, OptionPickler}

import java.sql.{PreparedStatement, ResultSet, Statement}

trait DbApi {
  def savepoint[T](block: DbApi.Savepoint => T): T
  def rollback(): Unit

  def toSqlQuery[Q, R](query: Q, castParams: Boolean = false)(implicit qr: Queryable[Q, R]): String
  def run[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
      implicit qr: Queryable[Q, R]
  ): R
  def stream[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
      implicit qr: Queryable[Q, Seq[R]]
  ): Generator[R]
  def runUpdate(sql: SqlStr): Int
  def runRawUpdate(sql: String, variables: Any*): Int
  def runRawUpdateBatch(sql: Seq[String]): Seq[Int]
  def runQuery[T](sql: SqlStr)(block: ResultSet => T): T
  def runRawQuery[T](sql: String, variables: Any*)(block: ResultSet => T): T
}

object DbApi {

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
  ) extends DbApi {
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
    ): (String, Seq[SqlStr.Interp.TypeInterp[_]], Seq[MappedType[_]]) = {
      val ctx = Context(Map(), Map(), config, dialectConfig.defaultQueryableSuffix)
      val (sqlStr, mappedTypes) = qr.toSqlQuery(query, ctx)
      val flattened = SqlStr.flatten(sqlStr)
      val queryStr = flattened.queryParts.zipAll(flattened.params, "", null).map {
        case (part, null) => part
        case (part, param) =>
          val jdbcTypeString = param.mappedType.typeString
          if (castParams) part + s"CAST(? AS $jdbcTypeString)" else part + "?"
      }.mkString

      (queryStr, flattened.params, mappedTypes)
    }

    def run[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
        implicit qr: Queryable[Q, R]
    ): R = {
      val (exprs, statement) = prepareRun(query, fetchSize, queryTimeoutSeconds)

      if (qr.isExecuteUpdate(query)) statement.executeUpdate().asInstanceOf[R]
      else {
        val resultSet: ResultSet = statement.executeQuery()

        try {
          if (qr.singleRow(query)) {
            assert(resultSet.next())
            val res = handleResultRow(resultSet, qr.valueReader(query), exprs, config)
            assert(!resultSet.next())
            res
          } else {
            val arrVisitor = qr.valueReader(query).visitArray(-1, -1)
            while (resultSet.next()) {
              val rowRes = handleResultRow(resultSet, arrVisitor.subVisitor, exprs, config)
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
      Seq(queryTimeoutSeconds, config.defaultQueryTimeoutSeconds).find(_ != -1)
        .foreach(statement.setQueryTimeout)

      for ((p, n) <- params.zipWithIndex) {
        p.mappedType.asInstanceOf[MappedType[Any]].put(statement, n + 1, p.value)
      }
      (exprs, statement)
    }

    def stream[Q, R](query: Q, fetchSize: Int = -1, queryTimeoutSeconds: Int = -1)(
        implicit qr: Queryable[Q, Seq[R]]
    ): Generator[R] = new Generator[R] {
      def generate(handleItem: R => Generator.Action): Generator.Action = {
        val (exprs, statement) = prepareRun(query, fetchSize, queryTimeoutSeconds)

        val resultSet: ResultSet = statement.executeQuery()
        try {
          val visitor = qr.valueReader(query).asInstanceOf[OptionPickler.SeqLikeReader[Seq, R]].r
          var action: Generator.Action = Generator.Continue
          while (resultSet.next() && action == Generator.Continue) {

            val rowRes = handleResultRow(resultSet, visitor, exprs, config)
            action = handleItem(rowRes)
          }

          action
        } finally {
          resultSet.close()
          statement.close()
        }
      }
    }
  }

  class SavepointApiImpl(savepoint: java.sql.Savepoint, rollback0: () => Unit) extends Savepoint {
    def savepointId = savepoint.getSavepointId
    def savepointName = savepoint.getSavepointName
    def rollback() = rollback0()
  }

  def handleResultRow[V](
      resultSet: ResultSet,
      rowVisitor: Visitor[_, V],
      exprs: Seq[MappedType[_]],
      config: Config
  ): V = {

    val keys = Array.newBuilder[IndexedSeq[String]]
    val values = Array.newBuilder[Object]
    val metadata = resultSet.getMetaData

    for (i <- Range(0, metadata.getColumnCount)) {
      val k = metadata.getColumnLabel(i + 1).split(config.columnLabelDelimiter)
        .map(s => config.columnNameUnMapper(s.toLowerCase)).drop(1)

      val v = exprs(i).get(resultSet, i + 1).asInstanceOf[Object]

      keys.addOne(k)
      values.addOne(v)
    }

    FlatJson.unflatten[V](keys.result(), values.result(), rowVisitor)
  }
}
