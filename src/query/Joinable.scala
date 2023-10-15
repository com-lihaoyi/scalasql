package usql.query

/**
 * Something that can be joined; typically a [[Select]] or a [[Table]]
 */
trait Joinable[T] {
  def select: Select[T]
}
