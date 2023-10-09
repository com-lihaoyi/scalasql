package usql

import usql.SqlStr.SqlStringSyntax

object ExprOps extends ExprOps
trait ExprOps {
  // List of ANSI SQL operators http://users.atw.hu/sqlnut/sqlnut2-chp-2-sect-2.html
  // List of ANSI SQL scalar functions http://users.atw.hu/sqlnut/sqlnut2-chp-4-sect-4.html
  // List of ANSI SQL aggregate functions https://www.oreilly.com/library/view/sql-in-a/9780596155322/ch04s02.html
  implicit class ExprIntOps0(v: Expr[Int]) {
    /** Addition */
    def +(x: Expr[Int]): Expr[Int] = Expr { implicit ctx => usql"$v + $x" }

    /** Subtraction */
    def -(x: Expr[Int]): Expr[Int] = Expr { implicit ctx => usql"$v - $x" }

    /** Multiplication */
    def *(x: Expr[Int]): Expr[Int] = Expr { implicit ctx => usql"$v * $x" }

    /** Division */
    def /(x: Expr[Int]): Expr[Int] = Expr { implicit ctx => usql"$v / $x" }

    /** Remainder */
    def %(x: Expr[Int]): Expr[Int] = Expr { implicit ctx => usql"MOD($v, $x)" }

    /** Greater than */
    def >(x: Expr[Int]): Expr[Boolean] = Expr { implicit ctx => usql"$v > $x" }

    /** Less than */
    def <(x: Expr[Int]): Expr[Boolean] = Expr { implicit ctx => usql"$v < $x" }

    /** Greater than or equal to */
    def >=(x: Expr[Int]): Expr[Boolean] = Expr { implicit ctx => usql"$v >= $x" }

    /** Less than or equal to */
    def <=(x: Expr[Int]): Expr[Boolean] = Expr { implicit ctx => usql"$v <= $x" }

    /** Bitwise AND */
    def &(x: Expr[Int]): Expr[Boolean] = Expr { implicit ctx => usql"$v * $x" }

    /** Bitwise OR */
    def |(x: Expr[Int]): Expr[Boolean] = Expr { implicit ctx => usql"$v | $x" }

    /** Bitwise XOR */
    def ^(x: Expr[Int]): Expr[Boolean] = Expr { implicit ctx => usql"$v ^ $x" }

    /** TRUE if the operand is within a range */
    def between(x: Expr[Int], y: Expr[Int]): Expr[Boolean] = Expr { implicit ctx => usql"$v BETWEEN $x AND $y" }

    /** Unary Positive Operator */
    def unary_+ : Expr[Int] = Expr { implicit ctx => usql"+$v"}

    /** Unary Negation Operator */
    def unary_- : Expr[Int] = Expr { implicit ctx => usql"-$v"}

    /** Unary Bitwise NOT Operator */
    def unary_~ : Expr[Int] = Expr { implicit ctx => usql"~$v"}

    /** Returns the absolute value of a number. */
    def abs: Expr[Int] = Expr { implicit ctx => usql"ABS($v)"}

    /** Returns the remainder of one number divided into another. */
    def mod(x: Expr[Int]) : Expr[Int] = Expr { implicit ctx => usql"MOD($v, $x)"}

    /** Returns an integer value representing the number of bits in another value. */
    def bitLength: Expr[Int] = Expr { implicit ctx => usql"BIT_LENGTH($v)"}

    /** Rounds a noninteger value upwards to the next greatest integer. Returns an integer value unchanged. */
    def ceil: Expr[Int] = Expr { implicit ctx => usql"CEIL($v)"}

    /** Rounds a noninteger value downwards to the next least integer. Returns an integer value unchanged. */
    def floor: Expr[Int] = Expr { implicit ctx => usql"FLOOR($v)"}

    /** Raises a value to the power of the mathematical constant known as e. */
    def expr(x: Expr[Int]): Expr[Int] = Expr { implicit ctx => usql"EXP($v, $x)"}

    /** Returns the natural logarithm of a number. */
    def ln: Expr[Int] = Expr { implicit ctx => usql"LN($v)"}

    /** Raises a number to a specified power. */
    def pow(x: Expr[Int]): Expr[Int] = Expr { implicit ctx => usql"POW($v, $x)"}

    /** Computes the square root of a number. */
    def sqrt: Expr[Int] = Expr { implicit ctx => usql"SQRT($v)"}
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

  def queryExpr[T, V](v: Query[T])
                     (f: QueryToSql.Context => SqlStr)
                     (implicit qr: Queryable[Expr[V], _]): Expr[V] = Expr { implicit ctx =>
    v.copy[Expr[V]](
      expr = Expr { implicit ctx => f(ctx) }
    ).toSqlExpr
  }


  implicit class ExprSeqIntOps0(v: Query[Expr[Int]]) {
    /** Computes the sum of column values */
    def sum: Expr[Int] = queryExpr(v)(implicit ctx => usql"SUM(${v.expr})")

    /** Finds the minimum value in a column  */
    def min: Expr[Int] = queryExpr(v)(implicit ctx => usql"MIN(${v.expr})")

    /** Finds the maximum value in a column  */
    def max: Expr[Int] = queryExpr(v)(implicit ctx => usql"MAX(${v.expr})")

    /** Computes the average value of a column */
    def avg: Expr[Int] = queryExpr(v)(implicit ctx => usql"AVG(${v.expr})")
  }

  implicit class ExprSeqOps0[T](v: Query[T])(implicit qr: Queryable[T, _]) {
    /** Counts the rows */
    def count: Expr[Int] = queryExpr(v)(implicit ctx => usql"COUNT(1)")

    /** Computes the sum of column values */
    def sumBy(f: T => Expr[Int]): Expr[Int] = queryExpr(v)(implicit ctx => usql"SUM(${f(v.expr)})")

    /** Finds the minimum value in a column  */
    def minBy(f: T => Expr[Int]): Expr[Int] = queryExpr(v)(implicit ctx => usql"MIN(${f(v.expr)})")

    /** Finds the maximum value in a column  */
    def maxBy(f: T => Expr[Int]): Expr[Int] = queryExpr(v)(implicit ctx => usql"MAX(${f(v.expr)})")

    /** Computes the average value of a column */
    def avgBy(f: T => Expr[Int]): Expr[Int] = queryExpr(v)(implicit ctx => usql"AVG(${f(v.expr)})")

    /** TRUE if any value in a set is TRUE */
    def any(f: T => Expr[Boolean]): Expr[Boolean] = queryExpr(v)(implicit ctx => usql"ANY(${f(v.expr)})")

    /** TRUE if all values in a set are TRUE */
    def all(f: T => Expr[Boolean]): Expr[Boolean] = queryExpr(v)(implicit ctx => usql"ALL(${f(v.expr)})")

    /** TRUE if the operand is equal to one of a list of expressions or one or more rows returned by a subquery */
    def contains(e: Expr[_]): Expr[Boolean] = queryExpr(v)(implicit ctx => usql"ALL($e in $v})")
  }
}
