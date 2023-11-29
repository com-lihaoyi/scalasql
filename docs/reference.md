[//]: # (GENERATED SOURCES, DO NOT EDIT DIRECTLY)
# ScalaSql Reference Library

This page contains example queries for the ScalaSql, taken from the
ScalaSql test suite. You can use this as a reference to see what kinds
of operations ScalaSql supports and how these operations are translated
into raw SQL to be sent to the database for execution.

If browsing this on Github, you can open the `Outline` pane on the right
to quickly browse through the headers, or use `Cmd-F` to search for specific
SQL keywords to see how to generate them from Scala (e.g. try searching for
`LEFT JOIN` or `WHEN`)

Note that ScalaSql may generate different SQL in certain cases for different
databases, due to differences in how each database parses SQL. These differences
are typically minor, and as long as you use the right `Dialect` for your database
ScalaSql should do the right thing for you.

## DbApi
Basic usage of `db.*` operations such as `db.run`
### DbApi.run

Most common usage of `dbClient.transaction`/`db.run`
to run a simple query within a transaction

```scala
dbClient.transaction { db =>
  db.run(Buyer.select) ==> List(
    Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
    Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
    Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
  )
}
```






### DbApi.runSql

`db.runSql` can be used to run `sql"..."` strings, while providing a
specified type that the query results will be deserialized as the specified
type. `db.runSql` supports the all the same data types as `db.run`:
primitives, date and time types, tuples, `Foo[Id]` `case class`s, and
any combination of these.

The `sql"..."` string interpolator automatically converts interpolated values
into prepared statement variables, avoidin SQL injection vulnerabilities. You
can also interpolate other `sql"..."` strings, or finally use `SqlStr.raw` for
the rare cases where you want to interpolate a trusted `java.lang.String` into
the `sql"..."` query without escaping.

```scala
dbClient.transaction { db =>
  val filterId = 2
  val output = db.runSql[String](
    sql"SELECT name FROM buyer WHERE id = $filterId"
  )(ExprQueryable)
  assert(output == Seq("叉烧包"))

  val output2 = db.runSql[(String, LocalDate)](
    sql"SELECT name, date_of_birth FROM buyer WHERE id = $filterId"
  )
  assert(
    output2 ==
      Seq(("叉烧包", LocalDate.parse("1923-11-12")))
  )

  val output3 = db.runSql[(String, LocalDate, Buyer[Id])](
    sql"SELECT name, date_of_birth, * FROM buyer WHERE id = $filterId"
  )
  assert(
    output3 ==
      Seq(
        (
          "叉烧包",
          LocalDate.parse("1923-11-12"),
          Buyer[Id](
            id = 2,
            name = "叉烧包",
            dateOfBirth = LocalDate.parse("1923-11-12")
          )
        )
      )
  )
}
```






### DbApi.updateSql

Similar to `db.runQuery`, `db.runUpdate` allows you to pass in a `SqlStr`, but runs
an update rather than a query and expects to receive a single number back from the
database indicating the number of rows inserted or updated

```scala
dbClient.transaction { db =>
  val newName = "Moo Moo Cow"
  val newDateOfBirth = LocalDate.parse("2000-01-01")
  val count = db
    .updateSql(
      sql"INSERT INTO buyer (name, date_of_birth) VALUES($newName, $newDateOfBirth)"
    )
  assert(count == 1)

  db.run(Buyer.select) ==> List(
    Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
    Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
    Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
    Buyer[Id](4, "Moo Moo Cow", LocalDate.parse("2000-01-01"))
  )
}
```






### DbApi.runRaw

`runRawQuery` is similar to `runQuery` but allows you to pass in the SQL strings
"raw", along with `?` placeholders and interpolated variables passed separately.

```scala
dbClient.transaction { db =>
  val output = db.runRaw[String]("SELECT name FROM buyer WHERE id = ?", Seq(2))
  assert(output == Seq("叉烧包"))
}
```






### DbApi.updateRaw

`runRawUpdate` is similar to `runRawQuery`, but for update queries that
return a single number

```scala
dbClient.transaction { db =>
  val count = db.updateRaw(
    "INSERT INTO buyer (name, date_of_birth) VALUES(?, ?)",
    Seq("Moo Moo Cow", LocalDate.parse("2000-01-01"))
  )
  assert(count == 1)

  db.run(Buyer.select) ==> List(
    Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
    Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
    Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
    Buyer[Id](4, "Moo Moo Cow", LocalDate.parse("2000-01-01"))
  )
}
```






### DbApi.stream

`db.stream` can be run on queries that return `Seq[T]`s, and makes them
return `geny.Generator[T]`s instead. This allows you to deserialize and
process the returned database rows incrementally without buffering the
entire `Seq[T]` in memory. Not that the underlying JDBC driver and the
underlying database may each perform their own buffering depending on
their implementation

```scala
dbClient.transaction { db =>
  val output = collection.mutable.Buffer.empty[String]

  db.stream(Buyer.select).generate { buyer =>
    output.append(buyer.name)
    if (buyer.id >= 2) Generator.End else Generator.Continue
  }

  output ==> List("James Bond", "叉烧包")
}
```






### DbApi.streamSql

`.streamSql` provides a lower level interface to `.stream`, allowing you to pass
in a `SqlStr` of the form `sql"..."`, while allowing you to process the returned rows
in a streaming fashion without.

```scala
dbClient.transaction { db =>
  val excluded = "James Bond"
  val output = db
    .streamSql[Buyer[Id]](sql"SELECT * FROM buyer where name != $excluded")
    .takeWhile(_.id <= 2)
    .map(_.name)
    .toList

  output ==> List("叉烧包")
}
```






### DbApi.streamRaw

`.streamRaw` provides a lowest level interface to `.stream`, allowing you to pass
in a `java.lang.String` and a `Seq[Any]` representing the interpolated prepared
statement variables

```scala
dbClient.transaction { db =>
  val excluded = "James Bond"
  val output = db
    .streamRaw[Buyer[Id]]("SELECT * FROM buyer WHERE buyer.name <> ?", Seq(excluded))
    .takeWhile(_.id <= 2)
    .map(_.name)
    .toList

  output ==> List("叉烧包")
}
```






## Transaction
Usage of transactions, rollbacks, and savepoints
### Transaction.simple.commit

Common workflow to create a transaction and run a `delete` inside of it. The effect
of the `delete` is visible both inside the transaction and outside after the
transaction completes successfully and commits

```scala
dbClient.transaction { implicit db =>
  db.run(Purchase.select.size) ==> 7

  db.run(Purchase.delete(_ => true)) ==> 7

  db.run(Purchase.select.size) ==> 0
}

dbClient.transaction(_.run(Purchase.select.size)) ==> 0
```






### Transaction.simple.isolation

You can use `.updateRaw` to perform `SET TRANSACTION ISOLATION LEVEL` commands,
allowing you to configure the isolation and performance characteristics of
concurrent transactions in your database

```scala
dbClient.transaction { implicit db =>
  db.updateRaw("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE")

  db.run(Purchase.select.size) ==> 7

  db.run(Purchase.delete(_ => true)) ==> 7

  db.run(Purchase.select.size) ==> 0
}

dbClient.transaction(_.run(Purchase.select.size)) ==> 0
```






### Transaction.simple.rollback

Example of explicitly rolling back a transaction using the `db.rollback()` method.
After rollback, the effects of the `delete` query are undone, and subsequent `select`
queries can see the previously-deleted entries both inside and outside the transaction

```scala
dbClient.transaction { implicit db =>
  db.run(Purchase.select.size) ==> 7

  db.run(Purchase.delete(_ => true)) ==> 7

  db.run(Purchase.select.size) ==> 0

  db.rollback()

  db.run(Purchase.select.size) ==> 7
}

dbClient.transaction(_.run(Purchase.select.size)) ==> 7
```






### Transaction.simple.throw

Transactions are also rolled back if they terminate with an uncaught exception

```scala
try {
  dbClient.transaction { implicit db =>
    db.run(Purchase.select.size) ==> 7

    db.run(Purchase.delete(_ => true)) ==> 7

    db.run(Purchase.select.size) ==> 0

    throw new FooException
  }
} catch { case e: FooException => /*donothing*/ }

dbClient.transaction(_.run(Purchase.select.size)) ==> 7
```






### Transaction.savepoint.commit

Savepoints are like "sub" transactions: they let you declare a savepoint
and roll back any changes to the savepoint later. If a savepoint block
completes successfully, the savepoint changes are committed ("released")
and remain visible later in the transaction and outside of it

```scala
dbClient.transaction { implicit db =>
  db.run(Purchase.select.size) ==> 7

  db.run(Purchase.delete(_.id <= 3)) ==> 3
  db.run(Purchase.select.size) ==> 4

  db.savepoint { sp =>
    db.run(Purchase.delete(_ => true)) ==> 4
    db.run(Purchase.select.size) ==> 0
  }

  db.run(Purchase.select.size) ==> 0
}

dbClient.transaction(_.run(Purchase.select.size)) ==> 0
```






### Transaction.savepoint.rollback

Like transactions, savepoints support the `.rollback()` method, to undo any
changes since the start of the savepoint block.

```scala
dbClient.transaction { implicit db =>
  db.run(Purchase.select.size) ==> 7

  db.run(Purchase.delete(_.id <= 3)) ==> 3
  db.run(Purchase.select.size) ==> 4

  db.savepoint { sp =>
    db.run(Purchase.delete(_ => true)) ==> 4
    db.run(Purchase.select.size) ==> 0
    sp.rollback()
    db.run(Purchase.select.size) ==> 4
  }

  db.run(Purchase.select.size) ==> 4
}

dbClient.transaction(_.run(Purchase.select.size)) ==> 4
```






### Transaction.savepoint.throw

Savepoints also roll back their enclosed changes automatically if they
terminate with an uncaught exception

```scala
dbClient.transaction { implicit db =>
  db.run(Purchase.select.size) ==> 7

  db.run(Purchase.delete(_.id <= 3)) ==> 3
  db.run(Purchase.select.size) ==> 4

  try {
    db.savepoint { sp =>
      db.run(Purchase.delete(_ => true)) ==> 4
      db.run(Purchase.select.size) ==> 0
      throw new FooException
    }
  } catch {
    case e: FooException => /*donothing*/
  }

  db.run(Purchase.select.size) ==> 4
}

dbClient.transaction(_.run(Purchase.select.size)) ==> 4
```






### Transaction.doubleSavepoint.commit

Only one transaction can be present at a time, but savepoints can be arbitrarily nested.
Uncaught exceptions or explicit `.rollback()` calls would roll back changes done during
the inner savepoint/transaction blocks, while leaving changes applied during outer
savepoint/transaction blocks in-place

```scala
dbClient.transaction { implicit db =>
  db.run(Purchase.select.size) ==> 7

  db.run(Purchase.delete(_.id <= 2)) ==> 2
  db.run(Purchase.select.size) ==> 5

  db.savepoint { sp1 =>
    db.run(Purchase.delete(_.id <= 4)) ==> 2
    db.run(Purchase.select.size) ==> 3

    db.savepoint { sp2 =>
      db.run(Purchase.delete(_.id <= 6)) ==> 2
      db.run(Purchase.select.size) ==> 1
    }

    db.run(Purchase.select.size) ==> 1
  }

  db.run(Purchase.select.size) ==> 1
}

dbClient.transaction(_.run(Purchase.select.size)) ==> 1
```






## Select
Basic `SELECT` operations: map, filter, join, etc.
### Select.constant

The most simple thing you can query in the database is an `Expr`. These do not need
to be related to any database tables, and translate into raw `SELECT` calls without
`FROM`.

```scala
Expr(1) + Expr(2)
```


*
    ```sql
    SELECT (? + ?) AS res
    ```



*
    ```scala
    3
    ```



### Select.table

You can list the contents of a table via the query `Table.select`. It returns a
`Seq[CaseClass[Id]]` with the entire contents of the table. Note that listing
entire tables can be prohibitively expensive on real-world databases, and you
should generally use `filter`s as shown below

```scala
Buyer.select
```


*
    ```sql
    SELECT
      buyer0.id AS res__id,
      buyer0.name AS res__name,
      buyer0.date_of_birth AS res__date_of_birth
    FROM buyer buyer0
    ```



*
    ```scala
    Seq(
      Buyer[Id](id = 1, name = "James Bond", dateOfBirth = LocalDate.parse("2001-02-03")),
      Buyer[Id](id = 2, name = "叉烧包", dateOfBirth = LocalDate.parse("1923-11-12")),
      Buyer[Id](id = 3, name = "Li Haoyi", dateOfBirth = LocalDate.parse("1965-08-09"))
    )
    ```



### Select.filter.single

ScalaSql's `.filter` translates to SQL `WHERE`, in this case we
are searching for rows with a particular `buyerId`

```scala
ShippingInfo.select.filter(_.buyerId `=` 2)
```


*
    ```sql
    SELECT
      shipping_info0.id AS res__id,
      shipping_info0.buyer_id AS res__buyer_id,
      shipping_info0.shipping_date AS res__shipping_date
    FROM shipping_info shipping_info0
    WHERE (shipping_info0.buyer_id = ?)
    ```



*
    ```scala
    Seq(
      ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03")),
      ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06"))
    )
    ```



### Select.filter.multiple

You can stack multiple `.filter`s on a query.

```scala
ShippingInfo.select
  .filter(_.buyerId `=` 2)
  .filter(_.shippingDate `=` LocalDate.parse("2012-05-06"))
```


*
    ```sql
    SELECT
      shipping_info0.id AS res__id,
      shipping_info0.buyer_id AS res__buyer_id,
      shipping_info0.shipping_date AS res__shipping_date
    FROM shipping_info shipping_info0
    WHERE (shipping_info0.buyer_id = ?)
    AND (shipping_info0.shipping_date = ?)
    ```



*
    ```scala
    Seq(ShippingInfo[Id](id = 3, buyerId = 2, shippingDate = LocalDate.parse("2012-05-06")))
    ```



### Select.filter.dotSingle.pass

Queries that you expect to return a single row can be annotated with `.single`.
This changes the return type of the `.select` from `Seq[T]` to just `T`, and throws
an exception if zero or multiple rows were returned

```scala
ShippingInfo.select
  .filter(_.buyerId `=` 2)
  .filter(_.shippingDate `=` LocalDate.parse("2012-05-06"))
  .single
```


*
    ```sql
    SELECT
      shipping_info0.id AS res__id,
      shipping_info0.buyer_id AS res__buyer_id,
      shipping_info0.shipping_date AS res__shipping_date
    FROM shipping_info shipping_info0
    WHERE (shipping_info0.buyer_id = ?)
    AND (shipping_info0.shipping_date = ?)
    ```



*
    ```scala
    ShippingInfo[Id](id = 3, buyerId = 2, shippingDate = LocalDate.parse("2012-05-06"))
    ```



### Select.filter.combined

You can perform multiple checks in a single filter using `&&`

```scala
ShippingInfo.select
  .filter(p => p.buyerId `=` 2 && p.shippingDate `=` LocalDate.parse("2012-05-06"))
```


*
    ```sql
    SELECT
      shipping_info0.id AS res__id,
      shipping_info0.buyer_id AS res__buyer_id,
      shipping_info0.shipping_date AS res__shipping_date
    FROM shipping_info shipping_info0
    WHERE ((shipping_info0.buyer_id = ?)
    AND (shipping_info0.shipping_date = ?))
    ```



*
    ```scala
    Seq(ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06")))
    ```



### Select.map.single

`.map` allows you to select exactly what you want to return from
a query, rather than returning the entire row. Here, we return
only the `name`s of the `Buyer`s

```scala
Buyer.select.map(_.name)
```


*
    ```sql
    SELECT buyer0.name AS res FROM buyer buyer0
    ```



*
    ```scala
    Seq("James Bond", "叉烧包", "Li Haoyi")
    ```



### Select.map.filterMap

The common use case of `SELECT FROM WHERE` can be achieved via `.select.filter.map` in ScalaSql

```scala
Product.select.filter(_.price < 100).map(_.name)
```


*
    ```sql
    SELECT product0.name AS res FROM product product0 WHERE (product0.price < ?)
    ```



*
    ```scala
    Seq("Face Mask", "Socks", "Cookie")
    ```



### Select.map.tuple2

You can return multiple values from your `.map` by returning a tuple in your query,
which translates into a `Seq[Tuple]` being returned when the query is run

```scala
Buyer.select.map(c => (c.name, c.id))
```


*
    ```sql
    SELECT buyer0.name AS res__0, buyer0.id AS res__1 FROM buyer buyer0
    ```



*
    ```scala
    Seq(("James Bond", 1), ("叉烧包", 2), ("Li Haoyi", 3))
    ```



### Select.map.tuple3



```scala
Buyer.select.map(c => (c.name, c.id, c.dateOfBirth))
```


*
    ```sql
    SELECT
      buyer0.name AS res__0,
      buyer0.id AS res__1,
      buyer0.date_of_birth AS res__2
    FROM buyer buyer0
    ```



*
    ```scala
    Seq(
      ("James Bond", 1, LocalDate.parse("2001-02-03")),
      ("叉烧包", 2, LocalDate.parse("1923-11-12")),
      ("Li Haoyi", 3, LocalDate.parse("1965-08-09"))
    )
    ```



### Select.map.interpolateInMap

You can perform operations inside the `.map` to change what you return

```scala
Product.select.map(_.price * 2)
```


*
    ```sql
    SELECT (product0.price * ?) AS res FROM product product0
    ```



*
    ```scala
    Seq(17.76, 600, 6.28, 246.9, 2000.0, 0.2)
    ```



### Select.map.heterogenousTuple

`.map` can return any combination of tuples, `case class`es, and primitives,
arbitrarily nested. here we return a tuple of `(Int, Buyer[Id])`

```scala
Buyer.select.map(c => (c.id, c))
```


*
    ```sql
    SELECT
      buyer0.id AS res__0,
      buyer0.id AS res__1__id,
      buyer0.name AS res__1__name,
      buyer0.date_of_birth AS res__1__date_of_birth
    FROM buyer buyer0
    ```



*
    ```scala
    Seq(
      (1, Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03"))),
      (2, Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))),
      (3, Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")))
    )
    ```



### Select.toExpr

`SELECT` queries that return a single row and column can be used as SQL expressions
in standard SQL databases. In ScalaSql, this is done by the `.toExpr` method,
which turns a `Select[T]` into an `Expr[T]`. Note that if the `Select` returns more
than one row or column, the database may select a row arbitrarily or will throw
an exception at runtime (depend on implenmentation)

```scala
Product.select.map(p =>
  (
    p.name,
    Purchase.select
      .filter(_.productId === p.id)
      .sortBy(_.total)
      .desc
      .take(1)
      .map(_.total)
      .toExpr
  )
)
```


*
    ```sql
    SELECT
      product0.name AS res__0,
      (SELECT purchase1.total AS res
        FROM purchase purchase1
        WHERE (purchase1.product_id = product0.id)
        ORDER BY res DESC
        LIMIT ?) AS res__1
    FROM product product0
    ```



*
    ```scala
    Seq(
      ("Face Mask", 888.0),
      ("Guitar", 900.0),
      ("Socks", 15.7),
      ("Skate Board", 493.8),
      ("Camera", 10000.0),
      ("Cookie", 1.3)
    )
    ```



### Select.subquery

ScalaSql generally combines operations like `.map` and `.filter` to minimize the
number of subqueries to keep the generated SQL readable. If you explicitly want
a subquery for some reason (e.g. to influence the database query planner), you can
use the `.subquery` to force a query to be translated into a standalone subquery

```scala
Buyer.select.subquery.map(_.name)
```


*
    ```sql
    SELECT subquery0.res__name AS res
    FROM (SELECT buyer0.name AS res__name FROM buyer buyer0) subquery0
    ```



*
    ```scala
    Seq("James Bond", "叉烧包", "Li Haoyi")
    ```



### Select.aggregate.single

You can use methods like `.sumBy` or `.sum` on your queries to generate
SQL `SUM(...)` aggregates

```scala
Purchase.select.sumBy(_.total)
```


*
    ```sql
    SELECT SUM(purchase0.total) AS res FROM purchase purchase0
    ```



*
    ```scala
    12343.2
    ```



### Select.aggregate.multiple

If you want to perform multiple aggregates at once, you can use the `.aggregate` method
which takes a function allowing you to call multiple aggregates inside of it

```scala
Purchase.select.aggregate(q => (q.sumBy(_.total), q.maxBy(_.total)))
```


*
    ```sql
    SELECT SUM(purchase0.total) AS res__0, MAX(purchase0.total) AS res__1 FROM purchase purchase0
    ```



*
    ```scala
    (12343.2, 10000.0)
    ```



### Select.groupBy.simple

ScalaSql's `.groupBy` method translates into a SQL `GROUP BY`. Unlike the normal
`.groupBy` provided by `scala.Seq`, ScalaSql's `.groupBy` requires you to pass
an aggregate as a second parameter, mirroring the SQL requirement that any
column not part of the `GROUP BY` clause has to be in an aggregate.

```scala
Purchase.select.groupBy(_.productId)(_.sumBy(_.total))
```


*
    ```sql
    SELECT purchase0.product_id AS res__0, SUM(purchase0.total) AS res__1
    FROM purchase purchase0
    GROUP BY purchase0.product_id
    ```



*
    ```scala
    Seq((1, 932.4), (2, 900.0), (3, 15.7), (4, 493.8), (5, 10000.0), (6, 1.30))
    ```



### Select.groupBy.having

`.filter` calls following a `.groupBy` are automatically translated to SQL `HAVING` clauses

```scala
Purchase.select.groupBy(_.productId)(_.sumBy(_.total)).filter(_._2 > 100).filter(_._1 > 1)
```


*
    ```sql
    SELECT purchase0.product_id AS res__0, SUM(purchase0.total) AS res__1
    FROM purchase purchase0
    GROUP BY purchase0.product_id
    HAVING (SUM(purchase0.total) > ?) AND (purchase0.product_id > ?)
    ```



*
    ```scala
    Seq((2, 900.0), (4, 493.8), (5, 10000.0))
    ```



### Select.groupBy.filterHaving



```scala
Purchase.select
  .filter(_.count > 5)
  .groupBy(_.productId)(_.sumBy(_.total))
  .filter(_._2 > 100)
```


*
    ```sql
    SELECT purchase0.product_id AS res__0, SUM(purchase0.total) AS res__1
    FROM purchase purchase0
    WHERE (purchase0.count > ?)
    GROUP BY purchase0.product_id
    HAVING (SUM(purchase0.total) > ?)
    ```



*
    ```scala
    Seq((1, 888.0), (5, 10000.0))
    ```



### Select.distinct.nondistinct

Normal queries can allow duplicates in the returned row values, as seen below.
You can use the `.distinct` operator (translates to SQl's `SELECT DISTINCT`)
to eliminate those duplicates

```scala
Purchase.select.map(_.shippingInfoId)
```


*
    ```sql
    SELECT purchase0.shipping_info_id AS res FROM purchase purchase0
    ```



*
    ```scala
    Seq(1, 1, 1, 2, 2, 3, 3)
    ```



### Select.distinct.distinct



```scala
Purchase.select.map(_.shippingInfoId).distinct
```


*
    ```sql
    SELECT DISTINCT purchase0.shipping_info_id AS res FROM purchase purchase0
    ```



*
    ```scala
    Seq(1, 2, 3)
    ```



### Select.contains

ScalaSql's `.contains` method translates into SQL's `IN` syntax, e.g. here checking if a
subquery contains a column as part of a `WHERE` clause

```scala
Buyer.select.filter(b => ShippingInfo.select.map(_.buyerId).contains(b.id))
```


*
    ```sql
    SELECT buyer0.id AS res__id, buyer0.name AS res__name, buyer0.date_of_birth AS res__date_of_birth
    FROM buyer buyer0
    WHERE (buyer0.id IN (SELECT shipping_info1.buyer_id AS res FROM shipping_info shipping_info1))
    ```



*
    ```scala
    Seq(
      Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
      Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
    )
    ```



### Select.nonEmpty

ScalaSql's `.nonEmpty` and `.isEmpty` translates to SQL's `EXISTS` and `NOT EXISTS` syntax

```scala
Buyer.select
  .map(b => (b.name, ShippingInfo.select.filter(_.buyerId `=` b.id).map(_.id).nonEmpty))
```


*
    ```sql
    SELECT
      buyer0.name AS res__0,
      (EXISTS (SELECT
        shipping_info1.id AS res
        FROM shipping_info shipping_info1
        WHERE (shipping_info1.buyer_id = buyer0.id))) AS res__1
    FROM buyer buyer0
    ```



*
    ```scala
    Seq(("James Bond", true), ("叉烧包", true), ("Li Haoyi", false))
    ```



### Select.isEmpty



```scala
Buyer.select
  .map(b => (b.name, ShippingInfo.select.filter(_.buyerId `=` b.id).map(_.id).isEmpty))
```


*
    ```sql
    SELECT
      buyer0.name AS res__0,
      (NOT EXISTS (SELECT
        shipping_info1.id AS res
        FROM shipping_info shipping_info1
        WHERE (shipping_info1.buyer_id = buyer0.id))) AS res__1
    FROM buyer buyer0
    ```



*
    ```scala
    Seq(("James Bond", false), ("叉烧包", false), ("Li Haoyi", true))
    ```



### Select.nestedTuples

Queries can output arbitrarily nested tuples of `Expr[T]` and `case class`
instances of `Foo[Expr]`, which will be de-serialized into nested tuples
of `T` and `Foo[Id]`s. The `AS` aliases assigned to each column will contain
the path of indices and field names used to populate the final returned values

```scala
Buyer.select
  .join(ShippingInfo)(_.id === _.buyerId)
  .sortBy(_._1.id)
  .map { case (b, s) => (b.id, (b, (s.id, s))) }
```


*
    ```sql
    SELECT
      buyer0.id AS res__0,
      buyer0.id AS res__1__0__id,
      buyer0.name AS res__1__0__name,
      buyer0.date_of_birth AS res__1__0__date_of_birth,
      shipping_info1.id AS res__1__1__0,
      shipping_info1.id AS res__1__1__1__id,
      shipping_info1.buyer_id AS res__1__1__1__buyer_id,
      shipping_info1.shipping_date AS res__1__1__1__shipping_date
    FROM buyer buyer0
    JOIN shipping_info shipping_info1 ON (buyer0.id = shipping_info1.buyer_id)
    ORDER BY res__1__0__id
    ```



*
    ```scala
    Seq[(Int, (Buyer[Id], (Int, ShippingInfo[Id])))](
      (
        1,
        (
          Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
          (2, ShippingInfo[Id](2, 1, LocalDate.parse("2012-04-05")))
        )
      ),
      (
        2,
        (
          Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
          (1, ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03")))
        )
      ),
      (
        2,
        (
          Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
          (3, ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06")))
        )
      )
    )
    ```



### Select.case.when

ScalaSql's `caseWhen` method translates into SQL's `CASE`/`WHEN`/`ELSE`/`END` syntax,
allowing you to perform basic conditionals as part of your SQL query

```scala
Product.select.map(p =>
  db.caseWhen(
    (p.price > 200) -> (p.name + " EXPENSIVE"),
    (p.price > 5) -> (p.name + " NORMAL"),
    (p.price <= 5) -> (p.name + " CHEAP")
  )
)
```


*
    ```sql
    SELECT
      CASE
        WHEN (product0.price > ?) THEN (product0.name || ?)
        WHEN (product0.price > ?) THEN (product0.name || ?)
        WHEN (product0.price <= ?) THEN (product0.name || ?)
      END AS res
    FROM product product0
    ```



*
    ```scala
    Seq(
      "Face Mask NORMAL",
      "Guitar EXPENSIVE",
      "Socks CHEAP",
      "Skate Board NORMAL",
      "Camera EXPENSIVE",
      "Cookie CHEAP"
    )
    ```



### Select.case.else



```scala
Product.select.map(p =>
  db.caseWhen(
    (p.price > 200) -> (p.name + " EXPENSIVE"),
    (p.price > 5) -> (p.name + " NORMAL")
  ).`else` { p.name + " UNKNOWN" }
)
```


*
    ```sql
    SELECT
      CASE
        WHEN (product0.price > ?) THEN (product0.name || ?)
        WHEN (product0.price > ?) THEN (product0.name || ?)
        ELSE (product0.name || ?)
      END AS res
    FROM product product0
    ```



*
    ```scala
    Seq(
      "Face Mask NORMAL",
      "Guitar EXPENSIVE",
      "Socks UNKNOWN",
      "Skate Board NORMAL",
      "Camera EXPENSIVE",
      "Cookie UNKNOWN"
    )
    ```



## Join
inner `JOIN`s, `JOIN ON`s, self-joins, `LEFT`/`RIGHT`/`OUTER` `JOIN`s
### Join.joinFilter

ScalaSql's `.join` or `.join` methods correspond to SQL `JOIN` and `JOIN ... ON ...`.
These perform an inner join between two tables, with an optional `ON` predicate. You can
also `.filter` and `.map` the results of the join, making use of the columns joined from
the two tables

```scala
Buyer.select.join(ShippingInfo)(_.id `=` _.buyerId).filter(_._1.name `=` "叉烧包")
```


*
    ```sql
    SELECT
      buyer0.id AS res__0__id,
      buyer0.name AS res__0__name,
      buyer0.date_of_birth AS res__0__date_of_birth,
      shipping_info1.id AS res__1__id,
      shipping_info1.buyer_id AS res__1__buyer_id,
      shipping_info1.shipping_date AS res__1__shipping_date
    FROM buyer buyer0
    JOIN shipping_info shipping_info1 ON (buyer0.id = shipping_info1.buyer_id)
    WHERE (buyer0.name = ?)
    ```



*
    ```scala
    Seq(
      (
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
        ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03"))
      ),
      (
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
        ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06"))
      )
    )
    ```



### Join.joinFilterMap



```scala
Buyer.select
  .join(ShippingInfo)(_.id `=` _.buyerId)
  .filter(_._1.name `=` "James Bond")
  .map(_._2.shippingDate)
```


*
    ```sql
    SELECT shipping_info1.shipping_date AS res
    FROM buyer buyer0
    JOIN shipping_info shipping_info1 ON (buyer0.id = shipping_info1.buyer_id)
    WHERE (buyer0.name = ?)
    ```



*
    ```scala
    Seq(LocalDate.parse("2012-04-05"))
    ```



### Join.selfJoin

ScalaSql supports a "self join", where a table is joined with itself. This
is done by simply having the same table be on the left-hand-side and right-hand-side
of your `.join` or `.join` method. The two example self-joins below are trivial,
but illustrate how to do it in case you want to do a self-join in a more realistic setting.

```scala
Buyer.select.join(Buyer)(_.id `=` _.id)
```


*
    ```sql
    SELECT
      buyer0.id AS res__0__id,
      buyer0.name AS res__0__name,
      buyer0.date_of_birth AS res__0__date_of_birth,
      buyer1.id AS res__1__id,
      buyer1.name AS res__1__name,
      buyer1.date_of_birth AS res__1__date_of_birth
    FROM buyer buyer0
    JOIN buyer buyer1 ON (buyer0.id = buyer1.id)
    ```



*
    ```scala
    Seq(
      (
        Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
        Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03"))
      ),
      (
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
      ),
      (
        Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
        Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
      )
    )
    ```



### Join.selfJoin2



```scala
Buyer.select.join(Buyer)(_.id <> _.id)
```


*
    ```sql
    SELECT
      buyer0.id AS res__0__id,
      buyer0.name AS res__0__name,
      buyer0.date_of_birth AS res__0__date_of_birth,
      buyer1.id AS res__1__id,
      buyer1.name AS res__1__name,
      buyer1.date_of_birth AS res__1__date_of_birth
    FROM buyer buyer0
    JOIN buyer buyer1 ON (buyer0.id <> buyer1.id)
    ```



*
    ```scala
    Seq(
      (
        Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
      ),
      (
        Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
        Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
      ),
      (
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
        Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03"))
      ),
      (
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
        Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
      ),
      (
        Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
        Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03"))
      ),
      (
        Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
      )
    )
    ```



### Join.mapForGroupBy

Using non-trivial queries in the `for`-comprehension may result in subqueries
being generated

```scala
for ((name, dateOfBirth) <- Buyer.select.groupBy(_.name)(_.minBy(_.dateOfBirth)))
  yield (name, dateOfBirth)
```


*
    ```sql
    SELECT buyer0.name AS res__0, MIN(buyer0.date_of_birth) AS res__1
    FROM buyer buyer0
    GROUP BY buyer0.name
    ```



*
    ```scala
    Seq(
      ("James Bond", LocalDate.parse("2001-02-03")),
      ("Li Haoyi", LocalDate.parse("1965-08-09")),
      ("叉烧包", LocalDate.parse("1923-11-12"))
    )
    ```



### Join.leftJoin

ScalaSql supports `LEFT JOIN`s, `RIGHT JOIN`s and `OUTER JOIN`s via the
`.leftJoin`/`.rightJoin`/`.outerJoin` methods

```scala
Buyer.select.leftJoin(ShippingInfo)(_.id `=` _.buyerId)
```


*
    ```sql
    SELECT
      buyer0.id AS res__0__id,
      buyer0.name AS res__0__name,
      buyer0.date_of_birth AS res__0__date_of_birth,
      shipping_info1.id AS res__1__id,
      shipping_info1.buyer_id AS res__1__buyer_id,
      shipping_info1.shipping_date AS res__1__shipping_date
    FROM buyer buyer0
    LEFT JOIN shipping_info shipping_info1 ON (buyer0.id = shipping_info1.buyer_id)
    ```



*
    ```scala
    Seq(
      (
        Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
        Some(ShippingInfo[Id](2, 1, LocalDate.parse("2012-04-05")))
      ),
      (
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
        Some(ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03")))
      ),
      (
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
        Some(ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06")))
      ),
      (Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")), None)
    )
    ```



### Join.leftJoinMap

`.leftJoin`s return a `JoinNullable[Q]` for the right hand entry. This is similar
to `Option[Q]` in Scala, supports a similar set of operations (e.g. `.map`),
and becomes an `Option[Q]` after the query is executed

```scala
Buyer.select
  .leftJoin(ShippingInfo)(_.id `=` _.buyerId)
  .map { case (b, si) => (b.name, si.map(_.shippingDate)) }
```


*
    ```sql
    SELECT buyer0.name AS res__0, shipping_info1.shipping_date AS res__1
    FROM buyer buyer0
    LEFT JOIN shipping_info shipping_info1 ON (buyer0.id = shipping_info1.buyer_id)
    ```



*
    ```scala
    Seq(
      ("James Bond", Some(LocalDate.parse("2012-04-05"))),
      ("Li Haoyi", None),
      ("叉烧包", Some(LocalDate.parse("2010-02-03"))),
      ("叉烧包", Some(LocalDate.parse("2012-05-06")))
    )
    ```



### Join.leftJoinMap2



```scala
Buyer.select
  .leftJoin(ShippingInfo)(_.id `=` _.buyerId)
  .map { case (b, si) => (b.name, si.map(s => (s.id, s.shippingDate))) }
```


*
    ```sql
    SELECT
      buyer0.name AS res__0,
      shipping_info1.id AS res__1__0,
      shipping_info1.shipping_date AS res__1__1
    FROM buyer buyer0
    LEFT JOIN shipping_info shipping_info1 ON (buyer0.id = shipping_info1.buyer_id)
    ```



*
    ```scala
    Seq(
      ("James Bond", Some((2, LocalDate.parse("2012-04-05")))),
      ("Li Haoyi", None),
      ("叉烧包", Some((1, LocalDate.parse("2010-02-03")))),
      ("叉烧包", Some((3, LocalDate.parse("2012-05-06"))))
    )
    ```



### Join.leftJoinExpr

`JoinNullable[Expr[T]]`s can be implicitly used as `Expr[Option[T]]`s. This allows
them to participate in any database query logic than any other `Expr[Option[T]]`s
can participate in, such as being used as sort key or in computing return values
(below).

```scala
Buyer.select
  .leftJoin(ShippingInfo)(_.id `=` _.buyerId)
  .map { case (b, si) => (b.name, si.map(_.shippingDate)) }
  .sortBy(_._2)
  .nullsFirst
```


*
    ```sql
    SELECT buyer0.name AS res__0, shipping_info1.shipping_date AS res__1
    FROM buyer buyer0
    LEFT JOIN shipping_info shipping_info1 ON (buyer0.id = shipping_info1.buyer_id)
    ORDER BY res__1 NULLS FIRST
    ```



*
    ```scala
    Seq[(String, Option[LocalDate])](
      ("Li Haoyi", None),
      ("叉烧包", Some(LocalDate.parse("2010-02-03"))),
      ("James Bond", Some(LocalDate.parse("2012-04-05"))),
      ("叉烧包", Some(LocalDate.parse("2012-05-06")))
    )
    ```



### Join.leftJoinIsEmpty

You can use the `.isEmpty` method on `JoinNullable[T]` to check whether a joined table
is `NULL`, by specifying a specific non-nullable column to test against.

```scala
Buyer.select
  .leftJoin(ShippingInfo)(_.id `=` _.buyerId)
  .map { case (b, si) => (b.name, si.nonEmpty(_.id)) }
  .distinct
  .sortBy(_._1)
```


*
    ```sql
    SELECT DISTINCT buyer0.name AS res__0, (shipping_info1.id IS NOT NULL) AS res__1
    FROM buyer buyer0
    LEFT JOIN shipping_info shipping_info1 ON (buyer0.id = shipping_info1.buyer_id)
    ORDER BY res__0
    ```



*
    ```scala
    Seq(
      ("James Bond", true),
      ("Li Haoyi", false),
      ("叉烧包", true)
    )
    ```



### Join.leftJoinExpr2



```scala
Buyer.select
  .leftJoin(ShippingInfo)(_.id `=` _.buyerId)
  .map { case (b, si) => (b.name, si.map(_.shippingDate) > b.dateOfBirth) }
```


*
    ```sql
    SELECT
      buyer0.name AS res__0,
      (shipping_info1.shipping_date > buyer0.date_of_birth) AS res__1
    FROM buyer buyer0
    LEFT JOIN shipping_info shipping_info1 ON (buyer0.id = shipping_info1.buyer_id)
    ```



*
    ```scala
    Seq(
      ("James Bond", true),
      ("Li Haoyi", false),
      ("叉烧包", true),
      ("叉烧包", true)
    )
    ```



### Join.leftJoinExprExplicit

The conversion from `JoinNullable[T]` to `Expr[Option[T]]` can also be performed
explicitly via `JoinNullable.toExpr(...)`

```scala
Buyer.select
  .leftJoin(ShippingInfo)(_.id `=` _.buyerId)
  .map { case (b, si) =>
    (b.name, JoinNullable.toExpr(si.map(_.shippingDate)) > b.dateOfBirth)
  }
```


*
    ```sql
    SELECT
      buyer0.name AS res__0,
      (shipping_info1.shipping_date > buyer0.date_of_birth) AS res__1
    FROM buyer buyer0
    LEFT JOIN shipping_info shipping_info1 ON (buyer0.id = shipping_info1.buyer_id)
    ```



*
    ```scala
    Seq(
      ("James Bond", true),
      ("Li Haoyi", false),
      ("叉烧包", true),
      ("叉烧包", true)
    )
    ```



### Join.rightJoin



```scala
ShippingInfo.select.rightJoin(Buyer)(_.buyerId `=` _.id)
```


*
    ```sql
    SELECT
      shipping_info0.id AS res__0__id,
      shipping_info0.buyer_id AS res__0__buyer_id,
      shipping_info0.shipping_date AS res__0__shipping_date,
      buyer1.id AS res__1__id,
      buyer1.name AS res__1__name,
      buyer1.date_of_birth AS res__1__date_of_birth
    FROM shipping_info shipping_info0
    RIGHT JOIN buyer buyer1 ON (shipping_info0.buyer_id = buyer1.id)
    ```



*
    ```scala
    Seq(
      (
        Some(ShippingInfo[Id](2, 1, LocalDate.parse("2012-04-05"))),
        Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03"))
      ),
      (
        Some(ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03"))),
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
      ),
      (
        Some(ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06"))),
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
      ),
      (None, Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")))
    )
    ```



### Join.outerJoin



```scala
ShippingInfo.select.outerJoin(Buyer)(_.buyerId `=` _.id)
```


*
    ```sql
    SELECT
      shipping_info0.id AS res__0__id,
      shipping_info0.buyer_id AS res__0__buyer_id,
      shipping_info0.shipping_date AS res__0__shipping_date,
      buyer1.id AS res__1__id,
      buyer1.name AS res__1__name,
      buyer1.date_of_birth AS res__1__date_of_birth
    FROM shipping_info shipping_info0
    FULL OUTER JOIN buyer buyer1 ON (shipping_info0.buyer_id = buyer1.id)
    ```



*
    ```scala
    Seq(
      (
        Option(ShippingInfo[Id](2, 1, LocalDate.parse("2012-04-05"))),
        Option(Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")))
      ),
      (
        Option(ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03"))),
        Option(Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")))
      ),
      (
        Option(ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06"))),
        Option(Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")))
      ),
      (Option.empty, Option(Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))))
    )
    ```



### Join.crossJoin

`.crossJoin` can be used to generate a SQL `CROSS JOIN`, which allows you
to perform a `JOIN` with an `ON` clause in a consistent way across databases

```scala
Buyer.select
  .crossJoin(ShippingInfo)
  .filter { case (b, s) => b.id `=` s.buyerId }
  .map { case (b, s) => (b.name, s.shippingDate) }
```


*
    ```sql
    SELECT buyer0.name AS res__0, shipping_info1.shipping_date AS res__1
    FROM buyer buyer0
    CROSS JOIN shipping_info shipping_info1
    WHERE (buyer0.id = shipping_info1.buyer_id)
    ```



*
    ```scala
    Seq(
      ("James Bond", LocalDate.parse("2012-04-05")),
      ("叉烧包", LocalDate.parse("2010-02-03")),
      ("叉烧包", LocalDate.parse("2012-05-06"))
    )
    ```



## FlatJoin
inner `JOIN`s, `JOIN ON`s, self-joins, `LEFT`/`RIGHT`/`OUTER` `JOIN`s
### FlatJoin.join

"flat" joins using `for`-comprehensions are allowed. These allow you to
"flatten out" the nested tuples you get from normal `.join` clauses,
letting you write natural looking queries without deeply nested tuples.

```scala
for {
  b <- Buyer.select
  si <- ShippingInfo.join(_.buyerId `=` b.id)
} yield (b.name, si.shippingDate)
```


*
    ```sql
    SELECT buyer0.name AS res__0, shipping_info1.shipping_date AS res__1
    FROM buyer buyer0
    JOIN shipping_info shipping_info1 ON (shipping_info1.buyer_id = buyer0.id)
    ```



*
    ```scala
    Seq(
      ("James Bond", LocalDate.parse("2012-04-05")),
      ("叉烧包", LocalDate.parse("2010-02-03")),
      ("叉烧包", LocalDate.parse("2012-05-06"))
    )
    ```



### FlatJoin.join3

"flat" joins using `for`-comprehensions can have multiple `.join` clauses that
translate to SQL `JOIN ON`s, as well as `if` clauses that translate to SQL
`WHERE` clauses. This example uses multiple flat `.join`s together with `if`
clauses to query the products purchased by the user `"Li Haoyi"` that have
a price more than `1.0` dollars

```scala
for {
  b <- Buyer.select
  if b.name === "Li Haoyi"
  si <- ShippingInfo.join(_.id `=` b.id)
  pu <- Purchase.join(_.shippingInfoId `=` si.id)
  pr <- Product.join(_.id `=` pu.productId)
  if pr.price > 1.0
} yield (b.name, pr.name, pr.price)
```


*
    ```sql
    SELECT buyer0.name AS res__0, product3.name AS res__1, product3.price AS res__2
    FROM buyer buyer0
    JOIN shipping_info shipping_info1 ON (shipping_info1.id = buyer0.id)
    JOIN purchase purchase2 ON (purchase2.shipping_info_id = shipping_info1.id)
    JOIN product product3 ON (product3.id = purchase2.product_id)
    WHERE (buyer0.name = ?) AND (product3.price > ?)
    ```



*
    ```scala
    Seq(
      ("Li Haoyi", "Face Mask", 8.88)
    )
    ```



### FlatJoin.leftJoin

Flat joins can also support `.leftJoin`s, where the table being joined
is given to you as a `JoinNullable[T]`

```scala
for {
  b <- Buyer.select
  si <- ShippingInfo.leftJoin(_.buyerId `=` b.id)
} yield (b.name, si.map(_.shippingDate))
```


*
    ```sql
    SELECT buyer0.name AS res__0, shipping_info1.shipping_date AS res__1
    FROM buyer buyer0
    LEFT JOIN shipping_info shipping_info1 ON (shipping_info1.buyer_id = buyer0.id)
    ```



*
    ```scala
    Seq(
      ("James Bond", Some(LocalDate.parse("2012-04-05"))),
      ("Li Haoyi", None),
      ("叉烧包", Some(LocalDate.parse("2010-02-03"))),
      ("叉烧包", Some(LocalDate.parse("2012-05-06")))
    )
    ```



### FlatJoin.flatMap

You can also perform inner joins via `flatMap`, either by directly
calling `.flatMap` or via `for`-comprehensions as below. This can help
reduce the boilerplate when dealing with lots of joins.

```scala
Buyer.select
  .flatMap(b => ShippingInfo.crossJoin().map((b, _)))
  .filter { case (b, s) => b.id `=` s.buyerId && b.name `=` "James Bond" }
  .map(_._2.shippingDate)
```


*
    ```sql
    SELECT shipping_info1.shipping_date AS res
    FROM buyer buyer0
    CROSS JOIN shipping_info shipping_info1
    WHERE ((buyer0.id = shipping_info1.buyer_id) AND (buyer0.name = ?))
    ```



*
    ```scala
    Seq(LocalDate.parse("2012-04-05"))
    ```



### FlatJoin.flatMapFor

You can also perform inner joins via `flatMap

```scala
for {
  b <- Buyer.select
  s <- ShippingInfo.crossJoin()
  if b.id `=` s.buyerId && b.name `=` "James Bond"
} yield s.shippingDate
```


*
    ```sql
    SELECT shipping_info1.shipping_date AS res
    FROM buyer buyer0
    CROSS JOIN shipping_info shipping_info1
    WHERE ((buyer0.id = shipping_info1.buyer_id) AND (buyer0.name = ?))
    ```



*
    ```scala
    Seq(LocalDate.parse("2012-04-05"))
    ```



### FlatJoin.flatMapForFilter



```scala
for {
  b <- Buyer.select.filter(_.name `=` "James Bond")
  s <- ShippingInfo.crossJoin().filter(b.id `=` _.buyerId)
} yield s.shippingDate
```


*
    ```sql
    SELECT shipping_info1.shipping_date AS res
    FROM buyer buyer0
    CROSS JOIN shipping_info shipping_info1
    WHERE (buyer0.name = ?) AND (buyer0.id = shipping_info1.buyer_id)
    ```



*
    ```scala
    Seq(LocalDate.parse("2012-04-05"))
    ```



### FlatJoin.flatMapForJoin

Using queries with `join`s in a `for`-comprehension is supported, with the
generated `JOIN`s being added to the `FROM` clause generated by the `.flatMap`.

```scala
for {
  (b, si) <- Buyer.select.join(ShippingInfo)(_.id `=` _.buyerId)
  (pu, pr) <- Purchase.select.join(Product)(_.productId `=` _.id).crossJoin()
  if si.id `=` pu.shippingInfoId
} yield (b.name, pr.name)
```


*
    ```sql
    SELECT buyer0.name AS res__0, subquery2.res__1__name AS res__1
    FROM buyer buyer0
    JOIN shipping_info shipping_info1 ON (buyer0.id = shipping_info1.buyer_id)
    CROSS JOIN (SELECT
        purchase2.shipping_info_id AS res__0__shipping_info_id,
        product3.name AS res__1__name
      FROM purchase purchase2
      JOIN product product3 ON (purchase2.product_id = product3.id)) subquery2
    WHERE (shipping_info1.id = subquery2.res__0__shipping_info_id)
    ```



*
    ```scala
    Seq(
      ("James Bond", "Camera"),
      ("James Bond", "Skate Board"),
      ("叉烧包", "Cookie"),
      ("叉烧包", "Face Mask"),
      ("叉烧包", "Face Mask"),
      ("叉烧包", "Guitar"),
      ("叉烧包", "Socks")
    )
    ```



### FlatJoin.flatMapForGroupBy

Using non-trivial queries in the `for`-comprehension may result in subqueries
being generated

```scala
for {
  (name, dateOfBirth) <- Buyer.select.groupBy(_.name)(_.minBy(_.dateOfBirth))
  shippingInfo <- ShippingInfo.crossJoin()
} yield (name, dateOfBirth, shippingInfo.id, shippingInfo.shippingDate)
```


*
    ```sql
    SELECT
      subquery0.res__0 AS res__0,
      subquery0.res__1 AS res__1,
      shipping_info1.id AS res__2,
      shipping_info1.shipping_date AS res__3
    FROM (SELECT buyer0.name AS res__0, MIN(buyer0.date_of_birth) AS res__1
      FROM buyer buyer0
      GROUP BY buyer0.name) subquery0
    CROSS JOIN shipping_info shipping_info1
    ```



*
    ```scala
    Seq(
      ("James Bond", LocalDate.parse("2001-02-03"), 1, LocalDate.parse("2010-02-03")),
      ("James Bond", LocalDate.parse("2001-02-03"), 2, LocalDate.parse("2012-04-05")),
      ("James Bond", LocalDate.parse("2001-02-03"), 3, LocalDate.parse("2012-05-06")),
      ("Li Haoyi", LocalDate.parse("1965-08-09"), 1, LocalDate.parse("2010-02-03")),
      ("Li Haoyi", LocalDate.parse("1965-08-09"), 2, LocalDate.parse("2012-04-05")),
      ("Li Haoyi", LocalDate.parse("1965-08-09"), 3, LocalDate.parse("2012-05-06")),
      ("叉烧包", LocalDate.parse("1923-11-12"), 1, LocalDate.parse("2010-02-03")),
      ("叉烧包", LocalDate.parse("1923-11-12"), 2, LocalDate.parse("2012-04-05")),
      ("叉烧包", LocalDate.parse("1923-11-12"), 3, LocalDate.parse("2012-05-06"))
    )
    ```



### FlatJoin.flatMapForGroupBy2

Using non-trivial queries in the `for`-comprehension may result in subqueries
being generated

```scala
for {
  (name, dateOfBirth) <- Buyer.select.groupBy(_.name)(_.minBy(_.dateOfBirth))
  (shippingInfoId, shippingDate) <- ShippingInfo.select
    .groupBy(_.id)(_.minBy(_.shippingDate))
    .crossJoin()
} yield (name, dateOfBirth, shippingInfoId, shippingDate)
```


*
    ```sql
    SELECT
      subquery0.res__0 AS res__0,
      subquery0.res__1 AS res__1,
      subquery1.res__0 AS res__2,
      subquery1.res__1 AS res__3
    FROM (SELECT
        buyer0.name AS res__0,
        MIN(buyer0.date_of_birth) AS res__1
      FROM buyer buyer0
      GROUP BY buyer0.name) subquery0
    CROSS JOIN (SELECT
        shipping_info1.id AS res__0,
        MIN(shipping_info1.shipping_date) AS res__1
      FROM shipping_info shipping_info1
      GROUP BY shipping_info1.id) subquery1
    ```



*
    ```scala
    Seq(
      ("James Bond", LocalDate.parse("2001-02-03"), 1, LocalDate.parse("2010-02-03")),
      ("James Bond", LocalDate.parse("2001-02-03"), 2, LocalDate.parse("2012-04-05")),
      ("James Bond", LocalDate.parse("2001-02-03"), 3, LocalDate.parse("2012-05-06")),
      ("Li Haoyi", LocalDate.parse("1965-08-09"), 1, LocalDate.parse("2010-02-03")),
      ("Li Haoyi", LocalDate.parse("1965-08-09"), 2, LocalDate.parse("2012-04-05")),
      ("Li Haoyi", LocalDate.parse("1965-08-09"), 3, LocalDate.parse("2012-05-06")),
      ("叉烧包", LocalDate.parse("1923-11-12"), 1, LocalDate.parse("2010-02-03")),
      ("叉烧包", LocalDate.parse("1923-11-12"), 2, LocalDate.parse("2012-04-05")),
      ("叉烧包", LocalDate.parse("1923-11-12"), 3, LocalDate.parse("2012-05-06"))
    )
    ```



### FlatJoin.flatMapForCompound

Using non-trivial queries in the `for`-comprehension may result in subqueries
being generated

```scala
for {
  b <- Buyer.select.sortBy(_.id).asc.take(1)
  si <- ShippingInfo.select.sortBy(_.id).asc.take(1).crossJoin()
} yield (b.name, si.shippingDate)
```


*
    ```sql
    SELECT
      subquery0.res__name AS res__0,
      subquery1.res__shipping_date AS res__1
    FROM
      (SELECT buyer0.id AS res__id, buyer0.name AS res__name
      FROM buyer buyer0
      ORDER BY res__id ASC
      LIMIT ?) subquery0
    CROSS JOIN (SELECT
        shipping_info1.id AS res__id,
        shipping_info1.shipping_date AS res__shipping_date
      FROM shipping_info shipping_info1
      ORDER BY res__id ASC
      LIMIT ?) subquery1
    ```



*
    ```scala
    Seq(
      ("James Bond", LocalDate.parse("2010-02-03"))
    )
    ```



## Insert
Basic `INSERT` operations
### Insert.single.simple

`Table.insert.values` inserts a single row into the given table, with the specified
 columns assigned to the given values, and any non-specified columns left `NULL`
 or assigned to their default values

```scala
Buyer.insert.columns(
  _.name := "test buyer",
  _.dateOfBirth := LocalDate.parse("2023-09-09"),
  _.id := 4
)
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)
    ```



*
    ```scala
    1
    ```



----



```scala
Buyer.select.filter(_.name `=` "test buyer")
```




*
    ```scala
    Seq(Buyer[Id](4, "test buyer", LocalDate.parse("2023-09-09")))
    ```



### Insert.single.partial



```scala
Buyer.insert
  .columns(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"))
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth) VALUES (?, ?)
    ```



*
    ```scala
    1
    ```



----



```scala
Buyer.select.filter(_.name `=` "test buyer")
```




*
    ```scala
    Seq(Buyer[Id](4, "test buyer", LocalDate.parse("2023-09-09")))
    ```



### Insert.batch.simple

`Table.insert.batched` inserts multiple rows into the given table, with the
relevant columns declared once in the first parameter list and the given
values provided as tuples in the second parameter list

```scala
Buyer.insert.batched(_.name, _.dateOfBirth, _.id)(
  ("test buyer A", LocalDate.parse("2001-04-07"), 4),
  ("test buyer B", LocalDate.parse("2002-05-08"), 5),
  ("test buyer C", LocalDate.parse("2003-06-09"), 6)
)
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth, id)
    VALUES
      (?, ?, ?),
      (?, ?, ?),
      (?, ?, ?)
    ```



*
    ```scala
    3
    ```



----



```scala
Buyer.select
```




*
    ```scala
    Seq(
      Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
      Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
      Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
      Buyer[Id](4, "test buyer A", LocalDate.parse("2001-04-07")),
      Buyer[Id](5, "test buyer B", LocalDate.parse("2002-05-08")),
      Buyer[Id](6, "test buyer C", LocalDate.parse("2003-06-09"))
    )
    ```



### Insert.batch.partial



```scala
Buyer.insert.batched(_.name, _.dateOfBirth)(
  ("test buyer A", LocalDate.parse("2001-04-07")),
  ("test buyer B", LocalDate.parse("2002-05-08")),
  ("test buyer C", LocalDate.parse("2003-06-09"))
)
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth)
    VALUES (?, ?), (?, ?), (?, ?)
    ```



*
    ```scala
    3
    ```



----



```scala
Buyer.select
```




*
    ```scala
    Seq(
      Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
      Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
      Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
      // id=4,5,6 comes from auto increment
      Buyer[Id](4, "test buyer A", LocalDate.parse("2001-04-07")),
      Buyer[Id](5, "test buyer B", LocalDate.parse("2002-05-08")),
      Buyer[Id](6, "test buyer C", LocalDate.parse("2003-06-09"))
    )
    ```



### Insert.select.caseclass

`Table.insert.select` inserts rows into the given table based on the given `Table.select`
clause, and translates directly into SQL's `INSERT INTO ... SELECT` syntax.

```scala
Buyer.insert.select(
  identity,
  Buyer.select
    .filter(_.name <> "Li Haoyi")
    .map(b => b.copy(id = b.id + Buyer.select.maxBy(_.id)))
)
```


*
    ```sql
    INSERT INTO buyer (id, name, date_of_birth)
    SELECT
      (buyer0.id + (SELECT MAX(buyer1.id) AS res FROM buyer buyer1)) AS res__id,
      buyer0.name AS res__name,
      buyer0.date_of_birth AS res__date_of_birth
    FROM buyer buyer0
    WHERE (buyer0.name <> ?)
    ```



*
    ```scala
    2
    ```



----



```scala
Buyer.select
```




*
    ```scala
    Seq(
      Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
      Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
      Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
      Buyer[Id](4, "James Bond", LocalDate.parse("2001-02-03")),
      Buyer[Id](5, "叉烧包", LocalDate.parse("1923-11-12"))
    )
    ```



### Insert.select.simple



```scala
Buyer.insert.select(
  x => (x.name, x.dateOfBirth),
  Buyer.select.map(x => (x.name, x.dateOfBirth)).filter(_._1 <> "Li Haoyi")
)
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth)
    SELECT buyer0.name AS res__0, buyer0.date_of_birth AS res__1
    FROM buyer buyer0
    WHERE (buyer0.name <> ?)
    ```



*
    ```scala
    2
    ```



----



```scala
Buyer.select
```




*
    ```scala
    Seq(
      Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
      Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
      Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
      // id=4,5 comes from auto increment, 6 is filtered out in the select
      Buyer[Id](4, "James Bond", LocalDate.parse("2001-02-03")),
      Buyer[Id](5, "叉烧包", LocalDate.parse("1923-11-12"))
    )
    ```



## Update
Basic `UPDATE` queries
### Update.update

`Table.update` takes a predicate specifying the rows to update, and a
`.set` clause that allows you to specify the values assigned to columns
on those rows

```scala
Buyer
  .update(_.name `=` "James Bond")
  .set(_.dateOfBirth := LocalDate.parse("2019-04-07"))
