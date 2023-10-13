package usql

import usql.SqlStr.SqlStringSyntax

object ExprOps extends ExprOps
trait ExprOps {
  // List of ANSI SQL operators http://users.atw.hu/sqlnut/sqlnut2-chp-2-sect-2.html
  // List of ANSI SQL scalar functions http://users.atw.hu/sqlnut/sqlnut2-chp-4-sect-4.html
  // List of ANSI SQL aggregate functions https://www.oreilly.com/library/view/sql-in-a/9780596155322/ch04s02.html
  implicit class ExprIntOps0[T: Numeric](v: Expr[T]) {
    /** Addition */
    def +[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"$v + $x" }

    /** Subtraction */
    def -[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"$v - $x" }

    /** Multiplication */
    def *[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"$v * $x" }

    /** Division */
    def /[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"$v / $x" }

    /** Remainder */
    def %[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"MOD($v, $x)" }

    /** Greater than */
    def >[V: Numeric](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => usql"$v > $x" }

    /** Less than */
    def <[V: Numeric](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => usql"$v < $x" }

    /** Greater than or equal to */
    def >=[V: Numeric](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => usql"$v >= $x" }

    /** Less than or equal to */
    def <=[V: Numeric](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => usql"$v <= $x" }

    /** Bitwise AND */
    def &[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"$v & $x" }

    /** Bitwise OR */
    def |[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"$v | $x" }

    /** Bitwise XOR */
    def ^[V: Numeric](x: Expr[V]): Expr[T] = Expr { implicit ctx => usql"$v ^ $x" }

    /** TRUE if the operand is within a range */
    def between(x: Expr[Int], y: Expr[Int]): Expr[Boolean] = Expr { implicit ctx => usql"$v BETWEEN $x AND $y" }

    /** Unary Positive Operator */
    def unary_+ : Expr[T] = Expr { implicit ctx => usql"+$v"}

    /** Unary Negation Operator */
    def unary_- : Expr[T] = Expr { implicit ctx => usql"-$v"}

    /** Unary Bitwise NOT Operator */
    def unary_~ : Expr[T] = Expr { implicit ctx => usql"~$v"}

    /** Returns the absolute value of a number. */
    def abs: Expr[T] = Expr { implicit ctx => usql"ABS($v)"}

    /** Returns the remainder of one number divided into another. */
    def mod[V: Numeric](x: Expr[V]) : Expr[T] = Expr { implicit ctx => usql"MOD($v, $x)"}

    /** Rounds a noninteger value upwards to the next greatest integer. Returns an integer value unchanged. */
    def ceil: Expr[T] = Expr { implicit ctx => usql"CEIL($v)"}

    /** Rounds a noninteger value downwards to the next least integer. Returns an integer value unchanged. */
    def floor: Expr[T] = Expr { implicit ctx => usql"FLOOR($v)"}

    /** Raises a value to the power of the mathematical constant known as e. */
    def exp(x: Expr[T]): Expr[T] = Expr { implicit ctx => usql"EXP($v, $x)"}

    /** Returns the natural logarithm of a number. */
    def ln: Expr[T] = Expr { implicit ctx => usql"LN($v)"}

    /** Raises a number to a specified power. */
    def pow(x: Expr[T]): Expr[T] = Expr { implicit ctx => usql"POW($v, $x)"}

    /** Computes the square root of a number. */
    def sqrt: Expr[T] = Expr { implicit ctx => usql"SQRT($v)"}
  }

  implicit class ExprStringOps0(v: Expr[String]) {
    /** TRUE if the operand matches a pattern */
    def like[T](x: Expr[T]): Expr[Boolean] = Expr { implicit ctx => usql"$v LIKE $x" }

    /** Returns an integer value representing the starting position of a string within the search string. */
    def position(x: Expr[String]): Expr[Int] = Expr { implicit ctx => usql"POSITION($v, $x)" }

    /** Converts a string to all lowercase characters. */
    def toLowerCase: Expr[String] = Expr { implicit ctx => usql"LOWER($v)" }

    /** Converts a string to all uppercase characters. */
    def toUpperCase: Expr[String] = Expr { implicit ctx => usql"UPPER($v)" }

    /** Removes leading characters, trailing characters, or both from a character string. */
    def trim: Expr[String] = Expr { implicit ctx => usql"TRIM($v)" }

    /** Returns a portion of a string. */
    def substring(start: Expr[Int], length: Expr[Int]): Expr[String] = Expr { implicit ctx => usql"SUBSTRING($v, $start, $length)" }

    /** Returns the result of replacing a substring of one string with another. */
    def overlay(replacement: Expr[String], start: Expr[Int], length: Expr[Int] = null): Expr[String] = Expr { implicit ctx =>
      val lengthStr = if (length == null) usql"" else usql" FOR $length"
      usql"OVERLAY($v PLACING $replacement FROM $start$lengthStr)"
    }
  }

