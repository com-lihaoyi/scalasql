package usql

/**
 * Represents a SQL query with interpolated `?`s expressions and the associated
 * interpolated values, of type [[Interp]]
 */
case class SqlStr(queryParts: Seq[String], params: Seq[Interp], $sqlString: Unit) {
  def +(other: SqlStr) = new SqlStr(
    queryParts.init ++ Seq(queryParts.last + other.queryParts.head)  ++ other.queryParts.tail,
    params ++ other.params,
    ()
  )
}

object SqlStr {
  implicit def writer: OptionPickler.ReadWriter[SqlStr] = OptionPickler.macroRW

  implicit class SqlStringSyntax(sc: StringContext) {
    def usql(args: Interp*) = new SqlStr(sc.parts, args, ())
  }

  def join(strs: Seq[SqlStr], sep: SqlStr): SqlStr = {
    if (strs.isEmpty) usql""
    else strs.reduce(_ + sep + _)
  }

  def raw(s: String) = new SqlStr(Seq(s), Nil, ())
}

sealed trait Interp
object Interp{
  implicit def stringInterp(s: String): Interp = StringInterp(s)
  case class StringInterp(s: String) extends Interp
  implicit def stringWriter: OptionPickler.ReadWriter[StringInterp] = OptionPickler.macroRW

  implicit def intInterp(i: Int): Interp = IntInterp(i)
  case class IntInterp(i: Int) extends Interp
  implicit def intWriter: OptionPickler.ReadWriter[IntInterp] = OptionPickler.macroRW

  implicit def doubleInterp(d: Double): Interp = DoubleInterp(d)
  case class DoubleInterp(d: Double) extends Interp
  implicit def doubleWriter: OptionPickler.ReadWriter[DoubleInterp] = OptionPickler.macroRW

  implicit def booleanInterp(b: Boolean): Interp = BooleanInterp(b)
  case class BooleanInterp(b: Boolean) extends Interp
  implicit def booleanWriter: OptionPickler.ReadWriter[BooleanInterp] = OptionPickler.macroRW

  implicit def interpWriter: OptionPickler.ReadWriter[Interp] = OptionPickler.macroRW
}
