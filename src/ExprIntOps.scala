package usql

import usql.SqlString.SqlStringSyntax

object ExprIntOps extends ExprIntOps
trait ExprIntOps {

  implicit class ExprIntOps0(v: Expr[Int]) {
    def *(x: Int): Expr[Int] = new Expr[Int] {
      def toSqlExpr: SqlString = v.toSqlExpr ++ usql" * $x"

      def toTables = v.toTables
    }

    def >(x: Int): Expr[Boolean] = new Expr[Boolean] {
      def toSqlExpr: SqlString = v.toSqlExpr ++ usql" > $x"

      def toTables = v.toTables
    }
  }
  implicit class ExprOps0(v: Expr[_]) {
    def ===(x: Int): Expr[Boolean] = new Expr[Boolean] {
      def toSqlExpr: SqlString = v.toSqlExpr ++ usql" = $x"

      def toTables = v.toTables
    }
    def ===(x: String): Expr[Boolean] = new Expr[Boolean] {
      def toSqlExpr: SqlString = v.toSqlExpr ++ usql" = $x"

      def toTables = v.toTables
    }
    def ===(x: Expr[_]): Expr[Boolean] = new Expr[Boolean] {
      def toSqlExpr: SqlString = v.toSqlExpr ++ usql" = " ++ x.toSqlExpr

      def toTables = v.toTables ++ x.toTables
    }
  }
  implicit class ExprBooleanOps0(v: Expr[Boolean]) {
    def &&(x: Expr[Boolean]): Expr[Boolean] = new Expr[Boolean] {
      def toSqlExpr: SqlString = v.toSqlExpr ++ usql" AND " ++ x.toSqlExpr

      def toTables = v.toTables ++ x.toTables
    }
  }
}
