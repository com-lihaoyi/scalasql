package usql

import usql.SqlString.SqlStringSyntax

object ExprIntOps extends ExprIntOps
trait ExprIntOps {

  implicit class ExprIntOps0(v: Expr[Int]) {
    def *(x: Int): Atomic[Int] = new Atomic[Int] {
      def toSqlExpr: SqlString = v.asInstanceOf[Atomic[_]].toSqlExpr ++ usql" * $x"

      def toTables = v.toTables
    }

    def >(x: Int): Atomic[Boolean] = new Atomic[Boolean] {
      def toSqlExpr: SqlString = v.asInstanceOf[Atomic[_]].toSqlExpr ++ usql" > $x"

      def toTables = v.toTables
    }
  }
  implicit class ExprOps0(v: Expr[_]) {
    def ===(x: Int): Atomic[Boolean] = new Atomic[Boolean] {
      def toSqlExpr: SqlString = v.asInstanceOf[Atomic[_]].toSqlExpr ++ usql" = $x"

      def toTables = v.toTables
    }
    def ===(x: String): Atomic[Boolean] = new Atomic[Boolean] {
      def toSqlExpr: SqlString = v.asInstanceOf[Atomic[_]].toSqlExpr ++ usql" = $x"

      def toTables = v.toTables
    }
    def ===(x: Expr[_]): Atomic[Boolean] = new Atomic[Boolean] {
      def toSqlExpr: SqlString = v.asInstanceOf[Atomic[_]].toSqlExpr ++ usql" = " ++ x.asInstanceOf[Atomic[_]].toSqlExpr

      def toTables = v.toTables
    }
  }
  implicit class ExprBooleanOps0(v: Expr[Boolean]) {
    def &&(x: Expr[Boolean]): Atomic[Boolean] = new Atomic[Boolean] {
      def toSqlExpr: SqlString = v.asInstanceOf[Atomic[_]].toSqlExpr ++ usql" AND " ++ x.asInstanceOf[Atomic[_]].toSqlExpr

      def toTables = v.toTables
    }
  }
}
