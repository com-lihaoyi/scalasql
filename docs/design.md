# Design

The rough dataflow of how ScalaSql works is given by the following diagram:

```
   {Table.select,update,map,
    filter,join,aggregate}                         +-------->
           |                                       |
           |                                       |
{Expr[Int],Select[Q],Update[Q]                {Int,Seq[R],
   CaseCls[Expr],Tuple[Q]}                 CaseCls[Id],Tuple[R]}
           |                                       |
           |                                       |
           +-------------+           +-------------+
                         |           |
                         v           |
           +------ DatabaseApi#run(q: Q): R <------+
           |                                       |
         Q |                                       | R
           |                                       |
           v                                       |
 Queryable#{walk,toSqlStr}           Queryable#valueReader
           |    |                            ^     ^
           |    |                            |     |
    SqlStr |    +------Seq[TypeMapper]-------+     | ResultSet
           |                                       |
           |                                       |
           +---------> java.sql.execute -----------+
```

1. We start off constructing a query of type `Q`: an expression, query, or
   case-class/tuple containing expressions.

2. These get converted into a `SqlStr` and `Seq[TypeMapper]` using the
   `Queryable[Q, R]` typeclass

3. We execute the `SqlStr` using JDBC/`java.sql` APIs and get back a `ResultSet`

4. We use `Queryable[Q, R]#valueReader` and `Seq[TypeMapper]` to convert the
   `ResultSet` back into a Scala type `R`: typically primitive types, collections,
   or case-classes/tuples containing primitive types

5. That Scala value of type `R` is returned to the application for use

## Queryable Hierarchy

The entire ScalaSql codebase is built around the `Queryable[Q, R]` typeclass:

- `Q` -> `R` given `Queryable[Q, R]`
   - `Query[Q_r]` -> `R_r`
   - `Q_r` -> `R_r` given `Queryable.Row[Q_r, R_r]`
      - `TupleN[Q_r1, Q_r2, ... Q_rn]` -> `TupleN[R_r1, R_r2, ... R_rn]`
      - `CaseClass[Expr]` -> `CaseClass[Id]`
      - `Expr[T]` -> `T`

We need to use a `Queryable` typeclass here for two reasons:

1. We do not control some of the types that can be queryable: `TupleN` and `CaseClass`
   are "vanilla" types which happen to have members which a queryable, and cannot be
   made to inherit from any ScalaSql base class the way `Query` and `Expr` can.

2. We need to control the return type: the mapping from `Q` to `R` is somewhat
   arbitrary, with `Query[Q_r] -> R_r` and `Expr[T] -> T` being straightforward unwrapping
   of the type parameter but the `CaseClass[Expr] -> CaseClass[Id]` and `TupleN` not
   following that pattern. Thus we need to use the typeclass to encode this logic

Unlike `Queryable[Q, R]`,`Query[Q_r]` and `Expr[T]` are open class hierarchies. This is
done intentionally to allow users to easily `override` portions of this logic at runtime:
e.g. MySql has a different syntax for `UPDATE`/`JOIN` commands than most other databases,
and it can easily subclass `scalasql.query.Update` and override `toSqlQuery`. In general,
this allows the various SQL dialects to modify or extend ScalaSql's core classes
independently of each other, and lets users further customize ScalaSql in their own
downstream code.

## Operations

`Expr[T]` is a type with no built-in members, as its operations depend on entirely what
`T` is: `Expr[Int]` allows arithmetic, `Expr[String]` allows string operations, and so on.
All of these `Expr[T]` operations are thus provided as extension methods that are
imported with the SQL dialect and apply depending on the type `T`.

These extension methods are provided via implicit conversions to classes implementing
these methods, and can also be overriden or extended by the various SQL dialects: e.g.
MySQL uses `CONCAT` rather than `||` for string concatenation, Postgres provides
`.reverse`/`REVERSE` while MySQL doesn't. Again, this allows each dialect to customize
the APIs they expose independently of each other, and makes it easy for users to define
their own operations not provided by the base library.

In general, we try to use extension methods on `Expr[T]` as much as possible, rather
than static methods that receive `Expr[T]` as parameters. This has two goals:

1. To emulate normal Scala primitive APIs: strings have `.substring`, `.toUpperCase`,
   `.trim` operations, rather than static methods e.g. `String.toUpperCase(s: String)`.
   ScalaSql aims to make user code "look like" normal Scala, and thus it follows
   that style

2. To avoid namespace collisions: extension methods via `implicit class`es tend 
   to cause fewer namespace collisions than imported static methods, as only the
   name of the `implicit class` must be imported for all extension methods to 
   be available

