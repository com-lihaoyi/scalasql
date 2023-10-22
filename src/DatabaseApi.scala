package usql

import renderer.{Context, Interp, SqlStr}
import upickle.core.Visitor
import usql.DatabaseApi.handleResultRow
import usql.utils.FlatJson

import java.sql.{ResultSet, Statement}
import scala.collection.mutable

class DatabaseApi(
    connection: java.sql.Connection,
    tableNameMapper: String => String = identity,
    tableNameUnMapper: String => String = identity,
    columnNameMapper: String => String = identity,
    columnNameUnMapper: String => String = identity
) {

  def runRaw(sql: String) = {
    val statement: Statement = connection.createStatement()
    try statement.executeUpdate(sql)
    finally statement.close()
  }

  def toSqlQuery[Q](query: Q)(implicit qr: Queryable[Q, _]): String = {
    val (str, params) = toSqlQuery0(query)
    str
  }

  def toSqlQuery0[Q](query: Q)(implicit qr: Queryable[Q, _]): (String, Seq[Interp]) = {
    val ctx = new Context(Map(), Map(), tableNameMapper, columnNameMapper)
    val flattened = SqlStr.flatten(qr.toSqlQuery(query, ctx))
    val queryStr = flattened.queryParts.mkString("?")
    (queryStr, flattened.params)
  }

  def run[Q, R](query: Q)(implicit qr: Queryable[Q, R]): R = {

    val (str, params) = toSqlQuery0(query)
    val statement = connection.prepareStatement(str)

    for ((p, n) <- params.zipWithIndex) p match {
      case Interp.StringInterp(s) => statement.setString(n + 1, s)
      case Interp.IntInterp(i) => statement.setInt(n + 1, i)
      case Interp.DoubleInterp(d) => statement.setDouble(n + 1, d)
      case Interp.BooleanInterp(b) => statement.setBoolean(n + 1, b)
      case Interp.DateInterp(d) => statement.setDate(n + 1, d)
      case Interp.TimeInterp(t) => statement.setTime(n + 1, t)
      case Interp.TimestampInterp(ts) => statement.setTimestamp(n + 1, ts)
    }

    if (qr.isExecuteUpdate(query)) statement.executeUpdate().asInstanceOf[R]
    else {
      val resultSet: ResultSet = statement.executeQuery()

      try {
        if (qr.singleRow(query)) {
          assert(resultSet.next())
          val res = handleResultRow(resultSet, columnNameUnMapper, qr.valueReader)
          assert(!resultSet.next())
          res
        } else {
          val arrVisitor = qr.valueReader.visitArray(-1, -1)
          while (resultSet.next()) {
            val rowRes = handleResultRow(resultSet, columnNameUnMapper, arrVisitor.subVisitor)
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
      columnNameUnMapper: String => String,
      rowVisitor: Visitor[_, V]
  ): V = {

    val keys = Array.newBuilder[IndexedSeq[String]]
    val values = Array.newBuilder[String]
    val metadata = resultSet.getMetaData

    for (i <- Range(0, metadata.getColumnCount)) {
      val k = metadata.getColumnLabel(i + 1)
        .split(FlatJson.delimiter)
        .map(s => columnNameUnMapper(s.toLowerCase))
        .drop(1)

      val v = resultSet.getString(i + 1)

      keys.addOne(k)
      values.addOne(v)
    }

    FlatJson.unflatten[V](keys.result(), values.result(), rowVisitor)
  }
}
