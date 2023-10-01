package usql

/**
 * Converts back and forth between a tree-shaped JSON and flat key-value map
 */
object FlatJson {

  val delimiter = "__"
  val basePrefix = ""
  def flatten(x: ujson.Value) = flatten0(x, basePrefix)
  def flatten0(x: ujson.Value, prefix: String): Seq[(String, String)] = {
    x match {
      case ujson.Obj(kvs) =>
        kvs.toSeq.flatMap { case (k, v) => flatten0(v, prefix + delimiter + k) }
      case ujson.Str(s) => Seq(prefix -> s)
    }
  }

  def unflatten(kvs: Seq[(String, String)]): ujson.Value = unflatten0(kvs)(basePrefix)
  def unflatten0(kvs: Seq[(String, String)]): ujson.Value = {
    val root = ujson.Obj()

    for ((k, v) <- kvs) {
      val segments = k.split(delimiter)
      var current = root
      for (s <- segments.init) {
        if (!current.value.contains(s)) current(s) = ujson.Obj()
        current = current(s).asInstanceOf[ujson.Obj]
      }

      current(segments.last) = v
    }

    root
  }
}
