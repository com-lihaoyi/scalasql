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

  def unflatten(kvs: Seq[(String, ujson.Value)]): ujson.Value = unflatten0(kvs)(basePrefix)
  def unflatten0(kvs: Seq[(String, ujson.Value)]): ujson.Value = {
    val root: ujson.Value = ujson.Obj()

    for ((k, v) <- kvs) {

      val segments = k.split(delimiter)
      var current = root

      var (prevS, prevIsDigit) = (segments.head, segments.head.forall(_.isDigit))
      for (i <- Range(1, segments.size)) {
        val nextS = segments(i)
        val nextIsDigit = nextS.forall(_.isDigit)
        lazy val nextContainer0 = if (nextIsDigit) ujson.Arr() else ujson.Obj()
        val nextContainer = if (prevIsDigit) {
          val d = prevS.toInt
          val currArrLen = current.arr.length
          if (d == currArrLen) {
            current.arr.append(nextContainer0)
            nextContainer0
          } else if (d < currArrLen) current(d)
          else ???
        } else {
          current.obj.getOrElseUpdate(prevS, nextContainer0)
        }
        prevS = nextS
        prevIsDigit = nextIsDigit
        current = nextContainer
      }

      val lastS = segments.last

      if (lastS.forall(_.isDigit)) current.arr.append(v)
      else current(lastS) = v
    }

    root
  }
}
