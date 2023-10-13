package usql

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

  implicit def apply[T](x: T)(implicit conv: T => Interp) = new Expr[T] {
    override def toSqlExpr0(implicit ctx: QueryToSql.Context): SqlStr = new SqlStr(Seq("", ""), Seq(conv(x)), false)
  }
}