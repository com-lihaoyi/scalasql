package scalasql.utils

import scalasql.Config
import upickle.core.Visitor
import scalasql.query.Expr
import scalasql.renderer.{Context, SqlStr}

/**
 * Converts back and forth between a tree-shaped JSON and flat key-value map
 */
object FlatJson {

  def flatten(x: Seq[(List[String], Expr[_])], context: Context): Seq[(String, SqlStr)] = {
    x.map { case (k, v) =>
      (
        (context.config.columnLabelPrefix +: k).mkString(context.config.columnLabelDelimiter),
        v.toSqlQuery(context)._1
      )
    }
  }

  /**
   * Walk the [[ResultSet]]'s column values and labels and feed them into [[rowVisitor]]
   * to re-construct the Scala object of type [[V]].
   */
  def unflatten[V](
      keys: IndexedSeq[IndexedSeq[String]],
      values: IndexedSeq[Object],
      nulls: IndexedSeq[Boolean],
      rowVisitor: Visitor[_, _]
  ): V = {

    /**
     * Similar to `groupBy`, but assumes groups are contiguous within the collection, and works
     * on start/end/depth indices and "returns" the groups via a callback to avoid allocating
     * intermediate data structures.
     */
    def groupedOn(start: Int, end: Int, depth: Int)(callback: (String, Int, Int) => Unit) = {
      var prevKey = keys(start)(depth)
      var prevIndex = start
      for (i <- Range(start, end)) {
        val nextKey = keys(i)(depth)
        if (nextKey != prevKey) {
          callback(prevKey, prevIndex, i)
          prevKey = nextKey
          prevIndex = i
        }
      }

      callback(prevKey, prevIndex, end)
    }

    /**
     * Recurse over the 2D collection of `keys` using `startIndex`, `endIndex`, and `depth`
     * to minimize the allocation of intermediate data structures
     */
    def rec(startIndex: Int, endIndex: Int, depth: Int, visitor: Visitor[_, _]): (Any, Int, Int) = {
      if (startIndex == endIndex - 1 && depth == keys(startIndex).length) {
        val v = values(startIndex)
        (v, if (nulls(startIndex)) 1 else 0, if (nulls(startIndex)) 0 else 1)

        // Hack to check if a random key looks like a number,
        // in which case this data represents an array
      } else if (keys(startIndex)(depth).head.isDigit) {
        val arrVisitor = visitor.visitArray(-1, -1).narrow
        var nulls = 0
        var nonNulls = 0
        groupedOn(startIndex, endIndex, depth) { (key, chunkStart, chunkEnd) =>
          val (v, subNulls, subNonNulls) = rec(chunkStart, chunkEnd, depth + 1, arrVisitor.subVisitor)
          arrVisitor.visitValue(v, -1)
          nulls += subNulls
          nonNulls += subNonNulls
        }

        val result0 = arrVisitor.visitEnd(-1)
        val result =
          if (!arrVisitor.isInstanceOf[OptionPickler.NullableArrVisitor[_, _]]) result0
          else (if (nulls != 0 && nonNulls == 0) None else Some(result0))
        (result, nulls, nonNulls)
      } else {

        val objVisitor = visitor.visitObject(-1, true, -1).narrow
        var nulls =  0
        var nonNulls = 0
        groupedOn(startIndex, endIndex, depth) { (key, chunkStart, chunkEnd) =>
          val keyVisitor = objVisitor.visitKey(-1)
          objVisitor.visitKeyValue(keyVisitor.visitString(key, -1))
          val (v, subNulls, subNonNulls) = rec(chunkStart, chunkEnd, depth + 1, objVisitor.subVisitor)
          objVisitor.visitValue(v, -1)
          nulls += subNulls
          nonNulls += subNonNulls
        }

        val result0 = objVisitor.visitEnd(-1)

        val result =
          if (!objVisitor.isInstanceOf[OptionPickler.NullableObjVisitor[_, _]]) result0
          else (if (nulls != 0 && nonNulls == 0) None else Some(result0 ))

        (result, nulls, nonNulls)
      }
    }

    rec(0, keys.length, 0, rowVisitor.asInstanceOf[Visitor[Any, Any]])._1.asInstanceOf[V]
  }
}
