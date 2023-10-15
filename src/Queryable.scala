package usql

import OptionPickler.Reader
import usql.QueryToSql.Context
import usql.SqlStr.{SqlStringSyntax, opt, optSeq}

/**
 * Typeclass to indicate that we are able to evaluate a query of type [[Q]] to
 * return a result of type [[R]]. Involves two operations: flattening a structured
 * query to a flat list of expressions via [[walk]], and reading a JSON-ish
 * tree-shaped blob back into a return value via [[valueReader]]
 */
trait Queryable[Q, R]{
  def walk(q: Q): Seq[(List[String], Expr[_])]
  def valueReader: Reader[R]
  def unpack(t: ujson.Value): ujson.Value = t.arr.head

  def toSqlQuery(q: Q, ctx: QueryToSql.Context): SqlStr = {
    QueryToSql.sqlExprsStr[Q, R](q, this, ctx)._2
  }
}

object Queryable{
  implicit def ExprQueryable[T](implicit valueReader0: Reader[T]): Queryable[Expr[T], T] =
    new ExprQueryable[T]()

  class ExprQueryable[T](implicit valueReader0: Reader[T]) extends Queryable[Expr[T], T]{
    def walk(q: Expr[T]) = Seq(Nil -> q)
    def valueReader = valueReader0

  }

  implicit def QueryQueryable[Q, R](implicit qr: Queryable[Q, R]): Queryable[Query[Q], Seq[R]] =
    new QueryQueryable()(qr)

  class QueryQueryable[Q, R](implicit qr: Queryable[Q, R]) extends Queryable[Query[Q], Seq[R]]{
    def walk(q: Query[Q]) = qr.walk(q.expr)
    def valueReader = OptionPickler.SeqLikeReader(qr.valueReader, Vector.iterableFactory)
    override def unpack(t: ujson.Value) = t

    override def toSqlQuery(q: Query[Q], ctx: QueryToSql.Context): SqlStr = {
      QueryToSql.toSqlQuery0(q, qr, ctx.tableNameMapper, ctx.columnNameMapper)._2
    }
  }

  implicit def UpdateReturningQueryable[Q, R](implicit qr: Queryable[Q, R]): Queryable[UpdateReturning[Q, R], Seq[R]] =
    new UpdateReturningQueryable[Q, R]()(qr)

  class UpdateReturningQueryable[Q, R](implicit qr: Queryable[Q, R]) extends Queryable[UpdateReturning[Q, R], Seq[R]]{
    def walk(ur: UpdateReturning[Q, R]): Seq[(List[String], Expr[_])] = qr.walk(ur.returning)

    override def unpack(t: ujson.Value) = t
    def valueReader: OptionPickler.Reader[Seq[R]] = OptionPickler.SeqLikeReader(qr.valueReader, Vector.iterableFactory)

    override def toSqlQuery(q: UpdateReturning[Q, R], ctx0: QueryToSql.Context): SqlStr = {
      val (namedFromsMap, fromSelectables, exprNaming, context) = QueryToSql.computeContext(
        ctx0.tableNameMapper,
        ctx0.columnNameMapper,
        q.update.joins.flatMap(_.from).map(_.from),
        Some(q.update.table)
      )

      implicit val ctx: Context = context

      val tableName = SqlStr.raw(ctx.tableNameMapper(q.update.table.value.tableName))
      val updateList = q.update.set0.map{case (k, v) =>
        val setLhsCtxt = new QueryToSql.Context(
          ctx.fromNaming + (q.update.table -> ""),
          ctx.exprNaming,
          ctx.tableNameMapper,
          ctx.columnNameMapper
        )

        val kStr = k.toSqlExpr(setLhsCtxt)
        usql"$kStr = $v"
      }
      val sets = SqlStr.join(updateList, usql", ")

      val (from, fromOns) = q.update.joins.headOption match{
        case None => (usql"", Nil)
        case Some(firstJoin) =>
          val (froms, ons) = firstJoin.from.map { jf => (fromSelectables(jf.from)._2, jf.on) }.unzip
          (usql" FROM " + SqlStr.join(froms, usql", "), ons.flatten)
      }

      val where = SqlStr.optSeq(fromOns ++ q.update.where) { where =>
        usql" WHERE " + SqlStr.join(where.map(_.toSqlExpr), usql" AND ")
      }


      val joins = optSeq(q.update.joins.drop(1)) { joins =>
        SqlStr.join(
          joins.map { join =>
            val joinPrefix = SqlStr.opt(join.prefix)(s => usql" ${SqlStr.raw(s)} ")
            val joinSelectables = SqlStr.join(
              join.from.map { jf => fromSelectables(jf.from)._2 + SqlStr.opt(jf.on)(on => usql" ON $on") }
            )

            usql"$joinPrefix JOIN $joinSelectables"
          }
        )
      }

      val (flattenedExpr, exprStr) = QueryToSql.sqlExprsStr0(q.returning, qr, ctx, usql"")

      val returning = usql" RETURNING " + exprStr
      val res = usql"UPDATE $tableName SET " + sets + from + joins + where + returning
      val flattened = SqlStr.flatten(res)
      flattened
    }
  }

