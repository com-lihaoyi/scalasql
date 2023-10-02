package usql

case class SqlString(queryParts: Seq[String], params: Seq[Interp], $sqlString: Unit) {
  def ++(other: SqlString) = new SqlString(
    queryParts.init ++ Seq(queryParts.last + other.queryParts.head)  ++ other.queryParts.tail,
    params ++ other.params,
    ()
  )
}
object SqlString {
  implicit def writer: OptionPickler.ReadWriter[SqlString] = OptionPickler.macroRW

  implicit class SqlStringSyntax(sc: StringContext) {
    def usql(args: Interp*) = new SqlString(sc.parts, args, ())
  }

  def join(strs: Seq[SqlString], sep: SqlString): SqlString = {
    if (strs.isEmpty) usql""
    else strs.reduce(_ ++ sep ++ _)
  }
  def raw(s: String) = new SqlString(Seq(s), Nil, ())
}

sealed trait Interp
object Interp{
  implicit def stringInterp(s: String): Interp = StringInterp(s)
  case class StringInterp(s: String) extends Interp
  implicit def stringWriter: OptionPickler.ReadWriter[StringInterp] = OptionPickler.macroRW

  implicit def intInterp(i: Int): Interp = IntInterp(i)
  case class IntInterp(i: Int) extends Interp
  implicit def intWriter: OptionPickler.ReadWriter[IntInterp] = OptionPickler.macroRW

  implicit def doubleInterp(d: Double) = DoubleInterp(d)
  case class DoubleInterp(d: Double) extends Interp
  implicit def doubleWriter: OptionPickler.ReadWriter[DoubleInterp] = OptionPickler.macroRW

  implicit def booleanInterp(b: Boolean) = BooleanInterp(b)
  case class BooleanInterp(b: Boolean) extends Interp
  implicit def booleanWriter: OptionPickler.ReadWriter[BooleanInterp] = OptionPickler.macroRW

  implicit def interpWriter: OptionPickler.ReadWriter[Interp] = OptionPickler.macroRW
}
