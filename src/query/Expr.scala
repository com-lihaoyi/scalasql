package usql.query

import usql.renderer.SqlStr.SqlStringSyntax
import usql.renderer.{Context, Interp, SelectToSql, SqlStr}

trait Expr[T] {
  final def toSqlExpr(implicit ctx: Context): SqlStr = {
    ctx.exprNaming.get(this).getOrElse(toSqlExpr0)
  }

  def toSqlExpr0(implicit ctx: Context): SqlStr
}

object Expr{
  def apply[T](f: Context => SqlStr): Expr[T] = new Simple[T](f)
  class Simple[T](f: Context => SqlStr) extends Expr[T]{
    def toSqlExpr0(implicit ctx: Context): SqlStr = f(ctx)
  }

  implicit def apply[T](x: T)(implicit conv: T => Interp): Expr[T] = new Expr[T] {
    override def toSqlExpr0(implicit ctx: Context): SqlStr = new SqlStr(Seq("", ""), Seq(conv(x)), false)
  }


}