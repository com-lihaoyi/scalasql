package scalasql.core

import scalasql.core.SqlStr.SqlStringSyntax

/**
 * A single "value" in your SQL query that can be mapped to and from
 * a Scala value of a particular type [[T]]
 */
trait Expr[T] extends SqlStr.Renderable {
  private[scalasql] final def renderSql(ctx: Context): SqlStr = {
    ctx.exprNaming.get(this.exprIdentity).getOrElse(renderToSql0(ctx))
  }

  protected def renderToSql0(implicit ctx: Context): SqlStr

  override def toString: String =
    throw new Exception("Expr#toString is not defined. Use Expr#exprToString")

  override def equals(other: Any): Boolean = throw new Exception(
    "Expr#equals is not defined. Use Expr#exprIdentity for your equality checks"
  )
  private lazy val exprIdentity: Expr.Identity = new Expr.Identity()
  private def exprToString: String = super.toString

  /**
   * Some syntax like `for` comprehensions likes to generate spurious `Expr(true)`
   * clauses. We need to mark them as such so we can filter them out later during
   * code generation
   */
  protected def exprIsLiteralTrue: Boolean = false
}

object Expr {
  def isLiteralTrue[T](e: Expr[T]): Boolean = e.exprIsLiteralTrue
  def toString[T](e: Expr[T]): String = e.exprToString

  def identity[T](e: Expr[T]): Identity = e.exprIdentity
  class Identity()

  implicit def ExprQueryable[E[_] <: Expr[?], T](
      implicit mt: TypeMapper[T]
  ): Queryable.Row[E[T], T] = new ExprQueryable[E, T]()

  class ExprQueryable[E[_] <: Expr[?], T](
      implicit tm: TypeMapper[T]
  ) extends Queryable.Row[E[T], T] {
    def walkLabels(): Seq[List[String]] = Seq(Nil)
    def walkExprs(q: E[T]): Seq[Expr[?]] = Seq(q)

    override def construct(args: Queryable.ResultSetIterator): T = args.get(tm)

    def deconstruct(r: T): E[T] = Expr[T] { implicit ctx: Context =>
      sql"$r"
    }.asInstanceOf[E[T]]
  }

  def apply[T](f: Context => SqlStr): Expr[T] = new Simple[T](f)
  implicit def optionalize[T](e: Expr[T]): Expr[Option[T]] = {
    new Simple[Option[T]](e.renderToSql0(_))
  }
  class Simple[T](f: Context => SqlStr) extends Expr[T] {
    def renderToSql0(implicit ctx: Context): SqlStr = f(ctx)
  }

  implicit def apply[T](
      x: T
  )(implicit conv: T => SqlStr.Interp): Expr[T] = {
    apply0[T](x)(conv)
  }
  def apply0[T](
      x: T,
      exprIsLiteralTrue0: Boolean = false
  )(implicit conv: T => SqlStr.Interp): Expr[T] = new Expr[T] {
    override def renderToSql0(implicit ctx: Context): SqlStr =
      new SqlStr(Array("", ""), Array(conv(x)), false, Array.empty[Expr.Identity])
    protected override def exprIsLiteralTrue = exprIsLiteralTrue0
  }

}