  implicit class ExprOps0(v: Expr[_]) {
    /** Equals to */
    def ===[T](x: Expr[T]): Expr[Boolean] = Expr{ implicit ctx => usql"$v = $x" }

    /** Not equal to */
    def !==[T](x: Expr[T]): Expr[Boolean] = Expr{ implicit ctx => usql"$v <> $x" }
  }

  implicit class ExprBooleanOps0(v: Expr[Boolean]) {
    /** TRUE if both Boolean expressions are TRUE */
    def &&(x: Expr[Boolean]): Expr[Boolean] = Expr { implicit ctx => usql"$v AND $x"}

    /** TRUE if either Boolean expression is TRUE */
    def ||(x: Expr[Boolean]): Expr[Boolean] = Expr { implicit ctx => usql"$v OR $x"}

    /** Reverses the value of any other Boolean operator */
    def unary_! : Expr[Boolean] = Expr { implicit ctx => usql"NOT $v"}
  }



  implicit class ExprSeqIntOps0[V: Numeric](v: QueryLike[Expr[V]])(implicit qr: Queryable[Expr[V], V]) {
    /** Computes the sum of column values */
    def sum: Expr[V] = v.queryExpr(implicit ctx => usql"SUM(${v.expr})")

    /** Finds the minimum value in a column  */
    def min: Expr[V] = v.queryExpr(implicit ctx => usql"MIN(${v.expr})")

    /** Finds the maximum value in a column  */
    def max: Expr[V] = v.queryExpr(implicit ctx => usql"MAX(${v.expr})")

    /** Computes the average value of a column */
    def avg: Expr[V] = v.queryExpr(implicit ctx => usql"AVG(${v.expr})")
  }

  implicit class ExprSeqOps0[T](v: QueryLike[T])(implicit qr: Queryable[T, _]) {
    /** Counts the rows */
    def size: Expr[Int] = v.queryExpr(implicit ctx => usql"COUNT(1)")

    /** Computes the sum of column values */
    def sumBy[V: Numeric](f: T => Expr[V])(implicit qr: Queryable[Expr[V], V]): Expr[V] = v.queryExpr(implicit ctx => usql"SUM(${f(v.expr)})")

    /** Finds the minimum value in a column  */
    def minBy[V: Numeric](f: T => Expr[V])(implicit qr: Queryable[Expr[V], V]): Expr[V] = v.queryExpr(implicit ctx => usql"MIN(${f(v.expr)})")

    /** Finds the maximum value in a column  */
    def maxBy[V: Numeric](f: T => Expr[V])(implicit qr: Queryable[Expr[V], V]): Expr[V] = v.queryExpr(implicit ctx => usql"MAX(${f(v.expr)})")

    /** Computes the average value of a column */
    def avgBy[V: Numeric](f: T => Expr[V])(implicit qr: Queryable[Expr[V], V]): Expr[V] = v.queryExpr(implicit ctx => usql"AVG(${f(v.expr)})")

    /** TRUE if any value in a set is TRUE */
    def any(f: T => Expr[Boolean]): Expr[Boolean] = v.queryExpr(implicit ctx => usql"ANY(${f(v.expr)})")

    /** TRUE if all values in a set are TRUE */
    def all(f: T => Expr[Boolean]): Expr[Boolean] = v.queryExpr(implicit ctx => usql"ALL(${f(v.expr)})")

    /** TRUE if the operand is equal to one of a list of expressions or one or more rows returned by a subquery */
//    def contains(e: Expr[_]): Expr[Boolean] = v.queryExpr(implicit ctx => usql"ALL($e in $v})")
  }
}

