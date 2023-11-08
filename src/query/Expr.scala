package scalasql.query

import scalasql.{Config, MappedType, Queryable}
import scalasql.renderer.{Context, SqlStr}
import scalasql.utils.OptionPickler

/**
 * A single "value" in your SQL query that can be mapped to and from
 * a Scala value of a particular type [[T]]
 */
trait Expr[T] extends SqlStr.Renderable {
  protected def mappedType: MappedType[T]
  final def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]]) = {
    (ctx.exprNaming.get(this.exprIdentity).getOrElse(toSqlExpr0), Seq(mappedType))
  }

  protected def toSqlExpr0(implicit ctx: Context): SqlStr

  override def toString: String =
    throw new Exception("Expr#toString is not defined. Use Expr#exprToString")

  override def equals(other: Any): Boolean = throw new Exception(
    "Expr#equals is not defined. Use Expr#exprIdentity for your equality checks"
  )
  private lazy val exprIdentity: Expr.Identity = new Expr.Identity()
  private def exprToString: String = super.toString
}

object Expr {
  def getMappedType[T](e: Expr[T]): MappedType[T] = e.mappedType
  def getToString[T](e: Expr[T]): String = e.exprToString

  def getIdentity[T](e: Expr[T]): Identity = e.exprIdentity
  class Identity()

  implicit def toExprable[E[_] <: Expr[_], T](
      implicit valueReader0: OptionPickler.Reader[T]
  ): Queryable.Row[E[T], T] = new toExprable[E, T]()

  class toExprable[E[_] <: Expr[_], T](implicit valueReader0: OptionPickler.Reader[T])
      extends Queryable.Row[E[T], T] {
    def walk(q: E[T]) = Seq(Nil -> q)

    def valueReader = valueReader0

    def valueReader(q: E[T]): OptionPickler.Reader[T] = valueReader0
  }

  def apply[T](f: Context => SqlStr)(implicit mappedType: MappedType[T]): Expr[T] = new Simple[T](f)
  implicit def optionalize[T](e: Expr[T]): Expr[Option[T]] = {
    new Simple[Option[T]](e.toSqlExpr0(_))(MappedType.OptionType(getMappedType(e)))
  }
  class Simple[T](f: Context => SqlStr)(implicit val mappedType: MappedType[T]) extends Expr[T] {
    def toSqlExpr0(implicit ctx: Context): SqlStr = f(ctx)
  }

  implicit def from(x: Int): Expr[Int] = apply(x)
  implicit def from(x: Long): Expr[Long] = apply(x)
  implicit def from(x: Boolean): Expr[Boolean] = apply(x)
  implicit def from(x: Double): Expr[Double] = apply(x)
  implicit def from(x: scala.math.BigDecimal): Expr[scala.math.BigDecimal] = apply(x)
  implicit def from(x: String): Expr[String] = apply(x)
  implicit def apply[T](
      x: T
  )(implicit conv: T => SqlStr.Interp, mappedType0: MappedType[T]): Expr[T] = new Expr[T] {
    def mappedType = mappedType0
    override def toSqlExpr0(implicit ctx: Context): SqlStr =
      new SqlStr(Seq("", ""), Seq(conv(x)), false, Nil)
  }
}
