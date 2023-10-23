package scalasql

import renderer.{Context, SqlStr}
import upickle.core.Visitor
import scalasql.DatabaseApi.handleResultRow
import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.utils.FlatJson

import java.sql.{ResultSet, Statement}
import scala.collection.mutable

class DatabaseApi(
    connection: java.sql.Connection,
    tableNameMapper: String => String = identity,
    tableNameUnMapper: String => String = identity,
    columnNameMapper: String => String = identity,
    columnNameUnMapper: String => String = identity,
    defaultQueryableSuffix: String,
    castParams: Boolean
) {

  def runRaw(sql: String) = {
    val statement: Statement = connection.createStatement()

    try {
      for(s <- sql.split(';') if s.trim.nonEmpty){
        statement.executeUpdate(s)
      }
    }
    finally statement.close()
  }

  def toSqlQuery[Q, R](query: Q, castParams: Boolean = false)(implicit qr: Queryable[Q, R]): String = {
    val (str, params) = toSqlQuery0(query)
    str
  }

  def toSqlQuery0[Q, R](query: Q,
                        castParams: Boolean = false)
                       (implicit qr: Queryable[Q, R]): (String, Seq[SqlStr.Interp.TypeInterp[_]]) = {
    val ctx = Context(Map(), Map(), tableNameMapper, columnNameMapper, defaultQueryableSuffix)
    val flattened = SqlStr.flatten(qr.toSqlQuery(query, ctx))
    val queryStr = flattened.queryParts.zipAll(flattened.params, "", null)
      .map{
        case (part, null) => part
        case (part, param) =>
          if (castParams) part + s"CAST(? AS ${param.jdbcType})"
          else part + "?"
      }
      .mkString

    (queryStr, flattened.params)
  }

  def run[Q, R](query: Q)(implicit qr: Queryable[Q, R]): R = {

    val (str, params) = toSqlQuery0(query, castParams)
    val statement = connection.prepareStatement(str)

    for ((p, n) <- params.zipWithIndex) {
      statement.setObject(n + 1, p.value)
    }

    if (qr.isExecuteUpdate(query)) statement.executeUpdate().asInstanceOf[R]
    else {
      val resultSet: ResultSet = statement.executeQuery()

      try {
        if (qr.singleRow(query)) {
          assert(resultSet.next())
          val res = handleResultRow(resultSet, columnNameUnMapper, qr.valueReader(query))
          assert(!resultSet.next())
          res
        } else {
          val arrVisitor = qr.valueReader(query).visitArray(-1, -1)
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
