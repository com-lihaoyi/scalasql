package scalasql.dbzio

import zio.*

import scalasql.*

extension [R, E, T](t: ZIO[R, E, Seq[T]])
  /** Assert at most one result exists. Fails with `MultipleResults` if 2+ rows returned. */
  def atMostOne(
      using DbEntityName[T]
  ): ZIO[R, E | DbLookupException.MultipleResults[T], Option[T]] =
    t.flatMap: chunk =>
      ZIO.cond(chunk.size < 2, chunk.headOption, DbLookupException.MultipleResults[T])

extension [R, E, T](t: ZIO[R, E, Option[T]])
  /** Unwrap `Option`, failing with `NotFound` if `None`. */
  def orNotFound(using DbEntityName[T]): ZIO[R, E | DbLookupException.NotFound[T], T] =
    t.flatMap(ZIO.getOrFailWith(DbLookupException.NotFound[T]))
