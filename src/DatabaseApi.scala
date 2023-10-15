package usql

import renderer.{Context, Interp, SelectToSql, SqlStr}

import java.sql.{ResultSet, Statement}

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
    toSqlQuery0(query).queryParts.mkString("?")
  }

  def toSqlQuery0[T, V](query: T)
                       (implicit qr: Queryable[T, V]): SqlStr = {
    val ctx = new Context(Map(), Map(), tableNameMapper, columnNameMapper)
    SqlStr.flatten(qr.toSqlQuery(query, ctx))
  }

  def run[T, V](query: T)
               (implicit qr: Queryable[T, V]): V = {

    val querySqlStr = toSqlQuery0(query)

    val queryStr = querySqlStr.queryParts.mkString("?")
    val statement = connection.prepareStatement(queryStr)

    for((p, n) <- querySqlStr.params.zipWithIndex) p match{
      case Interp.StringInterp(s) => statement.setString(n + 1, s)
      case Interp.IntInterp(i) => statement.setInt(n + 1, i)
      case Interp.DoubleInterp(d) => statement.setDouble(n + 1, d)
      case Interp.BooleanInterp(b) => statement.setBoolean(n + 1, b)
    }

    if (qr.isExecuteUpdate){

      statement.executeUpdate().asInstanceOf[V]
    }else {
      val resultSet: ResultSet = statement.executeQuery()

      val jsonRes = collection.mutable.ArrayBuffer.empty[ujson.Value]
      try {
        while (resultSet.next()) {
          val kvs = collection.mutable.Buffer.empty[(String, ujson.Value)]
          val meta = resultSet.getMetaData

          for (i <- Range(0, meta.getColumnCount)) {
            val v = resultSet.getString(i + 1) match{
              case null => ujson.Null
              case s => ujson.Str(s)
            }
            kvs.append(meta.getColumnLabel(i + 1).toLowerCase -> v)
          }

          val json = FlatJson.unflatten(kvs.toSeq)
          def unMapJson(j: ujson.Value): ujson.Value = j match{
            case ujson.Obj(kvs) => ujson.Obj.from(kvs.map{case (k, v) => (columnNameUnMapper(k), unMapJson(v))})
            case ujson.Arr(vs) => ujson.Arr(vs.map(unMapJson))
            case j => j
          }

          val unMappedJson = unMapJson(json)
          jsonRes.append(unMappedJson)
        }
      } finally {
        resultSet.close()
        statement.close()
      }

      OptionPickler.read[V](qr.unpack(new ujson.Arr(jsonRes)))(qr.valueReader)
    }
  }
}
