package usql

object ExprIntOps extends ExprIntOps
trait ExprIntOps {

  implicit class ExprIntOps0(v: Expr[Int]) {
    def *(x: Int): Atomic[Int] = new Atomic[Int] {
      def toSqlExpr: String = s"${v.asInstanceOf[Atomic[_]].toSqlExpr} * $x"

      def toTables = v.toTables
    }

    def >(x: Int): Atomic[Boolean] = new Atomic[Boolean] {
      def toSqlExpr: String = s"${v.asInstanceOf[Atomic[_]].toSqlExpr} > $x"

      def toTables = v.toTables
    }
    def ===(x: Int): Atomic[Boolean] = new Atomic[Boolean] {
      def toSqlExpr: String = s"${v.asInstanceOf[Atomic[_]].toSqlExpr} = $x"

      def toTables = v.toTables
    }
  }
}