```


*
    ```sql
    UPDATE buyer SET date_of_birth = ? WHERE (buyer.name = ?)
    ```



*
    ```scala
    1
    ```



----



```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth).single
```




*
    ```scala
    LocalDate.parse("2019-04-07")
    ```



----



```scala
Buyer.select.filter(_.name `=` "Li Haoyi").map(_.dateOfBirth).single
```




*
    ```scala
    LocalDate.parse("1965-08-09" /* not updated */ )
    ```



### Update.bulk

The predicate to `Table.update` is mandatory, to avoid anyone forgetting to
provide one and accidentally bulk-updating all rows in their table. If you
really do want to update all rows in the table, you can provide the predicate `_ => true`

```scala
Buyer.update(_ => true).set(_.dateOfBirth := LocalDate.parse("2019-04-07"))
```


*
    ```sql
    UPDATE buyer SET date_of_birth = ?
    ```



*
    ```scala
    3
    ```



----



```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth).single
```




*
    ```scala
    LocalDate.parse("2019-04-07")
    ```



----



```scala
Buyer.select.filter(_.name `=` "Li Haoyi").map(_.dateOfBirth).single
```




*
    ```scala
    LocalDate.parse("2019-04-07")
    ```



### Update.multiple

This example shows how to update multiple columns in a single `Table.update` call

```scala
Buyer
  .update(_.name `=` "James Bond")
  .set(_.dateOfBirth := LocalDate.parse("2019-04-07"), _.name := "John Dee")
