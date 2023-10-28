package scalasql

import renderer.{Context, SqlStr}
import upickle.core.Visitor
import scalasql.DatabaseApi.handleResultRow
import scalasql.dialects.DialectConfig
import scalasql.utils.FlatJson

import java.sql.{JDBCType, ResultSet, Statement}

class DatabaseApi(
    connection: java.sql.Connection,
    config: Config,
    dialectConfig: DialectConfig
) {
  var rolledBack = false
  def transaction[T](t: => T) = {
    connection.setAutoCommit(false)
    try {
      val res = t
      if (rolledBack) connection.rollback()
      else connection.commit()
      res
    }
    catch{case e: Throwable =>
      connection.rollback()
      throw e
    } finally{
      rolledBack = false
      connection.setAutoCommit(true)
    }
  }
  def runRaw(sql: String) = {
    val statement: Statement = connection.createStatement()

    try statement.executeUpdate(sql)
    finally statement.close()
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
        val jdbcTypeString = param.mappedType.jdbcType match {
          case JDBCType.TIMESTAMP_WITH_TIMEZONE => "TIMESTAMP WITH TIME ZONE"
          case JDBCType.TIME_WITH_TIMEZONE => "TIME WITH TIME ZONE"
          case n => n.toString
        }
        if (castParams) part + s"CAST(? AS $jdbcTypeString)" else part + "?"
    }.mkString

    (queryStr, flattened.params, mappedTypes)
  }

  def run[Q, R](query: Q)(implicit qr: Queryable[Q, R]): R = {

    val (str, params, exprs) = toSqlQuery0(query, dialectConfig.castParams)
    val statement = connection.prepareStatement(str)

    for ((p, n) <- params.zipWithIndex) {
      p.mappedType.asInstanceOf[scalasql.MappedType[Any]].put(statement, n + 1, p.value)
//      statement.setObject(n + 1, p.value)
    }

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
}

object DatabaseApi {
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
