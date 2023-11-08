package scalasql.query

import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.renderer.{Context, SqlStr}
import scalasql.utils.OptionPickler
import scalasql.{Column, MappedType, Queryable}

/**
 * A SQL `INSERT VALUES` query
 */
trait InsertValues[Q, R] extends InsertReturnable[Q] with Query[Int] {
  def columns: Seq[Column.ColumnExpr[_]]
  def valuesLists: Seq[Seq[Expr[_]]]
}
object InsertValues {
  class Impl[Q, R](
      insert: Insert[Q, R],
      val columns: Seq[Column.ColumnExpr[_]],
      val valuesLists: Seq[Seq[Expr[_]]]
  )(implicit val qr: Queryable[Q, R])
      extends InsertValues[Q, R] {
    def table = insert.table
    def expr: Q = insert.expr

    override def toSqlQuery(implicit ctx: Context) = (
      new Renderer(columns, ctx, valuesLists, table.value.tableName).render(),
      Seq(MappedType.IntType)
    )
    def walk() = Nil
    override def singleRow = true
    override def isExecuteUpdate = true

    override def valueReader: OptionPickler.Reader[Int] = implicitly
  }

  class Renderer(
      columns0: Seq[Column.ColumnExpr[_]],
      prevContext: Context,
      valuesLists: Seq[Seq[Expr[_]]],
      tableName: String
  ) {

    implicit lazy val ctx = prevContext.withExprNaming(Map()).withFromNaming(Map())
    lazy val columns = SqlStr
      .join(columns0.map(c => SqlStr.raw(ctx.config.columnNameMapper(c.name))), sql", ")
    lazy val values = SqlStr.join(
      valuesLists
        .map(values => sql"(" + SqlStr.join(values.map(_.toSqlQuery._1), sql", ") + sql")"),
      sql", "
    )
    def render() = {
      sql"INSERT INTO ${SqlStr.raw(ctx.config.tableNameMapper(tableName))} ($columns) VALUES $values"
    }
  }
}
