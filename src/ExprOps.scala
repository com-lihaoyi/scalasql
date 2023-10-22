package usql

import usql.operations.ExprStringOps
import usql.query.{Aggregatable, Expr, Select}
import usql.renderer.SqlStr.SqlStringSyntax


trait ExprOps {
  implicit def ExprBooleanOpsConv(v: Expr[Boolean]): operations.ExprBooleanOps =
    new operations.ExprBooleanOps(v)
  implicit def ExprIntOpsConv[T: Numeric](v: Expr[T]): operations.ExprNumericOps[T] =
    new operations.ExprNumericOps(v)
  implicit def ExprOpsConv(v: Expr[_]): operations.ExprOps = new operations.ExprOps(v)
  implicit def ExprStringOpsConv(v: Expr[String]): operations.ExprStringOps
  implicit def AggNumericOpsConv[V: Numeric](v: Aggregatable[Expr[V]])(implicit
      qr: Queryable[Expr[V], V]
  ): operations.AggNumericOps[V] =
    new operations.AggNumericOps(v)

  implicit def AggOpsConv[T](v: Aggregatable[T])(implicit
      qr: Queryable[T, _]
  ): operations.AggOps[T] =
    new operations.AggOps(v)

  implicit def SelectOpsConv[T](v: Select[T]): operations.SelectOps[T] =
    new operations.SelectOps(v)
}

object PostgresExprOps extends PostgresExprOps{
  class ExprStringOps(v: Expr[String]) extends operations.ExprStringOps(v){
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => usql"POSITION($x IN $v)" }
  }
}
trait PostgresExprOps extends ExprOps{
  override implicit def ExprStringOpsConv(v: Expr[String]): ExprStringOps = new PostgresExprOps.ExprStringOps(v)
}

object SqliteExprOps extends SqliteExprOps{
  class ExprStringOps(v: Expr[String]) extends operations.ExprStringOps(v){

    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => usql"INSTR($v, $x)" }
  }
}
trait SqliteExprOps extends ExprOps{
  override implicit def ExprStringOpsConv(v: Expr[String]): ExprStringOps = new SqliteExprOps.ExprStringOps(v)
}

object MySqlExprOps extends MySqlExprOps{
  class ExprStringOps(v: Expr[String]) extends operations.ExprStringOps(v){
    def indexOf(x: Expr[String]): Expr[Int] = Expr { implicit ctx => usql"POSITION($x IN $v)" }
  }
}
trait MySqlExprOps extends ExprOps{
  override implicit def ExprStringOpsConv(v: Expr[String]): ExprStringOps = new MySqlExprOps.ExprStringOps(v)
}