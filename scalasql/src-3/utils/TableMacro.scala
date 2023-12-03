package scalasql.utils

import scala.quoted.*

trait TableMacros{
  inline def initMetadata[V[_[_]]]()(using table: scalasql.Table[V]): scalasql.Table.Metadata[V] = ${TableMacros.initMetadata0[V]()('table)}

}
object TableMacros{
  def initMetadata0[V[_[_]]]()(table: Sql[scalasql.Table[V]])(using Type[V], Quotes): Sql[scalasql.Table.Metadata[V]] = {
    import quotes.reflect._
    println(Type.of[V])
    println(Type.of[V[Sql]])
    println(Type.show[V])
    println(Type.show[V[Sql]])
    println(TypeTree.of[V[Sql]].symbol)
//    val queryParams = for (applyParam <- applyParameters) yield {
//      val name = applyParam.name
//      type T = applyParam.info.typeArgs.head
//      '{scalasql.Column[T]()(implicitly, ${name.toString}, ${c.prefix}).expr($tableRef)}
//    }
//
//    val flattenExprs = for (applyParam <- applyParameters) yield {
//      val name = applyParam.name
//      q"_root_.scalasql.Table.Internal.flattenPrefixed(table.${TermName(name.toString)}, ${name.toString})"
//    }

//    val allFlattenedExprs = flattenExprs.reduceLeft((l, r) => q"$l ++ $r")


    '{
      new scalasql.Table.Metadata[V](
        new scalasql.Table.Internal.TableQueryable(
          table => ???.asInstanceOf[Seq[(List[String], scalasql.query.Sql[_])]],
          ???.asInstanceOf[OptionPickler.Reader[V[scalasql.Id]]]
        ),
        (tableRef: scalasql.query.TableRef) => ???
      )
    }
  }
}