package scalasql.dbzio.tests

import zio.*
import zio.test.*

import scalasql.*
import scalasql.dbzio.*

object SyntaxSpec extends ZIOSpecDefault:

  def spec = suite("Syntax")(
    suite("atMostOne")(
      test("returns None for empty seq"):
        val effect: UIO[Seq[String]] = ZIO.succeed(Seq.empty)
        for result <- effect.atMostOne
        yield assertTrue(result.isEmpty)
      ,
      test("returns Some for single element"):
        val effect: UIO[Seq[String]] = ZIO.succeed(Seq("only"))
        for result <- effect.atMostOne
        yield assertTrue(result.contains("only"))
      ,
      test("fails with MultipleResults for 2+ elements"):
        val effect: UIO[Seq[String]] = ZIO.succeed(Seq("a", "b"))
        for exit <- effect.atMostOne.exit
        yield assertTrue(exit.isFailure)
    ),
    suite("orNotFound")(
      test("unwraps Some"):
        val effect: UIO[Option[String]] = ZIO.succeed(Some("found"))
        for result <- effect.orNotFound
        yield assertTrue(result == "found")
      ,
      test("fails with NotFound for None"):
        val effect: UIO[Option[String]] = ZIO.succeed(None)
        for exit <- effect.orNotFound.exit
        yield assertTrue(exit match
          case Exit.Failure(cause) =>
            cause.failureOption.exists(_.isInstanceOf[DbLookupException.NotFound[?]])
          case _ => false)
    )
  )