Similarly, operations on `Select`, `Update`, etc. are mostly methods that aim to follow
the Scala collections style.

However, there are some operations that do not map nicely to extension methods:
`caseWhen`, `values`, etc. These are left as static methods that are brought into
scope when you call `import scalasql.dialects.MyDialect._`.

## Internal APIs

ScalaSql separates "internal" APIs used for implementation details from "external"
APIs meant to be user facing by marking the internal APIs as `protected`. However,
some "internal" APIs are also used by advanced users who wish to extend Mill, and thus 
cannot be restricted to the `scalasql` package: for these we provide same-named getter
methods on the class's companion object, e.g. `Select.selectSimpleFrom`
`Select.selectRenderer`, or `Select.selectLhsMap`. These allow the API to be exposed
while still keep the external-facing method namespace clean, and preserving the
separation between external APIs meant for common use from internal APIs meant for
implementation or advanced users.

## Goals

1. **A database library suitable for use for "getting started"**: prioritizing ease
   of use and simplicity over performance, flexibility, or purity.

2. **Vanilla-Scala-Like Syntax and Semantics**: ScalaSql aims to look like normal
   operations on Scala collections, Scala tuples, Scala `case class`es. Naturally
   there will be some differences, some incidental and some fundamental. But the 
   hope is that any Scala programmer who picks up ScalaSql will be able to begin
   using it effectively leveraging all they already know about programming in Scala,
   without having to learn an entirely new DSL

3. **Typed, structured queries**: your queries should be able to return primitives,
   `case class`es, tuples, collections, etc.. A large portion of JDBC boilerplate
   is translating logic "Scala collections" operations into SQL, and translating
   the "flat" key-value SQL result sets back into something more structured. This
   library aims to automate that process

4. **Minimal boilerplate**: both at table definition-site and at query-site. We
   should not need to duplicate case class field definitions 3-5 times in order to
   define our table schema.

5. **Low implementation/maintenance burden**: we don't have a VC funded company behind
   this library, so it should be both implementable and maintainable by <1 person
   in free time

6. **90% coverage of common SQL APIs, user-extensible to handle the rest**: we should
   aim to support the most common use cases out of the box, and for use cases we cannot
   support we should provide a mechanism for the user to extend the library to support
   them. That ensures the library is both easy to get started with and can scale beyond
   toy examples.

7. Simple translation to SQL: ScalaSql translates Scala expressions to SQL queries
   in a straightforward manner, without complex query optimizations or transformations.
   This makes it easy to predict what query your ScalaSql code will generate, or match
   up queries in your database logs with callsites on your Scala code.

## Non-Goals

