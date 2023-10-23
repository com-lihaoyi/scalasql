package usql.renderer

import usql.query.{Expr, Select}

/**
 * Represents a SQL query with interpolated `?`s expressions and the associated
 * interpolated values, of type [[Interp]]
 */
class SqlStr(
    private val queryParts: Seq[String],
    private val params: Seq[Interp],
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
      params: Seq[Interp.Simple],
      isCompleteQuery: Boolean
  )

  def opt[T](t: Option[T])(f: T => SqlStr) = t.map(f).getOrElse(usql"")
  def optSeq[T](t: Seq[T])(f: Seq[T] => SqlStr) = if (t.nonEmpty) f(t) else usql""

  def flatten(self: SqlStr): Flattened = {
    val finalParts = collection.mutable.Buffer[String]()
    val finalArgs = collection.mutable.Buffer[Interp.Simple]()

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

          case s: Interp.Simple => finalArgs.append(s)
        }
      }

      addFinalPart(self.queryParts.last)
      if (!topLevel && self.isCompleteQuery) addFinalPart(")")
    }

    rec(self, true)
    Flattened(finalParts.toSeq, finalArgs.toSeq, self.isCompleteQuery)
  }

  implicit class SqlStringSyntax(sc: StringContext) {
    def usql(args: Interp*) = new SqlStr(sc.parts, args, false)
  }

  def join(strs: Seq[SqlStr], sep: SqlStr = usql""): SqlStr = {
    if (strs.isEmpty) usql""
    else strs.reduce(_ + sep + _)
  }

  def raw(s: String) = new SqlStr(Seq(s), Nil, false)
}

sealed trait Interp

object Interp {
  sealed trait Simple extends Interp{
    def castType: String
  }

  trait Renderable {
    def toSqlQuery(implicit ctx: Context): SqlStr
  }

  implicit def stringInterp(s: String): Interp = StringInterp(s)
  case class StringInterp(s: String) extends Simple{
    def castType = "LONGVARCHAR"
  }

  implicit def intInterp(i: Int): Interp = IntInterp(i)
  case class IntInterp(i: Int) extends Simple{
    def castType = "INT"
  }

  implicit def doubleInterp(d: Double): Interp = DoubleInterp(d)
  case class DoubleInterp(d: Double) extends Simple{
    def castType = "DOUBLE"
  }

  implicit def booleanInterp(b: Boolean): Interp = BooleanInterp(b)
  case class BooleanInterp(b: Boolean) extends Simple{
    def castType = "BOOLEAN"
  }

  implicit def dateInterp(b: java.sql.Date): Interp = DateInterp(b)
  case class DateInterp(b: java.sql.Date) extends Simple{
    def castType = "DATE"
  }

  implicit def timeInterp(b: java.sql.Time): Interp = TimeInterp(b)
  case class TimeInterp(b: java.sql.Time) extends Simple{
    def castType = "TIME"
  }

  implicit def timestampInterp(b: java.sql.Timestamp): Interp = TimestampInterp(b)
  case class TimestampInterp(b: java.sql.Timestamp) extends Simple{
    def castType = "TIMESTAMP"
  }

  implicit def renderableInterp(t: Renderable)(implicit ctx: Context): Interp =
    SqlStrInterp(t.toSqlQuery(ctx))

  implicit def sqlStrInterp(s: SqlStr): Interp = SqlStrInterp(s)
  case class SqlStrInterp(s: SqlStr) extends Interp
}
