package scalasql.query

import scalasql.{Queryable, Table}

/**
 * Models a SQL `ORDER BY` clause
 */
case class OrderBy(expr: Expr[_], ascDesc: Option[AscDesc], nulls: Option[Nulls])

sealed trait AscDesc

object AscDesc {

  /**
   * Models a SQL `ASC` clause
   */
  case object Asc extends AscDesc

  /**
   * Models a SQL `DESC` clause
   */
  case object Desc extends AscDesc
}

sealed trait Nulls

object Nulls {

  /**
   * Models a SQL `NULLS FIRST` clause
   */
  case object First extends Nulls

  /**
   * Models a SQL `NULSL LAST` clause
   */
  case object Last extends Nulls
}

/**
 * Models a SQL `FROM` clause
 */
sealed trait From
class TableRef(val value: Table.Base) extends From {
  override def toString = s"TableRef(${value.tableName})"
}
class SubqueryRef[Q, R](val value: Select[Q, R], val qr: Queryable[Q, R]) extends From

/**
 * Models a SQL `GROUP BY` clause
 */
case class GroupBy(key: Expr[_], select: () => Select[_, _], having: Seq[Expr[_]])

/**
 * Models a SQL `JOIN` clause
 */
case class Join(prefix: String, from: Seq[Join.From])
object Join {
  case class From(from: scalasql.query.From, on: Option[Expr[_]])
}
