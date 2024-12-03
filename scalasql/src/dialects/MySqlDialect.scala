package scalasql.dialects

import scalasql.{Sc, operations}
import scalasql.query.{
  AscDesc,
  Column,
  CompoundSelect,
  GroupBy,
  InsertColumns,
  Join,
  JoinOps,
  Joinable,
  LateralJoinOps,
  Nulls,
  OrderBy,
  Query,
  SubqueryRef,
  Table,
  TableRef,
  Values
}
import scalasql.core.{
  Aggregatable,
  Context,
  Expr,
  DbApi,
  DialectTypeMappers,
  JoinNullable,
  Queryable,
  ExprsToSql,
  SqlStr,
  TypeMapper,
  WithSqlExpr
}
import scalasql.core.SqlStr.{Renderable, SqlStringSyntax, optSeq}
import scalasql.operations.{ConcatOps, MathOps, PadOps}
import scalasql.renderer.JoinsToSql

import java.sql.PreparedStatement
import java.time.{Instant, LocalDateTime}
import java.util.UUID
import scala.reflect.ClassTag
import scalasql.query.Select

trait MySqlDialect extends Dialect {
  protected def dialectCastParams = false

  override implicit def ByteType: TypeMapper[Byte] = new MySqlByteType
  class MySqlByteType extends ByteType { override def castTypeString = "SIGNED" }

  override implicit def ShortType: TypeMapper[Short] = new MySqlShortType
  class MySqlShortType extends ShortType { override def castTypeString = "SIGNED" }

  override implicit def IntType: TypeMapper[Int] = new MySqlIntType
  class MySqlIntType extends IntType { override def castTypeString = "SIGNED" }

  override implicit def LongType: TypeMapper[Long] = new MySqlLongType
  class MySqlLongType extends LongType { override def castTypeString = "SIGNED" }

  override implicit def StringType: TypeMapper[String] = new MySqlStringType
  class MySqlStringType extends StringType { override def castTypeString = "CHAR" }

  override implicit def LocalDateTimeType: TypeMapper[LocalDateTime] = new MySqlLocalDateTimeType
  class MySqlLocalDateTimeType extends LocalDateTimeType {
    override def castTypeString = "DATETIME"
  }

  override implicit def InstantType: TypeMapper[Instant] = new MySqlInstantType
  class MySqlInstantType extends InstantType { override def castTypeString = "DATETIME" }

  override implicit def UtilDateType: TypeMapper[java.util.Date] = new MySqlUtilDateType
  class MySqlUtilDateType extends UtilDateType { override def castTypeString = "DATETIME" }

  override implicit def UuidType: TypeMapper[UUID] = new MySqlUuidType

  class MySqlUuidType extends UuidType {
    override def put(r: PreparedStatement, idx: Int, v: UUID) = {
      r.setObject(idx, v.toString)
    }
  }

  override implicit def EnumType[T <: Enumeration#Value](
      implicit constructor: String => T
  ): TypeMapper[T] = new MySqlEnumType[T]
  class MySqlEnumType[T](implicit constructor: String => T) extends EnumType[T] {
    override def put(r: PreparedStatement, idx: Int, v: T): Unit = r.setString(idx, v.toString)
  }

  override implicit def ExprTypedOpsConv[T: ClassTag](v: Expr[T]): operations.ExprTypedOps[T] =
    new MySqlDialect.ExprTypedOps(v)

  override implicit def ExprStringOpsConv(v: Expr[String]): MySqlDialect.ExprStringOps[String] =
    new MySqlDialect.ExprStringOps(v)

  override implicit def ExprBlobOpsConv(
      v: Expr[geny.Bytes]
  ): MySqlDialect.ExprStringLikeOps[geny.Bytes] =
    new MySqlDialect.ExprStringLikeOps(v)

  override implicit def TableOpsConv[V[_[_]]](t: Table[V]): scalasql.dialects.TableOps[V] =
    new MySqlDialect.TableOps(t)

  implicit def OnConflictableUpdate[V[_[_]], R](
      query: InsertColumns[V, R]
  ): MySqlDialect.OnConflictable[V[Column], Int] =
    new MySqlDialect.OnConflictable[V[Column], Int](query, WithSqlExpr.get(query), query.table)

  override implicit def DbApiQueryOpsConv(db: => DbApi): DbApiQueryOps = new DbApiQueryOps(this) {
    override def values[Q, R](ts: Seq[R])(implicit qr: Queryable.Row[Q, R]): Values[Q, R] =
      new MySqlDialect.Values(ts)
  }