```


*
    ```sql
    UPDATE buyer SET date_of_birth = ?, name = ? WHERE (buyer.name = ?)
    ```



*
    ```scala
    1
    ```



----



```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```




*
    ```scala
    Seq[LocalDate]( /* not found due to rename */ )
    ```



----



```scala
Buyer.select.filter(_.name `=` "John Dee").map(_.dateOfBirth)
```




*
    ```scala
    Seq(LocalDate.parse("2019-04-07"))
    ```



### Update.dynamic

The values assigned to columns in `Table.update` can also be computed `Expr[T]`s,
not just literal Scala constants. This example shows how to to update the name of
the row for `James Bond` with it's existing name in uppercase

```scala
Buyer.update(_.name `=` "James Bond").set(c => c.name := c.name.toUpperCase)
```


*
    ```sql
    UPDATE buyer SET name = UPPER(buyer.name) WHERE (buyer.name = ?)
    ```



*
    ```scala
    1
    ```



----



```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```




*
    ```scala
    Seq[LocalDate]( /* not found due to rename */ )
    ```



----



```scala
Buyer.select.filter(_.name `=` "JAMES BOND").map(_.dateOfBirth)
```




*
    ```scala
    Seq(LocalDate.parse("2001-02-03"))
    ```



## Delete
Basic `DELETE` operations
### Delete.single

`Table.delete` takes a mandatory predicate specifying what rows you want to delete.
The most common case is to specify the ID of the row you want to delete

```scala
Purchase.delete(_.id `=` 2)
```


*
    ```sql
    DELETE FROM purchase WHERE (purchase.id = ?)
    ```



*
    ```scala
    1
    ```



----



```scala
Purchase.select
```




*
    ```scala
    Seq(
      Purchase[Id](id = 1, shippingInfoId = 1, productId = 1, count = 100, total = 888.0),
      // id==2 got deleted
      Purchase[Id](id = 3, shippingInfoId = 1, productId = 3, count = 5, total = 15.7),
      Purchase[Id](id = 4, shippingInfoId = 2, productId = 4, count = 4, total = 493.8),
      Purchase[Id](id = 5, shippingInfoId = 2, productId = 5, count = 10, total = 10000.0),
      Purchase[Id](id = 6, shippingInfoId = 3, productId = 1, count = 5, total = 44.4),
      Purchase[Id](id = 7, shippingInfoId = 3, productId = 6, count = 13, total = 1.3)
    )
    ```



### Delete.multiple

Although specifying a single ID to delete is the most common case, you can pass
in arbitrary predicates, e.g. in this example deleting all rows _except_ for the
one with a particular ID

```scala
Purchase.delete(_.id <> 2)
```


*
    ```sql
    DELETE FROM purchase WHERE (purchase.id <> ?)
    ```



*
    ```scala
    6
    ```



----



```scala
Purchase.select
```




*
    ```scala
    Seq(Purchase[Id](id = 2, shippingInfoId = 1, productId = 2, count = 3, total = 900.0))
    ```



### Delete.all

If you actually want to delete all rows in the table, you can explicitly
pass in the predicate `_ => true`

```scala
Purchase.delete(_ => true)
```


*
    ```sql
    DELETE FROM purchase WHERE ?
    ```



*
    ```scala
    7
    ```



----



```scala
Purchase.select
```




*
    ```scala
    Seq[Purchase[Id]](
      // all Deleted
    )
    ```



## CompoundSelect
Compound `SELECT` operations: sort, take, drop, union, unionAll, etc.
### CompoundSelect.sort.simple

ScalaSql's `.sortBy` method translates into SQL `ORDER BY`

```scala
Product.select.sortBy(_.price).map(_.name)
```


*
    ```sql
    SELECT product0.name AS res FROM product product0 ORDER BY product0.price
    ```



*
    ```scala
    Seq("Cookie", "Socks", "Face Mask", "Skate Board", "Guitar", "Camera")
    ```



### CompoundSelect.sort.twice

If you want to sort by multiple columns, you can call `.sortBy` multiple times,
each with its own call to `.asc` or `.desc`. Note that the rightmost call to `.sortBy`
takes precedence, following the Scala collections `.sortBy` semantics, and so the
right-most `.sortBy` in ScalaSql becomes the _left_-most entry in the SQL `ORDER BY` clause

```scala
Purchase.select.sortBy(_.productId).asc.sortBy(_.shippingInfoId).desc
```


*
    ```sql
    SELECT
      purchase0.id AS res__id,
      purchase0.shipping_info_id AS res__shipping_info_id,
      purchase0.product_id AS res__product_id,
      purchase0.count AS res__count,
      purchase0.total AS res__total
    FROM purchase purchase0
    ORDER BY res__shipping_info_id DESC, res__product_id ASC
    ```



*
    ```scala
    Seq(
      Purchase[Id](6, 3, 1, 5, 44.4),
      Purchase[Id](7, 3, 6, 13, 1.3),
      Purchase[Id](4, 2, 4, 4, 493.8),
      Purchase[Id](5, 2, 5, 10, 10000.0),
      Purchase[Id](1, 1, 1, 100, 888.0),
      Purchase[Id](2, 1, 2, 3, 900.0),
      Purchase[Id](3, 1, 3, 5, 15.7)
    )
    ```



### CompoundSelect.sort.sortLimit

ScalaSql also supports various combinations of `.take` and `.drop`, translating to SQL
`LIMIT` or `OFFSET`

```scala
Product.select.sortBy(_.price).map(_.name).take(2)
```


*
    ```sql
    SELECT product0.name AS res FROM product product0 ORDER BY product0.price LIMIT ?
    ```



*
    ```scala
    Seq("Cookie", "Socks")
    ```



### CompoundSelect.sort.sortOffset



```scala
Product.select.sortBy(_.price).map(_.name).drop(2)
```


*
    ```sql
    SELECT product0.name AS res FROM product product0 ORDER BY product0.price OFFSET ?
    ```



*
    ```scala
    Seq("Face Mask", "Skate Board", "Guitar", "Camera")
    ```



### CompoundSelect.sort.sortLimitTwiceHigher

Note that `.drop` and `.take` follow Scala collections' semantics, so calling e.g. `.take`
multiple times takes the value of the smallest `.take`, while calling `.drop` multiple
times accumulates the total amount dropped

```scala
Product.select.sortBy(_.price).map(_.name).take(2).take(3)
```


*
    ```sql
    SELECT product0.name AS res FROM product product0 ORDER BY product0.price LIMIT ?
    ```



*
    ```scala
    Seq("Cookie", "Socks")
    ```



### CompoundSelect.sort.sortLimitTwiceLower



```scala
Product.select.sortBy(_.price).map(_.name).take(2).take(1)
```


*
    ```sql
    SELECT product0.name AS res FROM product product0 ORDER BY product0.price LIMIT ?
    ```



*
    ```scala
    Seq("Cookie")
    ```



### CompoundSelect.sort.sortLimitOffset



```scala
Product.select.sortBy(_.price).map(_.name).drop(2).take(2)
```


*
    ```sql
    SELECT product0.name AS res FROM product product0 ORDER BY product0.price LIMIT ? OFFSET ?
    ```



*
    ```scala
    Seq("Face Mask", "Skate Board")
    ```



### CompoundSelect.sort.sortLimitOffsetTwice



```scala
Product.select.sortBy(_.price).map(_.name).drop(2).drop(2).take(1)
```


*
    ```sql
    SELECT product0.name AS res FROM product product0 ORDER BY product0.price LIMIT ? OFFSET ?
    ```



*
    ```scala
    Seq("Guitar")
    ```



### CompoundSelect.sort.sortOffsetLimit



```scala
Product.select.sortBy(_.price).map(_.name).drop(2).take(2)
```


*
    ```sql
    SELECT product0.name AS res FROM product product0 ORDER BY product0.price LIMIT ? OFFSET ?
    ```



*
    ```scala
    Seq("Face Mask", "Skate Board")
    ```



### CompoundSelect.distinct

ScalaSql's `.distinct` translates to SQL's `SELECT DISTINCT`

```scala
Purchase.select.sortBy(_.total).desc.take(3).map(_.shippingInfoId).distinct
```


*
    ```sql
    SELECT DISTINCT subquery0.res AS res
    FROM (SELECT purchase0.shipping_info_id AS res
      FROM purchase purchase0
      ORDER BY purchase0.total DESC
      LIMIT ?) subquery0
    ```



*
    ```scala
    Seq(1, 2)
    ```



### CompoundSelect.flatMap

Many operations in SQL cannot be done in certain orders, unless you move part of the logic into
a subquery. ScalaSql does this automatically for you, e.g. doing a `flatMap`, `.sumBy`, or
`.aggregate` after a `.sortBy`/`.take`, the LHS `.sortBy`/`.take` is automatically extracted
into a subquery

```scala
Purchase.select.sortBy(_.total).desc.take(3).flatMap { p =>
  Product.crossJoin().filter(_.id === p.productId).map(_.name)
}
```


*
    ```sql
    SELECT product1.name AS res
    FROM (SELECT purchase0.product_id AS res__product_id, purchase0.total AS res__total
      FROM purchase purchase0
      ORDER BY res__total DESC
      LIMIT ?) subquery0
    CROSS JOIN product product1
    WHERE (product1.id = subquery0.res__product_id)
    ```



*
    ```scala
    Seq("Camera", "Face Mask", "Guitar")
    ```



### CompoundSelect.sumBy



```scala
Purchase.select.sortBy(_.total).desc.take(3).sumBy(_.total)
```


*
    ```sql
    SELECT SUM(subquery0.res__total) AS res
    FROM (SELECT purchase0.total AS res__total
      FROM purchase purchase0
      ORDER BY res__total DESC
      LIMIT ?) subquery0
    ```



*
    ```scala
    11788.0
    ```



### CompoundSelect.aggregate



```scala
Purchase.select
  .sortBy(_.total)
  .desc
  .take(3)
  .aggregate(p => (p.sumBy(_.total), p.avgBy(_.total)))
