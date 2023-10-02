package usql

import usql.SqlString.SqlStringSyntax

case class Query[T](expr: T, filters: Seq[Expr[Boolean]] = Nil) {
  def map[V](f: T => V): Query[V] = Query(f(expr), filters)
  def filter(f: T => Expr[Boolean]): Query[T] = Query(expr, filters ++ Seq(f(expr)))
  def join[V](other: Query[V])(on: (T, V) => Expr[Boolean]): Query[(T, V)] =
    Query((expr, other.expr), Seq(on(expr, other.expr)) ++ filters ++ other.filters)
}

trait Expr[T] {
  def toSqlExpr: SqlString
  def toTables: Set[Table.Base]
}

object Expr{
  implicit def exprW[T]: OptionPickler.Writer[Expr[T]] = {
    OptionPickler.writer[SqlString].comap[Expr[T]](_.toSqlExpr)
  }
  def apply[T](x: T)(implicit conv: T => Interp) = new Expr[T] {
    override def toSqlExpr: SqlString = new SqlString(Seq("", ""), Seq(conv(x)), ())
    override def toTables: Set[Table.Base] = Set()
  }
}

case class Column[T]()(implicit val name: sourcecode.Name,
                       val table: Table.Base) {
  def expr: Expr[T] = new Expr[T] {
    def toSqlExpr =
      SqlString.raw(DatabaseApi.tableNameMapper.value(table.tableName)) ++
        usql"." ++
        SqlString.raw(DatabaseApi.columnNameMapper.value(name.value))

    def toTables = Set(table)
  }
}

object Column{
  implicit def columnW[T]: OptionPickler.Writer[Column[T]] = {
    OptionPickler.writer[SqlString].comap[Column[T]](_.expr.toSqlExpr)
  }
}

