package usql
object Ast {

  case class Select(exprs: Seq[Expr],
                    from: From,
                    as: Option[String],
                    joins: Seq[Join],
                    where: Option[Expr],
                    groupBy: Option[Expr],
                    having: Option[Expr],
                    orderBy: Option[OrderBy],
                    limit: Option[Int])

  case class OrderBy(expr: Expr,
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

  case class Expr()

  sealed trait From

  case class Table(schemaName: Option[String],
                   tableName: String,
                   tableAlias: Option[String]) extends From

  case class Join(from: From, as: Option[String], on: Option[Expr])
}