```


*
    ```sql
    SELECT SUM(subquery0.res__total) AS res__0, AVG(subquery0.res__total) AS res__1
    FROM (SELECT purchase0.total AS res__total
      FROM purchase purchase0
      ORDER BY res__total DESC
      LIMIT ?) subquery0
    ```



*
    ```scala
    (11788.0, 3929.0)
    ```



### CompoundSelect.union

ScalaSql's `.union`/`.unionAll`/`.intersect`/`.except` translate into SQL's
`UNION`/`UNION ALL`/`INTERSECT`/`EXCEPT`.

```scala
Product.select
  .map(_.name.toLowerCase)
  .union(Product.select.map(_.kebabCaseName.toLowerCase))
```


*
    ```sql
    SELECT LOWER(product0.name) AS res
    FROM product product0
    UNION
    SELECT LOWER(product0.kebab_case_name) AS res
    FROM product product0
    ```



*
    ```scala
    Seq(
      "camera",
      "cookie",
      "face mask",
      "face-mask",
      "guitar",
      "skate board",
      "skate-board",
      "socks"
    )
    ```



### CompoundSelect.unionAll



```scala
Product.select
  .map(_.name.toLowerCase)
  .unionAll(Product.select.map(_.kebabCaseName.toLowerCase))
```


*
    ```sql
    SELECT LOWER(product0.name) AS res
    FROM product product0
    UNION ALL
    SELECT LOWER(product0.kebab_case_name) AS res
    FROM product product0
    ```



*
    ```scala
    Seq(
      "face mask",
      "guitar",
      "socks",
      "skate board",
      "camera",
      "cookie",
      "face-mask",
      "guitar",
      "socks",
      "skate-board",
      "camera",
      "cookie"
    )
    ```



### CompoundSelect.intersect



```scala
Product.select
  .map(_.name.toLowerCase)
  .intersect(Product.select.map(_.kebabCaseName.toLowerCase))
```


*
    ```sql
    SELECT LOWER(product0.name) AS res
    FROM product product0
    INTERSECT
    SELECT LOWER(product0.kebab_case_name) AS res
    FROM product product0
    ```



*
    ```scala
    Seq("camera", "cookie", "guitar", "socks")
    ```



### CompoundSelect.except



```scala
Product.select
  .map(_.name.toLowerCase)
  .except(Product.select.map(_.kebabCaseName.toLowerCase))
```


*
    ```sql
    SELECT LOWER(product0.name) AS res
    FROM product product0
    EXCEPT
    SELECT LOWER(product0.kebab_case_name) AS res
    FROM product product0
    ```



*
    ```scala
    Seq("face mask", "skate board")
    ```



### CompoundSelect.unionAllUnionSort

Performing a `.sortBy` after `.union` or `.unionAll` applies the sort
to both sides of the `union`/`unionAll`, behaving identically to Scala or SQL

```scala
Product.select
  .map(_.name.toLowerCase)
  .unionAll(Buyer.select.map(_.name.toLowerCase))
  .union(Product.select.map(_.kebabCaseName.toLowerCase))
  .sortBy(identity)
```


*
    ```sql
    SELECT LOWER(product0.name) AS res
    FROM product product0
    UNION ALL
    SELECT LOWER(buyer0.name) AS res
    FROM buyer buyer0
    UNION
    SELECT LOWER(product0.kebab_case_name) AS res
    FROM product product0
    ORDER BY res
    ```



*
    ```scala
    Seq(
      "camera",
      "cookie",
      "face mask",
      "face-mask",
      "guitar",
      "james bond",
      "li haoyi",
      "skate board",
      "skate-board",
      "socks",
      "叉烧包"
    )
    ```



### CompoundSelect.unionAllUnionSortLimit



```scala
Product.select
  .map(_.name.toLowerCase)
  .unionAll(Buyer.select.map(_.name.toLowerCase))
  .union(Product.select.map(_.kebabCaseName.toLowerCase))
  .sortBy(identity)
  .drop(4)
  .take(4)
```


*
    ```sql
    SELECT LOWER(product0.name) AS res
    FROM product product0
    UNION ALL
    SELECT LOWER(buyer0.name) AS res
    FROM buyer buyer0
    UNION
    SELECT LOWER(product0.kebab_case_name) AS res
    FROM product product0
    ORDER BY res
    LIMIT ?
    OFFSET ?
    ```



*
    ```scala
    Seq("guitar", "james bond", "li haoyi", "skate board")
    ```



## UpdateJoin
`UPDATE` queries that use `JOIN`s
### UpdateJoin.join

ScalaSql supports performing `UPDATE`s with `FROM`/`JOIN` clauses using the
`.update.join` methods

```scala
Buyer
  .update(_.name `=` "James Bond")
  .join(ShippingInfo)(_.id `=` _.buyerId)
  .set(c => c._1.dateOfBirth := c._2.shippingDate)
```


*
    ```sql
    UPDATE buyer
    SET date_of_birth = shipping_info0.shipping_date
    FROM shipping_info shipping_info0
    WHERE (buyer.id = shipping_info0.buyer_id) AND (buyer.name = ?)
    ```



*
    ```scala
    1
    ```



----



```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```




*
    ```scala
    Seq(LocalDate.parse("2012-04-05"))
    ```



### UpdateJoin.multijoin

Multiple joins are supported, e.g. the below example where we join the `Buyer` table
three times against `ShippingInfo`/`Purchase`/`Product` to determine what to update

```scala
Buyer
  .update(_.name `=` "James Bond")
  .join(ShippingInfo)(_.id `=` _.buyerId)
  .join(Purchase)(_._2.id `=` _.shippingInfoId)
  .join(Product)(_._3.productId `=` _.id)
  .filter(t => t._4.name.toLowerCase `=` t._4.kebabCaseName.toLowerCase)
  .set(c => c._1.name := c._4.name)
```


*
    ```sql
    UPDATE buyer
    SET name = product2.name
    FROM shipping_info shipping_info0
    JOIN purchase purchase1 ON (shipping_info0.id = purchase1.shipping_info_id)
    JOIN product product2 ON (purchase1.product_id = product2.id)
    WHERE (buyer.id = shipping_info0.buyer_id)
    AND (buyer.name = ?)
    AND (LOWER(product2.name) = LOWER(product2.kebab_case_name))
    ```



*
    ```scala
    1
    ```



----



```scala
Buyer.select.filter(_.id `=` 1).map(_.name)
```




*
    ```scala
    Seq("Camera")
    ```



### UpdateJoin.joinSubquery

In addition to `JOIN`ing against another table, you can also perform `JOIN`s against
subqueries by passing in a `.select` query to `.join`

```scala
Buyer
  .update(_.name `=` "James Bond")
  .join(ShippingInfo.select.sortBy(_.id).asc.take(2))(_.id `=` _.buyerId)
  .set(c => c._1.dateOfBirth := c._2.shippingDate)
```


*
    ```sql
    UPDATE buyer SET date_of_birth = subquery0.res__shipping_date
    FROM (SELECT
        shipping_info0.id AS res__id,
        shipping_info0.buyer_id AS res__buyer_id,
        shipping_info0.shipping_date AS res__shipping_date
      FROM shipping_info shipping_info0
      ORDER BY res__id ASC
      LIMIT ?) subquery0
    WHERE (buyer.id = subquery0.res__buyer_id) AND (buyer.name = ?)
    ```



*
    ```scala
    1
    ```



----



```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```




*
    ```scala
    Seq(LocalDate.parse("2012-04-05"))
    ```



### UpdateJoin.joinSubqueryEliminatedColumn



```scala
Buyer
  .update(_.name `=` "James Bond")
  // Make sure the `SELECT shipping_info0.shipping_info_id AS res__shipping_info_id`
  // column gets eliminated since it is not used outside the subquery
  .join(ShippingInfo.select.sortBy(_.id).asc.take(2))(_.id `=` _.buyerId)
  .set(c => c._1.dateOfBirth := LocalDate.parse("2000-01-01"))
```


*
    ```sql
    UPDATE buyer SET date_of_birth = ?
    FROM (SELECT
        shipping_info0.id AS res__id,
        shipping_info0.buyer_id AS res__buyer_id
      FROM shipping_info shipping_info0
      ORDER BY res__id ASC
      LIMIT ?) subquery0
    WHERE (buyer.id = subquery0.res__buyer_id) AND (buyer.name = ?)
    ```



*
    ```scala
    1
    ```



----



```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```




*
    ```scala
    Seq(LocalDate.parse("2000-01-01"))
    ```



## UpdateSubQuery
`UPDATE` queries that use Subqueries
### UpdateSubQuery.setSubquery

You can use subqueries to compute the values you want to update, using
aggregates like `.maxBy` to convert the `Select[T]` into an `Expr[T]`

```scala
Product.update(_ => true).set(_.price := Product.select.maxBy(_.price))
```


*
    ```sql
    UPDATE product
    SET price = (SELECT MAX(product1.price) AS res FROM product product1)
    ```



*
    ```scala
    6
    ```



----



```scala
Product.select.map(p => (p.id, p.name, p.price))
```




*
    ```scala
    Seq(
      (1, "Face Mask", 1000.0),
      (2, "Guitar", 1000.0),
      (3, "Socks", 1000.0),
      (4, "Skate Board", 1000.0),
      (5, "Camera", 1000.0),
      (6, "Cookie", 1000.0)
    )
    ```



### UpdateSubQuery.whereSubquery

Subqueries and aggregates can also be used in the `WHERE` clause, defined by the
predicate passed to `Table.update

