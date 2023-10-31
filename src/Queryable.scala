package scalasql

import utils.OptionPickler.Reader
import renderer.{Context, ExprsToSql, SelectToSql, SqlStr}
import scalasql.query.{Expr, Query}
import scalasql.renderer.SqlStr.SqlStringSyntax
import scalasql.utils.OptionPickler

/**
 * Typeclass to indicate that we are able to evaluate a query of type [[Q]] to
 * return a result of type [[R]]. Involves two operations: flattening a structured
 * query to a flat list of expressions via [[walk]], and reading a JSON-ish
 * tree-shaped blob back into a return value via [[valueReader]]
 */
trait Queryable[-Q, R] {
  def isExecuteUpdate(q: Q): Boolean = false
  def walk(q: Q): Seq[(List[String], Expr[_])]
  def valueReader(q: Q): Reader[R]
  def singleRow(q: Q): Boolean = true

  def toSqlQuery(q: Q, ctx: Context): (SqlStr, Seq[MappedType[_]]) = {
    val walked = this.walk(q)
    val res = ExprsToSql(walked, sql"", ctx)
    (
      if (res.isCompleteQuery) res else res + SqlStr.raw(ctx.defaultQueryableSuffix),
      walked.map(t => Expr.getMappedType(t._2))
    )
  }
}

object Queryable {
  implicit def QueryQueryable[R]: Queryable[Query[R], R] = new Query.Queryable[Query[R], R]()

  private class TupleNQueryable[Q, R](
      val walk0: Q => Seq[Seq[(List[String], Expr[_])]],
      val valueReader0: Q => Reader[R]
  ) extends Queryable[Q, R] {
    def walk(q: Q) = {
      walk0(q).zipWithIndex.map { case (v, i) => (i.toString, v) }.flatMap { case (prefix, vs0) =>
        vs0.map { case (k, v) => (prefix +: k, v) }
      }
    }

    override def valueReader(q: Q): OptionPickler.Reader[R] = valueReader0(q)
  }

  import scalasql.utils.OptionPickler._

  implicit def Tuple2Queryable[Q1, Q2, R1, R2](
      implicit q1: Queryable[Q1, R1],
      q2: Queryable[Q2, R2]
  ): Queryable[(Q1, Q2), (R1, R2)] = {
    new Queryable.TupleNQueryable(
      t => Seq(q1.walk(t._1), q2.walk(t._2)),
      t => utils.OptionPickler.Tuple2Reader(q1.valueReader(t._1), q2.valueReader(t._2))
    )
  }

  implicit def Tuple3Queryable[Q1, Q2, Q3, R1, R2, R3](
      implicit q1: Queryable[Q1, R1],
      q2: Queryable[Q2, R2],
      q3: Queryable[Q3, R3]
  ): Queryable[(Q1, Q2, Q3), (R1, R2, R3)] = {
    new Queryable.TupleNQueryable(
      t => Seq(q1.walk(t._1), q2.walk(t._2), q3.walk(t._3)),
      t =>
        utils.OptionPickler
          .Tuple3Reader(q1.valueReader(t._1), q2.valueReader(t._2), q3.valueReader(t._3))
    )
  }

  implicit def Tuple4Queryable[Q1, Q2, Q3, Q4, R1, R2, R3, R4](
      implicit q1: Queryable[Q1, R1],
      q2: Queryable[Q2, R2],
      q3: Queryable[Q3, R3],
      q4: Queryable[Q4, R4]
  ): Queryable[(Q1, Q2, Q3, Q4), (R1, R2, R3, R4)] = {
    new Queryable.TupleNQueryable(
      t => Seq(q1.walk(t._1), q2.walk(t._2), q3.walk(t._3), q4.walk(t._4)),
      t =>
        utils.OptionPickler.Tuple4Reader(
          q1.valueReader(t._1),
          q2.valueReader(t._2),
          q3.valueReader(t._3),
          q4.valueReader(t._4)
        )
    )
  }

  implicit def Tuple5Queryable[Q1, Q2, Q3, Q4, Q5, R1, R2, R3, R4, R5](
      implicit q1: Queryable[Q1, R1],
      q2: Queryable[Q2, R2],
      q3: Queryable[Q3, R3],
      q4: Queryable[Q4, R4],
      q5: Queryable[Q5, R5]
  ): Queryable[(Q1, Q2, Q3, Q4, Q5), (R1, R2, R3, R4, R5)] = {
    new Queryable.TupleNQueryable(
      t => Seq(q1.walk(t._1), q2.walk(t._2), q3.walk(t._3), q4.walk(t._4), q5.walk(t._5)),
      t =>
        utils.OptionPickler.Tuple5Reader(
          q1.valueReader(t._1),
          q2.valueReader(t._2),
          q3.valueReader(t._3),
          q4.valueReader(t._4),
          q5.valueReader(t._5)
        )
    )
  }

  implicit def Tuple6Queryable[Q1, Q2, Q3, Q4, Q5, Q6, R1, R2, R3, R4, R5, R6](
      implicit q1: Queryable[Q1, R1],
      q2: Queryable[Q2, R2],
      q3: Queryable[Q3, R3],
      q4: Queryable[Q4, R4],
      q5: Queryable[Q5, R5],
      q6: Queryable[Q6, R6]
  ): Queryable[(Q1, Q2, Q3, Q4, Q5, Q6), (R1, R2, R3, R4, R5, R6)] = {
    new Queryable.TupleNQueryable(
      t =>
        Seq(
          q1.walk(t._1),
          q2.walk(t._2),
          q3.walk(t._3),
          q4.walk(t._4),
          q5.walk(t._5),
          q6.walk(t._6)
        ),
      t =>
        utils.OptionPickler.Tuple6Reader(
          q1.valueReader(t._1),
          q2.valueReader(t._2),
          q3.valueReader(t._3),
          q4.valueReader(t._4),
          q5.valueReader(t._5),
          q6.valueReader(t._6)
        )
    )
  }
}