  implicit def LateralJoinOpsConv[C[_, _], Q, R](wrapped: JoinOps[C, Q, R] & Joinable[Q, R])(
      implicit qr: Queryable.Row[Q, R]
  ): LateralJoinOps[C, Q, R] = new LateralJoinOps(wrapped)

  implicit def ExprAggOpsConv[T](v: Aggregatable[Expr[T]]): operations.ExprAggOps[T] =
    new MySqlDialect.ExprAggOps(v)

  implicit class SelectForUpdateConv[Q, R](r: Select[Q, R]) {

    /**
     * SELECT .. FOR UPDATE acquires an exclusive lock, blocking other transactions from
     * modifying or locking the selected rows, which is for managing concurrent transactions
     * and ensuring data consistency in multi-step operations.
     */
    def forUpdate: Select[Q, R] =
      Select.withExprSuffix(r, true, _ => sql" FOR UPDATE")

    /**
     * SELECT ... FOR SHARE: Locks the selected rows for reading, allowing other transactions
     * to read but not modify the locked rows
     */
    def forShare: Select[Q, R] =
      Select.withExprSuffix(r, true, _ => sql" FOR SHARE")

    /**
     * SELECT ... FOR UPDATE NOWAIT: Immediately returns an error if the selected rows are
     * already locked, instead of waiting
     */
    def forUpdateNoWait: Select[Q, R] =
      Select.withExprSuffix(r, true, _ => sql" FOR UPDATE NOWAIT")

    /**
     * SELECT ... FOR UPDATE SKIP LOCKED: Skips any rows that are already locked by other
     * transactions, instead of waiting
     */
    def forUpdateSkipLocked: Select[Q, R] =
      Select.withExprSuffix(r, true, _ => sql" FOR UPDATE SKIP LOCKED")
  }

  override implicit def DbApiOpsConv(db: => DbApi): MySqlDialect.DbApiOps =
    new MySqlDialect.DbApiOps(this)

}

object MySqlDialect extends MySqlDialect {

  class DbApiOps(dialect: DialectTypeMappers)
      extends scalasql.operations.DbApiOps(dialect)
      with ConcatOps
      with MathOps {

    /**
     * Returns a random value in the range 0.0 <= x < 1.0
     */
    def rand: Expr[Double] = Expr { _ => sql"RAND()" }
  }

  class ExprAggOps[T](v: Aggregatable[Expr[T]]) extends scalasql.operations.ExprAggOps[T](v) {
    def mkString(sep: Expr[String] = null)(implicit tm: TypeMapper[T]): Expr[String] = {
      val sepRender = Option(sep).getOrElse(sql"''")
      v.aggregateExpr(expr =>
        implicit ctx => sql"GROUP_CONCAT(CONCAT($expr, '') SEPARATOR ${sepRender})"
      )
    }
  }

  class ExprTypedOps[T: ClassTag](v: Expr[T]) extends operations.ExprTypedOps(v) {

    /** Equals to */
    override def ===[V: ClassTag](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx =>
      (isNullable[T], isNullable[V]) match {
        case (true, true) => sql"($v <=> $x)"
        case _ => sql"($v = $x)"
      }
    }

    /** Not equal to */
    override def !==[V: ClassTag](x: Expr[V]): Expr[Boolean] = Expr { implicit ctx =>
      (isNullable[T], isNullable[V]) match {
        case (true, true) => sql"(NOT ($v <=> $x))"
        case _ => sql"($v <> $x)"
      }
    }

  }

  class ExprStringOps[T](v: Expr[T]) extends ExprStringLikeOps(v) with operations.ExprStringOps[T]
  class ExprStringLikeOps[T](protected val v: Expr[T])
      extends operations.ExprStringLikeOps(v)
      with PadOps {
    override def +(x: Expr[T]): Expr[T] = Expr { implicit ctx => sql"CONCAT($v, $x)" }

    override def startsWith(other: Expr[T]): Expr[Boolean] = Expr { implicit ctx =>
      sql"($v LIKE CONCAT($other, '%'))"
    }

    override def endsWith(other: Expr[T]): Expr[Boolean] = Expr { implicit ctx =>
      sql"($v LIKE CONCAT('%', $other))"
    }

    override def contains(other: Expr[T]): Expr[Boolean] = Expr { implicit ctx =>
      sql"($v LIKE CONCAT('%', $other, '%'))"
    }

    def indexOf(x: Expr[T]): Expr[Int] = Expr { implicit ctx => sql"POSITION($x IN $v)" }
    def reverse: Expr[T] = Expr { implicit ctx => sql"REVERSE($v)" }
  }