```scala
Product.update(_.price `=` Product.select.maxBy(_.price)).set(_.price := 0)
```


*
    ```sql
    UPDATE product
    SET price = ?
    WHERE (product.price = (SELECT MAX(product1.price) AS res FROM product product1))
    ```



*
    ```scala
    1
    ```



----



```scala
Product.select.map(p => (p.id, p.name, p.price))
```




*
    ```scala
    Seq(
      (1, "Face Mask", 8.88),
      (2, "Guitar", 300.0),
      (3, "Socks", 3.14),
      (4, "Skate Board", 123.45),
      (5, "Camera", 0.0),
      (6, "Cookie", 0.1)
    )
    ```



## Returning
Queries using `INSERT` or `UPDATE` with `RETURNING`
### Returning.insert.single

ScalaSql's `.returning` clause translates to SQL's `RETURNING` syntax, letting
you perform insertions or updates and return values from the query (rather than
returning a single integer representing the rows affected). This is especially
useful for retrieving the auto-generated table IDs that many databases support.

Note that `.returning`/`RETURNING` is not supported in MySql, H2 or HsqlDB

```scala
Buyer.insert
  .columns(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"))
  .returning(_.id)
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth) VALUES (?, ?) RETURNING buyer.id AS res
    ```



*
    ```scala
    Seq(4)
    ```



----



```scala
Buyer.select.filter(_.name `=` "test buyer")
```




*
    ```scala
    Seq(Buyer[Id](4, "test buyer", LocalDate.parse("2023-09-09")))
    ```



### Returning.insert.dotSingle

If your `.returning` query is expected to be a single row, the `.single` method is
supported to convert the returned `Seq[T]` into a single `T`. `.single` throws an
exception if zero or multiple rows are returned.

```scala
Buyer.insert
  .columns(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"))
  .returning(_.id)
  .single
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth) VALUES (?, ?) RETURNING buyer.id AS res
    ```



*
    ```scala
    4
    ```



----



```scala
Buyer.select.filter(_.name `=` "test buyer")
```




*
    ```scala
    Seq(Buyer[Id](4, "test buyer", LocalDate.parse("2023-09-09")))
    ```



### Returning.insert.multiple



```scala
Buyer.insert
  .batched(_.name, _.dateOfBirth)(
    ("test buyer A", LocalDate.parse("2001-04-07")),
    ("test buyer B", LocalDate.parse("2002-05-08")),
    ("test buyer C", LocalDate.parse("2003-06-09"))
  )
  .returning(_.id)
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth)
    VALUES
      (?, ?),
      (?, ?),
      (?, ?)
    RETURNING buyer.id AS res
    ```



*
    ```scala
    Seq(4, 5, 6)
    ```



----



```scala
Buyer.select
```




*
    ```scala
    Seq(
      Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
      Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
      Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
      // id=4,5,6 comes from auto increment
      Buyer[Id](4, "test buyer A", LocalDate.parse("2001-04-07")),
      Buyer[Id](5, "test buyer B", LocalDate.parse("2002-05-08")),
      Buyer[Id](6, "test buyer C", LocalDate.parse("2003-06-09"))
    )
    ```



### Returning.insert.select

All variants of `.insert` and `.update` support `.returning`, e.g. the example below
applies to `.insert.select`, and the examples further down demonstrate its usage with
`.update` and `.delete`

```scala
Buyer.insert
  .select(
    x => (x.name, x.dateOfBirth),
    Buyer.select.map(x => (x.name, x.dateOfBirth)).filter(_._1 <> "Li Haoyi")
  )
  .returning(_.id)
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth)
    SELECT
      buyer0.name AS res__0,
      buyer0.date_of_birth AS res__1
    FROM buyer buyer0
    WHERE (buyer0.name <> ?)
    RETURNING buyer.id AS res
    ```



*
    ```scala
    Seq(4, 5)
    ```



----



```scala
Buyer.select
```




*
    ```scala
    Seq(
      Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
      Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
      Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
      // id=4,5 comes from auto increment, 6 is filtered out in the select
      Buyer[Id](4, "James Bond", LocalDate.parse("2001-02-03")),
      Buyer[Id](5, "叉烧包", LocalDate.parse("1923-11-12"))
    )
    ```



### Returning.update.single



```scala
Buyer
  .update(_.name `=` "James Bond")
  .set(_.dateOfBirth := LocalDate.parse("2019-04-07"))
  .returning(_.id)
```


*
    ```sql
    UPDATE buyer SET date_of_birth = ? WHERE (buyer.name = ?) RETURNING buyer.id AS res
    ```



*
    ```scala
    Seq(1)
    ```



----



```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```




*
    ```scala
    Seq(LocalDate.parse("2019-04-07"))
    ```



### Returning.update.multiple



```scala
Buyer
  .update(_.name `=` "James Bond")
  .set(_.dateOfBirth := LocalDate.parse("2019-04-07"), _.name := "John Dee")
  .returning(c => (c.id, c.name, c.dateOfBirth))
```


*
    ```sql
    UPDATE buyer
    SET date_of_birth = ?, name = ? WHERE (buyer.name = ?)
    RETURNING buyer.id AS res__0, buyer.name AS res__1, buyer.date_of_birth AS res__2
    ```



*
    ```scala
    Seq((1, "John Dee", LocalDate.parse("2019-04-07")))
    ```



### Returning.delete



```scala
Purchase.delete(_.shippingInfoId `=` 1).returning(_.total)
```


*
    ```sql
    DELETE FROM purchase WHERE (purchase.shipping_info_id = ?) RETURNING purchase.total AS res
    ```



*
    ```scala
    Seq(888.0, 900.0, 15.7)
    ```



----



```scala
Purchase.select
```




*
    ```scala
    Seq(
      // id=1,2,3 had shippingInfoId=1 and thus got deleted
      Purchase[Id](id = 4, shippingInfoId = 2, productId = 4, count = 4, total = 493.8),
      Purchase[Id](id = 5, shippingInfoId = 2, productId = 5, count = 10, total = 10000.0),
      Purchase[Id](id = 6, shippingInfoId = 3, productId = 1, count = 5, total = 44.4),
      Purchase[Id](id = 7, shippingInfoId = 3, productId = 6, count = 13, total = 1.3)
    )
    ```



## OnConflict
Queries using `ON CONFLICT DO UPDATE` or `ON CONFLICT DO NOTHING`
### OnConflict.ignore

ScalaSql's `.onConflictIgnore` translates into SQL's `ON CONFLICT DO NOTHING`

Note that H2 and HsqlDb do not support `onConflictIgnore` and `onConflictUpdate`, while
MySql only supports `onConflictUpdate` but not `onConflictIgnore`.

```scala
Buyer.insert
  .columns(
    _.name := "test buyer",
    _.dateOfBirth := LocalDate.parse("2023-09-09"),
    _.id := 1 // This should cause a primary key conflict
  )
  .onConflictIgnore(_.id)
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING
    ```



*
    ```scala
    0
    ```



### OnConflict.ignore.returningEmpty



```scala
Buyer.insert
  .columns(
    _.name := "test buyer",
    _.dateOfBirth := LocalDate.parse("2023-09-09"),
    _.id := 1 // This should cause a primary key conflict
  )
  .onConflictIgnore(_.id)
  .returning(_.name)
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)
    ON CONFLICT (id) DO NOTHING
    RETURNING buyer.name AS res
    ```



*
    ```scala
    Seq.empty[String]
    ```



### OnConflict.ignore.returningOne



```scala
Buyer.insert
  .columns(
    _.name := "test buyer",
    _.dateOfBirth := LocalDate.parse("2023-09-09"),
    _.id := 4 // This should cause a primary key conflict
  )
  .onConflictIgnore(_.id)
  .returning(_.name)
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)
    ON CONFLICT (id) DO NOTHING
    RETURNING buyer.name AS res
    ```



*
    ```scala
    Seq("test buyer")
    ```



### OnConflict.update

ScalaSql's `.onConflictUpdate` translates into SQL's `ON CONFLICT DO UPDATE`

```scala
Buyer.insert
  .columns(
    _.name := "test buyer",
    _.dateOfBirth := LocalDate.parse("2023-09-09"),
    _.id := 1 // This should cause a primary key conflict
  )
  .onConflictUpdate(_.id)(_.name := "TEST BUYER CONFLICT")
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET name = ?
    ```



*
    ```scala
    1
    ```



----



```scala
Buyer.select
```




*
    ```scala
    Seq(
      Buyer[Id](1, "TEST BUYER CONFLICT", LocalDate.parse("2001-02-03")),
      Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
      Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
    )
    ```



### OnConflict.computed



```scala
Buyer.insert
  .columns(
    _.name := "test buyer",
    _.dateOfBirth := LocalDate.parse("2023-09-09"),
    _.id := 1 // This should cause a primary key conflict
  )
  .onConflictUpdate(_.id)(v => v.name := v.name.toUpperCase)
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET name = UPPER(buyer.name)
    ```



*
    ```scala
    1
    ```



----



```scala
Buyer.select
```




*
    ```scala
    Seq(
      Buyer[Id](1, "JAMES BOND", LocalDate.parse("2001-02-03")),
      Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
      Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
    )
    ```



### OnConflict.returning



```scala
Buyer.insert
  .columns(
    _.name := "test buyer",
    _.dateOfBirth := LocalDate.parse("2023-09-09"),
    _.id := 1 // This should cause a primary key conflict
  )
  .onConflictUpdate(_.id)(v => v.name := v.name.toUpperCase)
  .returning(_.name)
  .single
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)
    ON CONFLICT (id) DO UPDATE
    SET name = UPPER(buyer.name)
    RETURNING buyer.name AS res
    ```



*
    ```scala
    "JAMES BOND"
    ```



## Values
Basic `VALUES` operations
### Values.basic

You can use `Values` to generate a SQL `VALUES` clause

```scala
db.values(Seq(1, 2, 3))
```


*
    ```sql
    VALUES (?), (?), (?)
    ```



*
    ```scala
    Seq(1, 2, 3)
    ```



### Values.contains

`Values` supports `.contains`

```scala
db.values(Seq(1, 2, 3)).contains(1)
```


*
    ```sql
    SELECT (? IN (VALUES (?), (?), (?))) AS res
    ```



*
    ```scala
    true
    ```



### Values.max

`Values` supports aggregate functions like `.max`

```scala
db.values(Seq(1, 2, 3)).max
```


*
    ```sql
    SELECT MAX(subquery0.column1) AS res FROM (VALUES (?), (?), (?)) subquery0
    ```



*
    ```scala
    3
    ```



### Values.map

`Values` supports most `.select` operators like `.map`, `.filter`, `.crossJoin`, and so on

```scala
db.values(Seq(1, 2, 3)).map(_ + 1)
```


*
    ```sql
    SELECT (subquery0.column1 + ?) AS res FROM (VALUES (?), (?), (?)) subquery0
    ```



*
    ```scala
    Seq(2, 3, 4)
    ```



### Values.filter



```scala
db.values(Seq(1, 2, 3)).filter(_ > 2)
```


*
    ```sql
    SELECT subquery0.column1 AS res FROM (VALUES (?), (?), (?)) subquery0 WHERE (subquery0.column1 > ?)
    ```



*
    ```scala
    Seq(3)
    ```



### Values.crossJoin



```scala
db.values(Seq(1, 2, 3)).crossJoin(db.values(Seq(4, 5, 6))).map {
  case (a, b) => (a * 10 + b)
}
```


*
    ```sql
    SELECT ((subquery0.column1 * ?) + subquery1.column1) AS res
    FROM (VALUES (?), (?), (?)) subquery0
    CROSS JOIN (VALUES (?), (?), (?)) subquery1
    ```



*
    ```scala
    Seq(14, 15, 16, 24, 25, 26, 34, 35, 36)
    ```



### Values.joinValuesAndTable

You can also mix `values` calls and normal `selects` in the same query, e.g. with joins

```scala
for {
  name <- db.values(Seq("Socks", "Face Mask", "Camera"))
  product <- Product.join(_.name === name)
} yield (name, product.price)
```


*
    ```sql
    SELECT subquery0.column1 AS res__0, product1.price AS res__1
    FROM (VALUES (?), (?), (?)) subquery0
    JOIN product product1 ON (product1.name = subquery0.column1)
    ```



*
    ```scala
    Seq(("Socks", 3.14), ("Face Mask", 8.88), ("Camera", 1000.0))
    ```



## LateralJoin

    `JOIN LATERAL`, for the databases that support it. This allows you to use the
    expressions defined in tables on the left-hand-side of the join in a
    subquery on the right-hand-side of the join, v.s. normal `JOIN`s which only
    allow you to use left-hand-side expressions in the `ON` expression but not
    in the `FROM` subquery.
  
### LateralJoin.crossJoinLateral



```scala
Buyer.select
  .crossJoinLateral(b => ShippingInfo.select.filter { s => b.id `=` s.buyerId })
  .map { case (b, s) => (b.name, s.shippingDate) }
```


*
    ```sql
    SELECT buyer0.name AS res__0, subquery1.res__shipping_date AS res__1
    FROM buyer buyer0
    CROSS JOIN LATERAL (SELECT shipping_info1.shipping_date AS res__shipping_date
      FROM shipping_info shipping_info1
      WHERE (buyer0.id = shipping_info1.buyer_id)) subquery1
    ```



*
    ```scala
    Seq(
      ("James Bond", LocalDate.parse("2012-04-05")),
      ("叉烧包", LocalDate.parse("2010-02-03")),
      ("叉烧包", LocalDate.parse("2012-05-06"))
    )
    ```



### LateralJoin.crossJoinLateralFor



```scala
for {
  b <- Buyer.select
  s <- ShippingInfo.select.filter { s => b.id `=` s.buyerId }.crossJoinLateral()
} yield (b.name, s.shippingDate)
```


*
    ```sql
    SELECT buyer0.name AS res__0, subquery1.res__shipping_date AS res__1
    FROM buyer buyer0
    CROSS JOIN LATERAL (SELECT shipping_info1.shipping_date AS res__shipping_date
      FROM shipping_info shipping_info1
      WHERE (buyer0.id = shipping_info1.buyer_id)) subquery1
    ```



*
    ```scala
    Seq(
      ("James Bond", LocalDate.parse("2012-04-05")),
      ("叉烧包", LocalDate.parse("2010-02-03")),
      ("叉烧包", LocalDate.parse("2012-05-06"))
    )
    ```



### LateralJoin.joinLateral



```scala
Buyer.select
  .joinLateral(b => ShippingInfo.select.filter { s => b.id `=` s.buyerId })((_, _) => true)
  .map { case (b, s) => (b.name, s.shippingDate) }
```


*
    ```sql
    SELECT buyer0.name AS res__0, subquery1.res__shipping_date AS res__1
    FROM buyer buyer0
    JOIN LATERAL (SELECT shipping_info1.shipping_date AS res__shipping_date
      FROM shipping_info shipping_info1
      WHERE (buyer0.id = shipping_info1.buyer_id)) subquery1
      ON ?
    ```



*
    ```scala
    Seq(
      ("James Bond", LocalDate.parse("2012-04-05")),
      ("叉烧包", LocalDate.parse("2010-02-03")),
      ("叉烧包", LocalDate.parse("2012-05-06"))
    )
    ```



### LateralJoin.joinLateralFor



```scala
for {
  b <- Buyer.select
  s <- ShippingInfo.select.filter { s => b.id `=` s.buyerId }.joinLateral(_ => Expr(true))
} yield (b.name, s.shippingDate)
```


*
    ```sql
    SELECT buyer0.name AS res__0, subquery1.res__shipping_date AS res__1
    FROM buyer buyer0
    JOIN LATERAL (SELECT shipping_info1.shipping_date AS res__shipping_date
      FROM shipping_info shipping_info1
      WHERE (buyer0.id = shipping_info1.buyer_id)) subquery1
    ON ?
    ```



*
    ```scala
    Seq(
      ("James Bond", LocalDate.parse("2012-04-05")),
      ("叉烧包", LocalDate.parse("2010-02-03")),
      ("叉烧包", LocalDate.parse("2012-05-06"))
    )
    ```



### LateralJoin.leftJoin

ScalaSql supports `LEFT JOIN`s, `RIGHT JOIN`s and `OUTER JOIN`s via the
`.leftJoin`/`.rightJoin`/`.outerJoin` methods

```scala
Buyer.select.leftJoinLateral(b => ShippingInfo.select.filter(b.id `=` _.buyerId))((_, _) =>
  Expr(true)
)
```


*
    ```sql
    SELECT
      buyer0.id AS res__0__id,
      buyer0.name AS res__0__name,
      buyer0.date_of_birth AS res__0__date_of_birth,
      subquery1.res__id AS res__1__id,
      subquery1.res__buyer_id AS res__1__buyer_id,
      subquery1.res__shipping_date AS res__1__shipping_date
    FROM buyer buyer0
    LEFT JOIN LATERAL (SELECT
        shipping_info1.id AS res__id,
        shipping_info1.buyer_id AS res__buyer_id,
        shipping_info1.shipping_date AS res__shipping_date
      FROM shipping_info shipping_info1
      WHERE (buyer0.id = shipping_info1.buyer_id)) subquery1 ON ?
    ```



*
    ```scala
    Seq(
      (
        Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
        Some(ShippingInfo[Id](2, 1, LocalDate.parse("2012-04-05")))
      ),
      (
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
        Some(ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03")))
      ),
      (
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
        Some(ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06")))
      ),
      (Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")), None)
    )
    ```



### LateralJoin.leftJoinFor

ScalaSql supports `LEFT JOIN`s, `RIGHT JOIN`s and `OUTER JOIN`s via the
`.leftJoin`/`.rightJoin`/`.outerJoin` methods

```scala
for {
  b <- Buyer.select
  s <- ShippingInfo.select.filter(b.id `=` _.buyerId).leftJoinLateral(_ => Expr(true))
} yield (b, s)
```


*
    ```sql
    SELECT
      buyer0.id AS res__0__id,
      buyer0.name AS res__0__name,
      buyer0.date_of_birth AS res__0__date_of_birth,
      subquery1.res__id AS res__1__id,
      subquery1.res__buyer_id AS res__1__buyer_id,
      subquery1.res__shipping_date AS res__1__shipping_date
    FROM buyer buyer0
    LEFT JOIN LATERAL (SELECT
        shipping_info1.id AS res__id,
        shipping_info1.buyer_id AS res__buyer_id,
        shipping_info1.shipping_date AS res__shipping_date
      FROM shipping_info shipping_info1
      WHERE (buyer0.id = shipping_info1.buyer_id)) subquery1 ON ?
    ```



*
    ```scala
    Seq(
      (
        Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
        Some(ShippingInfo[Id](2, 1, LocalDate.parse("2012-04-05")))
      ),
      (
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
        Some(ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03")))
      ),
      (
        Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
        Some(ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06")))
      ),
      (Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")), None)
    )
    ```



## WindowFunction
Window functions using `OVER`
### WindowFunction.simple.rank

Window functions like `rank()` are supported. You can use the `.over`, `.partitionBy`,
and `.sortBy`

```scala
Purchase.select.map(p =>
  (
    p.shippingInfoId,
    p.total,
    db.rank().over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      RANK() OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq[(Int, Double, Int)](
      (1, 15.7, 1),
      (1, 888.0, 2),
      (1, 900.0, 3),
      (2, 493.8, 1),
      (2, 10000.0, 2),
      (3, 1.3, 1),
      (3, 44.4, 2)
    )
    ```



### WindowFunction.simple.rowNumber



```scala
Purchase.select.map(p =>
  (
    p.shippingInfoId,
    p.total,
    db.rowNumber().over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      ROW_NUMBER() OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq[(Int, Double, Int)](
      (1, 15.7, 1),
      (1, 888.0, 2),
      (1, 900.0, 3),
      (2, 493.8, 1),
      (2, 10000.0, 2),
      (3, 1.3, 1),
      (3, 44.4, 2)
    )
    ```



### WindowFunction.simple.denseRank