  private class TupleNQueryable[Q, R](val walk0: Q => Seq[Seq[(List[String], Expr[_])]])
                                     (implicit val valueReader: Reader[R]) extends Queryable[Q, R] {
    def walk(q: Q) = walkIndexed(walk0(q))

    def walkIndexed(items: Seq[Seq[(List[String], Expr[_])]]) = {
      walkPrefixed(items.zipWithIndex.map { case (v, i) => (i.toString, v) })
    }

    def walkPrefixed(items: Seq[(String, Seq[(List[String], Expr[_])])]) = {
      items.flatMap { case (prefix, vs0) => vs0.map { case (k, v) => (prefix +: k, v) } }
    }
  }

  private implicit def queryableToReader[R](implicit q: Queryable[_, R]): Reader[R] = q.valueReader

  implicit def Tuple2Queryable[
    Q1, Q2, R1, R2
  ](implicit q1: Queryable[Q1, R1],
    q2: Queryable[Q2, R2]): Queryable[(Q1, Q2), (R1, R2)] = {
    val QueryQueryable = ()
    new Queryable.TupleNQueryable(t => Seq(q1.walk(t._1), q2.walk(t._2)))
  }

  implicit def Tuple3Queryable[
    Q1, Q2, Q3, R1, R2, R3
  ](implicit q1: Queryable[Q1, R1],
    q2: Queryable[Q2, R2],
    q3: Queryable[Q3, R3]): Queryable[(Q1, Q2, Q3), (R1, R2, R3)] = {
    val QueryQueryable = ()
    new Queryable.TupleNQueryable(t => Seq(q1.walk(t._1), q2.walk(t._2), q3.walk(t._3)))
  }

  implicit def Tuple4Queryable[
    Q1, Q2, Q3, Q4, R1, R2, R3, R4
  ](implicit q1: Queryable[Q1, R1],
    q2: Queryable[Q2, R2],
    q3: Queryable[Q3, R3],
    q4: Queryable[Q4, R4]): Queryable[(Q1, Q2, Q3, Q4), (R1, R2, R3, R4)] = {
    val QueryQueryable = ()
    new Queryable.TupleNQueryable(
      t => Seq(q1.walk(t._1), q2.walk(t._2), q3.walk(t._3), q4.walk(t._4))
    )
  }

  implicit def Tuple5Queryable[
    Q1, Q2, Q3, Q4, Q5, R1, R2, R3, R4, R5
  ](implicit q1: Queryable[Q1, R1],
    q2: Queryable[Q2, R2],
    q3: Queryable[Q3, R3],
    q4: Queryable[Q4, R4],
    q5: Queryable[Q5, R5]): Queryable[(Q1, Q2, Q3, Q4, Q5), (R1, R2, R3, R4, R5)] = {
    val QueryQueryable = ()
    new Queryable.TupleNQueryable(
      t => Seq(q1.walk(t._1), q2.walk(t._2), q3.walk(t._3), q4.walk(t._4), q5.walk(t._5))
    )
  }

  implicit def Tuple6Queryable[
    Q1, Q2, Q3, Q4, Q5, Q6, R1, R2, R3, R4, R5, R6
  ](implicit q1: Queryable[Q1, R1],
    q2: Queryable[Q2, R2],
    q3: Queryable[Q3, R3],
    q4: Queryable[Q4, R4],
    q5: Queryable[Q5, R5],
    q6: Queryable[Q6, R6]): Queryable[(Q1, Q2, Q3, Q4, Q5, Q6), (R1, R2, R3, R4, R5, R6)] = {
    val QueryQueryable = ()
    new Queryable.TupleNQueryable(
      t => Seq(q1.walk(t._1), q2.walk(t._2), q3.walk(t._3), q4.walk(t._4), q5.walk(t._5), q6.walk(t._6))
    )
  }
}
