package scalasql.query

/**
 * Something that can be joined; typically a [[Select]] or a [[Table]]
 */
trait Joinable[Q, R] {
  def select: Select[Q, R]
  def isTrivialJoin: Boolean
}
