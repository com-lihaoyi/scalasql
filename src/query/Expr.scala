package usql.query

import usql.renderer.SqlStr.SqlStringSyntax
import usql.renderer.{Context, Interp, SelectToSql, SqlStr}

trait Expr[T] {
  final def toSqlExpr(implicit ctx: Context): SqlStr = {
    ctx.exprNaming.get(this.exprIdentity).getOrElse(toSqlExpr0)
  }

  def toSqlExpr0(implicit ctx: Context): SqlStr

  override def toString: String = throw new Exception("Expr#toString is not defined")
  override def equals(other: Any): Boolean = throw new Exception("Expr#equals is not defined")
  val exprIdentity = new Expr.Identity()
}

object Expr{
  class Identity()
  def apply[T](f: Context => SqlStr): Expr[T] = new Simple[T](f)
  class Simple[T](f: Context => SqlStr) extends Expr[T]{
    def toSqlExpr0(implicit ctx: Context): SqlStr = f(ctx)
  }

  implicit def apply[T](x: T)(implicit conv: T => Interp): Expr[T] = new Expr[T] {
    override def toSqlExpr0(implicit ctx: Context): SqlStr = new SqlStr(Seq("", ""), Seq(conv(x)), false)
  }


}