package usql

import usql.SqlStr.SqlStringSyntax
import Select._
trait SelectLike[Q]{
  def expr: Q
  def queryExpr[V](f: QueryToSql.Context => SqlStr)
                  (implicit qr: Queryable[Expr[V], V]): Expr[V]
}

class SelectProxy[Q](val expr: Q) extends SelectLike [Q]{
  def queryExpr[V](f: QueryToSql.Context => SqlStr)
                  (implicit qr: Queryable[Expr[V], V]): Expr[V] = {
    Expr[V]{implicit c => f(c)}
  }
}
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
 * https://www.postgresql.org/docs/current/sql-select.html
 *
 */
case class Select[Q](expr: Q,
                     from: Seq[From],
                     joins: Seq[Join],
                     where: Seq[Expr[_]],
                     groupBy0: Option[GroupBy],
                     orderBy: Option[OrderBy],
                     limit: Option[Int],
                     offset: Option[Int])
                    (implicit val qr: Queryable[Q, _]) extends Expr[Seq[Q]] with SelectLike[Q] with From{

  def simple(args: Iterable[_]*) = args.forall(_.isEmpty)
  def queryExpr[V](f: QueryToSql.Context => SqlStr)
                     (implicit qr: Queryable[Expr[V], V]): Expr[V] = {
    Expr[V] { implicit ctx:  QueryToSql.Context =>
      this.copy[Expr[V]](expr = Expr[V] { implicit ctx: QueryToSql.Context => f(ctx) }).toSqlExpr
    }
  }

  def subquery(implicit qr: Queryable[Q, _]) = new SubqueryRef[Q](this, qr)

  def map[V](f: Q => V)(implicit qr: Queryable[V, _]): Select[V] = {
    copy(expr = f(expr))
  }

  def flatMap[V](f: Q => Select[V])(implicit qr: Queryable[V, _]): Select[V] = {
    val other = f(expr)
    if (simple(other.groupBy0, other.orderBy, other.limit, other.offset)) {
      Select(
        other.expr,
        from ++ other.from,
        joins ++ other.joins,
        where ++ other.where,
        groupBy0,
        orderBy,
        limit,
        offset
      )
    }else{
      ???
    }
  }

  def filter(f: Q => Expr[Boolean]): Select[Q] = {
    (groupBy0.isEmpty, simple(limit, offset)) match{
      case (true, true) => copy(where = where ++ Seq(f(expr)))
      case (false, true) => copy(groupBy0 = groupBy0.map(g => g.copy(having = g.having ++ Seq(f(expr)))))
      case (false, _) => Select(expr, Seq(subquery), Nil, Seq(f(expr)), None, None, None, None)
    }
  }

  def sortBy(f: Q => Expr[_]) = {
    if (simple(limit, offset)) copy(orderBy = Some(OrderBy(f(expr), None, None)))
    else Select(expr, Seq(subquery), Nil, Nil, None, Some(OrderBy(f(expr), None, None)), None, None)
  }

  def asc = copy(orderBy = Some(orderBy.get.copy(ascDesc = Some(AscDesc.Asc))))
  def desc = copy(orderBy = Some(orderBy.get.copy(ascDesc = Some(AscDesc.Desc))))

  def nullsFirst = copy(orderBy = Some(orderBy.get.copy(nulls = Some(Nulls.First))))
  def nullsLast = copy(orderBy = Some(orderBy.get.copy(nulls = Some(Nulls.Last))))

  def drop(n: Int) = copy(offset = Some(offset.getOrElse(0) + n), limit = limit.map(_ - n))

  def take(n: Int) = copy(limit = Some(limit.fold(n)(math.min(_, n))))

  def join[V](other: Select[V])
             (implicit qr: Queryable[V, _]): Select[(Q, V)] = join0(other, None)

  def aggregate[E, V](f: SelectProxy[Q] => E)
                     (implicit qr: Queryable[E, V]): Expr[V] = {

    Expr[V]{implicit ctx: QueryToSql.Context =>
      this.copy(expr = f(new SelectProxy[Q](expr))).toSqlExpr
    }
  }

  def groupBy[K, V](groupKey: Q => K)
                   (groupAggregate: SelectProxy[Q] => V)
                   (implicit qrk: Queryable[K, _], qrv: Queryable[V, _]): Select[(K, V)] = {
    val groupKeyValue = groupKey(expr)
    val Seq((_, groupKeyExpr)) = qrk.walk(groupKeyValue)
    val newExpr = (groupKeyValue, groupAggregate(new SelectProxy[Q](this.expr)))
    val groupByOpt = Some(GroupBy(groupKeyExpr, Nil))
    if (simple(orderBy, limit, offset)) this.copy(expr = newExpr, groupBy0 = groupByOpt)
    else Select(
      expr = newExpr,
      from = Seq(new SubqueryRef[Q](this, qr)),
      joins = Nil,
      where = Nil,
      groupBy0 = groupByOpt,
      orderBy = None,
      limit = None,
      offset = None
    )
  }

  def joinOn[V](other: Select[V])
               (on: (Q, V) => Expr[Boolean])
               (implicit qr: Queryable[V, _]): Select[(Q, V)] = join0(other, Some(on))

  def join0[V](other: Select[V],
               on: Option[(Q, V) => Expr[Boolean]])
              (implicit joinQr: Queryable[V, _]): Select[(Q, V)] = {

    val thisTrivial = simple(this.groupBy0, this.orderBy, this.limit, this.offset)

    val otherTrivial = simple(other.groupBy0, other.orderBy, other.limit, other.offset)

    Select(
      expr = (expr, other.expr),
      from = if (thisTrivial) from else Seq(subquery),
      joins = (if (thisTrivial) joins else Nil) ++
        (if (otherTrivial) Seq(Join(None, other.from.map(JoinFrom(_, on.map(_(expr, other.expr))))))
        else Seq(Join(None, Seq(JoinFrom(new SubqueryRef(other, joinQr), on.map(_(expr, other.expr))))))),
      where = (if (thisTrivial) where else Nil) ++ (if (otherTrivial) other.where else Nil),
      groupBy0 = if (thisTrivial) groupBy0 else None,
      orderBy = if (thisTrivial) orderBy else None,
      limit = if (thisTrivial) limit else None,
      offset = if (thisTrivial) offset else None
    )
  }

  override def toSqlExpr0(implicit ctx: QueryToSql.Context): SqlStr = {
    Queryable.QueryQueryable(qr).toSqlQuery(this, ctx).copy(isCompleteQuery = true)
  }
}

object Select {
  def fromTable[T](e: T, table: TableRef)(implicit qr: Queryable[T, _]) = {
    Select(e, Seq(table), Nil, Nil, None, None, None, None)
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
  class TableRef(val value: Table.Base) extends From{
    override def toString = s"TableRef(${value.tableName})"
  }
  class SubqueryRef[T](val value: Select[T], val qr: Queryable[T, _]) extends From

  case class GroupBy(expr: Expr[_], having: Seq[Expr[_]])

  case class Join(prefix: Option[String], from: Seq[JoinFrom])

  case class JoinFrom(from: From, on: Option[Expr[_]])
}

