package scalasql.core

import scalasql.core.SqlStr.SqlStringSyntax

/**
 * A single "value" in your SQL query that can be mapped to and from
 * a Scala value of a particular type [[T]]
 */
trait Sql[T] extends SqlStr.Renderable {
  protected final def renderToSql(ctx: Context): SqlStr = {
    ctx.exprNaming.get(this.exprIdentity).getOrElse(renderToSql0(ctx))
  }

  protected def renderToSql0(implicit ctx: Context): SqlStr

  override def toString: String =
    throw new Exception("Sql#toString is not defined. Use Sql#exprToString")

  override def equals(other: Any): Boolean = throw new Exception(
    "Sql#equals is not defined. Use Sql#exprIdentity for your equality checks"
  )
  private lazy val exprIdentity: Sql.Identity = new Sql.Identity()
  private def exprToString: String = super.toString

  /**
   * Some syntax like `for` comprehensions likes to generate spurious `Sql(true)`
   * clauses. We need to mark them as such so we can filter them out later during
   * code generation
   */
  protected def exprIsLiteralTrue: Boolean = false
}

object Sql {
  def isLiteralTrue[T](e: Sql[T]): Boolean = e.exprIsLiteralTrue
  def toString[T](e: Sql[T]): String = e.exprToString

  def identity[T](e: Sql[T]): Identity = e.exprIdentity
  class Identity()

  implicit def ExprQueryable[E[_] <: Sql[_], T](
      implicit mt: TypeMapper[T]
  ): Queryable.Row[E[T], T] = new ExprQueryable[E, T]()

  class ExprQueryable[E[_] <: Sql[_], T](
      implicit tm: TypeMapper[T]
  ) extends Queryable.Row[E[T], T] {
    def walkLabels() = Seq(Nil)
    def walkExprs(q: E[T]) = Seq(q)

    override def construct(args: Queryable.ResultSetIterator): T = args.get(tm)

    def deconstruct(r: T): E[T] = Sql[T] { implicit ctx: Context =>
      sql"$r"
    }.asInstanceOf[E[T]]
  }

  def apply[T](f: Context => SqlStr): Sql[T] = new Simple[T](f)
  implicit def optionalize[T](e: Sql[T]): Sql[Option[T]] = {
    new Simple[Option[T]](e.renderToSql0(_))
  }
  class Simple[T](f: Context => SqlStr) extends Sql[T] {
    def renderToSql0(implicit ctx: Context): SqlStr = f(ctx)
  }

  implicit def apply[T](
      x: T
  )(implicit conv: T => SqlStr.Interp): Sql[T] = {
    apply0[T](x)(conv)
  }
  def apply0[T](
      x: T,
      exprIsLiteralTrue0: Boolean = false
  )(implicit conv: T => SqlStr.Interp): Sql[T] = new Sql[T] {
    override def renderToSql0(implicit ctx: Context): SqlStr =
      new SqlStr(Array("", ""), Array(conv(x)), false, Array.empty[Sql.Identity])
    protected override def exprIsLiteralTrue = exprIsLiteralTrue0
  }

}