```scala
Purchase.select.map(p =>
  (
    p.shippingInfoId,
    p.total,
    db.denseRank().over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      DENSE_RANK() OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq[(Int, Double, Int)](
      (1, 15.7, 1),
      (1, 888.0, 2),
      (1, 900.0, 3),
      (2, 493.8, 1),
      (2, 10000.0, 2),
      (3, 1.3, 1),
      (3, 44.4, 2)
    )
    ```



----



```scala
Purchase.select.map(p =>
  (
    p.shippingInfoId,
    p.total,
    db.denseRank().over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      DENSE_RANK() OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq[(Int, Double, Int)](
      (1, 15.7, 1),
      (1, 888.0, 2),
      (1, 900.0, 3),
      (2, 493.8, 1),
      (2, 10000.0, 2),
      (3, 1.3, 1),
      (3, 44.4, 2)
    )
    ```



### WindowFunction.simple.percentRank



```scala
Purchase.select.map(p =>
  (
    p.shippingInfoId,
    p.total,
    db.percentRank().over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      PERCENT_RANK() OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq[(Int, Double, Double)](
      (1, 15.7, 0.0),
      (1, 888.0, 0.5),
      (1, 900.0, 1.0),
      (2, 493.8, 0.0),
      (2, 10000.0, 1.0),
      (3, 1.3, 0.0),
      (3, 44.4, 1.0)
    )
    ```



### WindowFunction.simple.cumeDist



```scala
Purchase.select.map(p =>
  (
    p.shippingInfoId,
    p.total,
    db.cumeDist().over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      CUME_DIST() OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq[(Int, Double, Double)](
      (1, 15.7, 0.3333333333333333),
      (1, 888.0, 0.6666666666666666),
      (1, 900.0, 1.0),
      (2, 493.8, 0.5),
      (2, 10000.0, 1.0),
      (3, 1.3, 0.5),
      (3, 44.4, 1.0)
    )
    ```



### WindowFunction.simple.ntile



```scala
Purchase.select.map(p =>
  (
    p.shippingInfoId,
    p.total,
    db.ntile(3).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      NTILE(?) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq[(Int, Double, Int)](
      (1, 15.7, 1),
      (1, 888.0, 2),
      (1, 900.0, 3),
      (2, 493.8, 1),
      (2, 10000.0, 2),
      (3, 1.3, 1),
      (3, 44.4, 2)
    )
    ```



### WindowFunction.simple.lag



```scala
Purchase.select.map(p =>
  (
    p.shippingInfoId,
    p.total,
    db.lag(p.total, 1, -1.0).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      LAG(purchase0.total, ?, ?) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq[(Int, Double, Double)](
      (1, 15.7, -1.0),
      (1, 888.0, 15.7),
      (1, 900.0, 888.0),
      (2, 493.8, -1.0),
      (2, 10000.0, 493.8),
      (3, 1.3, -1.0),
      (3, 44.4, 1.3)
    )
    ```



### WindowFunction.simple.lead



```scala
Purchase.select.map(p =>
  (
    p.shippingInfoId,
    p.total,
    db.lead(p.total, 1, -1.0).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      LEAD(purchase0.total, ?, ?) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq[(Int, Double, Double)](
      (1, 15.7, 888.0),
      (1, 888.0, 900.0),
      (1, 900.0, -1.0),
      (2, 493.8, 10000.0),
      (2, 10000.0, -1.0),
      (3, 1.3, 44.4),
      (3, 44.4, -1.0)
    )
    ```



### WindowFunction.simple.firstValue



```scala
Purchase.select.map(p =>
  (
    p.shippingInfoId,
    p.total,
    db.firstValue(p.total).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      FIRST_VALUE(purchase0.total) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq[(Int, Double, Double)](
      (1, 15.7, 15.7),
      (1, 888.0, 15.7),
      (1, 900.0, 15.7),
      (2, 493.8, 493.8),
      (2, 10000.0, 493.8),
      (3, 1.3, 1.3),
      (3, 44.4, 1.3)
    )
    ```



### WindowFunction.simple.lastValue



```scala
Purchase.select.map(p =>
  (
    p.shippingInfoId,
    p.total,
    db.lastValue(p.total).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      LAST_VALUE(purchase0.total) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq[(Int, Double, Double)](
      (1, 15.7, 15.7),
      (1, 888.0, 888.0),
      (1, 900.0, 900.0),
      (2, 493.8, 493.8),
      (2, 10000.0, 10000.0),
      (3, 1.3, 1.3),
      (3, 44.4, 44.4)
    )
    ```



### WindowFunction.simple.nthValue



```scala
Purchase.select.map(p =>
  (
    p.shippingInfoId,
    p.total,
    db.nthValue(p.total, 2).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      NTH_VALUE(purchase0.total, ?) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq[(Int, Double, Double)](
      (1, 15.7, 0.0),
      (1, 888.0, 888.0),
      (1, 900.0, 888.0),
      (2, 493.8, 0.0),
      (2, 10000.0, 10000.0),
      (3, 1.3, 0.0),
      (3, 44.4, 44.4)
    )
    ```



### WindowFunction.aggregate.sumBy

You can use `.mapAggregate` to use aggregate functions as window function

```scala
Purchase.select.mapAggregate((p, ps) =>
  (
    p.shippingInfoId,
    p.total,
    ps.sumBy(_.total).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      SUM(purchase0.total) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq(
      (1, 15.7, 15.7),
      (1, 888.0, 903.7),
      (1, 900.0, 1803.7),
      (2, 493.8, 493.8),
      (2, 10000.0, 10493.8),
      (3, 1.3, 1.3),
      (3, 44.4, 45.699999999999996)
    )
    ```



### WindowFunction.aggregate.avgBy

Window functions like `rank()` are supported. You can use the `.over`, `.partitionBy`,
and `.sortBy`

```scala
Purchase.select.mapAggregate((p, ps) =>
  (
    p.shippingInfoId,
    p.total,
    ps.avgBy(_.total).over.partitionBy(p.shippingInfoId).sortBy(p.total).asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      AVG(purchase0.total) OVER (PARTITION BY purchase0.shipping_info_id ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq(
      (1, 15.7, 15.7),
      (1, 888.0, 451.85),
      (1, 900.0, 601.2333333333333),
      (2, 493.8, 493.8),
      (2, 10000.0, 5246.9),
      (3, 1.3, 1.3),
      (3, 44.4, 22.849999999999998)
    )
    ```



### WindowFunction.frames

You can have further control over the window function call via `.frameStart`,
`.frameEnd`, `.exclude`

```scala
Purchase.select.mapAggregate((p, ps) =>
  (
    p.shippingInfoId,
    p.total,
    ps.sumBy(_.total)
      .over
      .partitionBy(p.shippingInfoId)
      .sortBy(p.total)
      .asc
      .frameStart
      .preceding()
      .frameEnd
      .following()
      .exclude
      .currentRow
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      SUM(purchase0.total)
      OVER (PARTITION BY purchase0.shipping_info_id
        ORDER BY purchase0.total ASC
        ROWS BETWEEN UNBOUNDED PRECEDING
        AND UNBOUNDED FOLLOWING EXCLUDE CURRENT ROW) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq[(Int, Double, Double)](
      (1, 15.7, 1788.0),
      (1, 888.0, 915.7),
      (1, 900.0, 903.7),
      (2, 493.8, 10000.0),
      (2, 10000.0, 493.8),
      (3, 1.3, 44.4),
      (3, 44.4, 1.3)
    )
    ```



### WindowFunction.filter

ScalaSql allows `.filter` to be used after `over` to add a SQL `FILTER` clause
to your window function call, allowing you to exclude certain rows from the
window.

```scala
Purchase.select.mapAggregate((p, ps) =>
  (
    p.shippingInfoId,
    p.total,
    ps.sumBy(_.total)
      .over
      .filter(p.total > 100)
      .partitionBy(p.shippingInfoId)
      .sortBy(p.total)
      .asc
  )
)
```


*
    ```sql
    SELECT
      purchase0.shipping_info_id AS res__0,
      purchase0.total AS res__1,
      SUM(purchase0.total)
        FILTER (WHERE (purchase0.total > ?))
        OVER (PARTITION BY purchase0.shipping_info_id
          ORDER BY purchase0.total ASC) AS res__2
    FROM purchase purchase0
    ```



*
    ```scala
    Seq[(Int, Double, Double)](
      (1, 15.7, 0.0),
      (1, 888.0, 888.0),
      (1, 900.0, 1788.0),
      (2, 493.8, 493.8),
      (2, 10000.0, 10493.8),
      (3, 1.3, 0.0),
      (3, 44.4, 0.0)
    )
    ```



## SubQuery
Queries that explicitly use subqueries (e.g. for `JOIN`s) or require subqueries to preserve the Scala semantics of the various operators
### SubQuery.sortTakeJoin

A ScalaSql `.join` referencing a `.select` translates straightforwardly
into a SQL `JOIN` on a subquery

```scala
Purchase.select
  .join(Product.select.sortBy(_.price).desc.take(1))(_.productId `=` _.id)
  .map { case (purchase, product) => purchase.total }
```


*
    ```sql
    SELECT purchase0.total AS res
    FROM purchase purchase0
    JOIN (SELECT product1.id AS res__id, product1.price AS res__price
      FROM product product1
      ORDER BY res__price DESC
      LIMIT ?) subquery1
    ON (purchase0.product_id = subquery1.res__id)
    ```



*
    ```scala
    Seq(10000.0)
    ```



### SubQuery.sortTakeFrom

Some sequences of operations cannot be expressed as a single SQL query,
and thus translate into an outer query wrapping a subquery inside the `FROM`.
An example of this is performing a `.join` after a `.take`: SQL does not
allow you to put `JOIN`s after `LIMIT`s, and so the only way to write this
in SQL is as a subquery.

```scala
Product.select.sortBy(_.price).desc.take(1).join(Purchase)(_.id `=` _.productId).map {
  case (product, purchase) => purchase.total
}
```


*
    ```sql
    SELECT purchase1.total AS res
    FROM (SELECT product0.id AS res__id, product0.price AS res__price
      FROM product product0
      ORDER BY res__price DESC
      LIMIT ?) subquery0
    JOIN purchase purchase1 ON (subquery0.res__id = purchase1.product_id)
    ```



*
    ```scala
    Seq(10000.0)
    ```



### SubQuery.sortTakeFromAndJoin

This example shows a ScalaSql query that results in a subquery in both
the `FROM` and the `JOIN` clause of the generated SQL query.

```scala
Product.select
  .sortBy(_.price)
  .desc
  .take(3)
  .join(Purchase.select.sortBy(_.count).desc.take(3))(_.id `=` _.productId)
  .map { case (product, purchase) => (product.name, purchase.count) }
```


*
    ```sql
    SELECT
      subquery0.res__name AS res__0,
      subquery1.res__count AS res__1
    FROM (SELECT
        product0.id AS res__id,
        product0.name AS res__name,
        product0.price AS res__price
      FROM product product0
      ORDER BY res__price DESC
      LIMIT ?) subquery0
    JOIN (SELECT
        purchase1.product_id AS res__product_id,
        purchase1.count AS res__count
      FROM purchase purchase1
      ORDER BY res__count DESC
      LIMIT ?) subquery1
    ON (subquery0.res__id = subquery1.res__product_id)
    ```



*
    ```scala
    Seq(("Camera", 10))
    ```



### SubQuery.sortLimitSortLimit

Performing multiple sorts with `.take`s in between is also something
that requires subqueries, as a single query only allows a single `LIMIT`
clause after the `ORDER BY`

```scala
Product.select.sortBy(_.price).desc.take(4).sortBy(_.price).asc.take(2).map(_.name)
```


*
    ```sql
    SELECT subquery0.res__name AS res
    FROM (SELECT
        product0.name AS res__name,
        product0.price AS res__price
      FROM product product0
      ORDER BY res__price DESC
      LIMIT ?) subquery0
    ORDER BY subquery0.res__price ASC
    LIMIT ?
    ```



*
    ```scala
    Seq("Face Mask", "Skate Board")
    ```



### SubQuery.sortGroupBy



```scala
Purchase.select.sortBy(_.count).take(5).groupBy(_.productId)(_.sumBy(_.total))
```


*
    ```sql
    SELECT subquery0.res__product_id AS res__0, SUM(subquery0.res__total) AS res__1
    FROM (SELECT
        purchase0.product_id AS res__product_id,
        purchase0.count AS res__count,
        purchase0.total AS res__total
      FROM purchase purchase0
      ORDER BY res__count
      LIMIT ?) subquery0
    GROUP BY subquery0.res__product_id
    ```



*
    ```scala
    Seq((1, 44.4), (2, 900.0), (3, 15.7), (4, 493.8), (5, 10000.0))
    ```



### SubQuery.groupByJoin



```scala
Purchase.select.groupBy(_.productId)(_.sumBy(_.total)).join(Product)(_._1 `=` _.id).map {
  case (productId, total, product) => (product.name, total)
}
```


*
    ```sql
    SELECT
      product1.name AS res__0,
      subquery0.res__1 AS res__1
    FROM (SELECT
        purchase0.product_id AS res__0,
        SUM(purchase0.total) AS res__1
      FROM purchase purchase0
      GROUP BY purchase0.product_id) subquery0
    JOIN product product1 ON (subquery0.res__0 = product1.id)
    ```



*
    ```scala
    Seq(
      ("Camera", 10000.0),
      ("Cookie", 1.3),
      ("Face Mask", 932.4),
      ("Guitar", 900.0),
      ("Skate Board", 493.8),
      ("Socks", 15.7)
    )
    ```



### SubQuery.subqueryInFilter

You can use `.select`s and aggregate operations like `.size` anywhere an expression is
expected; these translate into SQL subqueries as expressions. SQL
subqueries-as-expressions require that the subquery returns exactly 1 row and 1 column,
which is something the aggregate operation (in this case `.sum`/`COUNT(1)`) helps us
ensure. Here, we do subquery in a `.filter`/`WHERE`.

```scala
Buyer.select.filter(c => ShippingInfo.select.filter(p => c.id `=` p.buyerId).size `=` 0)
```


*
    ```sql
    SELECT
      buyer0.id AS res__id,
      buyer0.name AS res__name,
      buyer0.date_of_birth AS res__date_of_birth
    FROM buyer buyer0
    WHERE ((SELECT
        COUNT(1) AS res
        FROM shipping_info shipping_info1
        WHERE (buyer0.id = shipping_info1.buyer_id)) = ?)
    ```



*
    ```scala
    Seq(Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")))
    ```



### SubQuery.subqueryInMap

Similar to the above example, but we do the subquery/aggregate in
a `.map` instead of a `.filter`

```scala
Buyer.select.map(c => (c, ShippingInfo.select.filter(p => c.id `=` p.buyerId).size))
```


*
    ```sql
    SELECT
      buyer0.id AS res__0__id,
      buyer0.name AS res__0__name,
      buyer0.date_of_birth AS res__0__date_of_birth,
      (SELECT COUNT(1) AS res
        FROM shipping_info shipping_info1
        WHERE (buyer0.id = shipping_info1.buyer_id)) AS res__1
    FROM buyer buyer0
    ```



*
    ```scala
    Seq(
      (Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")), 1),
      (Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")), 2),
      (Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")), 0)
    )
    ```



### SubQuery.subqueryInMapNested



```scala
Buyer.select.map(c => (c, ShippingInfo.select.filter(p => c.id `=` p.buyerId).size `=` 1))
```


*
    ```sql
    SELECT
      buyer0.id AS res__0__id,
      buyer0.name AS res__0__name,
      buyer0.date_of_birth AS res__0__date_of_birth,
      ((SELECT
        COUNT(1) AS res
        FROM shipping_info shipping_info1
        WHERE (buyer0.id = shipping_info1.buyer_id)) = ?) AS res__1
    FROM buyer buyer0
    ```



*
    ```scala
    Seq(
      (Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")), true),
      (Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")), false),
      (Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")), false)
    )
    ```



### SubQuery.selectLimitUnionSelect



```scala
Buyer.select
  .map(_.name.toLowerCase)
  .take(2)
  .unionAll(Product.select.map(_.kebabCaseName.toLowerCase))
```


*
    ```sql
    SELECT subquery0.res AS res
    FROM (SELECT
        LOWER(buyer0.name) AS res
      FROM buyer buyer0
      LIMIT ?) subquery0
    UNION ALL
    SELECT LOWER(product0.kebab_case_name) AS res
    FROM product product0
    ```



*
    ```scala
    Seq("james bond", "叉烧包", "face-mask", "guitar", "socks", "skate-board", "camera", "cookie")
    ```



### SubQuery.selectUnionSelectLimit



```scala
Buyer.select
  .map(_.name.toLowerCase)
  .unionAll(Product.select.map(_.kebabCaseName.toLowerCase).take(2))
```


*
    ```sql
    SELECT LOWER(buyer0.name) AS res
    FROM buyer buyer0
    UNION ALL
    SELECT subquery0.res AS res
    FROM (SELECT
        LOWER(product0.kebab_case_name) AS res
      FROM product product0
      LIMIT ?) subquery0
    ```



*
    ```scala
    Seq("james bond", "叉烧包", "li haoyi", "face-mask", "guitar")
    ```



### SubQuery.exceptAggregate



```scala
Product.select
  .map(p => (p.name.toLowerCase, p.price))
  // `p.name.toLowerCase` and  `p.kebabCaseName.toLowerCase` are not eliminated, because
  // they are important to the semantics of EXCEPT (and other non-UNION-ALL operators)
  .except(Product.select.map(p => (p.kebabCaseName.toLowerCase, p.price)))
  .aggregate(ps => (ps.maxBy(_._2), ps.minBy(_._2)))
```


*
    ```sql
    SELECT
      MAX(subquery0.res__1) AS res__0,
      MIN(subquery0.res__1) AS res__1
    FROM (SELECT
        LOWER(product0.name) AS res__0,
        product0.price AS res__1
      FROM product product0
      EXCEPT
      SELECT
        LOWER(product0.kebab_case_name) AS res__0,
        product0.price AS res__1
      FROM product product0) subquery0
    ```



*
    ```scala
    (123.45, 8.88)
    ```



### SubQuery.unionAllAggregate



```scala
Product.select
  .map(p => (p.name.toLowerCase, p.price))
  // `p.name.toLowerCase` and  `p.kebabCaseName.toLowerCase` get eliminated,
  // as they are not selected by the enclosing query, and cannot affect the UNION ALL
  .unionAll(Product.select.map(p => (p.kebabCaseName.toLowerCase, p.price)))
  .aggregate(ps => (ps.maxBy(_._2), ps.minBy(_._2)))
```


*
    ```sql
    SELECT
      MAX(subquery0.res__1) AS res__0,
      MIN(subquery0.res__1) AS res__1
    FROM (SELECT product0.price AS res__1
      FROM product product0
      UNION ALL
      SELECT product0.price AS res__1
      FROM product product0) subquery0
    ```



*
    ```scala
    (1000.0, 0.1)
    ```



### SubQuery.deeplyNested

Subqueries can be arbitrarily nested. This example traverses four tables
to find the price of the most expensive product bought by each Buyer, but
instead of using `JOIN`s it uses subqueries nested 4 layers deep. While this
example is contrived, it demonstrates how nested ScalaSql `.select` calls
translate directly into nested SQL subqueries.

To turn the ScalaSql `Select[T]` into an `Expr[T]`, you can either use
an aggregate method like `.sumBy(...): Expr[Int]` that generates a `SUM(...)`
aggregate, or via the `.toExpr` method that leaves the subquery untouched.
SQL requires that subqueries used as expressions must return a single row
and single column, and if the query returns some other number of rows/columns
most databases will throw an exception, though some like Sqlite will pick
the first row/column arbitrarily.