1. **Novel Programming Paradigms**: [SLICK](https://github.com/slick/slick) went all-in
   on Reactive/Async programming, while large portions of the Scala community are all in
   on various flavors of pure functional programming (IO Monads, Finally Tagless, etc.).
   ScalaSql doesn't know about any of that: it performs the bare minimum translation of
   Scala code to blocking JDBC executions and JDBC result sets back to Scala values.
   Anyone who wants something different can wrap as necessary.

2. **Compile-time query generation**: like [ZIO-Quill](https://github.com/zio/zio-quill).
   Not because I don't want it: high runtime performance + compile-time logging of
   queries is great!. But because compile-time query generation adds enough complexity 
   that I wouldn't able to implement it in a reasonable timeframe and won't be able to
   maintain it effectively on a shoe-string time budget going forward.

3. **Novel Database Designs**: Databases often have problems around un-predictable 
   query plans, limited options for defining indices, confusing performance characteristics,
   and many other things. ScalaSql aims to fix none of these problems: it just aims to 
   provide a nicer way to define SQL queries in Scala code, and return the results
   as Scala values. Someone else can improve the databases themselves.

4. **Full Coverage of SQL APIs**: the full set of SQL APIs is huge, different for
   each database, and ever-changing. We thus do not aim to have complete coverage,
   aiming instead for common use cases and leaving more esoteric things to the user
   to either extend the library to support or drop down to raw SQL.

5. **ORM/ActiveRecord-esque Features**: Most Scala code is immutable by default, and works
   by transforming immutable Scala collections of immutable values through pure functions. 
   ScalaSql aims to follow that style, rather than trying to emulate mutable objects. This
   should fit into the prevalent style of the enclosing Scala application, and avoid the
   difficult edge cases that emerge when trying to emulate local mutable state via database 
   queries 

## Comparisons


|                                    | ScalaSql | Quill     | SLICK      | Squeryl | ScalikeJDBC | Doobie |
|------------------------------------|----------|-----------|------------|---------|-------------|--------|
| Async/Monadic                      | No       | Yes (ZIO) | Yes (DBIO) | No      | No          | Yes    |
| Compile-Time Query Generation      | No       | Yes       | No         | No      | No          | No     |
| Scala-collection-like Query DSL    | Yes      | Yes       | Yes        | No      | No          | No     |
| Query Optimizer                    | No       | Yes       | Yes        | No      | No          | No     |
| ORM/ActiveRecord-esque Features    | No       | No        | No         | Yes     | Yes         | No     |
| Lightweight Model Class Definition | Yes      | Yes       | No         | No      | No          | Yes    |

ScalaSql aims to be a stripped-down version of the more sophisticated database libraries
in the Scala ecosystem, focused purely on converting scala "query" data structures to SQL
and SQL ResultSets back to Scala data types. It does not provide more sophisticated
asynchronous, monadic, or compile-time operations, nor does it provide the "ORM/ActiveRecord"
style of emulating mutable objects via database operations.

### Quill

Quill focuses a lot on compile-time query generation, while ScalaSql does not.
Compile-time query generation has a ton of advantages - zero runtime overhead, compile
time query logging, etc. - but also comes with a lot of complexity, both in
user-facing complexity and internal maintainability:

1. From a user perspective, Quill naturally does not support the entire
   Scala language as part of its queries, and my experience using it is that the boundary
   between what's "supported" vs "not" is often not clear. Exactly how the translation
   to SQL happens is also relatively opaque. In contrast, ScalaSql has database operations
   clearly separated from Scala operations via the `Expr[T]` vs `T` types, and the translation
   to SQL is handled via normal classes with `.toSqlStr` and `.toSqlQuery` methods that are
   composed and invoked at runtime.

2. From a maintainers perspective, Quill's compile-time approach means it needs two
   completely distinct implementations for Scala 2 and Scala 3, and is largely implemented
   via relatively advanced compile-time metaprogramming techniques. In contrast, ScalaSql
   shares the vast majority of its logic between Scala 2 and Scala 3, and is implemented
   mostly via vanilla classes and methods that should be familiar even to those without
   deep expertise in the Scala language

### SLICK

SlICK invests in two major areas that ScalaSql does not: the DBIO Monad for managing
transactions, and query optimization. These areAS result in a lot of user-facing and
internal complexity for the library. While it is possible to paper over some of this
complexity with libraries such as [Blocking Slick](https://github.com/gitbucket/blocking-slick),
the essential complexity remains to cause confusion for users and burden for maintainers.
For example, the book _Essential Slick_ contains a whole
[chapter on manipulating `DBIO[T]` values](https://books.underscore.io/essential-slick/essential-slick-3.html#combining),
which distracts from the essential task of _querying the database_.

ScalaSql aims to do without these two things:

1. We assume that most applications are not ultra-high-concurrency, and blocking database
   operations are fine. If slow languages with blocking operations are enough for
   ultra-large-scale systems like Dropbox (Python), Github (Ruby), or Instagram (Youtube),
   they should be enough for the vast majority of systems a developer may want to build.

2. We assume that most queries are relatively simple, most database query optimizers do
   a reasonable job, and the increased _predictability_ and _simplicity_ of not doing
   advanced query optimizations in the application layer more than make up from the
   increased performance from the optimized database queries. For the rare scenarios where
   this is not true, the developer can fall back to raw SQL queries.

### Squeryl

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
well, making it unfamiliar to people with either Scala or raw SQL expertise.

While ScalaSql still has some syntax edge cases - e.g. using triple `===` instead of double
`==` - overall it should give us a much more familiar experience to Scala programmers than
libraries like Squeryl (above) or ScalikeJDBC (below).

### ScalikeJDBC

Like Squeryl, ScalikeJDBC has a SQL-like DSL, while ScalaSql aims to have a
Scala-collections-like DSL:

**ScalaSql**
```scala
val programmers = db.run(
   Programmer.select
     .join(Company)(_.companyId == _.id)
     .filter{case (p, c) => !p.isDeleted}
     .sortBy{case (p, c) => p.createdAt}
     .take(10)
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

### Doobie

Doobie aims to let you write raw SQL strings, with some niceties around parameter
interpolation and parsing SQL result sets into Scala data structures, but without
any kind of strongly-typed data model for the queries you write. ScalaSql also lets
you write raw `sql"..."` strings, but the primary interface is meant to be the
strongly-typed collection-like API.
