package usql

import usql.Types.Id

trait Atomic[T] extends Expr[T]{
  def toSqlExpr: String
  def toAtomics: Seq[Atomic[_]] = Seq(this)
}

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

  implicit def rw: upickle.default.ReadWriter[V[Id]]
}

case class Column[T]()(implicit val name: sourcecode.Name,
                       val table: Table0) extends Atomic[T]{
  def toSqlExpr = table.tableName + "." + name.value
  def toTables = Set(table)
}

object Types{
  type Id[T] = T
}