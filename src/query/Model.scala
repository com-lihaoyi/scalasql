package usql.query

import usql.{Queryable, Table}

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

trait From
class TableRef(val value: Table.Base) extends From {
  override def toString = s"TableRef(${value.tableName})"
}
class SubqueryRef[T](val value: Select[T], val qr: Queryable[T, _]) extends From

case class GroupBy(expr: Expr[_], having: Seq[Expr[_]])

case class Join(prefix: Option[String], from: Seq[JoinFrom])
case class JoinFrom(from: From, on: Option[Expr[_]])
