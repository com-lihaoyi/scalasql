package scalasql.renderer

import scalasql.MappedType

import java.sql.JDBCType

/**
 * Represents a SQL query with interpolated `?`s expressions and the associated
 * interpolated values, of type [[Interp]]
 */
class SqlStr(
    private val queryParts: Seq[String],
    private val params: Seq[SqlStr.Interp],
    val isCompleteQuery: Boolean
) {
  def +(other: SqlStr) = new SqlStr(
    queryParts.init ++ Seq(queryParts.last + other.queryParts.head) ++ other.queryParts.tail,
    params ++ other.params,
    false
  )

  def withCompleteQuery(v: Boolean) = new SqlStr(queryParts, params, v)
}

object SqlStr {
  case class Flattened(
      queryParts: Seq[String],
      params: Seq[Interp.TypeInterp[_]],
      isCompleteQuery: Boolean
  )

  def opt[T](t: Option[T])(f: T => SqlStr) = t.map(f).getOrElse(sql"")
  def optSeq[T](t: Seq[T])(f: Seq[T] => SqlStr) = if (t.nonEmpty) f(t) else sql""

  def flatten(self: SqlStr): Flattened = {
    val finalParts = collection.mutable.Buffer[String]()
    val finalArgs = collection.mutable.Buffer[Interp.TypeInterp[_]]()

    def rec(self: SqlStr, topLevel: Boolean): Unit = {
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
    Flattened(finalParts.toSeq, finalArgs.toSeq, self.isCompleteQuery)
  }

  implicit class SqlStringSyntax(sc: StringContext) {
    def sql(args: Interp*) = new SqlStr(sc.parts, args, false)
  }

  def join(strs: Seq[SqlStr], sep: SqlStr = sql""): SqlStr = {
    if (strs.isEmpty) sql"" else strs.reduce(_ + sep + _)
  }

  def raw(s: String) = new SqlStr(Seq(s), Nil, false)

  trait Renderable {
    def toSqlQuery(implicit ctx: Context): (SqlStr, Seq[MappedType[_]])
  }

  sealed trait Interp
  object Interp {
    implicit def renderableInterp(t: Renderable)(implicit ctx: Context): Interp =
      SqlStrInterp(t.toSqlQuery(ctx)._1)

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
