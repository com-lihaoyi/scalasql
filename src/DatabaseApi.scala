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
  def handleResultRow[V](resultSet: ResultSet, columnNameUnMapper: String => String, rowVisitor: Visitor[_, V]): V  = {
    val kvs = collection.mutable.Buffer.empty[(List[String], String)]
    val meta = resultSet.getMetaData

    for (i <- Range(0, meta.getColumnCount)) {
      val k = meta.getColumnLabel(i + 1).toLowerCase.split(FlatJson.delimiter).map(columnNameUnMapper).toList.drop(1)
      val v = resultSet.getString(i + 1)
      kvs.append(k -> v)
    }

    def visitValue(group: Seq[(List[String], String)], v: ObjArrVisitor[Any, Any]) = {
      val sub = v.subVisitor.asInstanceOf[Visitor[Any, Any]]
      val groupTail = group.map { case (k, v) => (k.tail, v) }
      v.visitValue(rec(groupTail, sub), -1)
    }

    def rec(kvs: Seq[(List[String], String)], visitor: Visitor[Any, Any]): Any = {
      kvs match{
        case Seq((Nil, null)) => visitor.visitNull(-1)
        case Seq((Nil, v)) => visitor.visitString(v, -1)
        case _ =>
          val grouped = kvs.groupBy(_._1.head)
          // Hack to check if a random key looks like a number,
          // in which case this data represents an array
          if (kvs.head._1.head.head.isDigit){
            val arrVisitor = visitor.visitArray(-1, -1)
            for ((k, group) <- grouped.toSeq.sortBy(_._1.toInt)) visitValue(group, arrVisitor)
            arrVisitor.visitEnd(-1)
          }else {
            val objVisitor = visitor.visitObject(-1, true, -1)
            for ((k, group) <- grouped){
              val keyVisitor = objVisitor.visitKey(-1)
              objVisitor.visitKeyValue(keyVisitor.visitString(k, -1))
              visitValue(group, objVisitor)
            }
            objVisitor.visitEnd(-1)
          }
      }
    }

    rec(kvs.toSeq, rowVisitor.asInstanceOf[Visitor[Any, Any]]).asInstanceOf[V]
  }
}