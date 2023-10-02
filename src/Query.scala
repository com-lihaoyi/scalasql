package usql

case class Query[T](expr: T, filter: Seq[Expr[Boolean]] = Nil) {
  def map[V](f: T => V): Query[V] = Query(f(expr), filter)
  def filter(f: T => Expr[Boolean]): Query[T] = Query(expr, filter ++ Seq(f(expr)))
}

trait Expr[T] {
  def toAtomics: Seq[Atomic[_]]
  def toTables: Set[Table.Base]
}

trait Atomic[T] extends Expr[T]{
  def toSqlExpr: String
  def toAtomics: Seq[Atomic[_]] = Seq(this)
}
object Atomic{
  implicit def atomicW[T]: OptionPickler.Writer[Atomic[T]] = {
    OptionPickler.writer[String].comap[Atomic[T]](_.toSqlExpr)
  }
  def apply[T](x: T) = new Atomic[T] {
    override def toSqlExpr: String = x.toString
    override def toTables: Set[Table.Base] = Set()
  }
}

case class Column[T]()(implicit val name: sourcecode.Name,
                       val table: Table.Base) extends Atomic[T]{
  def toSqlExpr = DatabaseApi.tableNameMapper.value(table.tableName) + "." + DatabaseApi.columnNameMapper.value(name.value)
  def toTables = Set(table)
}

object Column{
  implicit def columnW[T]: OptionPickler.Writer[Column[T]] = {
    OptionPickler.writer[String].comap[Column[T]](_.toSqlExpr)
  }
}

