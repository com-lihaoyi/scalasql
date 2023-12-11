package example

import geny.Generator
import scalasql.{DbApi, Sc, Table}

import scalasql.SqliteDialect._
import scalasql.core.SqlStr.SqlStringSyntax

object CheatSheetExample {

  case class Foo[T[_]](
      id: T[Int],
      myStr: T[String],
      myInt: T[Int]
  )

  object Foo extends Table[Foo]

  case class Bar[T[_]](
      id: T[Int],
      fooId: T[Int]
  )

  object Bar extends Table[Bar]

  // Just make sure this stuff compiles; don't bother running it
  def main(args: Array[String]): Unit = {
    val db: DbApi = ???
    import scalasql.SqliteDialect._
    import scalasql.core.Expr

    val str = "hello"

    // Select Query
    db.run(Foo.select.filter(_.myStr === str)): Seq[Foo[Sc]]

    // Update Query
    db.run(Foo.update(_.myStr === str).set(_.myInt := 123)): Int

    /// **SQL Queries**
    // SQL Select Query
    db.runSql[Foo[Sc]](sql"-- SELECT * FROM foo WHERE foo.my_str = $str"): Seq[Foo[Sc]]

    // SQL Update Query
    db.updateSql(sql"UPDATE foo SET my_int = 123 WHERE foo.my_str = $str"): Int

    /// **Raw Queries**

    // Raw Select Query
    db.runRaw[Foo[Sc]]("SELECT * FROM foo WHERE foo.my_str = ?", Seq(str)): Seq[Foo[Sc]]

    // Raw Update Query
    db.updateRaw("UPDATE foo SET my_int = 123 WHERE foo.my_str = ?", Seq(str)): Int

    /// **Streaming Queries**

    // Streaming Select Query
    db.stream(Foo.select.filter(_.myStr === str)): Generator[Foo[Sc]]

    // Streaming SQL Select Query
    db.streamSql[Foo[Sc]](sql"SELECT * FROM foo WHERE foo.my_str = $str"): Generator[Foo[Sc]]

    /// ### Selects

    Foo.select // Seq[Foo[Sc]]
    // SELECT * FROM foo

    Foo.select.map(_.myStr) // Seq[String]
    // SELECT my_str FROM foo

    Foo.select.map(t => (t.myStr, t.myInt)) // Seq[(String, Int)]
    // SELECT my_str, my_int FROM foo

    Foo.select.sumBy(_.myInt) // Int
    // SELECT SUM(my_int) FROM foo

    Foo.select.sumByOpt(_.myInt) // Option[Int]
    // SELECT SUM(my_int) FROM foo

    Foo.select.size // Int
    // SELECT COUNT(1) FROM foo

    Foo.select.aggregate(fs => (fs.sumBy(_.myInt), fs.maxBy(_.myInt))) // (Int, Int)
    // SELECT SUM(my_int), MAX(my_int) FROM foo

    Foo.select.filter(_.myStr === "hello") // Seq[Foo[Sc]]
    // SELECT * FROM foo WHERE my_str = "hello"

    Foo.select.filter(_.myStr === Expr("hello")) // Seq[Foo[Sc]]
    // SELECT * FROM foo WHERE my_str = "hello"

    Foo.select.filter(_.myStr === "hello").single // Foo[Sc]
    // SELECT * FROM foo WHERE my_str = "hello"

    Foo.select.map(_.myInt).sorted.asc // Seq[Foo[Sc]]
    // SELECT * FROM foo ORDER BY my_int ASC

    Foo.select.sortBy(_.myInt).asc.take(20).drop(5) // Seq[Foo[Sc]]
    // SELECT * FROM foo ORDER BY my_int ASC LIMIT 15 OFFSET 5

    Foo.select.map(_.myInt.cast[String]) // Seq[String]
    // SELECT CAST(my_int AS VARCHAR) FROM foo

    Foo.select.join(Bar)(_.id === _.fooId) // Seq[(Foo[Sc], Bar[Sc])]
    // SELECT * FROM foo JOIN bar ON foo.id = foo2.foo_id

    Foo.select.leftJoin(Bar)(_.id === _.fooId) // Seq[(Foo[Sc], Option[Bar[Sc]])]
    // SELECT * FROM foo LEFT JOIN bar ON foo.id = foo2.foo_id

    Foo.select.rightJoin(Bar)(_.id === _.fooId) // Seq[(Option[Foo[Sc]], Bar[Sc])]
    // SELECT * FROM foo RIGHT JOIN bar ON foo.id = foo2.foo_id

    Foo.select.outerJoin(Bar)(_.id === _.fooId) // Seq[(Option[Foo[Sc]], Option[Bar[Sc]])]
    // SELECT * FROM foo FULL OUTER JOIN bar ON foo.id = foo2.foo_id

    for (f <- Foo.select; b <- Bar.join(f.id === _.fooId)) yield (f, b) // Seq[(Foo[Sc], Bar[Sc])]
    // SELECT * FROM foo JOIN bar ON foo.id = foo2.foo_id

    /// ### Insert/Update/Delete

    Foo.insert.columns(_.myStr := "hello", _.myInt := 123) // 1
    // INSERT INTO foo (my_str, my_int) VALUES ("hello", 123)

    Foo.insert.batched(_.myStr, _.myInt)(("a", 1), ("b", 2)) // 2
    // INSERT INTO foo (my_str, my_int) VALUES ("a", 1), ("b", 2)

    Foo.update(_.myStr === "hello").set(_.myInt := 123) // Int
    // UPDATE foo SET my_int = 123 WHERE foo.my_str = "hello"

    Foo.update(_.myStr === "a").set(t => t.myInt := t.myInt + 1) // Int
    // UPDATE foo SET my_int = foo.my_int + 1 WHERE foo.my_str = "a"

    Foo
      .update(_.myStr === "a")
      .set(t => t.myInt := t.myInt + 1)
      .returning(f => (f.id, f.myInt)) // Seq[(Int, Int)]
    // UPDATE foo SET my_int = foo.my_int + 1 WHERE foo.my_str = "a" RETURNING foo.id, foo.my_int

    Foo.delete(_.myStr === "hello") // Int
    // DELETE FROM foo WHERE foo.my_str = "hello"
  }
}
