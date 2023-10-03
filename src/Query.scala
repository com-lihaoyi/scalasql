package usql

import usql.SqlString.SqlStringSyntax

case class Query[T](expr: T,
                    from: Query.From,
                    joins: Seq[Query.Join],
                    where: Seq[Expr[_]],
                    groupBy: Option[Query.GroupBy],
                    having: Option[Expr[_]],
                    orderBy: Option[Query.OrderBy],
                    limit: Option[Int],
                    offset: Option[Int]) extends Query.From{
  def map[V](f: T => V): Query[V] = {
    copy(expr = f(expr))
  }

  def filter(f: T => Expr[Boolean]): Query[T] = {
    copy(where = Seq(f(expr)) ++ where)
  }

  def sortBy(f: T => Expr[_]) = {
    copy(orderBy = Some(Query.OrderBy(f(expr), None, None)))
  }

  def asc = copy(orderBy = Some(orderBy.get.copy(ascDesc = Some(Query.AscDesc.Asc))))
  def desc = copy(orderBy = Some(orderBy.get.copy(ascDesc = Some(Query.AscDesc.Desc))))

  def nullsFirst = copy(orderBy = Some(orderBy.get.copy(nulls = Some(Query.Nulls.First))))
  def nullsLast = copy(orderBy = Some(orderBy.get.copy(nulls = Some(Query.Nulls.Last))))

  def drop(n: Int) = copy(offset = Some(n))

  def take(n: Int) = copy(limit = Some(n))

  def join[V](other: Query[V])(on: (T, V) => Expr[Boolean]): Query[(T, V)] =
    Query(
      (expr, other.expr),
      from,
      joins,
      Seq(on(expr, other.expr)) ++ where ++ other.where,
      groupBy,
      having,
      orderBy,
      limit,
      offset
    )
}

object Query {
  def fromTable[T](e: T, table: usql.Table.Base) = {
    Query(e, Table(table), Nil, Nil, None, None, None, None, None)
  }

  case class OrderBy(expr: Expr[_],
                     ascDesc: Option[AscDesc],
                     nulls: Option[Nulls])

  sealed trait AscDesc
  object AscDesc {
    case object Asc extends AscDesc
    case object Desc extends AscDesc
  }

  sealed trait Nulls
  object Nulls {
    case object First extends Nulls
    case object Last extends Nulls
  }


  sealed trait From
  case class Table(t: usql.Table.Base) extends From
  case class GroupBy(expr: Expr[_], having: Option[Expr[_]])

  case class Join(from: From, as: Option[String], on: Option[Expr[_]])
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

