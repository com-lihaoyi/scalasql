package usql

import usql.SqlString.SqlStringSyntax

/**
 * Models the various components of a SQL query:
 *
 * {{{
 *  SELECT DISTINCT column, AGG_FUNC(column_or_expression), …
 *  FROM mytable
 *  JOIN another_table ON mytable.column = another_table.column
 *  WHERE constraint_expression
 *  GROUP BY column HAVING constraint_expression
 *  ORDER BY column ASC/DESC
 *  LIMIT count OFFSET COUNT;
 * }}}
 */
case class Query[T](expr: T,
                    from: Query.From,
                    joins: Seq[Query.Join],
                    where: Seq[Expr[_]],
                    groupBy: Option[Query.GroupBy],
                    orderBy: Option[Query.OrderBy],
                    limit: Option[Int],
                    offset: Option[Int]) extends Query.From{
  def map[V](f: T => V): Query[V] = {
    copy(expr = f(expr))
  }
  def flatMap[V](f: T => Query[V]): Query[V] = {
    val other = f(expr)
    Query(
      other.expr,
      from,
      joins ++ Seq(Query.Join(other.from, None, None)),
      where ++ other.where,
      groupBy,
      orderBy,
      limit,
      offset
    )
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
      joins ++ Seq(Query.Join(other.from, None, None)),
      Seq(on(expr, other.expr)) ++ where ++ other.where,
      groupBy,
      orderBy,
      limit,
      offset
    )
}

object Query {
  def fromTable[T](e: T, table: usql.Query.TableRef) = {
    Query(e, table, Nil, Nil, None, None, None, None)
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
  class TableRef(val value: Table.Base) extends From
  class SubqueryRef[T](val value: Query[T], val qr: Queryable[T, _]) extends From

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
  def expr(tableRef: Query.TableRef): Expr[T] = new Expr[T] {
    def toSqlExpr = {
      SqlString.raw(DatabaseApi.fromNaming.value(tableRef)) +
        usql"." +
        SqlString.raw(DatabaseApi.columnNameMapper.value(name.value))
    }

    def toTables = Set(table)
  }
}
