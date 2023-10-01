package usql

trait Expr[T] {
  def toAtomics: Seq[Atomic[_]]
  def toTables: Set[Table.Base]
}

trait Atomic[T] extends Expr[T]{
  def toSqlExpr: String
  def toAtomics: Seq[Atomic[_]] = Seq(this)
}
object Atomic{
  implicit def atomicW[T]: upickle.default.Writer[Atomic[T]] = {
    upickle.default.writer[String].comap[Atomic[T]](_.toSqlExpr)
  }
  def apply[T](x: T) = new Atomic[T] {
    override def toSqlExpr: String = x.toString
    override def toTables: Set[Table.Base] = Set()
  }
}

case class Column[T]()(implicit val name: sourcecode.Name,
                       val table: Table.Base) extends Atomic[T]{
  def toSqlExpr = table.tableName + "." + name.value
  def toTables = Set(table)
}
object Column{
  implicit def columnW[T]: upickle.default.Writer[Column[T]] = {
    upickle.default.writer[String].comap[Column[T]](_.toSqlExpr)
  }
}

case class Query[T](expr: T, filter: Seq[Expr[Boolean]] = Nil) {

  def map[V](f: T => V): Query[V] = Query(f(expr), filter)
  def filter(f: T => Expr[Boolean]): Query[T] = Query(expr, filter ++ Seq(f(expr)))

}

