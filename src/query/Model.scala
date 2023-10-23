package scalasql.query

import scalasql.{Queryable, Table}

case class OrderBy(expr: Expr[_], ascDesc: Option[AscDesc], nulls: Option[Nulls])

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

trait From
class TableRef(val value: Table.Base) extends From {
  override def toString = s"TableRef(${value.tableName})"
}
class SubqueryRef[Q, R](val value: Select[Q, R], val qr: Queryable[Q, R]) extends From

case class GroupBy(expr: Expr[_], having: Seq[Expr[_]])

case class Join(prefix: Option[String], from: Seq[JoinFrom])
case class JoinFrom(from: From, on: Option[Expr[_]])
