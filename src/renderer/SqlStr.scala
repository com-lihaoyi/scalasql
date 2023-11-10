package scalasql.renderer

import scalasql.MappedType
import scalasql.query.Expr

/**
 * A SQL query with interpolated `?`s expressions and the associated
 * interpolated values, of type [[Interp]]. Accumulates SQL snippets, parameters,
 * and referenced expressions in a tree structure to minimize copying overhead,
 * until [[SqlStr.flatten]] is called to convert it into a [[SqlStr.Flattened]]
 */
class SqlStr(
    private val queryParts: Seq[String],
    private val params: Seq[SqlStr.Interp],
    val isCompleteQuery: Boolean,
    private val referencedExprs: Seq[Expr.Identity]
) {
  def +(other: SqlStr) = {
    new SqlStr(SqlStr.plusParts, Seq(this, other), false, Nil)
  }

  def withCompleteQuery(v: Boolean) = new SqlStr(queryParts, params, v, referencedExprs)
}

object SqlStr {
  private val plusParts = Array("", "", "")
  class Flattened(
      val queryParts: Seq[String],
      val params: Seq[Interp.TypeInterp[_]],
      isCompleteQuery: Boolean,
      val referencedExprs: Seq[Expr.Identity]
  ) extends SqlStr(queryParts, params, isCompleteQuery, referencedExprs)

  /**
   * Helper method turn an `Option[T]` into a [[SqlStr]], returning
   * the empty string if the `Option` is `None`
   */
  def opt[T](t: Option[T])(f: T => SqlStr) = t.map(f).getOrElse(sql"")

  /**
   * Helper method turn an `Seq[T]` into a [[SqlStr]], returning
   * the empty string if the `Seq` is empty
   */
  def optSeq[T](t: Seq[T])(f: Seq[T] => SqlStr) = if (t.nonEmpty) f(t) else sql""

  /**
   * Flattens out a [[SqlStr]] into a single flattened [[SqlStr.Flattened]] object,
   * at which point you can use its `queryParts`, `params`, `referencedExprs`, etc.
   */
  def flatten(self: SqlStr): Flattened = {
    val finalParts = collection.mutable.Buffer[String]()
    val finalArgs = collection.mutable.Buffer[Interp.TypeInterp[_]]()
    val finalExprs = collection.mutable.Buffer[Expr.Identity]()

    def rec(self: SqlStr, topLevel: Boolean): Unit = {
      finalExprs.appendAll(self.referencedExprs)
      var boundary = true
      if (!topLevel && self.isCompleteQuery) addFinalPart("(")
      boundary = true

      def addFinalPart(s: String) = {
        if (boundary && finalParts.nonEmpty) finalParts(finalParts.length - 1) = finalParts.last + s
        else finalParts.append(s)
      }

      for ((p, a) <- self.queryParts.zip(self.params)) {
        addFinalPart(p)
        boundary = false
        a match {
          case si: Interp.SqlStrInterp =>
            rec(si.s, false)
            boundary = true

          case s: Interp.TypeInterp[_] => finalArgs.append(s)
        }
      }

      addFinalPart(self.queryParts.last)
      if (!topLevel && self.isCompleteQuery) addFinalPart(")")
    }

    rec(self, true)
    new Flattened(finalParts.toSeq, finalArgs.toSeq, self.isCompleteQuery, finalExprs.toSeq)
  }

  /**
   * Provides the sql"..." syntax for constructing [[SqlStr]]s
   */
  implicit class SqlStringSyntax(sc: StringContext) {
    def sql(args: Interp*) = new SqlStr(sc.parts, args, false, Nil)
  }

  /**
   * Joins a `Seq` of [[SqlStr]]s into a single [[SqlStr]] using the given [[sep]] separator
   */
  def join(strs: Seq[SqlStr], sep: SqlStr = sql""): SqlStr = {
    if (strs.isEmpty) sql"" else strs.reduce(_ + sep + _)
  }

  /**
   * Converts a raw `String` into a [[SqlStr]]. Note that this must be used
   * carefully to avoid SQL injection attacks.
   */
  def raw(s: String, referencedExprs: Seq[Expr.Identity] = Nil) =
    new SqlStr(Seq(s), Nil, false, referencedExprs)

  trait Renderable {
    def renderToSql(implicit ctx: Context): (SqlStr, Seq[MappedType[_]])
  }

  /**
   * Something that can be interpolated into a [[SqlStr]].
   */
  sealed trait Interp
  object Interp {
    implicit def renderableInterp(t: Renderable)(implicit ctx: Context): Interp =
      SqlStrInterp(t.renderToSql(ctx)._1)

    implicit def sqlStrInterp(s: SqlStr): Interp = SqlStrInterp(s)

    case class SqlStrInterp(s: SqlStr) extends Interp

    // Not sure why these two additional implicit conversions are needed
    implicit def strInterp(value: String): Interp = typeInterp(value)
    implicit def intInterp(value: Int): Interp = typeInterp(value)
    implicit def booleanInterp(value: Boolean): Interp = typeInterp(value)
    implicit def doubleInterp(value: Double): Interp = typeInterp(value)
    implicit def longInterp(value: Long): Interp = typeInterp(value)

    implicit def optionInterp[T: MappedType](value: Option[T]): Interp =
      typeInterp(value)(MappedType.OptionType)

    implicit def typeInterp[T: MappedType](value: T): Interp = TypeInterp(value)
    case class TypeInterp[T: MappedType](value: T) extends Interp {
      def mappedType: MappedType[T] = implicitly[MappedType[T]]
    }
  }
}
