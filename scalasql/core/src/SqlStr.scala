package scalasql.core

/**
 * A SQL query with interpolated `?`s expressions and the associated
 * interpolated values, of type [[Interp]]. Accumulates SQL snippets, parameters,
 * and referenced expressions in a tree structure to minimize copying overhead,
 * until [[SqlStr.flatten]] is called to convert it into a [[SqlStr.Flattened]]
 */
class SqlStr(
    private val queryParts: Array[CharSequence],
    private val interps: Array[SqlStr.Interp],
    val isCompleteQuery: Boolean,
    private val referencedExprs: Array[Expr.Identity]
) extends SqlStr.Renderable {
  def +(other: SqlStr) = {
    new SqlStr(
      SqlStr.plusParts,
      Array[SqlStr.Interp](this, other),
      false,
      SqlStr.emptyIdentityArray
    )
  }

  def withCompleteQuery(v: Boolean) = new SqlStr(queryParts, interps, v, referencedExprs)
  override def toString = SqlStr.flatten(this).renderSql(false)

  override private[scalasql] def renderSql(ctx: Context): SqlStr = this
}

object SqlStr {
  private val emptyIdentityArray = Array.empty[Expr.Identity]
  private val emptyInterpArray = Array.empty[SqlStr.Interp]
  private val plusParts = Array[CharSequence]("", "", "")

  /**
   * Represents a [[SqlStr]] that has been flattened out into a single set of
   * parallel arrays, allowing you to render it or otherwise make use of its data.
   */
  class Flattened(
      val queryParts: Array[CharSequence],
      val interps0: Array[Interp],
      isCompleteQuery: Boolean,
      val referencedExprs: Array[Expr.Identity]
  ) extends SqlStr(queryParts, interps0, isCompleteQuery, referencedExprs) {
    def interpsIterator = interps0.iterator.map(_.asInstanceOf[Interp.TypeInterp[?]])
    def renderSql(castParams: Boolean) = {
      val queryStr = queryParts.iterator
        .zipAll(interpsIterator, "", null)
        .map {
          case (part, null) => part
          case (part, param) =>
            val jdbcTypeString = param.mappedType.castTypeString
            if (castParams) part.toString + s"CAST(? AS $jdbcTypeString)" else part.toString + "?"
        }
        .mkString

      queryStr
    }

  }

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
    val finalParts = collection.mutable.ArrayBuilder.make[CharSequence]
    val finalInterps = collection.mutable.ArrayBuilder.make[Interp]
    val finalExprs = collection.mutable.ArrayBuilder.make[Expr.Identity]
    // Equivalent to `finalParts.last`, cached locally for performance
    var lastFinalPart: StringBuilder = null

    def rec(self: SqlStr, topLevel: Boolean): Unit = {
      val queryParts = self.queryParts

      def addFinalPart(boundary: Boolean, s: CharSequence) = {
        if (boundary && lastFinalPart != null) {
          lastFinalPart.append(s)
        } else {
          lastFinalPart = new StringBuilder()
          lastFinalPart.append(s)
          finalParts.addOne(lastFinalPart)
        }
      }
      queryParts.length match {
        case 0 => // do nothing
        case 1 =>
          addFinalPart(true, queryParts.head)
          finalExprs.addAll(self.referencedExprs)
        case _ =>
          val params = self.interps
          finalExprs.addAll(self.referencedExprs)
          var boundary = true
          val parenthesize = !topLevel && self.isCompleteQuery
          if (parenthesize) addFinalPart(boundary, "(")
          boundary = true

          var i = 0
          val length = params.length
          while (i < length) {
            val p = queryParts(i)
            val a = params(i)
            addFinalPart(boundary, p)
            boundary = false
            a match {
              case si: Interp.SqlStrInterp =>
                rec(si.s, false)
                boundary = true

              case s: Interp.TypeInterp[_] => finalInterps.addOne(s)
            }
            i += 1
          }

          addFinalPart(boundary, queryParts.last)
          if (parenthesize) addFinalPart(boundary, ")")
      }

    }

