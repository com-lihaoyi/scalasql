package scalasql.query

import scalasql.core.{Column, Context, DialectBase, Queryable, SqlStr, Table, WithExpr}
import scalasql.core.SqlStr.{Renderable, SqlStringSyntax}

trait InsertValues[V[_[_]], R] extends Query[Int] {
  def skipColumns(x: (V[Column] => Column[_])*): InsertValues[V, R]
}
object InsertValues {
  class Impl[V[_[_]], R](
      insert: Insert[V, R],
      values: Seq[R],
      dialect: DialectBase,
      qr: Queryable.Row[V[Column], R],
      skippedColumns: Seq[Column[_]]
  ) extends InsertValues[V, R] {
    protected def queryWalkLabels() = Nil

    protected def queryWalkExprs() = Nil

    override protected def queryIsSingleRow = true

    protected override def queryIsExecuteUpdate = true

    override protected def queryConstruct(args: Queryable.ResultSetIterator): Int =
      args.get(dialect.IntType)

    override protected def renderToSql(ctx: Context): SqlStr = {
      new Renderer(
        Table.tableName(insert.table.value),
        Table.tableLabels(insert.table.value),
        values,
        qr,
        skippedColumns
      )(ctx).render()
    }

    override def skipColumns(x: (V[Column] => Column[_])*) = {

      new Impl(
        insert,
        values,
        dialect,
        qr,
        skippedColumns ++ x.map(_(WithExpr.get(insert)))
      )
    }
  }
  class Renderer[Q, R](
      tableName: String,
      columnsList0: Seq[String],
      valuesList: Seq[R],
      qr: Queryable.Row[Q, R],
      skippedColumns: Seq[Column[_]]
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
      sql"INSERT INTO ${SqlStr.raw(ctx.config.tableNameMapper(tableName))} ($columns) VALUES $values"
    }
  }
}
