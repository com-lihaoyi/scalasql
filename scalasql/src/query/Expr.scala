package scalasql.query

import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.{Config, Queryable, TypeMapper}
import scalasql.renderer.{Context, ExprsToSql, SqlStr}
import scalasql.utils.OptionPickler

/**
 * A single "value" in your SQL query that can be mapped to and from
 * a Scala value of a particular type [[T]]
 */
trait Expr[T] extends SqlStr.Renderable {
  protected final def renderToSql(ctx: Context): SqlStr = {
    ctx.exprNaming.get(this.exprIdentity).getOrElse(toSqlExpr0(ctx))
  }

  protected def toSqlExpr0(implicit ctx: Context): SqlStr

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
  def getIsLiteralTrue[T](e: Expr[T]): Boolean = e.exprIsLiteralTrue
  def getToString[T](e: Expr[T]): String = e.exprToString

  def getIdentity[T](e: Expr[T]): Identity = e.exprIdentity
  class Identity()

  implicit def toExprable[E[_] <: Expr[_], T](
      implicit valueReader0: OptionPickler.Reader[T],
      mt: TypeMapper[T]
  ): Queryable.Row[E[T], T] = new toExprable[E, T]()

  class toExprable[E[_] <: Expr[_], T](
      implicit valueReader0: OptionPickler.Reader[T],
      mt: TypeMapper[T]
  ) extends Queryable.Row[E[T], T] {
    def walk(q: E[T]) = Seq(Nil -> q)

    def valueReader = valueReader0

    def valueReader(q: E[T]): OptionPickler.Reader[T] = valueReader0

    def toSqlStr(q: E[T], ctx: Context) = {
      val walked = this.walk(q)
      val res = ExprsToSql(walked, sql"", ctx)
      if (res.isCompleteQuery) res else res + SqlStr.raw(ctx.defaultQueryableSuffix)
    }
    def toTypeMappers(q: E[T]) = Seq(mt)
  }

  def apply[T](f: Context => SqlStr): Expr[T] = new Simple[T](f)
  implicit def optionalize[T](e: Expr[T]): Expr[Option[T]] = {
    new Simple[Option[T]](e.toSqlExpr0(_))
  }
  class Simple[T](f: Context => SqlStr) extends Expr[T] {
    def toSqlExpr0(implicit ctx: Context): SqlStr = f(ctx)
  }

  implicit def from(x: Int): Expr[Int] = apply(x)
  implicit def from(x: Long): Expr[Long] = apply(x)
  implicit def from(x: Boolean): Expr[Boolean] = apply0(x, x)
  implicit def from(x: Double): Expr[Double] = apply(x)
  implicit def from(x: scala.math.BigDecimal): Expr[scala.math.BigDecimal] = apply(x)
  implicit def from(x: String): Expr[String] = apply(x)
  implicit def apply[T](
      x: T
  )(implicit conv: T => SqlStr.Interp, mappedType0: TypeMapper[T]): Expr[T] = {
    apply0[T](x)(conv, mappedType0)
  }
  def apply0[T](
      x: T,
      exprIsLiteralTrue0: Boolean = false
  )(implicit conv: T => SqlStr.Interp, mappedType0: TypeMapper[T]): Expr[T] = new Expr[T] {
    def mappedType = mappedType0
    override def toSqlExpr0(implicit ctx: Context): SqlStr =
      new SqlStr(Seq("", ""), Seq(conv(x)), false, Nil)
    protected override def exprIsLiteralTrue = exprIsLiteralTrue0
  }

}