    rec(self, true)
    new Flattened(
      finalParts.result(),
      finalInterps.result(),
      self.isCompleteQuery,
      finalExprs.result()
    )
  }

  /**
   * Provides the sql"..." syntax for constructing [[SqlStr]]s
   */
  implicit class SqlStringSyntax(sc: StringContext) {
    def sql(args: Interp*) =
      new SqlStr(sc.parts.toArray, args.toArray, false, emptyIdentityArray)
  }

  /**
   * Joins a `Seq` of [[SqlStr]]s into a single [[SqlStr]] using the given [[sep]] separator
   */
  def join(strs: IterableOnce[SqlStr], sep: SqlStr = empty): SqlStr = {
    // Basically the same as `strs.iterator.reduceOption(_ + sep + _).getOrElse(empty)`,
    // but implemented manually to improve performance since this is a hot path
    val it = strs.iterator
    val first = it.nextOption()
    val firstIsEmpty = first.isEmpty
    if (firstIsEmpty) empty
    else if (!it.hasNext) first.get
    else {
      var lastFinalPart: StringBuilder = null
      val finalParts = collection.mutable.ArrayBuilder.make[CharSequence]
      val finalInterps = collection.mutable.ArrayBuilder.make[Interp]
      val finalExprs = collection.mutable.ArrayBuilder.make[Expr.Identity]
      def handle(s: SqlStr) = {
        s.queryParts.length match {
          case 0 => // donothing
          case 1 =>
            if (lastFinalPart == null) {
              lastFinalPart = new StringBuilder(s.queryParts.last.toString)
            } else {
              lastFinalPart.append(s.queryParts.last)
            }
          case _ =>
            if (lastFinalPart == null) {
              finalParts.addAll(s.queryParts, 0, s.queryParts.length - 1)
              lastFinalPart = new StringBuilder(s.queryParts.last.toString)
            } else {
              lastFinalPart.append(s.queryParts.head)
              finalParts.addOne(lastFinalPart)
              finalParts.addAll(s.queryParts, 1, s.queryParts.length - 2)
              lastFinalPart = new StringBuilder(s.queryParts.last.toString)
            }
        }

        finalInterps.addAll(s.interps)
        finalExprs.addAll(s.referencedExprs)
      }
      handle(first.get)
      while (it.hasNext) {
        handle(sep)
        handle(it.next())
      }

      finalParts.addOne(lastFinalPart)

      new SqlStr(finalParts.result(), finalInterps.result(), false, finalExprs.result())
    }
  }

  lazy val empty = sql""
  lazy val commaSep = sql", "

  /**
   * Converts a raw `String` into a [[SqlStr]]. Note that this must be used
   * carefully to avoid SQL injection attacks.
   */
  def raw(s: String, referencedExprs: Array[Expr.Identity] = emptyIdentityArray) =
    new SqlStr(Array(s), emptyInterpArray, false, referencedExprs)

  trait Renderable {
    private[scalasql] def renderSql(ctx: Context): SqlStr
  }

  object Renderable {
    def renderSql(x: Renderable)(implicit ctx: Context) = x.renderSql(ctx)
  }

  /**
   * Something that can be interpolated into a [[SqlStr]].
   */
  sealed trait Interp
  object Interp {
    implicit def renderableInterp(t: Renderable)(implicit ctx: Context): Interp =
      SqlStrInterp(Renderable.renderSql(t)(ctx))

    implicit def sqlStrInterp(s: SqlStr): Interp = SqlStrInterp(s)

    case class SqlStrInterp(s: SqlStr) extends Interp

    implicit def typeInterp[T: TypeMapper](value: T): Interp = TypeInterp(value)
    case class TypeInterp[T: TypeMapper](value: T) extends Interp {
      def mappedType: TypeMapper[T] = implicitly[TypeMapper[T]]
    }
  }
}
