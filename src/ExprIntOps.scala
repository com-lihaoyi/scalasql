package usql

import usql.SqlStr.SqlStringSyntax

object ExprIntOps extends ExprIntOps
trait ExprIntOps {

  implicit class ExprIntOps0(v: Expr[Int]) {
    def *(x: Int): Expr[Int] = new Expr[Int] {
      def toSqlExpr0(implicit ctx: QueryToSql.Context): SqlStr = v.toSqlExpr + usql" * $x"
    }

    def >(x: Int): Expr[Boolean] = new Expr[Boolean] {
      def toSqlExpr0(implicit ctx: QueryToSql.Context): SqlStr = v.toSqlExpr + usql" > $x"
    }
  }
  implicit class ExprOps0(v: Expr[_]) {
    def ===(x: Int): Expr[Boolean] = new Expr[Boolean] {
      def toSqlExpr0(implicit ctx: QueryToSql.Context): SqlStr = v.toSqlExpr + usql" = $x"
    }

    def ===(x: String): Expr[Boolean] = new Expr[Boolean] {
      def toSqlExpr0(implicit ctx: QueryToSql.Context): SqlStr = v.toSqlExpr + usql" = $x"
    }

    def ===(x: Expr[_]): Expr[Boolean] = new Expr[Boolean] {
      def toSqlExpr0(implicit ctx: QueryToSql.Context): SqlStr = v.toSqlExpr + usql" = " + x.toSqlExpr
    }
  }
  implicit class ExprBooleanOps0(v: Expr[Boolean]) {
    def &&(x: Expr[Boolean]): Expr[Boolean] = new Expr[Boolean] {
      def toSqlExpr0(implicit ctx: QueryToSql.Context): SqlStr = v.toSqlExpr + usql" AND " + x.toSqlExpr
    }
  }
}
