package usql

/**
 * Converts back and forth between a tree-shaped JSON and flat key-value map
 */
object FlatJson {

  val delimiter = "__"
  val basePrefix = "res"

  def flatten(x: ujson.Value): Seq[(String, String)] = flatten0(x, basePrefix)
  def flatten0(x: ujson.Value, prefix: String): Seq[(String, String)] = {
    x match {
      case ujson.Obj(kvs) =>
        kvs.toSeq.flatMap { case (k, v) => flatten0(v, prefix + delimiter + k) }
      case ujson.Arr(vs) =>
        vs.zipWithIndex.toSeq.flatMap { case (v, i) => flatten0(v, prefix + delimiter + i) }
      case ujson.Str(s) => Seq(prefix -> s)
    }
  }

  def unflatten(kvs: Seq[(String, String)]): ujson.Value = unflatten0(kvs)(basePrefix)
  def unflatten0(kvs: Seq[(String, String)]): ujson.Value = {
    val root: ujson.Value = ujson.Obj()

    for ((k, v) <- kvs) {
      val segments = k.split(delimiter).map(s => (s, s.forall(_.isDigit)))
      var current = root

      for (Array((s, isDigit), (nextS, nextIsDigit)) <- segments.sliding(2)) {
        def nextContainer0 = if (nextIsDigit) ujson.Arr() else ujson.Obj()
        val nextContainer = if (isDigit) {
          assert(s.toInt == current.arr.length)
          current.arr.append(nextContainer0)
          nextContainer0
        } else {
          current.obj.getOrElseUpdate(s, nextContainer0)
        }
        current = nextContainer
      }

      val (lastS, lastIsDigit) = segments.last

      if (lastIsDigit) current.arr.append(v)
      else current(lastS) = v
    }

    root
  }
}