```scala
Buyer.select.map { buyer =>
  buyer.name ->
    ShippingInfo.select
      .filter(_.buyerId === buyer.id)
      .map { shippingInfo =>
        Purchase.select
          .filter(_.shippingInfoId === shippingInfo.id)
          .map { purchase =>
            Product.select
              .filter(_.id === purchase.productId)
              .map(_.price)
              .sorted
              .desc
              .take(1)
              .toExpr
          }
          .sorted
          .desc
          .take(1)
          .toExpr
      }
      .sorted
      .desc
      .take(1)
      .toExpr
}
```


*
    ```sql
    SELECT
      buyer0.name AS res__0,
      (SELECT
        (SELECT
          (SELECT product3.price AS res
          FROM product product3
          WHERE (product3.id = purchase2.product_id)
          ORDER BY res DESC
          LIMIT ?) AS res
        FROM purchase purchase2
        WHERE (purchase2.shipping_info_id = shipping_info1.id)
        ORDER BY res DESC
        LIMIT ?) AS res
      FROM shipping_info shipping_info1
      WHERE (shipping_info1.buyer_id = buyer0.id)
      ORDER BY res DESC
      LIMIT ?) AS res__1
    FROM buyer buyer0
    ```



*
    ```scala
    Seq(
      ("James Bond", 1000.0),
      ("叉烧包", 300.0),
      ("Li Haoyi", 0.0)
    )
    ```



## WithCte
Basic `WITH`/Common-Table-Expression operations
### WithCte.simple

ScalaSql supports `WITH`-clauses, also known as "Common Table Expressions"
(CTEs), via the `.withCte` syntax.

```scala
db.withCte(Buyer.select.map(_.name)) { bs =>
  bs.map(_ + "-suffix")
}
```


*
    ```sql
    WITH cte0 (res) AS (SELECT buyer0.name AS res FROM buyer buyer0)
    SELECT (cte0.res || ?) AS res
    FROM cte0
    ```



*
    ```scala
    Seq("James Bond-suffix", "叉烧包-suffix", "Li Haoyi-suffix")
    ```



### WithCte.multiple

Multiple `withCte` blocks can be stacked, turning into chained `WITH` clauses
in the generated SQL

```scala
db.withCte(Buyer.select) { bs =>
  db.withCte(ShippingInfo.select) { sis =>
    bs.join(sis)(_.id === _.buyerId)
      .map { case (b, s) => (b.name, s.shippingDate) }
  }
}
```


*
    ```sql
    WITH
      cte0 (res__id, res__name) AS (SELECT
        buyer0.id AS res__id, buyer0.name AS res__name FROM buyer buyer0),
      cte1 (res__buyer_id, res__shipping_date) AS (SELECT
          shipping_info1.buyer_id AS res__buyer_id,
          shipping_info1.shipping_date AS res__shipping_date
        FROM shipping_info shipping_info1)
    SELECT cte0.res__name AS res__0, cte1.res__shipping_date AS res__1
    FROM cte0
    JOIN cte1 ON (cte0.res__id = cte1.res__buyer_id)
    ```



*
    ```scala
    Seq(
      ("叉烧包", LocalDate.parse("2010-02-03")),
      ("James Bond", LocalDate.parse("2012-04-05")),
      ("叉烧包", LocalDate.parse("2012-05-06"))
    )
    ```



### WithCte.eliminated

Only the necessary columns are exported from the `WITH` clause; columns that
are un-used in the downstream `SELECT` clause are eliminated

```scala
db.withCte(Buyer.select) { bs =>
  bs.map(_.name + "-suffix")
}
```


*
    ```sql
    WITH cte0 (res__name) AS (SELECT buyer0.name AS res__name FROM buyer buyer0)
    SELECT (cte0.res__name || ?) AS res
    FROM cte0
    ```



*
    ```scala
    Seq("James Bond-suffix", "叉烧包-suffix", "Li Haoyi-suffix")
    ```



### WithCte.subquery

ScalaSql's `withCte` can be used anywhere a `.select` operator can be used. The
generated `WITH` clauses may be wrapped in sub-queries in scenarios where they
cannot be easily combined into a single query

```scala
db.withCte(Buyer.select) { bs =>
  db.withCte(ShippingInfo.select) { sis =>
    bs.join(sis)(_.id === _.buyerId)
  }
}.join(
  db.withCte(Product.select) { prs =>
    Purchase.select.join(prs)(_.productId === _.id)
  }
)(_._2.id === _._1.shippingInfoId)
  .map { case (b, s, (pu, pr)) => (b.name, pr.name) }
```


*
    ```sql
    SELECT subquery0.res__0__name AS res__0, subquery1.res__1__name AS res__1
    FROM (WITH
        cte0 (res__id, res__name)
        AS (SELECT buyer0.id AS res__id, buyer0.name AS res__name FROM buyer buyer0),
        cte1 (res__id, res__buyer_id)
        AS (SELECT shipping_info1.id AS res__id, shipping_info1.buyer_id AS res__buyer_id
          FROM shipping_info shipping_info1)
      SELECT cte0.res__name AS res__0__name, cte1.res__id AS res__1__id
      FROM cte0
      JOIN cte1 ON (cte0.res__id = cte1.res__buyer_id)) subquery0
    JOIN (WITH
        cte1 (res__id, res__name)
        AS (SELECT product1.id AS res__id, product1.name AS res__name FROM product product1)
      SELECT
        purchase2.shipping_info_id AS res__0__shipping_info_id,
        cte1.res__name AS res__1__name
      FROM purchase purchase2
      JOIN cte1 ON (purchase2.product_id = cte1.res__id)) subquery1
    ON (subquery0.res__1__id = subquery1.res__0__shipping_info_id)
    ```



*
    ```scala
    Seq[(String, String)](
      ("James Bond", "Camera"),
      ("James Bond", "Skate Board"),
      ("叉烧包", "Cookie"),
      ("叉烧包", "Face Mask"),
      ("叉烧包", "Face Mask"),
      ("叉烧包", "Guitar"),
      ("叉烧包", "Socks")
    )
    ```



## ExprOps
Operations that can be performed on `Expr[T]` for any `T`
### ExprOps.numeric.greaterThan



```scala
Expr(6) > Expr(2)
```


*
    ```sql
    SELECT (? > ?) AS res
    ```



*
    ```scala
    true
    ```



### ExprOps.numeric.lessThan



```scala
Expr(6) < Expr(2)
```


*
    ```sql
    SELECT (? < ?) AS res
    ```



*
    ```scala
    false
    ```



### ExprOps.numeric.greaterThanOrEquals



```scala
Expr(6) >= Expr(2)
```


*
    ```sql
    SELECT (? >= ?) AS res
    ```



*
    ```scala
    true
    ```



### ExprOps.numeric.lessThanOrEquals



```scala
Expr(6) <= Expr(2)
```


*
    ```sql
    SELECT (? <= ?) AS res
    ```



*
    ```scala
    false
    ```



### ExprOps.string.greaterThan



```scala
Expr("A") > Expr("B")
```


*
    ```sql
    SELECT (? > ?) AS res
    ```



*
    ```scala
    false
    ```



### ExprOps.string.lessThan



```scala
Expr("A") < Expr("B")
```


*
    ```sql
    SELECT (? < ?) AS res
    ```



*
    ```scala
    true
    ```



### ExprOps.string.greaterThanOrEquals



```scala
Expr("A") >= Expr("B")
```


*
    ```sql
    SELECT (? >= ?) AS res
    ```



*
    ```scala
    false
    ```



### ExprOps.string.lessThanOrEquals



```scala
Expr("A") <= Expr("B")
```


*
    ```sql
    SELECT (? <= ?) AS res
    ```



*
    ```scala
    true
    ```



### ExprOps.boolean.greaterThan



```scala
Expr(true) > Expr(false)
```


*
    ```sql
    SELECT (? > ?) AS res
    ```



*
    ```scala
    true
    ```



### ExprOps.boolean.lessThan



```scala
Expr(true) < Expr(true)
```


*
    ```sql
    SELECT (? < ?) AS res
    ```



*
    ```scala
    false
    ```



### ExprOps.boolean.greaterThanOrEquals



```scala
Expr(true) >= Expr(true)
```


*
    ```sql
    SELECT (? >= ?) AS res
    ```



*
    ```scala
    true
    ```



### ExprOps.boolean.lessThanOrEquals



```scala
Expr(true) <= Expr(true)
```


*
    ```sql
    SELECT (? <= ?) AS res
    ```



*
    ```scala
    true
    ```



### ExprOps.cast.byte



```scala
Expr(45.12).cast[Byte]
```


*
    ```sql
    SELECT CAST(? AS INTEGER) AS res
    ```



*
    ```scala
    45: Byte
    ```



### ExprOps.cast.short



```scala
Expr(1234.1234).cast[Short]
```


*
    ```sql
    SELECT CAST(? AS SMALLINT) AS res
    ```



*
    ```scala
    1234: Short
    ```



### ExprOps.cast.int



```scala
Expr(1234.1234).cast[Int]
```


*
    ```sql
    SELECT CAST(? AS INTEGER) AS res
    ```



*
    ```scala
    1234
    ```



### ExprOps.cast.long



```scala
Expr(1234.1234).cast[Long]
```


*
    ```sql
    SELECT CAST(? AS BIGINT) AS res
    ```



*
    ```scala
    1234L
    ```



### ExprOps.cast.string



```scala
Expr(1234.5678).cast[String]
```


*
    ```sql
    SELECT CAST(? AS VARCHAR) AS res
    ```



*
    ```scala
    "1234.5678"
    ```



### ExprOps.cast.localdate



```scala
Expr("2001-02-03").cast[java.time.LocalDate]
```


*
    ```sql
    SELECT CAST(? AS DATE) AS res
    ```



*
    ```scala
    java.time.LocalDate.parse("2001-02-03")
    ```



### ExprOps.cast.localdatetime



```scala
Expr("2023-11-12 03:22:41").cast[java.time.LocalDateTime]
```


*
    ```sql
    SELECT CAST(? AS TIMESTAMP) AS res
    ```



*
    ```scala
    java.time.LocalDateTime.parse("2023-11-12T03:22:41")
    ```



### ExprOps.cast.instant



```scala
Expr("2007-12-03 10:15:30.00").cast[java.time.Instant]
```


*
    ```sql
    SELECT CAST(? AS TIMESTAMP) AS res
    ```



*
    ```scala
    java.time.Instant.parse("2007-12-03T02:15:30.00Z")
    ```



### ExprOps.cast.castNamed



```scala
Expr(1234.5678).castNamed[String](sql"CHAR(3)")
```


*
    ```sql
    SELECT CAST(? AS CHAR(3)) AS res
    ```



*
    ```scala
    "123"
    ```



## ExprBooleanOps
Operations that can be performed on `Expr[Boolean]`
### ExprBooleanOps.and



```scala
Expr(true) && Expr(true)
```


*
    ```sql
    SELECT (? AND ?) AS res
    ```



*
    ```scala
    true
    ```



----



```scala
Expr(false) && Expr(true)
```


*
    ```sql
    SELECT (? AND ?) AS res
    ```



*
    ```scala
    false
    ```



### ExprBooleanOps.or



```scala
Expr(false) || Expr(false)
```


*
    ```sql
    SELECT (? OR ?) AS res
    ```



*
    ```scala
    false
    ```



----



```scala
!Expr(false)
```


*
    ```sql
    SELECT (NOT ?) AS res
    ```



*
    ```scala
    true
    ```



## ExprNumericOps
Operations that can be performed on `Expr[T]` when `T` is numeric
### ExprNumericOps.plus



```scala
Expr(6) + Expr(2)
```


*
    ```sql
    SELECT (? + ?) AS res
    ```



*
    ```scala
    8
    ```



### ExprNumericOps.minus



```scala
Expr(6) - Expr(2)
```


*
    ```sql
    SELECT (? - ?) AS res
    ```



*
    ```scala
    4
    ```



### ExprNumericOps.times



```scala
Expr(6) * Expr(2)
```


*
    ```sql
    SELECT (? * ?) AS res
    ```



*
    ```scala
    12
    ```



### ExprNumericOps.divide



```scala
Expr(6) / Expr(2)
```


*
    ```sql
    SELECT (? / ?) AS res
    ```



*
    ```scala
    3
    ```



### ExprNumericOps.modulo



```scala
Expr(6) % Expr(2)
```


*
    ```sql
    SELECT MOD(?, ?) AS res
    ```



*
    ```scala
    0
    ```



### ExprNumericOps.bitwiseAnd



```scala
Expr(6) & Expr(2)
```


*
    ```sql
    SELECT (? & ?) AS res
    ```



*
    ```scala
    2
    ```



### ExprNumericOps.bitwiseOr



```scala
Expr(6) | Expr(3)
```


*
    ```sql
    SELECT (? | ?) AS res
    ```



*
    ```scala
    7
    ```



### ExprNumericOps.between



```scala
Expr(4).between(Expr(2), Expr(6))
```


*
    ```sql
    SELECT ? BETWEEN ? AND ? AS res
    ```



*
    ```scala
    true
    ```



### ExprNumericOps.unaryPlus



```scala
+Expr(-4)
```


*
    ```sql
    SELECT +? AS res
    ```



*
    ```scala
    -4
    ```



### ExprNumericOps.unaryMinus



```scala
-Expr(-4)
```


*
    ```sql
    SELECT -? AS res
    ```



*
    ```scala
    4
    ```



### ExprNumericOps.unaryTilde



```scala
~Expr(-4)
```


*
    ```sql
    SELECT ~? AS res
    ```



*
    ```scala
    3
    ```



### ExprNumericOps.abs



```scala
Expr(-4).abs
```


*
    ```sql
    SELECT ABS(?) AS res
    ```



*
    ```scala
    4
    ```



### ExprNumericOps.mod



```scala
Expr(8).mod(Expr(3))
```


*
    ```sql
    SELECT MOD(?, ?) AS res
    ```



*
    ```scala
    2
    ```



### ExprNumericOps.ceil



```scala
Expr(4.3).ceil
```


*
    ```sql
    SELECT CEIL(?) AS res
    ```



*
    ```scala
    5.0
    ```



### ExprNumericOps.floor



```scala
Expr(4.7).floor
```


*
    ```sql
    SELECT FLOOR(?) AS res
    ```



*
    ```scala
    4.0
    ```



----



```scala
Expr(4.7).floor
```


*
    ```sql
    SELECT FLOOR(?) AS res
    ```



*
    ```scala
    4.0
    ```



### ExprNumericOps.precedence



```scala
(Expr(2) + Expr(3)) * Expr(4)
```


*
    ```sql
    SELECT ((? + ?) * ?) AS res
    ```



*
    ```scala
    20
    ```



## ExprSeqNumericOps
Operations that can be performed on `Expr[Seq[T]]` where `T` is numeric
### ExprSeqNumericOps.sum



```scala
Purchase.select.map(_.count).sum
```


*
    ```sql
    SELECT SUM(purchase0.count) AS res FROM purchase purchase0
    ```



*
    ```scala
    140
    ```



### ExprSeqNumericOps.min



```scala
Purchase.select.map(_.count).min
```


*
    ```sql
    SELECT MIN(purchase0.count) AS res FROM purchase purchase0
    ```



*
    ```scala
    3
    ```



### ExprSeqNumericOps.max



```scala
Purchase.select.map(_.count).max
```


*
    ```sql
    SELECT MAX(purchase0.count) AS res FROM purchase purchase0
    ```



*
    ```scala
    100
    ```



### ExprSeqNumericOps.avg



```scala
Purchase.select.map(_.count).avg
```


*
    ```sql
    SELECT AVG(purchase0.count) AS res FROM purchase purchase0
    ```



*
    ```scala
    20
    ```



## ExprSeqOps
Operations that can be performed on `Expr[Seq[_]]`
### ExprSeqOps.size



```scala
Purchase.select.size
```


*
    ```sql
    SELECT COUNT(1) AS res FROM purchase purchase0
    ```



*
    ```scala
    7
    ```



### ExprSeqOps.sumBy.simple



```scala
Purchase.select.sumBy(_.count)
```


*
    ```sql
    SELECT SUM(purchase0.count) AS res FROM purchase purchase0
    ```



*
    ```scala
    140
    ```



### ExprSeqOps.sumBy.some



```scala
Purchase.select.sumByOpt(_.count)
```


*
    ```sql
    SELECT SUM(purchase0.count) AS res FROM purchase purchase0
    ```



*
    ```scala
    Option(140)
    ```



### ExprSeqOps.sumBy.none



```scala
Purchase.select.filter(_ => false).sumByOpt(_.count)
```


*
    ```sql
    SELECT SUM(purchase0.count) AS res FROM purchase purchase0 WHERE ?
    ```



*
    ```scala
    Option.empty[Int]
    ```



### ExprSeqOps.minBy.simple



```scala
Purchase.select.minBy(_.count)
```


*
    ```sql
    SELECT MIN(purchase0.count) AS res FROM purchase purchase0
    ```



*
    ```scala
    3
    ```



### ExprSeqOps.minBy.some



```scala
Purchase.select.minByOpt(_.count)
```


*
    ```sql
    SELECT MIN(purchase0.count) AS res FROM purchase purchase0
    ```



*
    ```scala
    Option(3)
    ```



### ExprSeqOps.minBy.none



```scala
Purchase.select.filter(_ => false).minByOpt(_.count)
```


*
    ```sql
    SELECT MIN(purchase0.count) AS res FROM purchase purchase0 WHERE ?
    ```



*
    ```scala
    Option.empty[Int]
    ```



### ExprSeqOps.maxBy.simple



```scala
Purchase.select.maxBy(_.count)
```


*
    ```sql
    SELECT MAX(purchase0.count) AS res FROM purchase purchase0
    ```



*
    ```scala
    100
    ```



### ExprSeqOps.maxBy.some



```scala
Purchase.select.maxByOpt(_.count)
```


*
    ```sql
    SELECT MAX(purchase0.count) AS res FROM purchase purchase0
    ```



*
    ```scala
    Option(100)
    ```



### ExprSeqOps.maxBy.none



```scala
Purchase.select.filter(_ => false).maxByOpt(_.count)
```


*
    ```sql
    SELECT MAX(purchase0.count) AS res FROM purchase purchase0 WHERE ?
    ```



*
    ```scala
    Option.empty[Int]
    ```



### ExprSeqOps.avgBy.simple



```scala
Purchase.select.avgBy(_.count)
```


*
    ```sql
    SELECT AVG(purchase0.count) AS res FROM purchase purchase0
    ```



*
    ```scala
    20
    ```



### ExprSeqOps.avgBy.some



```scala
Purchase.select.avgByOpt(_.count)
```


*
    ```sql
    SELECT AVG(purchase0.count) AS res FROM purchase purchase0
    ```



*
    ```scala
    Option(20)
    ```



### ExprSeqOps.avgBy.none



```scala
Purchase.select.filter(_ => false).avgByOpt(_.count)
```


*
    ```sql
    SELECT AVG(purchase0.count) AS res FROM purchase purchase0 WHERE ?
    ```



*
    ```scala
    Option.empty[Int]
    ```



### ExprSeqOps.mkString.simple



```scala
Buyer.select.map(_.name).mkString()
```


*
    ```sql
    SELECT STRING_AGG(buyer0.name || '', '') AS res FROM buyer buyer0
    ```



*
    ```scala
    "James Bond叉烧包Li Haoyi"
    ```



### ExprSeqOps.mkString.sep



```scala
Buyer.select.map(_.name).mkString(", ")
```


*
    ```sql
    SELECT STRING_AGG(buyer0.name || '', ?) AS res FROM buyer buyer0
    ```



*
    ```scala
    "James Bond, 叉烧包, Li Haoyi"
    ```



## ExprStringOps
Operations that can be performed on `Expr[String]`
### ExprStringOps.plus



```scala
Expr("hello") + Expr("world")
```


*
    ```sql
    SELECT (? || ?) AS res
    ```



*
    ```scala
    "helloworld"
    ```



### ExprStringOps.like



```scala
Expr("hello").like("he%")
```


*
    ```sql
    SELECT (? LIKE ?) AS res
    ```



