package scalasql.query

import scalasql.core.{Context, DialectTypeMappers, Expr, Queryable, SqlStr, WithSqlExpr}
import scalasql.core.SqlStr.SqlStringSyntax

trait InsertValues[V[_[_]], R]
    extends Returning.InsertBase[V[Column]]
    with Query.ExecuteUpdate[Int] {
  def skipColumns(x: (V[Column] => Column[?])*): InsertValues[V, R]
}
object InsertValues {
  class Impl[V[_[_]], R](
      insert: Insert[V, R],
      values: Seq[R],
      dialect: DialectTypeMappers,
      qr: Queryable.Row[V[Column], R],
      skippedColumns: Seq[Column[?]]
  ) extends InsertValues[V, R] {

    def table = insert.table
    protected def expr: V[Column] = WithSqlExpr.get(insert)
    override protected def queryConstruct(args: Queryable.ResultSetIterator): Int =
      args.get(dialect.IntType)

    override private[scalasql] def renderSql(ctx: Context): SqlStr = {
      new Renderer(
        Table.resolve(insert.table.value)(ctx),
        Table.labels(insert.table.value),
        values,
        qr,
        skippedColumns
      )(ctx).render()
    }

    override def skipColumns(x: (V[Column] => Column[?])*): InsertValues[V, R] = {

      new Impl(
        insert,
        values,
        dialect,
        qr,
        skippedColumns ++ x.map(_(WithSqlExpr.get(insert)))
      )
    }
  }
  class Renderer[Q, R](
      tableName: String,
      columnsList0: Seq[String],
      valuesList: Seq[R],
      qr: Queryable.Row[Q, R],
      skippedColumns: Seq[Column[?]]
  )(implicit ctx: Context) {

    lazy val skippedColumnsNames = skippedColumns.map(_.name).toSet

    lazy val (liveCols, liveIndices) = columnsList0.zipWithIndex.filter { case (c, i) =>
      !skippedColumnsNames.contains(c)
    }.unzip

    lazy val columns = SqlStr.join(
      liveCols.map(s => SqlStr.raw(ctx.config.columnNameMapper(s))),
      SqlStr.commaSep
    )

    lazy val liveIndicesSet = liveIndices.toSet

    val valuesSqls = valuesList.map { v =>
      val commaSeparated = SqlStr.join(
        qr.walkExprs(qr.deconstruct(v))
          .zipWithIndex
          .collect { case (s, i) if liveIndicesSet.contains(i) => sql"$s" },
        SqlStr.commaSep
      )
      sql"(" + commaSeparated + sql")"
    }

    lazy val values = SqlStr.join(valuesSqls, SqlStr.commaSep)

    def render() = {
      sql"INSERT INTO ${SqlStr.raw(tableName)} ($columns) VALUES $values"
    }
  }
}
