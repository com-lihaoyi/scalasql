package usql.query

/**
 * Something that can be joined; typically a [[Select]] or a [[Table]]
 */
trait Joinable[Q] {
  def select: Select[Q]
  def isTrivialJoin: Boolean
}
