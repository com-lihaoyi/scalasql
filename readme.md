# uSql

uSql is a small SQL library that allows type-safe low-boilerplate querying of 
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
   uSql in reactive/io-monad environments can easily wrap it as necessary.

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
   

# How to get there:

1. **A thin-ish wrapper around JDBC's built-in `java.sql.*` APIs**: we can allow extensibility
   for others to implement support for more fancy execution formats, but fundamentally JDBC
   works OK enough that it's a reasonable place to start. This is similar to how
   [Requests-Scala](https://github.com/com-lihaoyi/requests-scala) is a thin wrapper around
   `java.net.*`.

2. **Leverage [uPickle](https://github.com/com-lihaoyi/upickle)**: for both flattening 
   out of a structured query into "flat" SQL expressions, as well as for re-constructing
   a structured return value from a SQL row. uPickle is very good at converting between 
   tree-shaped structured Scala data structures and "flat" formats, and we should be
   able to use it here to do our conversions.

3. **Higher-kinded Database Row Objects**: by making the fields of the case class `Foo` 
   typed `T[V]` for arbitrary `T`s, we can use the same `case class` to model both the
   query expression `Foo[Expr]` as well as the output data `Foo[Value]`.

# Design

```
  {Expr[Int],Query[Q]               {Int,Seq[R],
   CaseCls[Expr],Tuple}              CaseCls[Val],Tuple}
           |                                ^
           |                                |
         Q +-----------+     +--------------+ R
                       |     |
                       v     |
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