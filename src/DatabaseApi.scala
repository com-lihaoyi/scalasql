package usql

import renderer.{Context, Interp, SelectToSql, SqlStr}
import upickle.core.{ArrVisitor, ObjArrVisitor, Visitor}
import usql.DatabaseApi.handleResultRow

import java.sql.{ResultSet, Statement}
import scala.collection.mutable

class DatabaseApi(connection: java.sql.Connection,
                  tableNameMapper: String => String = identity,
                  tableNameUnMapper: String => String = identity,
                  columnNameMapper: String => String = identity,
                  columnNameUnMapper: String => String = identity) {

  def runRaw(sql: String) = {
    val statement: Statement = connection.createStatement()
    try statement.executeUpdate(sql)
    finally statement.close()
  }

  def toSqlQuery[T, V](query: T)
                      (implicit qr: Queryable[T, V]): String = {
    val (str, params) = toSqlQuery0(query)
    str
  }

  def toSqlQuery0[T, V](query: T)
                       (implicit qr: Queryable[T, V]): (String, Seq[Interp]) = {
    val ctx = new Context(Map(), Map(), tableNameMapper, columnNameMapper)
    val flattened = SqlStr.flatten(qr.toSqlQueryUnwrapped(query, ctx))
    val queryStr0 = flattened.queryParts.mkString("?")
    val queryStr = if (flattened.isCompleteQuery) queryStr0.drop(1).dropRight(1) else queryStr0
    (queryStr, flattened.params)
  }

  def run[T, V](query: T)
               (implicit qr: Queryable[T, V]): V = {

    val (str, params) = toSqlQuery0(query)
    val statement = connection.prepareStatement(str)

    for((p, n) <- params.zipWithIndex) p match{
      case Interp.StringInterp(s) => statement.setString(n + 1, s)
      case Interp.IntInterp(i) => statement.setInt(n + 1, i)
      case Interp.DoubleInterp(d) => statement.setDouble(n + 1, d)
      case Interp.BooleanInterp(b) => statement.setBoolean(n + 1, b)
    }

    if (qr.isExecuteUpdate) statement.executeUpdate().asInstanceOf[V]
    else {
      val resultSet: ResultSet = statement.executeQuery()

      try {
        if (qr.singleRow){
          assert(resultSet.next())
          val res = handleResultRow(resultSet, columnNameUnMapper, qr.valueReader)
          assert(!resultSet.next())
          res
        }else {
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

object DatabaseApi{
  def handleResultRow[V](resultSet: ResultSet,
                         columnNameUnMapper: String => String,
                         rowVisitor: Visitor[_, V]): V  = {

    val keys = IndexedSeq.newBuilder[IndexedSeq[String]]
    val values = IndexedSeq.newBuilder[String]
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