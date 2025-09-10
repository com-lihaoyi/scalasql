package scalasql.dialects

import scalasql._
import sourcecode.Text
import utest._
import utils.MsSqlSuite

trait MsSqlDialectTests extends MsSqlSuite {
  def description = "Operations specific to working with Microsoft SQL Databases"

  case class BoolTypes[T[_]](
      nullable: T[Option[Boolean]],
      nonNullable: T[Boolean],
      a: T[Int],
      b: T[Int],
      comment: T[String]
  )

  object BoolTypes extends Table[BoolTypes]
  val value = BoolTypes[Sc](
    nullable = Some(false),
    nonNullable = true,
    a = 1,
    b = 2,
    "first"
  )
  val value2 = BoolTypes[Sc](
    nullable = Some(true),
    nonNullable = false,
    a = 10,
    b = 5,
    "second"
  )

  def tests = {
    Tests {

      test("top") - checker(
        query = Buyer.select.take(0),
        sql = """
        SELECT TOP(?) buyer0.id AS id, buyer0.name AS name, buyer0.date_of_birth AS date_of_birth
        FROM buyer buyer0
      """,
        value = Seq[Buyer[Sc]](),
        docs = """
        For ScalaSql's Microsoft SQL dialect provides, the `.take(n)` operator translates
        into a SQL `TOP(n)` clause
      """
      )

      test("bool vs bit") - checker.recorded(
        """Insert rows with BIT values""",
        Text {
          db.run(
            BoolTypes.insert.columns(
              _.nullable := value.nullable,
              _.nonNullable := value.nonNullable,
              _.a := value.a,
              _.b := value.b,
              _.comment := value.comment
            )
          ) ==> 1
          db.run(
            BoolTypes.insert.columns(
              _.nullable := value2.nullable,
              _.nonNullable := value2.nonNullable,
              _.a := value2.a,
              _.b := value2.b,
              _.comment := value2.comment
            )
          ) ==> 1
        }
      )

      test("uodate BIT") - checker(
        query = BoolTypes
          .update(_.a `=` 1)
          .set(_.nonNullable := true),
        sql = "UPDATE bool_types SET non_nullable = ? WHERE (bool_types.a = ?)"
      )

    }
  }
}
