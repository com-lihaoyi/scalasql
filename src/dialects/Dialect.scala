package scalasql.dialects

import scalasql.operations.TableOps
import scalasql.query.{Aggregatable, Expr, Select}
import scalasql.{MappedType, Queryable, Table, operations}

object Dialect {}
trait Dialect extends DialectConfig {
  implicit def ExprBooleanOpsConv(v: Expr[Boolean]): operations.ExprBooleanOps =
    new operations.ExprBooleanOps(v)
  implicit def ExprNumericOpsConv[T: Numeric: MappedType](
      v: Expr[T]
  ): operations.ExprNumericOps[T] = new operations.ExprNumericOps(v)
  implicit def ExprOpsConv(v: Expr[_]): operations.ExprOps = new operations.ExprOps(v)
  implicit def ExprOptionOpsConv[T](v: Expr[Option[T]]): operations.ExprOptionOps[T] =
    new operations.ExprOptionOps(v)
  implicit def ExprStringOpsConv(v: Expr[String]): operations.ExprStringOps
  implicit def AggNumericOpsConv[V: Numeric: MappedType](v: Aggregatable[Expr[V]])(
      implicit qr: Queryable[Expr[V], V]
  ): operations.AggNumericOps[V] = new operations.AggNumericOps(v)

  implicit def AggOpsConv[T](v: Aggregatable[T])(
      implicit qr: Queryable[T, _]
  ): operations.AggOps[T] = new operations.AggOps(v)

  implicit def SelectOpsConv[T](v: Select[T, _]): operations.SelectOps[T] =
    new operations.SelectOps(v)

  implicit def TableOpsConv[V[_[_]]](t: Table[V]): TableOps[V] = new TableOps(t)

  case class caseWhen[T: MappedType](values: (Expr[Boolean], Expr[T])*) extends Expr[T] {

    import scalasql.renderer.SqlStr.SqlStringSyntax
    import scalasql.renderer.{Context, SqlStr}

    def mappedType = implicitly

    def toSqlExpr0(implicit ctx: Context): SqlStr = {
      val whens = SqlStr
        .join(values.map { case (when, then) => sql"WHEN $when THEN $then" }, sql" ")
      sql"CASE $whens END"
    }
  }
}
