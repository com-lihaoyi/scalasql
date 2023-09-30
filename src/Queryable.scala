package usql

trait Queryable[T] {
  def toAtomics(t: T): Seq[Atomic[_]]
  def toTables(t: T): Set[Table0]
}
object Queryable{

  implicit def rw[V <: Expr[_]]: Queryable[V] = new Queryable[V] {
    def toAtomics(t: V): Seq[Atomic[_]] = t.toAtomics
    def toTables(t: V): Set[Table0] = t.toTables
  }
}


trait Expr[T] {
  def toAtomics: Seq[Atomic[_]]
  def toTables: Set[Table0]
}

object Expr{

}
