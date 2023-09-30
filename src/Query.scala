package usql

case class Query[T](expr: T,
                    filter: Seq[Expr[Boolean]] = Nil)
                   (implicit q: Queryable[T]) {

  def map[V: Queryable](f: T => V): Query[V] = Query(f(expr), filter)
  def filter(f: T => Expr[Boolean]): Query[T] = Query(expr, filter ++ Seq(f(expr)))

  def toSqlQuery: String = {
    val exprs = q.toAtomics(expr).map(_.toSqlExpr).mkString(", ")
    val tables = q.toTables(expr).map(_.tableName).mkString(", ")
    val filtersOpt =
      if (filter.isEmpty) ""
      else " WHERE " + filter.flatMap(_.toAtomics).map(_.toSqlExpr).mkString(" AND ")

    s"SELECT $exprs FROM $tables$filtersOpt"
  }
}
