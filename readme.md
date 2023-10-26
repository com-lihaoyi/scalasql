# ScalaSql

ScalaSql is a small SQL library that allows type-safe low-boilerplate querying of
SQL databases, using "standard" Scala collections operations running against
typed `Table` descriptions.

# Goals

1. **A database library suitable for use for "getting started"**: prioritizing ease
   of use and simplicity over performance, flexibility, or purity.

2. **Typed, structured queries**: your queries should be able to return primitives,
   `case class`es, tuples, collections, etc.. A large portion of JDBC boilerplate
   is translating logic "Scala collections" operations into SQL, and translating
   the "flat" key-value SQL result sets back into something more structured. This
   library aims to automate that process

3. **Minimal boilerplate**: both at table definition-site and at query-site. We
   should not need to duplicate case class field definitions 3-5 times in order to
   define our table schema.

4. **Simple types, as far as possible**

5. **Low implementation/maintenance burden**: we don't have a VC funded company behind
   this library, so it should be both implementable and maintainable by ~1 person
   in free time

6. **90% coverage of common SQL APIs, user-extensible to handle the rest**: we should
   aim to support the most common use cases out of the box, and for use cases we cannot
   support we should provide a mechanism for the user to extend the library to support
   them. That ensures the library is both easy to get started with and can scale beyond
   toy examples.

# Non-Goals

1. **Reactive support like or deep IO-Monad integration**, like
   [SLICK](https://github.com/slick/slick) or
   [ZIO-Quill](https://github.com/zio/zio-quill). The fundamental
   database operations using JDBC are neither async nor pure. Anyone who wants to use
   ScalaSql in reactive/io-monad environments can easily wrap it as necessary.

2. **Compile-time query execution**: like [ZIO-Quill](https://github.com/zio/zio-quill).
   Not because I don't want it (high runtime performance + compile-time logging of
   queries is great!) but because it adds enough complexity I don't think I'll be
   able to implement it in a reasonable timeframe

3. **Database query planning and index usage**: Databases often have problems
   around their query plans being un-predictable or non-deterministic, such that
   even with the appropriate indices the query planner may decide not to use them.
   This is not something we can fix at the client layer

4. **Full Coverage of SQL APIs**: the full set of SQL APIs is huge, different for
   each database, and ever-changing. We thus do not aim to have complete coverage,
   aiming instead for common use cases and leaving more esoteric things to the user
   to extend the library to support.


# Comparisons


|                                 | ScalaSql | Quill     | SLICK      | Squeryl | ScalikeJDBC | Doobie  |
|---------------------------------|----------|-----------|------------|---------|-------------|---------|
| Async/Monadic                   | No       | Yes (ZIO) | Yes (DBIO) | No      | No          | Yes     |
| Compile-Time Query Generation   | No       | Yes       | No         | No      | No          | No      |
| Scala-collection-like Query DSL | Yes      | Yes       | Yes        | No      | No          | No      |
| Query Optimizer                 | No       | Yes       | Yes        | No      | No          | No      |
| ORM/ActiveRecord-esque Features | No       | No        | No         | Yes     | Yes         | No      |


## Quill

Quill focuses a lot on compile-time query generation, while ScalaSql does not.
Compile-time query generation has a ton of advantages - zero runtime overhead, compile
time query logging, etc. - but also comes with a lot of complexity, both in
maintainability and in the user-facing API. ScalaSql aims for a lower bar: convenient
classes and methods that generate SQL queries at runtime.

## SLICK

SlICK invests in two major areas that ScalaSql does not: the DBIO Monad for managing
transactions, and query optimization. These areas result in a lot of user-facing and
internal complexity for the library. ScalaSql aims to do without them.

## Squeryl

ScalaSql uses a different DSL design from Squeryl, focusing on a Scala-collection-like
API rather than a SQL-like API:

**ScalaSql**
```scala
def songs = MusicDb.songs.filter(_.artistId === id)

val studentsWithAnAddress = students.filter(s => addresses.filter(a => s.addressId === a.id).nonEmpty)
```
**Squeryl**
```scala
def songs = from(MusicDb.songs)(s => where(s.artistId === id) select(s))

val studentsWithAnAddress = from(students)(s =>
  where(exists(from(addresses)((a) => where(s.addressId === a.id) select(a.id))))
  select(s)
)
```

ScalaSql aims to mimic both the syntax and semantics of Scala collections, which should
hopefully result in a library much more familiar to Scala programmers. Squeryl's DSL on
the other hand is unlike Scala collections, but as an embedded DSL is unlike raw SQL as
well, making it unfamiliar to people with either Scala or raw SQL expertise

## ScalikeJDBC

Like Squeryl, ScalikeJDBC has a SQL-like DSL, while ScalaSql aims to have a
Scala-collections-like DSL:

**ScalaSql**
```scala
val programmers = db.run(
   Programmer.select
     .join(Company)(_.companyId == _.id)
     .filter{case (p, c) => !p.isDeleted}
     .sortBy{case (p, c) => p.createdAt}
     .drop(take(10))
)
```

**ScalikeJDBC**
```scala
val programmers = DB.readOnly { implicit session =>
  withSQL {
    select
      .from(Programmer as p)
      .leftJoin(Company as c).on(p.companyId, c.id)
      .where.eq(p.isDeleted, false)
      .orderBy(p.createdAt)
      .limit(10)
      .offset(0)
  }.map(Programmer(p, c)).list.apply()
}
```

## Doobie

Doobie aims to let you write raw SQL strings, with some niceties around parameter
interpolation and parsing SQL result sets into Scala data structures, but without
any kind of strongly-typed data model for the queries you write. ScalaSql also lets
you write raw `sql"..."` strings, but the primary interface is meant to be the
strongly-typed collection-like API.

# Design

```
   {Table.select,update,map,
    filter,join,aggregate}
           |                               ^
           |                               |
  {Expr[Int],Select[Q],Update[Q]      {Int,Seq[R],
   CaseCls[Expr],Tuple[Q]}         CaseCls[Id],Tuple[R]}
           |                               |
           |                               |
           +-----------+       +-----------+
                       |       |
                       v       |
           +-- DatabaseApi#run(q: Q): R <--+
           |                               |
         Q |                               | R
           |                               |
           v                               |
 Queryable#{walk,toSqlQuery}       Queryable#valueReader
           |                               ^
           |                               |
    SqlStr |                               | ResultSet
           |                               |
           |                               |
           +-------> java.sql.execute -----+
```