  class TableOps[V[_[_]]](t: Table[V]) extends scalasql.dialects.TableOps[V](t) {
    override def update(
        filter: V[Column] => Expr[Boolean]
    ): Update[V[Column], V[Sc]] = {
      val ref = Table.ref(t)
      val metadata = Table.metadata(t)
      new Update(
        metadata.vExpr(ref, dialectSelf),
        ref,
        Nil,
        Nil,
        Seq(filter(metadata.vExpr(ref, dialectSelf)))
      )(
        t.containerQr2
      )
    }

    protected override def joinableToSelect: Select[V[Expr], V[Sc]] = {
      val ref = Table.ref(t)
      new SimpleSelect(
        Table.metadata(t).vExpr(ref, dialectSelf).asInstanceOf[V[Expr]],
        None,
        None,
        false,
        Seq(ref),
        Nil,
        Nil,
        None
      )(
        t.containerQr
      )
    }
  }

  class Update[Q, R](
      expr: Q,
      table: TableRef,
      set0: Seq[Column.Assignment[?]],
      joins: Seq[Join],
      where: Seq[Expr[?]]
  )(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.Update.Impl[Q, R](expr, table, set0, joins, where) {

    protected override def copy[Q, R](
        expr: Q = this.expr,
        table: TableRef = this.table,
        set0: Seq[Column.Assignment[?]] = this.set0,
        joins: Seq[Join] = this.joins,
        where: Seq[Expr[?]] = this.where
    )(implicit qr: Queryable.Row[Q, R], dialect: scalasql.core.DialectTypeMappers): Update[Q, R] =
      new Update(expr, table, set0, joins, where)

    private[scalasql] override def renderSql(ctx: Context) = {
      new UpdateRenderer(this.joins, this.table, this.set0, this.where, ctx).render()
    }

  }

  class UpdateRenderer(
      joins0: Seq[Join],
      table: TableRef,
      set0: Seq[Column.Assignment[?]],
      where0: Seq[Expr[?]],
      prevContext: Context
  ) extends scalasql.query.Update.Renderer(joins0, table, set0, where0, prevContext) {
    override lazy val updateList = set0.map { case assign =>
      val colStr = SqlStr.raw(prevContext.config.columnNameMapper(assign.column.name))
      sql"$tableName.$colStr = ${assign.value}"
    }

    lazy val whereAll = ExprsToSql.booleanExprs(sql" WHERE ", where0)
    override lazy val joinOns = joins0
      .map(_.from.map(_.on.map(t => SqlStr.flatten(Renderable.renderSql(t)))))

    override lazy val joins = optSeq(joins0)(JoinsToSql.joinsToSqlStr(_, renderedFroms, joinOns))
    override def render() = sql"UPDATE $tableName" + joins + sql" SET " + sets + whereAll
  }

  class OnConflictable[Q, R](val query: Query[R], expr: Q, table: TableRef) {

    def onConflictUpdate(c2: Q => Column.Assignment[?]*): OnConflictUpdate[Q, R] =
      new OnConflictUpdate(this, c2.map(_(expr)), table)
  }

  class OnConflictUpdate[Q, R](
      insert: OnConflictable[Q, R],
      updates: Seq[Column.Assignment[?]],
      table: TableRef
  ) extends Query.DelegateQuery[R] {
    protected def query: Query[?] = insert.query
    override def queryIsExecuteUpdate = true

    private[scalasql] def renderSql(ctx: Context) = {
      implicit val implicitCtx = Context.compute(ctx, Nil, Some(table))
      val str = Renderable.renderSql(insert.query)

      val updatesStr = SqlStr.join(
        updates.map { case assign => SqlStr.raw(assign.column.name) + sql" = ${assign.value}" },
        SqlStr.commaSep
      )
      str + sql" ON DUPLICATE KEY UPDATE $updatesStr"
    }

    override protected def queryConstruct(args: Queryable.ResultSetIterator): R = {
      Query.construct(insert.query, args)
    }
  }

  trait Select[Q, R] extends scalasql.query.Select[Q, R] {
    override def newCompoundSelect[Q, R](
        lhs: scalasql.query.SimpleSelect[Q, R],
        compoundOps: Seq[CompoundSelect.Op[Q, R]],
        orderBy: Seq[OrderBy],
        limit: Option[Int],
        offset: Option[Int]
    )(
        implicit qr: Queryable.Row[Q, R],
        dialect: scalasql.core.DialectTypeMappers
    ): scalasql.query.CompoundSelect[Q, R] = {
      new CompoundSelect(lhs, compoundOps, orderBy, limit, offset)
    }

    override def newSimpleSelect[Q, R](
        expr: Q,
        exprPrefix: Option[Context => SqlStr],
        exprSuffix: Option[Context => SqlStr],
        preserveAll: Boolean,
        from: Seq[Context.From],
        joins: Seq[Join],
        where: Seq[Expr[?]],
        groupBy0: Option[GroupBy]
    )(
        implicit qr: Queryable.Row[Q, R],
        dialect: scalasql.core.DialectTypeMappers
    ): scalasql.query.SimpleSelect[Q, R] = {
      new SimpleSelect(expr, exprPrefix, exprSuffix, preserveAll, from, joins, where, groupBy0)
    }
  }

  class SimpleSelect[Q, R](
      expr: Q,
      exprPrefix: Option[Context => SqlStr],
      exprSuffix: Option[Context => SqlStr],
      preserveAll: Boolean,
      from: Seq[Context.From],
      joins: Seq[Join],
      where: Seq[Expr[?]],
      groupBy0: Option[GroupBy]
  )(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.SimpleSelect(
        expr,
        exprPrefix,
        exprSuffix,
        preserveAll,
        from,
        joins,
        where,
        groupBy0
      )
      with Select[Q, R] {
    override def outerJoin[Q2, R2](other: Joinable[Q2, R2])(on: (Q, Q2) => Expr[Boolean])(
        implicit joinQr: Queryable.Row[Q2, R2]
    ): scalasql.query.Select[(JoinNullable[Q], JoinNullable[Q2]), (Option[R], Option[R2])] = {
      leftJoin(other)(on)
        .map { case (l, r) => (JoinNullable(l), r) }
        .union(rightJoin(other)(on).map { case (l, r) =>
          (l, JoinNullable(r))
        })
    }
  }

  class CompoundSelect[Q, R](
      lhs: scalasql.query.SimpleSelect[Q, R],
      compoundOps: Seq[scalasql.query.CompoundSelect.Op[Q, R]],
      orderBy: Seq[OrderBy],
      limit: Option[Int],
      offset: Option[Int]
  )(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.CompoundSelect(lhs, compoundOps, orderBy, limit, offset)
      with Select[Q, R] {
    protected override def selectRenderer(prevContext: Context): SubqueryRef.Wrapped.Renderer =
      new CompoundSelectRenderer(this, prevContext)
  }

  class CompoundSelectRenderer[Q, R](
      query: scalasql.query.CompoundSelect[Q, R],
      prevContext: Context
  ) extends scalasql.query.CompoundSelect.Renderer(query, prevContext) {

    override lazy val limitOpt = SqlStr
      .flatten(CompoundSelectRendererForceLimit.limitToSqlStr(query.limit, query.offset))

    override def orderToSqlStr(newCtx: Context) = {
      SqlStr.optSeq(query.orderBy) { orderBys =>
        val orderStr = SqlStr.join(
          orderBys.map { orderBy =>
            val exprStr = Renderable.renderSql(orderBy.expr)(newCtx)

            (orderBy.ascDesc, orderBy.nulls) match {
              case (Some(AscDesc.Asc), None | Some(Nulls.First)) => sql"$exprStr ASC"
              case (Some(AscDesc.Desc), Some(Nulls.First)) =>
                sql"$exprStr IS NULL DESC, $exprStr DESC"
              case (Some(AscDesc.Asc), Some(Nulls.Last)) => sql"$exprStr IS NULL ASC, $exprStr ASC"
              case (Some(AscDesc.Desc), None | Some(Nulls.Last)) => sql"$exprStr DESC"
              case (None, None) => exprStr
              case (None, Some(Nulls.First)) => sql"$exprStr IS NULL DESC, $exprStr"
              case (None, Some(Nulls.Last)) => sql"$exprStr IS NULL ASC, $exprStr"
            }
          },
          SqlStr.commaSep
        )

        sql" ORDER BY $orderStr"
      }
    }
  }

  class Values[Q, R](ts: Seq[R])(implicit qr: Queryable.Row[Q, R])
      extends scalasql.query.Values[Q, R](ts) {
    override protected def selectRenderer(prevContext: Context): SubqueryRef.Wrapped.Renderer =
      new ValuesRenderer[Q, R](this)(implicitly, prevContext)
    override protected def columnName(n: Int) = s"column_$n"
  }
  class ValuesRenderer[Q, R](v: Values[Q, R])(implicit qr: Queryable.Row[Q, R], ctx: Context)
      extends scalasql.query.Values.Renderer[Q, R](v) {
    override def wrapRow(t: R): SqlStr =
      sql"ROW(" + SqlStr.join(
        qr.walkExprs(qr.deconstruct(t)).map(i => sql"$i"),
        SqlStr.commaSep
      ) + sql")"
  }

}
