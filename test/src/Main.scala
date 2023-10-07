package usql

import mainargs.{ParserForMethods, main}
import ExprOps._


object Main {
  @main
  def main() = {
//    println(Query(Foo.bar).toSqlQuery)
//    println(Query(Foo.bar).map(_ * 2).toSqlQuery)
//    println(Query(Foo.bar).filter(_ > 3).map(_ * 2).toSqlQuery)
//    println(Query(Foo.bar).map(_ * 2).filter(_ > 3).toSqlQuery)
//    println(Query(Foo.bar).filter(_ > 3).map(_ * 2).filter(_ > 3).toSqlQuery)
//    println(Query(Foo.bar * 2).toSqlQuery)
//    println(Query(Foo(Foo.bar * 2, Foo.qux)).toSqlQuery)
//    println(Query(Foo(Foo.bar * 2, Foo.qux)).map(foo => Foo(foo.bar, foo.qux)).toSqlQuery)
//
//    println(Query(Foo.*).map(foo => Foo(foo.bar, foo.qux)).toSqlQuery)

  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}
