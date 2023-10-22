package usql.operations

import usql.query.Expr
import usql.renderer.SqlStr.SqlStringSyntax

class ExprOps(v: Expr[_]) {

  /** Equals to */
  def ===[T](x: Expr[T]): Expr[Boolean] = Expr { implicit ctx => usql"$v = $x" }

  /** Not equal to */
  def !==[T](x: Expr[T]): Expr[Boolean] = Expr { implicit ctx => usql"$v <> $x" }

  /** Greater than */
  def >[V](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => usql"$v > $x" }

  /** Less than */
  def <[V](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => usql"$v < $x" }

  /** Greater than or equal to */
  def >=[V](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => usql"$v >= $x" }

  /** Less than or equal to */
  def <=[V](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx => usql"$v <= $x" }
}
