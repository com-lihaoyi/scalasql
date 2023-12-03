package scalasql.core

/**
 * A SQL query with interpolated `?`s expressions and the associated
 * interpolated values, of type [[Interp]]. Accumulates SQL snippets, parameters,
 * and referenced expressions in a tree structure to minimize copying overhead,
 * until [[SqlStr.flatten]] is called to convert it into a [[SqlStr.Flattened]]
 */
class SqlStr(
    private val queryParts: collection.IndexedSeq[String],
    private val params: collection.IndexedSeq[SqlStr.Interp],
    val isCompleteQuery: Boolean,
    private val referencedExprs: collection.IndexedSeq[Sql.Identity]
) extends SqlStr.Renderable {
  def +(other: SqlStr) = {
    new SqlStr(
      SqlStr.plusParts,
      Array[SqlStr.Interp](this, other),
      false,
      Array.empty[Sql.Identity]
    )
  }

  def withCompleteQuery(v: Boolean) = new SqlStr(queryParts, params, v, referencedExprs)
  override def toString = SqlStr.flatten(this).queryParts.mkString("?")

  override protected def renderToSql(ctx: Context): SqlStr = this
}

object SqlStr {
  private val plusParts: IndexedSeq[String] = Array("", "", "")
  class Flattened(
      val queryParts: collection.IndexedSeq[String],
      val params: collection.IndexedSeq[Interp.TypeInterp[_]],
      isCompleteQuery: Boolean,
      val referencedExprs: collection.IndexedSeq[Sql.Identity]
  ) extends SqlStr(queryParts, params, isCompleteQuery, referencedExprs)

  /**
   * Helper method turn an `Option[T]` into a [[SqlStr]], returning
   * the empty string if the `Option` is `None`
   */
  def opt[T](t: Option[T])(f: T => SqlStr) = t.map(f).getOrElse(SqlStr.empty)

  /**
   * Helper method turn an `Seq[T]` into a [[SqlStr]], returning
   * the empty string if the `Seq` is empty
   */
  def optSeq[T](t: Seq[T])(f: Seq[T] => SqlStr) = if (t.nonEmpty) f(t) else SqlStr.empty

  /**
   * Flattens out a [[SqlStr]] into a single flattened [[SqlStr.Flattened]] object,
   * at which point you can use its `queryParts`, `params`, `referencedExprs`, etc.
   */
  def flatten(self: SqlStr): Flattened = {
    // Implement this in a mutable style because`it's pretty performance sensitive
    val finalParts = collection.mutable.ArrayBuffer.empty[String]
    val finalArgs = collection.mutable.ArrayBuffer.empty[Interp.TypeInterp[_]]
    val finalExprs = collection.mutable.ArrayBuffer.empty[Sql.Identity]
    // Equivalent to `finalParts.last`, cached locally for performance
    var lastFinalPart: String = null

    def rec(self: SqlStr, topLevel: Boolean): Unit = {
      val queryParts = self.queryParts
      val params = self.params
      finalExprs.appendAll(self.referencedExprs)
      var boundary = true
      val parenthesize = !topLevel && self.isCompleteQuery
      if (parenthesize) addFinalPart("(")
      boundary = true

      def addFinalPart(s: String) = {
        if (boundary && finalParts.nonEmpty) {
          lastFinalPart = lastFinalPart + s
          finalParts(finalParts.length - 1) = lastFinalPart
        } else {
          finalParts.append(s)
          lastFinalPart = s
        }
      }

      var i = 0
      val length = params.length
      while (i < length) {
        val p = queryParts(i)
        val a = params(i)
        addFinalPart(p)
        boundary = false
        a match {
          case si: Interp.SqlStrInterp =>
            rec(si.s, false)
            boundary = true

          case s: Interp.TypeInterp[_] => finalArgs.append(s)
        }
        i += 1
      }

      addFinalPart(queryParts(queryParts.length - 1))
      if (parenthesize) addFinalPart(")")
    }

    rec(self, true)
    new Flattened(finalParts, finalArgs, self.isCompleteQuery, finalExprs)
  }

  /**
   * Provides the sql"..." syntax for constructing [[SqlStr]]s
   */
  implicit class SqlStringSyntax(sc: StringContext) {
    def sql(args: Interp*) =
      new SqlStr(sc.parts.toIndexedSeq, args.toIndexedSeq, false, Array.empty[Sql.Identity])
  }

  /**
   * Joins a `Seq` of [[SqlStr]]s into a single [[SqlStr]] using the given [[sep]] separator
   */
  def join(strs: IterableOnce[SqlStr], sep: SqlStr = empty): SqlStr = {
    strs.iterator.reduceOption(_ + sep + _).getOrElse(empty)
  }

  lazy val empty = sql""
  lazy val commaSep = sql", "

  /**
   * Converts a raw `String` into a [[SqlStr]]. Note that this must be used
   * carefully to avoid SQL injection attacks.
   */
  def raw(s: String, referencedExprs: Array[Sql.Identity] = Array.empty) =
    new SqlStr(Array(s), Array.empty[SqlStr.Interp], false, referencedExprs)

  trait Renderable {
    protected def renderToSql(ctx: Context): SqlStr
  }

  object Renderable {
    def renderToSql(x: Renderable)(implicit ctx: Context) = x.renderToSql(ctx)
  }

  /**
   * Something that can be interpolated into a [[SqlStr]].
   */
  sealed trait Interp
  object Interp {
    implicit def renderableInterp(t: Renderable)(implicit ctx: Context): Interp =
      SqlStrInterp(Renderable.renderToSql(t)(ctx))

    implicit def sqlStrInterp(s: SqlStr): Interp = SqlStrInterp(s)

    case class SqlStrInterp(s: SqlStr) extends Interp

    // Not sure why these two additional implicit conversions are needed
    implicit def strInterp(value: String)(implicit tm: TypeMapper[String]): Interp = typeInterp(
      value
    )
    implicit def intInterp(value: Int)(implicit tm: TypeMapper[Int]): Interp = typeInterp(value)
    implicit def booleanInterp(value: Boolean)(implicit tm: TypeMapper[Boolean]): Interp =
      typeInterp(value)
    implicit def doubleInterp(value: Double)(implicit tm: TypeMapper[Double]): Interp = typeInterp(
      value
    )
    implicit def longInterp(value: Long)(implicit tm: TypeMapper[Long]): Interp = typeInterp(value)

    implicit def optionInterp[T: TypeMapper](value: Option[T])(
        implicit tm: TypeMapper[Option[T]]
    ): Interp =
      typeInterp(value)(tm)

    implicit def typeInterp[T: TypeMapper](value: T): Interp = TypeInterp(value)
    case class TypeInterp[T: TypeMapper](value: T) extends Interp {
      def mappedType: TypeMapper[T] = implicitly[TypeMapper[T]]
    }
  }
}
