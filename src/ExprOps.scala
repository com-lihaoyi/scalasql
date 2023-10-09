package usql

import usql.SqlStr.SqlStringSyntax

object ExprOps extends ExprOps
trait ExprOps {
  implicit class ExprIntOps0(v: Expr[Int]) {
    def *(x: Int): Expr[Int] = Expr { implicit ctx => usql"$v * $x" }
    def >(x: Int): Expr[Boolean] = Expr { implicit ctx => usql"$v > $x" }
  }
  implicit class ExprOps0(v: Expr[_]) {
    def ===(x: Int): Expr[Boolean] = Expr { implicit ctx => usql"$v = $x" }
    def ===(x: String): Expr[Boolean] = Expr { implicit ctx => usql"$v = $x" }
    def ===(x: Expr[_]): Expr[Boolean] = Expr{ implicit ctx => usql"$v = $x" }
  }
  implicit class ExprBooleanOps0(v: Expr[Boolean]) {
    def &&(x: Expr[Boolean]): Expr[Boolean] = Expr { implicit ctx => usql"$v AND $x"}
  }
  implicit class ExprSeqIntOps0(v: Query[Expr[Int]]) {
    def sum: Expr[Int] = Expr { implicit ctx =>
      v.copy[Expr[Int]](
        expr = Expr{implicit ctx: QueryToSql.Context => usql"SUM(${v.expr})"}
      ).toSqlExpr
    }
  }
  implicit class ExprSeqOps0[T](v: Query[T])(implicit qr: Queryable[T, _]) {
    def count: Expr[Int] = Expr { implicit ctx =>

      v.copy[Expr[Int]](
        expr = Expr{implicit ctx: QueryToSql.Context => usql"COUNT(1)"}
      ).toSqlExpr
    }

    def sumBy(f: T => Expr[Int]): Expr[Int] = Expr { implicit ctx =>
      v.copy[Expr[Int]](
        expr = Expr{implicit ctx: QueryToSql.Context => usql"SUM(${f(v.expr)})"}
      ).toSqlExpr
    }
  }
}
