package usql

trait Queryable[T] {
  def toAtomics(t: T): Seq[Atomic[_]]
  def toTables(t: T): Set[Table.Base]
}

object Queryable{
  implicit def exprQueryable[V <: Expr[_]]: Queryable[V] = new Queryable[V] {
    def toAtomics(t: V): Seq[Atomic[_]] = t.toAtomics
    def toTables(t: V): Set[Table.Base] = t.toTables
  }

  implicit def tuple2Queryable[T <: Expr[_], V <: Expr[_]]: Queryable[(T, V)] = new Queryable[(T, V)] {
    def toAtomics(t: (T, V)): Seq[Atomic[_]] = t._1.toAtomics ++ t._2.toAtomics
    def toTables(t: (T, V)): Set[Table.Base] = t._1.toTables ++ t._2.toTables
  }
}

trait Expr[T] {
  def toAtomics: Seq[Atomic[_]]
  def toTables: Set[Table.Base]
}

object Expr{

}