*
    ```scala
    true
    ```



### ExprStringOps.length



```scala
Expr("hello").length
```


*
    ```sql
    SELECT LENGTH(?) AS res
    ```



*
    ```scala
    5
    ```



### ExprStringOps.octetLength



```scala
Expr("叉烧包").octetLength
```


*
    ```sql
    SELECT OCTET_LENGTH(?) AS res
    ```



*
    ```scala
    9
    ```



### ExprStringOps.position



```scala
Expr("hello").indexOf("ll")
```


*
    ```sql
    SELECT POSITION(? IN ?) AS res
    ```



*
    ```scala
    3
    ```



### ExprStringOps.toLowerCase



```scala
Expr("Hello").toLowerCase
```


*
    ```sql
    SELECT LOWER(?) AS res
    ```



*
    ```scala
    "hello"
    ```



### ExprStringOps.trim



```scala
Expr("  Hello ").trim
```


*
    ```sql
    SELECT TRIM(?) AS res
    ```



*
    ```scala
    "Hello"
    ```



### ExprStringOps.ltrim



```scala
Expr("  Hello ").ltrim
```


*
    ```sql
    SELECT LTRIM(?) AS res
    ```



*
    ```scala
    "Hello "
    ```



### ExprStringOps.rtrim



```scala
Expr("  Hello ").rtrim
```


*
    ```sql
    SELECT RTRIM(?) AS res
    ```



*
    ```scala
    "  Hello"
    ```



### ExprStringOps.substring



```scala
Expr("Hello").substring(2, 2)
```


*
    ```sql
    SELECT SUBSTRING(?, ?, ?) AS res
    ```



*
    ```scala
    "el"
    ```



### ExprStringOps.startsWith



```scala
Expr("Hello").startsWith("Hel")
```


*
    ```sql
    SELECT (? LIKE ? || '%') AS res
    ```



*
    ```scala
    true
    ```



### ExprStringOps.endsWith



```scala
Expr("Hello").endsWith("llo")
```


*
    ```sql
    SELECT (? LIKE '%' || ?) AS res
    ```



*
    ```scala
    true
    ```



### ExprStringOps.contains



```scala
Expr("Hello").contains("ll")
```


*
    ```sql
    SELECT (? LIKE '%' || ? || '%') AS res
    ```



*
    ```scala
    true
    ```



## DataTypes
Basic operations on all the data types that ScalaSql supports mapping between Database types and Scala types
### DataTypes.constant



```scala
DataTypes.insert.columns(
  _.myTinyInt := value.myTinyInt,
  _.mySmallInt := value.mySmallInt,
  _.myInt := value.myInt,
  _.myBigInt := value.myBigInt,
  _.myDouble := value.myDouble,
  _.myBoolean := value.myBoolean,
  _.myLocalDate := value.myLocalDate,
  _.myLocalTime := value.myLocalTime,
  _.myLocalDateTime := value.myLocalDateTime,
  _.myInstant := value.myInstant,
  _.myVarBinary := value.myVarBinary,
  _.myUUID := value.myUUID,
  _.myEnum := value.myEnum
)
```




*
    ```scala
    1
    ```



----



```scala
DataTypes.select
```




*
    ```scala
    Seq(value)
    ```



### DataTypes.nonRoundTrip



```scala
NonRoundTripTypes.insert.columns(
  _.myOffsetDateTime := value.myOffsetDateTime,
  _.myZonedDateTime := value.myZonedDateTime
)
```




*
    ```scala
    1
    ```



----



```scala
NonRoundTripTypes.select
```




*
    ```scala
    Seq(normalize(value))
    ```



## Optional
Queries using columns that may be `NULL`, `Expr[Option[T]]` or `Option[T]` in Scala
### Optional



```scala
OptCols.insert.batched(_.myInt, _.myInt2)(
  (None, None),
  (Some(1), Some(2)),
  (Some(3), None),
  (None, Some(4))
)
```




*
    ```scala
    4
    ```



### Optional.selectAll

Nullable columns are modelled as `T[Option[V]]` fields on your `case class`,
and are returned to you as `Option[V]` values when you run a query. These
can be `Some` or `None`

```scala
OptCols.select
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Id](None, None),
      OptCols[Id](Some(1), Some(2)),
      OptCols[Id](Some(3), None),
      OptCols[Id](None, Some(4))
    )
    ```



### Optional.groupByMaxGet

Some aggregates return `Expr[Option[V]]`s, et.c. `.maxByOpt`

```scala
OptCols.select.groupBy(_.myInt)(_.maxByOpt(_.myInt2.get))
```


*
    ```sql
    SELECT opt_cols0.my_int AS res__0, MAX(opt_cols0.my_int2) AS res__1
    FROM opt_cols opt_cols0
    GROUP BY opt_cols0.my_int
    ```



*
    ```scala
    Seq(None -> Some(4), Some(1) -> Some(2), Some(3) -> None)
    ```



### Optional.isDefined

`.isDefined` on `Expr[Option[V]]` translates to a SQL
`IS NOT NULL` check

```scala
OptCols.select.filter(_.myInt.isDefined)
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int IS NOT NULL)
    ```



*
    ```scala
    Seq(OptCols[Id](Some(1), Some(2)), OptCols[Id](Some(3), None))
    ```



### Optional.isEmpty

`.isEmpty` on `Expr[Option[V]]` translates to a SQL
`IS NULL` check

```scala
OptCols.select.filter(_.myInt.isEmpty)
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int IS NULL)
    ```



*
    ```scala
    Seq(OptCols[Id](None, None), OptCols[Id](None, Some(4)))
    ```



### Optional.sqlEquals.nonOptionHit

Backticked `=` equality in ScalaSQL translates to a raw `=`
in SQL. This follows SQL `NULL` semantics, meaning that
`None = None` returns `false` rather than `true`

```scala
OptCols.select.filter(_.myInt `=` 1)
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int = ?)
    ```



*
    ```scala
    Seq(OptCols[Id](Some(1), Some(2)))
    ```



### Optional.sqlEquals.nonOptionMiss



```scala
OptCols.select.filter(_.myInt `=` 2)
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int = ?)
    ```



*
    ```scala
    Seq[OptCols[Id]]()
    ```



### Optional.sqlEquals.optionMiss



```scala
OptCols.select.filter(_.myInt `=` Option.empty[Int])
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int = ?)
    ```



*
    ```scala
    Seq[OptCols[Id]]()
    ```



### Optional.scalaEquals.someHit

`===` equality in ScalaSQL translates to a `IS NOT DISTINCT` in SQL.
This roughly follows Scala `==` semantics, meaning `None === None`
returns `true`

```scala
OptCols.select.filter(_.myInt === Option(1))
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int IS NOT DISTINCT FROM ?)
    ```



*
    ```scala
    Seq(OptCols[Id](Some(1), Some(2)))
    ```



### Optional.scalaEquals.noneHit



```scala
OptCols.select.filter(_.myInt === Option.empty[Int])
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int IS NOT DISTINCT FROM ?)
    ```



*
    ```scala
    Seq(OptCols[Id](None, None), OptCols[Id](None, Some(4)))
    ```



### Optional.scalaEquals.notEqualsSome



```scala
OptCols.select.filter(_.myInt !== Option(1))
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int IS DISTINCT FROM ?)
    ```



*
    ```scala
    Seq(
      OptCols[Id](None, None),
      OptCols[Id](Some(3), None),
      OptCols[Id](None, Some(value = 4))
    )
    ```



### Optional.scalaEquals.notEqualsNone



```scala
OptCols.select.filter(_.myInt !== Option.empty[Int])
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int IS DISTINCT FROM ?)
    ```



*
    ```scala
    Seq(
      OptCols[Id](Some(1), Some(2)),
      OptCols[Id](Some(3), None)
    )
    ```



### Optional.map

You can use operators like `.map` and `.flatMap` to work with
your `Expr[Option[V]]` values. These roughly follow the semantics
that you would be familiar with from Scala.

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + 10)))
```


*
    ```sql
    SELECT
      (opt_cols0.my_int + ?) AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Id](None, None),
      OptCols[Id](Some(11), Some(2)),
      OptCols[Id](Some(13), None),
      OptCols[Id](None, Some(4))
    )
    ```



### Optional.map2



```scala
OptCols.select.map(_.myInt.map(_ + 10))
```


*
    ```sql
    SELECT (opt_cols0.my_int + ?) AS res FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(None, Some(11), Some(13), None)
    ```



### Optional.flatMap



```scala
OptCols.select
  .map(d => d.copy[Expr](myInt = d.myInt.flatMap(v => d.myInt2.map(v2 => v + v2 + 10))))
```


*
    ```sql
    SELECT
      ((opt_cols0.my_int + opt_cols0.my_int2) + ?) AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Id](None, None),
      OptCols[Id](Some(13), Some(2)),
      // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
      OptCols[Id](None, None),
      OptCols[Id](None, Some(4))
    )
    ```



### Optional.mapGet

You can use `.get` to turn an `Expr[Option[V]]` into an `Expr[V]`. This follows
SQL semantics, such that `NULL`s anywhere in that selected column automatically
will turn the whole column `None` (if it's an `Expr[Option[V]]` column) or `null`
(if it's not an optional column)

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + d.myInt2.get + 1)))
```


*
    ```sql
    SELECT
      ((opt_cols0.my_int + opt_cols0.my_int2) + ?) AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Id](None, None),
      OptCols[Id](Some(4), Some(2)),
      // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
      OptCols[Id](None, None),
      OptCols[Id](None, Some(4))
    )
    ```



### Optional.rawGet



```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.get + d.myInt2.get + 1))
```


*
    ```sql
    SELECT
      ((opt_cols0.my_int + opt_cols0.my_int2) + ?) AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Id](None, None),
      OptCols[Id](Some(4), Some(2)),
      // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
      OptCols[Id](None, None),
      OptCols[Id](None, Some(4))
    )
    ```



### Optional.getOrElse



```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.getOrElse(-1)))
```


*
    ```sql
    SELECT
      COALESCE(opt_cols0.my_int, ?) AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Id](Some(-1), None),
      OptCols[Id](Some(1), Some(2)),
      OptCols[Id](Some(3), None),
      OptCols[Id](Some(-1), Some(4))
    )
    ```



### Optional.orElse



```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.orElse(d.myInt2)))
```


*
    ```sql
    SELECT
      COALESCE(opt_cols0.my_int, opt_cols0.my_int2) AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Id](None, None),
      OptCols[Id](Some(1), Some(2)),
      OptCols[Id](Some(3), None),
      OptCols[Id](Some(4), Some(4))
    )
    ```



### Optional.filter

`.filter` follows normal Scala semantics, and translates to a `CASE`/`WHEN (foo)`/`ELSE NULL`

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.filter(_ < 2)))
```


*
    ```sql
    SELECT
      CASE
        WHEN (opt_cols0.my_int < ?) THEN opt_cols0.my_int
        ELSE NULL
      END AS res__my_int,
      opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Id](None, None),
      OptCols[Id](Some(1), Some(2)),
      OptCols[Id](None, None),
      OptCols[Id](None, Some(4))
    )
    ```



### Optional.sorting.nullsLast

`.nullsLast` and `.nullsFirst` translate to SQL `NULLS LAST` and `NULLS FIRST` clauses

```scala
OptCols.select.sortBy(_.myInt).nullsLast
```


*
    ```sql
    SELECT opt_cols0.my_int AS res__my_int, opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    ORDER BY res__my_int NULLS LAST
    ```



*
    ```scala
    Seq(
      OptCols[Id](Some(1), Some(2)),
      OptCols[Id](Some(3), None),
      OptCols[Id](None, None),
      OptCols[Id](None, Some(4))
    )
    ```



### Optional.sorting.nullsFirst



```scala
OptCols.select.sortBy(_.myInt).nullsFirst
```


*
    ```sql
    SELECT opt_cols0.my_int AS res__my_int, opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    ORDER BY res__my_int NULLS FIRST
    ```



*
    ```scala
    Seq(
      OptCols[Id](None, None),
      OptCols[Id](None, Some(4)),
      OptCols[Id](Some(1), Some(2)),
      OptCols[Id](Some(3), None)
    )
    ```



### Optional.sorting.ascNullsLast



```scala
OptCols.select.sortBy(_.myInt).asc.nullsLast
```


*
    ```sql
    SELECT opt_cols0.my_int AS res__my_int, opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    ORDER BY res__my_int ASC NULLS LAST
    ```



*
    ```scala
    Seq(
      OptCols[Id](Some(1), Some(2)),
      OptCols[Id](Some(3), None),
      OptCols[Id](None, None),
      OptCols[Id](None, Some(4))
    )
    ```



### Optional.sorting.ascNullsFirst



```scala
OptCols.select.sortBy(_.myInt).asc.nullsFirst
```


*
    ```sql
    SELECT opt_cols0.my_int AS res__my_int, opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    ORDER BY res__my_int ASC NULLS FIRST
    ```



*
    ```scala
    Seq(
      OptCols[Id](None, None),
      OptCols[Id](None, Some(4)),
      OptCols[Id](Some(1), Some(2)),
      OptCols[Id](Some(3), None)
    )
    ```



### Optional.sorting.descNullsLast



```scala
OptCols.select.sortBy(_.myInt).desc.nullsLast
```


*
    ```sql
    SELECT opt_cols0.my_int AS res__my_int, opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    ORDER BY res__my_int DESC NULLS LAST
    ```



*
    ```scala
    Seq(
      OptCols[Id](Some(3), None),
      OptCols[Id](Some(1), Some(2)),
      OptCols[Id](None, None),
      OptCols[Id](None, Some(4))
    )
    ```



### Optional.sorting.descNullsFirst



```scala
OptCols.select.sortBy(_.myInt).desc.nullsFirst
```


*
    ```sql
    SELECT opt_cols0.my_int AS res__my_int, opt_cols0.my_int2 AS res__my_int2
    FROM opt_cols opt_cols0
    ORDER BY res__my_int DESC NULLS FIRST
    ```



*
    ```scala
    Seq(
      OptCols[Id](None, None),
      OptCols[Id](None, Some(4)),
      OptCols[Id](Some(3), None),
      OptCols[Id](Some(1), Some(2))
    )
    ```



## PostgresDialect
Operations specific to working with Postgres Databases
### PostgresDialect.distinctOn

ScalaSql's Postgres dialect provides teh `.distinctOn` operator, which translates
into a SQL `DISTINCT ON` clause

```scala
Purchase.select.distinctOn(_.shippingInfoId).sortBy(_.shippingInfoId).desc
```


*
    ```sql
    SELECT
      DISTINCT ON (purchase0.shipping_info_id) purchase0.id AS res__id,
      purchase0.shipping_info_id AS res__shipping_info_id,
      purchase0.product_id AS res__product_id,
      purchase0.count AS res__count,
      purchase0.total AS res__total
    FROM purchase purchase0
    ORDER BY res__shipping_info_id DESC
    ```



*
    ```scala
    Seq(
      Purchase[Id](6, 3, 1, 5, 44.4),
      Purchase[Id](4, 2, 4, 4, 493.8),
      Purchase[Id](2, 1, 2, 3, 900.0)
    )
    ```



### PostgresDialect.ltrim2



```scala
Expr("xxHellox").ltrim("x")
```


*
    ```sql
    SELECT LTRIM(?, ?) AS res
    ```



*
    ```scala
    "Hellox"
    ```



### PostgresDialect.rtrim2



```scala
Expr("xxHellox").rtrim("x")
```


*
    ```sql
    SELECT RTRIM(?, ?) AS res
    ```



*
    ```scala
    "xxHello"
    ```



### PostgresDialect.reverse



```scala
Expr("Hello").reverse
```


*
    ```sql
    SELECT REVERSE(?) AS res
    ```



*
    ```scala
    "olleH"
    ```



### PostgresDialect.lpad



```scala
Expr("Hello").lpad(10, "xy")
```


*
    ```sql
    SELECT LPAD(?, ?, ?) AS res
    ```



*
    ```scala
    "xyxyxHello"
    ```



### PostgresDialect.rpad



```scala
Expr("Hello").rpad(10, "xy")
```


*
    ```sql
    SELECT RPAD(?, ?, ?) AS res
    ```



*
    ```scala
    "Helloxyxyx"
    ```



## MySqlDialect
Operations specific to working with MySql Databases
### MySqlDialect.reverse



```scala
Expr("Hello").reverse
```


*
    ```sql
    SELECT REVERSE(?) AS res
    ```



*
    ```scala
    "olleH"
    ```



### MySqlDialect.lpad



```scala
Expr("Hello").lpad(10, "xy")
```


*
    ```sql
    SELECT LPAD(?, ?, ?) AS res
    ```



*
    ```scala
    "xyxyxHello"
    ```



### MySqlDialect.rpad



```scala
Expr("Hello").rpad(10, "xy")
```


*
    ```sql
    SELECT RPAD(?, ?, ?) AS res
    ```



*
    ```scala
    "Helloxyxyx"
    ```



### MySqlDialect.conflict.ignore



```scala
Buyer.insert
  .columns(
    _.name := "test buyer",
    _.dateOfBirth := LocalDate.parse("2023-09-09"),
    _.id := 1 // This should cause a primary key conflict
  )
  .onConflictUpdate(x => x.id := x.id)
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE id = buyer.id
    ```



*
    ```scala
    1
    ```



### MySqlDialect.conflict.update



```scala
Buyer.insert
  .columns(
    _.name := "test buyer",
    _.dateOfBirth := LocalDate.parse("2023-09-09"),
    _.id := 1 // This should cause a primary key conflict
  )
  .onConflictUpdate(_.name := "TEST BUYER CONFLICT")
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE name = ?
    ```



*
    ```scala
    2
    ```



----



```scala
Buyer.select
```




*
    ```scala
    Seq(
      Buyer[Id](1, "TEST BUYER CONFLICT", LocalDate.parse("2001-02-03")),
      Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
      Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
    )
    ```



### MySqlDialect.conflict.updateComputed



```scala
Buyer.insert
  .columns(
    _.name := "test buyer",
    _.dateOfBirth := LocalDate.parse("2023-09-09"),
    _.id := 1 // This should cause a primary key conflict
  )
  .onConflictUpdate(v => v.name := v.name.toUpperCase)
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE name = UPPER(buyer.name)
    ```



*
    ```scala
    2
    ```



----



```scala
Buyer.select
```




*
    ```scala
    Seq(
      Buyer[Id](1, "JAMES BOND", LocalDate.parse("2001-02-03")),
      Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
      Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
    )
    ```



## SqliteDialect
Operations specific to working with Sqlite Databases
### SqliteDialect.ltrim2



```scala
Expr("xxHellox").ltrim("x")
```


*
    ```sql
    SELECT LTRIM(?, ?) AS res
    ```



*
    ```scala
    "Hellox"
    ```



### SqliteDialect.rtrim2



```scala
Expr("xxHellox").rtrim("x")
```


*
    ```sql
    SELECT RTRIM(?, ?) AS res
    ```



*
    ```scala
    "xxHello"
    ```



## H2Dialect
Operations specific to working with H2 Databases
### H2Dialect.ltrim2



```scala
Expr("xxHellox").ltrim("x")
```


*
    ```sql
    SELECT LTRIM(?, ?) AS res
    ```



*
    ```scala
    "Hellox"
    ```



### H2Dialect.rtrim2



```scala
Expr("xxHellox").rtrim("x")
```


*
    ```sql
    SELECT RTRIM(?, ?) AS res
    ```



*
    ```scala
    "xxHello"
    ```



### H2Dialect.lpad



```scala
Expr("Hello").lpad(10, "xy")
```


*
    ```sql
    SELECT LPAD(?, ?, ?) AS res
    ```



*
    ```scala
    "xxxxxHello"
    ```



### H2Dialect.rpad



```scala
Expr("Hello").rpad(10, "xy")
```


*
    ```sql
    SELECT RPAD(?, ?, ?) AS res
    ```



*
    ```scala
    "Helloxxxxx"
    ```


