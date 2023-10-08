package usql

import usql.SqlStr.SqlStringSyntax
import Query._

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
 *
 * Good syntax reference:
 *
 * https://www.cockroachlabs.com/docs/stable/selection-queries#set-operations
 *
 */
case class Query[Q](expr: Q,
                    from: Seq[From],
                    joins: Seq[Join],
                    where: Seq[Expr[_]],
                    groupBy: Option[GroupBy],
                    orderBy: Option[OrderBy],
                    limit: Option[Int],
                    offset: Option[Int])
                   (implicit val qr: Queryable[Q, _]) extends Expr[Seq[Q]] with From{

  def subquery(implicit qr: Queryable[Q, _]) = new SubqueryRef[Q](this, qr)

  def map[V](f: Q => V)(implicit qr: Queryable[V, _]): Query[V] = {
    copy(expr = f(expr))
  }

  def flatMap[V](f: Q => Query[V])(implicit qr: Queryable[V, _]): Query[V] = {
    val other = f(expr)
    if (other.groupBy.isEmpty && other.orderBy.isEmpty && other.limit.isEmpty && other.offset.isEmpty) {
      Query(
        other.expr,
        from ++ other.from,
        joins ++ other.joins,
        where ++ other.where,
        groupBy,
        orderBy,
        limit,
        offset
      )
    }else{
      ???
    }
  }

  def filter(f: Q => Expr[Boolean]): Query[Q] = {
    (groupBy.isEmpty, limit.isEmpty, offset.isEmpty) match{
      case (true, true, true) => copy(where = where ++ Seq(f(expr)))
      case (false, true, true) => copy(groupBy = groupBy.map(g => g.copy(having = g.having ++ Seq(f(expr)))))
      case (false, _, _) => Query(expr, Seq(subquery), Nil, Seq(f(expr)), None, None, None, None)
    }
  }

  def sortBy(f: Q => Expr[_]) = {
    if (limit.isEmpty && offset.isEmpty) copy(orderBy = Some(OrderBy(f(expr), None, None)))
    else Query(expr, Seq(subquery), Nil, Nil, None, Some(OrderBy(f(expr), None, None)), None, None)
  }

  def asc = copy(orderBy = Some(orderBy.get.copy(ascDesc = Some(AscDesc.Asc))))
  def desc = copy(orderBy = Some(orderBy.get.copy(ascDesc = Some(AscDesc.Desc))))

  def nullsFirst = copy(orderBy = Some(orderBy.get.copy(nulls = Some(Nulls.First))))
  def nullsLast = copy(orderBy = Some(orderBy.get.copy(nulls = Some(Nulls.Last))))

  def drop(n: Int) = copy(offset = Some(offset.getOrElse(0) + n))

  def take(n: Int) = copy(limit = Some(limit.fold(n)(math.min(_, n))))

  def join[V](other: Query[V])
             (implicit qr: Queryable[V, _]): Query[(Q, V)] = join0(other, None)

  def joinOn[V](other: Query[V])
               (on: (Q, V) => Expr[Boolean])
               (implicit qr: Queryable[V, _]): Query[(Q, V)] = join0(other, Some(on))

  def join0[V](other: Query[V],
               on: Option[(Q, V) => Expr[Boolean]])
              (implicit joinQr: Queryable[V, _]): Query[(Q, V)] = {

    val thisTrivial =
      this.groupBy.isEmpty &&
      this.orderBy.isEmpty &&
      this.limit.isEmpty &&
      this.offset.isEmpty

    val otherTrivial =
      other.groupBy.isEmpty &&
      other.orderBy.isEmpty &&
      other.limit.isEmpty &&
      other.offset.isEmpty

    Query(
      expr = (expr, other.expr),
      from = if (thisTrivial) from else Seq(subquery),
      joins = (if (thisTrivial) joins else Nil) ++
        (if (otherTrivial) Seq(Join(None, other.from.map(JoinFrom(_, on.map(_(expr, other.expr))))))
        else Seq(Join(None, Seq(JoinFrom(new SubqueryRef(other, joinQr), on.map(_(expr, other.expr))))))),
      where = (if (thisTrivial) where else Nil) ++ (if (otherTrivial) other.where else Nil),
      groupBy = if (thisTrivial) groupBy else None,
      orderBy = if (thisTrivial) orderBy else None,
      limit = if (thisTrivial) limit else None,
      offset = if (thisTrivial) offset else None
    )
  }

  override def toSqlExpr0(implicit ctx: QueryToSql.Context): SqlStr = {
    qr.toSqlQuery(this.expr, ctx)
  }
}

object Query {
  def fromTable[T](e: T, table: TableRef)(implicit qr: Queryable[T, _]) = {
    Query(e, Seq(table), Nil, Nil, None, None, None, None)
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

  case class GroupBy(expr: Expr[_], having: Seq[Expr[_]])

  case class Join(prefix: Option[String], from: Seq[JoinFrom])

  case class JoinFrom(from: From, on: Option[Expr[_]])
}

trait Expr[T] {
  final def toSqlExpr(implicit ctx: QueryToSql.Context): SqlStr = {
    ctx.exprNaming.get(this).getOrElse(toSqlExpr0)
  }
  def toSqlExpr0(implicit ctx: QueryToSql.Context): SqlStr
}

object Expr{
  def apply[T](f: QueryToSql.Context => SqlStr): Expr[T] = new Simple[T](f)
  class Simple[T](f: QueryToSql.Context => SqlStr) extends Expr[T]{
    def toSqlExpr0(implicit ctx: QueryToSql.Context): SqlStr = f(ctx)
  }

  def apply[T](x: T)(implicit conv: T => Interp) = new Expr[T] {
    override def toSqlExpr0(implicit ctx: QueryToSql.Context): SqlStr = new SqlStr(Seq("", ""), Seq(conv(x)))
  }
}

case class Column[T]()(implicit val name: sourcecode.Name,
                       val table: Table.Base) {
  def expr(tableRef: TableRef): Expr[T] = new Expr[T] {
    def toSqlExpr0(implicit ctx: QueryToSql.Context) = {
      SqlStr.raw(ctx.fromNaming(tableRef)) +
        usql"." +
        SqlStr.raw(ctx.columnNameMapper(name.value))
    }
  }
}
