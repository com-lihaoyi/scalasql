package usql

trait Table0 {
  def tableName: String
}

abstract class Table[V[_[_]] <: Product]()(implicit name: sourcecode.Name) extends Table0 {
  val tableName = name.value.toLowerCase
  implicit def self: Table[V] = this

  def items[T[_]](t: V[T]): Seq[T[_]] = t.productIterator.map(_.asInstanceOf[T[_]]).toSeq

  implicit def queryable[X[_] <: Expr[_]]: Queryable[V[X]] = new Queryable[V[X]] {
    def toAtomics(t: V[X]): Seq[Atomic[_]] = items(t).flatMap(_.toAtomics)
    def toTables(t: V[X]): Set[Table0] = items(t).flatMap(_.toTables).toSet
  }
}