package scalasql.utils

trait TableMacros{
  def initMetadata[V[_[_]]](): scalasql.Table.Metadata[V] = ???
}