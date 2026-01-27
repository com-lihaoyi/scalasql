package scalasql.dialects

import scalasql._
import scalasql.core.Expr
import utest._
import utils.ScalaSqlSuite
import ujson.Value

trait PostgresJsonTests extends ScalaSqlSuite with PostgresDialect {
  def description = "JSON operations"

  // Mock DB for SQL generation checks only
  lazy val mockDb = new DbClient.Connection(null, new Config {})

  def check[Q, R](query: Q, sql: String)(implicit qr: Queryable[Q, R]) = {
    val result = mockDb.renderSql(query)
    val expected = sql.trim.replaceAll("\\s+", " ")
    assert(result == expected)
  }

  // Placeholder for checker required by ScalaSqlSuite, but we won't use it
  def checker = ???

  override def utestBeforeEach(path: Seq[String]): Unit = {}
  override def utestAfterEach(path: Seq[String]): Unit = {}

  case class JsonTable[T[_]](id: T[Int], data: T[ujson.Value], dataJson: T[ujson.Value])
  object JsonTable extends Table[JsonTable] {
     override def tableName = "json_table"
     override def tableColumnNameOverride(s: String) = s match {
       case "dataJson" => "data_json"
       case s => s
     }
  }

  def tests = Tests {
     test("access") - {
         test("key") - check(
            query = JsonTable.select.map(t => t.data -> "a"),
            sql = "SELECT json_table0.data -> ? AS res FROM json_table json_table0"
         )
         test("index") - check(
            query = JsonTable.select.map(t => t.data -> 0),
            sql = "SELECT json_table0.data -> ? AS res FROM json_table json_table0"
         )
         test("keyText") - check(
            query = JsonTable.select.map(t => t.data ->> "b"),
            sql = "SELECT json_table0.data ->> ? AS res FROM json_table json_table0"
         )
         test("indexText") - check(
            query = JsonTable.select.map(t => t.data ->> 1),
            sql = "SELECT json_table0.data ->> ? AS res FROM json_table json_table0"
         )
         test("path") - check(
            query = JsonTable.select.map(t => t.data #> "a"),
            sql = "SELECT json_table0.data #> ARRAY[?] AS res FROM json_table json_table0"
         )
         test("pathText") - check(
            query = JsonTable.select.map(t => t.data #>> "b"),
            sql = "SELECT json_table0.data #>> ARRAY[?] AS res FROM json_table json_table0"
         )
     }

     test("contains") - {
         test("right") - check(
            query = JsonTable.select.filter(t => t.data @> ujson.Obj("a" -> 1)).map(_.id),
            sql = "SELECT json_table0.id AS res FROM json_table json_table0 WHERE json_table0.data @> ?"
         )
         test("left") - check(
            query = JsonTable.select.filter(t => t.data <@ ujson.Arr(1, 2, 3)).map(_.id),
            sql = "SELECT json_table0.id AS res FROM json_table json_table0 WHERE json_table0.data <@ ?"
         )
     }

     test("exists") - {
         test("key") - check(
            query = JsonTable.select.filter(t => t.data ? "a").map(_.id),
            sql = "SELECT json_table0.id AS res FROM json_table json_table0 WHERE jsonb_exists(json_table0.data, ?)"
         )
         test("any") - check(
            query = JsonTable.select.filter(t => t.data ?| ("a", "z")).map(_.id),
            sql = "SELECT json_table0.id AS res FROM json_table json_table0 WHERE jsonb_exists_any(json_table0.data, ARRAY[?, ?])"
         )
         test("all") - check(
            query = JsonTable.select.filter(t => t.data ?& ("a", "b")).map(_.id),
            sql = "SELECT json_table0.id AS res FROM json_table json_table0 WHERE jsonb_exists_all(json_table0.data, ARRAY[?, ?])"
         )
     }

     test("concat") - check(
        query = JsonTable.select.filter(_.id === 1).map(t => t.data || ujson.Obj("c" -> 3)),
        sql = "SELECT json_table0.data || ? AS res FROM json_table json_table0 WHERE (json_table0.id = ?)"
     )

     test("delete") - {
         test("key") - check(
            query = JsonTable.select.filter(_.id === 1).map(t => t.data - "b"),
            sql = "SELECT json_table0.data - ? AS res FROM json_table json_table0 WHERE (json_table0.id = ?)"
         )
         test("index") - check(
            query = JsonTable.select.filter(_.id === 2).map(t => t.data - 1),
            sql = "SELECT json_table0.data - ? AS res FROM json_table json_table0 WHERE (json_table0.id = ?)"
         )
     }

     test("json") - {
         // Testing JSON type (non-binary)
         test("access") - check(
            query = JsonTable.select.map(t => t.dataJson -> "a"),
            sql = "SELECT json_table0.data_json -> ? AS res FROM json_table json_table0"
         )
     }

     test("insert") - {
        test("simple") - {
           val query = JsonTable.insert.values(
               JsonTable(
                   id = 3,
                   data = ujson.Obj("x" -> 10),
                   dataJson = ujson.Obj("y" -> 20)
               )
           )
           check(
               query,
               "INSERT INTO json_table (id, data, data_json) VALUES (?, ?, ?)"
           )
        }
     }
  }
}
