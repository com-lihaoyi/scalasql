[//]: # (GENERATED SOURCES, DO NOT EDIT DIRECTLY)
# ScalaSql Reference Library

This page contains example queries for the ScalaSql, taken from the
ScalaSql test suite. You can use this as a reference to see what kinds
of operations ScalaSql supports and how these operations are translated
into raw SQL to be sent to the database for execution.

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






### DbApi.runQuery

`db.runQuery` allows you to pass in a `SqlStr` using the `sql"..."` syntax,
allowing you to construct SQL strings and interpolate variables within them.
Interpolated variables automatically become prepared statement variables to
avoid SQL injection vulnerabilities. Takes a callback providing a `java.sql.ResultSet`
for you to use directly.

```scala
dbClient.transaction { db =>
  val filterId = 2
  val output = db.runQuery(sql"SELECT name FROM buyer WHERE id = $filterId") { rs =>
    val output = mutable.Buffer.empty[String]

    while (
      rs.next() match {
        case false => false
        case true =>
          output.append(rs.getString(1))
          true
      }
    ) ()
    output
  }

  assert(output == Seq("叉烧包"))
}
```






### DbApi.runUpdate

Similar to `db.runQuery`, `db.runUpdate` allows you to pass in a `SqlStr`, but runs
an update rather than a query and expects to receive a single number back from the
database indicating the number of rows inserted or updated

```scala
dbClient.transaction { db =>
  val newName = "Moo Moo Cow"
  val newDateOfBirth = LocalDate.parse("2000-01-01")
  val count = db
    .runUpdate(sql"INSERT INTO buyer (name, date_of_birth) VALUES($newName, $newDateOfBirth)")
  assert(count == 1)

  db.run(Buyer.select) ==> List(
    Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
    Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
    Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
    Buyer[Id](4, "Moo Moo Cow", LocalDate.parse("2000-01-01"))
  )
}
```






### DbApi.runRawQuery

`runRawQuery` is similar to `runQuery` but allows you to pass in the SQL strings
"raw", along with `?` placeholders and interpolated variables passed separately.

```scala
dbClient.transaction { db =>
  val output = db.runRawQuery("SELECT name FROM buyer WHERE id = ?", 2) { rs =>
    val output = mutable.Buffer.empty[String]

    while (
      rs.next() match {
        case false => false
        case true =>
          output.append(rs.getString(1))
          true
      }
    ) ()
    output
  }

  assert(output == Seq("叉烧包"))
}
```






### DbApi.runRawUpdate

`runRawUpdate` is similar to `runRawQuery`, but for update queries that
return a single number

```scala
dbClient.transaction { db =>
  val count = db.runRawUpdate(
    "INSERT INTO buyer (name, date_of_birth) VALUES(?, ?)",
    "Moo Moo Cow",
    LocalDate.parse("2000-01-01")
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

dbClient.autoCommit.run(Purchase.select.size) ==> 0
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

dbClient.autoCommit.run(Purchase.select.size) ==> 7
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

dbClient.autoCommit.run(Purchase.select.size) ==> 7
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

dbClient.autoCommit.run(Purchase.select.size) ==> 0
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

dbClient.autoCommit.run(Purchase.select.size) ==> 4
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

dbClient.autoCommit.run(Purchase.select.size) ==> 4
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

dbClient.autoCommit.run(Purchase.select.size) ==> 1
```






## Select
Basic `SELECT`` operations: map, filter, join, etc.
### Select.constant

The most simple thing you can query in the database is an `Expr`. These do not need
to be related to any database tables, and translate into raw `SELECT` calls without
`FROM`.

```scala
Expr(1) + Expr(2)
```


*
    ```sql
    SELECT ? + ? as res
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
      buyer0.id as res__id,
      buyer0.name as res__name,
      buyer0.date_of_birth as res__date_of_birth
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
      shipping_info0.id as res__id,
      shipping_info0.buyer_id as res__buyer_id,
      shipping_info0.shipping_date as res__shipping_date
    FROM shipping_info shipping_info0
    WHERE shipping_info0.buyer_id = ?
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
      shipping_info0.id as res__id,
      shipping_info0.buyer_id as res__buyer_id,
      shipping_info0.shipping_date as res__shipping_date
    FROM shipping_info shipping_info0
    WHERE shipping_info0.buyer_id = ?
    AND shipping_info0.shipping_date = ?
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
      shipping_info0.id as res__id,
      shipping_info0.buyer_id as res__buyer_id,
      shipping_info0.shipping_date as res__shipping_date
    FROM shipping_info shipping_info0
    WHERE shipping_info0.buyer_id = ?
    AND shipping_info0.shipping_date = ?
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
      shipping_info0.id as res__id,
      shipping_info0.buyer_id as res__buyer_id,
      shipping_info0.shipping_date as res__shipping_date
    FROM shipping_info shipping_info0
    WHERE shipping_info0.buyer_id = ?
    AND shipping_info0.shipping_date = ?
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
    SELECT buyer0.name as res FROM buyer buyer0
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
    SELECT product0.name as res FROM product product0 WHERE product0.price < ?
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
    SELECT buyer0.name as res__0, buyer0.id as res__1 FROM buyer buyer0
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
      buyer0.name as res__0,
      buyer0.id as res__1,
      buyer0.date_of_birth as res__2
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
    SELECT product0.price * ? as res FROM product product0
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
      buyer0.id as res__0,
      buyer0.id as res__1__id,
      buyer0.name as res__1__name,
      buyer0.date_of_birth as res__1__date_of_birth
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



### Select.exprQuery

`SELECT` queries that return a single row and column can be used as SQL expressions
in standard SQL databases. In ScalaSql, this is done by the `.exprQuery` method,
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
      .exprQuery
  )
)
```


*
    ```sql
    SELECT
      product0.name as res__0,
      (SELECT purchase0.total as res
        FROM purchase purchase0
        WHERE purchase0.product_id = product0.id
        ORDER BY res DESC
        LIMIT 1) as res__1
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
    SELECT subquery0.res__name as res
    FROM (SELECT buyer0.name as res__name FROM buyer buyer0) subquery0
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
    SELECT SUM(purchase0.total) as res FROM purchase purchase0
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
    SELECT SUM(purchase0.total) as res__0, MAX(purchase0.total) as res__1 FROM purchase purchase0
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
    SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
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
    SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
    FROM purchase purchase0
    GROUP BY purchase0.product_id
    HAVING SUM(purchase0.total) > ? AND purchase0.product_id > ?
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
    SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
    FROM purchase purchase0
    WHERE purchase0.count > ?
    GROUP BY purchase0.product_id
    HAVING SUM(purchase0.total) > ?
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
    SELECT purchase0.shipping_info_id as res FROM purchase purchase0
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
    SELECT DISTINCT purchase0.shipping_info_id as res FROM purchase purchase0
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
    SELECT buyer0.id as res__id, buyer0.name as res__name, buyer0.date_of_birth as res__date_of_birth
    FROM buyer buyer0
    WHERE buyer0.id in (SELECT shipping_info0.buyer_id as res FROM shipping_info shipping_info0)
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
      buyer0.name as res__0,
      EXISTS (SELECT
        shipping_info0.id as res
        FROM shipping_info shipping_info0
        WHERE shipping_info0.buyer_id = buyer0.id) as res__1
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
      buyer0.name as res__0,
      NOT EXISTS (SELECT
        shipping_info0.id as res
        FROM shipping_info shipping_info0
        WHERE shipping_info0.buyer_id = buyer0.id) as res__1
    FROM buyer buyer0
    ```



*
    ```scala
    Seq(("James Bond", false), ("叉烧包", false), ("Li Haoyi", true))
    ```



### Select.case.when

ScalaSql's `caseWhen` method translates into SQL's `CASE`/`WHEN`/`ELSE`/`END` syntax,
allowing you to perform basic conditionals as part of your SQL query

```scala
Product.select.map(p =>
  caseWhen(
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
        WHEN product0.price > ? THEN product0.name || ?
        WHEN product0.price > ? THEN product0.name || ?
        WHEN product0.price <= ? THEN product0.name || ?
      END as res
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
  caseWhen(
    (p.price > 200) -> (p.name + " EXPENSIVE"),
    (p.price > 5) -> (p.name + " NORMAL")
  ).`else` { p.name + " UNKNOWN" }
)
```


*
    ```sql
    SELECT
      CASE
        WHEN product0.price > ? THEN product0.name || ?
        WHEN product0.price > ? THEN product0.name || ?
        ELSE product0.name || ?
      END as res
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

ScalaSql's `.join` or `.joinOn` methods correspond to SQL `JOIN` and `JOIN ... ON ...`.
These perform an inner join between two tables, with an optional `ON` predicate. You can
also `.filter` and `.map` the results of the join, making use of the columns joined from
the two tables

```scala
Buyer.select.joinOn(ShippingInfo)(_.id `=` _.buyerId).filter(_._1.name `=` "叉烧包")
```


*
    ```sql
    SELECT
      buyer0.id as res__0__id,
      buyer0.name as res__0__name,
      buyer0.date_of_birth as res__0__date_of_birth,
      shipping_info1.id as res__1__id,
      shipping_info1.buyer_id as res__1__buyer_id,
      shipping_info1.shipping_date as res__1__shipping_date
    FROM buyer buyer0
    JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
    WHERE buyer0.name = ?
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
  .joinOn(ShippingInfo)(_.id `=` _.buyerId)
  .filter(_._1.name `=` "James Bond")
  .map(_._2.shippingDate)
```


*
    ```sql
    SELECT shipping_info1.shipping_date as res
    FROM buyer buyer0
    JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
    WHERE buyer0.name = ?
    ```



*
    ```scala
    Seq(LocalDate.parse("2012-04-05"))
    ```



### Join.selfJoin

ScalaSql supports a "self join", where a table is joined with itself. This
is done by simply having the same table be on the left-hand-side and right-hand-side
of your `.join` or `.joinOn` method. The two example self-joins below are trivial,
but illustrate how to do it in case you want to do a self-join in a more realistic setting.

```scala
Buyer.select.joinOn(Buyer)(_.id `=` _.id)
```


*
    ```sql
    SELECT
      buyer0.id as res__0__id,
      buyer0.name as res__0__name,
      buyer0.date_of_birth as res__0__date_of_birth,
      buyer1.id as res__1__id,
      buyer1.name as res__1__name,
      buyer1.date_of_birth as res__1__date_of_birth
    FROM buyer buyer0
    JOIN buyer buyer1 ON buyer0.id = buyer1.id
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
Buyer.select.joinOn(Buyer)(_.id <> _.id)
```


*
    ```sql
    SELECT
      buyer0.id as res__0__id,
      buyer0.name as res__0__name,
      buyer0.date_of_birth as res__0__date_of_birth,
      buyer1.id as res__1__id,
      buyer1.name as res__1__name,
      buyer1.date_of_birth as res__1__date_of_birth
    FROM buyer buyer0
    JOIN buyer buyer1 ON buyer0.id <> buyer1.id
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



### Join.flatMap

You can also perform inner joins via `flatMap`, either by directly
calling `.flatMap` or via `for`-comprehensions as below. This can help
reduce the boilerplate when dealing with lots of joins.

```scala
Buyer.select
  .flatMap(b => ShippingInfo.select.map((b, _)))
  .filter { case (b, s) => b.id `=` s.buyerId && b.name `=` "James Bond" }
  .map(_._2.shippingDate)
```


*
    ```sql
    SELECT shipping_info1.shipping_date as res
    FROM buyer buyer0, shipping_info shipping_info1
    WHERE buyer0.id = shipping_info1.buyer_id
    AND buyer0.name = ?
    ```



*
    ```scala
    Seq(LocalDate.parse("2012-04-05"))
    ```



### Join.flatMapFor

You can also perform inner joins via `flatMap

```scala
for {
  b <- Buyer.select
  s <- ShippingInfo.select
  if b.id `=` s.buyerId && b.name `=` "James Bond"
} yield s.shippingDate
```


*
    ```sql
    SELECT shipping_info1.shipping_date as res
    FROM buyer buyer0, shipping_info shipping_info1
    WHERE buyer0.id = shipping_info1.buyer_id
    AND buyer0.name = ?
    ```



*
    ```scala
    Seq(LocalDate.parse("2012-04-05"))
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
      buyer0.id as res__0__id,
      buyer0.name as res__0__name,
      buyer0.date_of_birth as res__0__date_of_birth,
      shipping_info1.id as res__1__id,
      shipping_info1.buyer_id as res__1__buyer_id,
      shipping_info1.shipping_date as res__1__shipping_date
    FROM buyer buyer0
    LEFT JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
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



### Join.rightJoin



```scala
ShippingInfo.select.rightJoin(Buyer)(_.buyerId `=` _.id)
```


*
    ```sql
    SELECT
      shipping_info0.id as res__0__id,
      shipping_info0.buyer_id as res__0__buyer_id,
      shipping_info0.shipping_date as res__0__shipping_date,
      buyer1.id as res__1__id,
      buyer1.name as res__1__name,
      buyer1.date_of_birth as res__1__date_of_birth
    FROM shipping_info shipping_info0
    RIGHT JOIN buyer buyer1 ON shipping_info0.buyer_id = buyer1.id
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
      shipping_info0.id as res__0__id,
      shipping_info0.buyer_id as res__0__buyer_id,
      shipping_info0.shipping_date as res__0__shipping_date,
      buyer1.id as res__1__id,
      buyer1.name as res__1__name,
      buyer1.date_of_birth as res__1__date_of_birth
    FROM shipping_info shipping_info0
    FULL OUTER JOIN buyer buyer1 ON shipping_info0.buyer_id = buyer1.id
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



## Insert
Basic `INSERT` operations
### Insert.single.simple

`Table.insert.values` inserts a single row into the given table, with the specified
 columns assigned to the given values, and any non-specified columns left `NULL`
 or assigned to their default values

```scala
Buyer.insert.values(
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
  .values(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"))
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
      buyer0.id + (SELECT MAX(buyer0.id) as res FROM buyer buyer0) as res__id,
      buyer0.name as res__name,
      buyer0.date_of_birth as res__date_of_birth
    FROM buyer buyer0
    WHERE buyer0.name <> ?
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
    SELECT buyer0.name as res__0, buyer0.date_of_birth as res__1
    FROM buyer buyer0
    WHERE buyer0.name <> ?
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
    UPDATE buyer SET date_of_birth = ? WHERE buyer.name = ?
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
    UPDATE buyer SET date_of_birth = ? WHERE ?
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
    UPDATE buyer SET date_of_birth = ?, name = ? WHERE buyer.name = ?
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
    UPDATE buyer SET name = UPPER(buyer.name) WHERE buyer.name = ?
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
    DELETE FROM purchase WHERE purchase.id = ?
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
    DELETE FROM purchase WHERE purchase.id <> ?
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
    SELECT product0.name as res FROM product product0 ORDER BY product0.price
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
      purchase0.id as res__id,
      purchase0.shipping_info_id as res__shipping_info_id,
      purchase0.product_id as res__product_id,
      purchase0.count as res__count,
      purchase0.total as res__total
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
    SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2
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
    SELECT product0.name as res FROM product product0 ORDER BY product0.price OFFSET 2
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
    SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2
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
    SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1
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
    SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2
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
    SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 4
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
    SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2
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
    SELECT DISTINCT subquery0.res as res
    FROM (SELECT purchase0.shipping_info_id as res
      FROM purchase purchase0
      ORDER BY purchase0.total DESC
      LIMIT 3) subquery0
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
  Product.select.filter(_.id === p.productId).map(_.name)
}
```


*
    ```sql
    SELECT product1.name as res
    FROM (SELECT purchase0.product_id as res__product_id, purchase0.total as res__total
      FROM purchase purchase0
      ORDER BY res__total DESC
      LIMIT 3) subquery0, product product1
    WHERE product1.id = subquery0.res__product_id
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
    SELECT SUM(subquery0.res__total) as res
    FROM (SELECT purchase0.total as res__total
      FROM purchase purchase0
      ORDER BY res__total DESC
      LIMIT 3) subquery0
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
    SELECT SUM(subquery0.res__total) as res__0, AVG(subquery0.res__total) as res__1
    FROM (SELECT purchase0.total as res__total
      FROM purchase purchase0
      ORDER BY res__total DESC
      LIMIT 3) subquery0
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
    SELECT LOWER(product0.name) as res
    FROM product product0
    UNION
    SELECT LOWER(product0.kebab_case_name) as res
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
    SELECT LOWER(product0.name) as res
    FROM product product0
    UNION ALL
    SELECT LOWER(product0.kebab_case_name) as res
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
    SELECT LOWER(product0.name) as res
    FROM product product0
    INTERSECT
    SELECT LOWER(product0.kebab_case_name) as res
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
    SELECT LOWER(product0.name) as res
    FROM product product0
    EXCEPT
    SELECT LOWER(product0.kebab_case_name) as res
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
    SELECT LOWER(product0.name) as res
    FROM product product0
    UNION ALL
    SELECT LOWER(buyer0.name) as res
    FROM buyer buyer0
    UNION
    SELECT LOWER(product0.kebab_case_name) as res
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
    SELECT LOWER(product0.name) as res
    FROM product product0
    UNION ALL
    SELECT LOWER(buyer0.name) as res
    FROM buyer buyer0
    UNION
    SELECT LOWER(product0.kebab_case_name) as res
    FROM product product0
    ORDER BY res
    LIMIT 4
    OFFSET 4
    ```



*
    ```scala
    Seq("guitar", "james bond", "li haoyi", "skate board")
    ```



## UpdateJoin
`UPDATE` queries that use `JOIN`s
### UpdateJoin.join

ScalaSql supports performing `UPDATE`s with `FROM`/`JOIN` clauses using the
`.update.joinOn` methods

```scala
Buyer
  .update(_.name `=` "James Bond")
  .joinOn(ShippingInfo)(_.id `=` _.buyerId)
  .set(c => c._1.dateOfBirth := c._2.shippingDate)
```


*
    ```sql
    UPDATE buyer
    SET date_of_birth = shipping_info0.shipping_date
    FROM shipping_info shipping_info0
    WHERE buyer.id = shipping_info0.buyer_id AND buyer.name = ?
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
  .joinOn(ShippingInfo)(_.id `=` _.buyerId)
  .joinOn(Purchase)(_._2.id `=` _.shippingInfoId)
  .joinOn(Product)(_._2.productId `=` _.id)
  .filter(t => t._2.name.toLowerCase `=` t._2.kebabCaseName.toLowerCase)
  .set(c => c._1._1._1.name := c._2.name)
```


*
    ```sql
    UPDATE buyer
    SET name = product2.name
    FROM shipping_info shipping_info0
    JOIN purchase purchase1 ON shipping_info0.id = purchase1.shipping_info_id
    JOIN product product2 ON purchase1.product_id = product2.id
    WHERE buyer.id = shipping_info0.buyer_id
    AND buyer.name = ?
    AND LOWER(product2.name) = LOWER(product2.kebab_case_name)
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
subqueries by passing in a `.select` query to `.joinOn`

```scala
Buyer
  .update(_.name `=` "James Bond")
  .joinOn(ShippingInfo.select.sortBy(_.id).asc.take(2))(_.id `=` _.buyerId)
  .set(c => c._1.dateOfBirth := c._2.shippingDate)
```


*
    ```sql
    UPDATE buyer SET date_of_birth = subquery0.res__shipping_date
    FROM (SELECT
        shipping_info0.id as res__id,
        shipping_info0.buyer_id as res__buyer_id,
        shipping_info0.shipping_date as res__shipping_date
      FROM shipping_info shipping_info0
      ORDER BY res__id ASC
      LIMIT 2) subquery0
    WHERE buyer.id = subquery0.res__buyer_id AND buyer.name = ?
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
  // Make sure the `SELECT shipping_info0.shipping_info_id as res__shipping_info_id`
  // column gets eliminated since it is not used outside the subquery
  .joinOn(ShippingInfo.select.sortBy(_.id).asc.take(2))(_.id `=` _.buyerId)
  .set(c => c._1.dateOfBirth := LocalDate.parse("2000-01-01"))
```


*
    ```sql
    UPDATE buyer SET date_of_birth = ?
    FROM (SELECT
        shipping_info0.id as res__id,
        shipping_info0.buyer_id as res__buyer_id
      FROM shipping_info shipping_info0
      ORDER BY res__id ASC
      LIMIT 2) subquery0
    WHERE buyer.id = subquery0.res__buyer_id AND buyer.name = ?
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
    SET price = (SELECT MAX(product0.price) as res FROM product product0)
    WHERE ?
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
    WHERE product.price = (SELECT MAX(product0.price) as res FROM product product0)
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
  .values(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"))
  .returning(_.id)
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth) VALUES (?, ?) RETURNING buyer.id as res
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
  .values(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"))
  .returning(_.id)
  .single
```


*
    ```sql
    INSERT INTO buyer (name, date_of_birth) VALUES (?, ?) RETURNING buyer.id as res
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
    RETURNING buyer.id as res
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
      buyer0.name as res__0,
      buyer0.date_of_birth as res__1
    FROM buyer buyer0
    WHERE buyer0.name <> ?
    RETURNING buyer.id as res
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
    UPDATE buyer SET date_of_birth = ? WHERE buyer.name = ? RETURNING buyer.id as res
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
    SET date_of_birth = ?, name = ? WHERE buyer.name = ?
    RETURNING buyer.id as res__0, buyer.name as res__1, buyer.date_of_birth as res__2
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
    DELETE FROM purchase WHERE purchase.shipping_info_id = ? RETURNING purchase.total as res
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
  .values(
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
  .values(
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
    RETURNING buyer.name as res
    ```



*
    ```scala
    Seq.empty[String]
    ```



### OnConflict.ignore.returningOne



```scala
Buyer.insert
  .values(
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
    RETURNING buyer.name as res
    ```



*
    ```scala
    Seq("test buyer")
    ```



### OnConflict.update

ScalaSql's `.onConflictUpdate` translates into SQL's `ON CONFLICT DO UPDATE`

```scala
Buyer.insert
  .values(
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
  .values(
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
  .values(
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
    RETURNING buyer.name as res
    ```



*
    ```scala
    "JAMES BOND"
    ```



## SubQuery
Queries that explicitly use subqueries (e.g. for `JOIN`s) or require subqueries to preserve the Scala semantics of the various operators
### SubQuery.sortTakeJoin

A ScalaSql `.joinOn` referencing a `.select` translates straightforwardly
into a SQL `JOIN` on a subquery

```scala
Purchase.select
  .joinOn(Product.select.sortBy(_.price).desc.take(1))(_.productId `=` _.id)
  .map { case (purchase, product) => purchase.total }
```


*
    ```sql
    SELECT purchase0.total as res
    FROM purchase purchase0
    JOIN (SELECT product0.id as res__id, product0.price as res__price
      FROM product product0
      ORDER BY res__price DESC
      LIMIT 1) subquery1
    ON purchase0.product_id = subquery1.res__id
    ```



*
    ```scala
    Seq(10000.0)
    ```



### SubQuery.sortTakeFrom

Some sequences of operations cannot be expressed as a single SQL query,
and thus translate into an outer query wrapping a subquery inside the `FROM`.
An example of this is performing a `.joinOn` after a `.take`: SQL does not
allow you to put `JOIN`s after `LIMIT`s, and so the only way to write this
in SQL is as a subquery.

```scala
Product.select.sortBy(_.price).desc.take(1).joinOn(Purchase)(_.id `=` _.productId).map {
  case (product, purchase) => purchase.total
}
```


*
    ```sql
    SELECT purchase1.total as res
    FROM (SELECT product0.id as res__id, product0.price as res__price
      FROM product product0
      ORDER BY res__price DESC
      LIMIT 1) subquery0
    JOIN purchase purchase1 ON subquery0.res__id = purchase1.product_id
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
  .joinOn(Purchase.select.sortBy(_.count).desc.take(3))(_.id `=` _.productId)
  .map { case (product, purchase) => (product.name, purchase.count) }
```


*
    ```sql
    SELECT
      subquery0.res__name as res__0,
      subquery1.res__count as res__1
    FROM (SELECT
        product0.id as res__id,
        product0.name as res__name,
        product0.price as res__price
      FROM product product0
      ORDER BY res__price DESC
      LIMIT 3) subquery0
    JOIN (SELECT
        purchase0.product_id as res__product_id,
        purchase0.count as res__count
      FROM purchase purchase0
      ORDER BY res__count DESC
      LIMIT 3) subquery1
    ON subquery0.res__id = subquery1.res__product_id
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
    SELECT subquery0.res__name as res
    FROM (SELECT
        product0.name as res__name,
        product0.price as res__price
      FROM product product0
      ORDER BY res__price DESC
      LIMIT 4) subquery0
    ORDER BY subquery0.res__price ASC
    LIMIT 2
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
    SELECT subquery0.res__product_id as res__0, SUM(subquery0.res__total) as res__1
    FROM (SELECT
        purchase0.product_id as res__product_id,
        purchase0.count as res__count,
        purchase0.total as res__total
      FROM purchase purchase0
      ORDER BY res__count
      LIMIT 5) subquery0
    GROUP BY subquery0.res__product_id
    ```



*
    ```scala
    Seq((1, 44.4), (2, 900.0), (3, 15.7), (4, 493.8), (5, 10000.0))
    ```



### SubQuery.groupByJoin



```scala
Purchase.select.groupBy(_.productId)(_.sumBy(_.total)).joinOn(Product)(_._1 `=` _.id).map {
  case ((productId, total), product) => (product.name, total)
}
```


*
    ```sql
    SELECT
      product1.name as res__0,
      subquery0.res__1 as res__1
    FROM (SELECT
        purchase0.product_id as res__0,
        SUM(purchase0.total) as res__1
      FROM purchase purchase0
      GROUP BY purchase0.product_id) subquery0
    JOIN product product1 ON subquery0.res__0 = product1.id
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
      buyer0.id as res__id,
      buyer0.name as res__name,
      buyer0.date_of_birth as res__date_of_birth
    FROM buyer buyer0
    WHERE (SELECT
        COUNT(1) as res
        FROM shipping_info shipping_info0
        WHERE buyer0.id = shipping_info0.buyer_id) = ?
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
      buyer0.id as res__0__id,
      buyer0.name as res__0__name,
      buyer0.date_of_birth as res__0__date_of_birth,
      (SELECT COUNT(1) as res FROM shipping_info shipping_info0 WHERE buyer0.id = shipping_info0.buyer_id) as res__1
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
      buyer0.id as res__0__id,
      buyer0.name as res__0__name,
      buyer0.date_of_birth as res__0__date_of_birth,
      (SELECT
        COUNT(1) as res
        FROM shipping_info shipping_info0
        WHERE buyer0.id = shipping_info0.buyer_id) = ? as res__1
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
    SELECT subquery0.res as res
    FROM (SELECT
        LOWER(buyer0.name) as res
      FROM buyer buyer0
      LIMIT 2) subquery0
    UNION ALL
    SELECT LOWER(product0.kebab_case_name) as res
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
    SELECT LOWER(buyer0.name) as res
    FROM buyer buyer0
    UNION ALL
    SELECT subquery0.res as res
    FROM (SELECT
        LOWER(product0.kebab_case_name) as res
      FROM product product0
      LIMIT 2) subquery0
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
      MAX(subquery0.res__1) as res__0,
      MIN(subquery0.res__1) as res__1
    FROM (SELECT
        LOWER(product0.name) as res__0,
        product0.price as res__1
      FROM product product0
      EXCEPT
      SELECT
        LOWER(product0.kebab_case_name) as res__0,
        product0.price as res__1
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
      MAX(subquery0.res__1) as res__0,
      MIN(subquery0.res__1) as res__1
    FROM (SELECT product0.price as res__1
      FROM product product0
      UNION ALL
      SELECT product0.price as res__1
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
              .sortBy(identity)
              .desc
              .take(1)
              .exprQuery
          }
          .sortBy(identity)
          .desc
          .take(1)
          .exprQuery
      }
      .sortBy(identity)
      .desc
      .take(1)
      .exprQuery
}
```


*
    ```sql
    SELECT
      buyer0.name as res__0,
      (SELECT
        (SELECT
          (SELECT product0.price as res
          FROM product product0
          WHERE product0.id = purchase0.product_id
          ORDER BY res DESC
          LIMIT 1) as res
        FROM purchase purchase0
        WHERE purchase0.shipping_info_id = shipping_info0.id
        ORDER BY res DESC
        LIMIT 1) as res
      FROM shipping_info shipping_info0
      WHERE shipping_info0.buyer_id = buyer0.id
      ORDER BY res DESC
      LIMIT 1) as res__1
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



## ExprBooleanOps
Operations that can be performed on `Expr[Boolean]`
### ExprBooleanOps.and



```scala
Expr(true) && Expr(true)
```


*
    ```sql
    SELECT ? AND ? as res
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
    SELECT ? AND ? as res
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
    SELECT ? OR ? as res
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
    SELECT NOT ? as res
    ```



*
    ```scala
    true
    ```



## ExprIntOps
Operations that can be performed on `Expr[T]` when `T` is numeric
### ExprIntOps.plus



```scala
Expr(6) + Expr(2)
```


*
    ```sql
    SELECT ? + ? as res
    ```



*
    ```scala
    8
    ```



### ExprIntOps.minus



```scala
Expr(6) - Expr(2)
```


*
    ```sql
    SELECT ? - ? as res
    ```



*
    ```scala
    4
    ```



### ExprIntOps.times



```scala
Expr(6) * Expr(2)
```


*
    ```sql
    SELECT ? * ? as res
    ```



*
    ```scala
    12
    ```



### ExprIntOps.divide



```scala
Expr(6) / Expr(2)
```


*
    ```sql
    SELECT ? / ? as res
    ```



*
    ```scala
    3
    ```



### ExprIntOps.modulo



```scala
Expr(6) % Expr(2)
```


*
    ```sql
    SELECT MOD(?, ?) as res
    ```



*
    ```scala
    0
    ```



### ExprIntOps.bitwiseAnd



```scala
Expr(6) & Expr(2)
```


*
    ```sql
    SELECT ? & ? as res
    ```



*
    ```scala
    2
    ```



### ExprIntOps.bitwiseOr



```scala
Expr(6) | Expr(3)
```


*
    ```sql
    SELECT ? | ? as res
    ```



*
    ```scala
    7
    ```



### ExprIntOps.between



```scala
Expr(4).between(Expr(2), Expr(6))
```


*
    ```sql
    SELECT ? BETWEEN ? AND ? as res
    ```



*
    ```scala
    true
    ```



### ExprIntOps.unaryPlus



```scala
+Expr(-4)
```


*
    ```sql
    SELECT +? as res
    ```



*
    ```scala
    -4
    ```



### ExprIntOps.unaryMinus



```scala
-Expr(-4)
```


*
    ```sql
    SELECT -? as res
    ```



*
    ```scala
    4
    ```



### ExprIntOps.unaryTilde



```scala
~Expr(-4)
```


*
    ```sql
    SELECT ~? as res
    ```



*
    ```scala
    3
    ```



### ExprIntOps.abs



```scala
Expr(-4).abs
```


*
    ```sql
    SELECT ABS(?) as res
    ```



*
    ```scala
    4
    ```



### ExprIntOps.mod



```scala
Expr(8).mod(Expr(3))
```


*
    ```sql
    SELECT MOD(?, ?) as res
    ```



*
    ```scala
    2
    ```



### ExprIntOps.ceil



```scala
Expr(4.3).ceil
```


*
    ```sql
    SELECT CEIL(?) as res
    ```



*
    ```scala
    5.0
    ```



### ExprIntOps.floor



```scala
Expr(4.7).floor
```


*
    ```sql
    SELECT FLOOR(?) as res
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
    SELECT FLOOR(?) as res
    ```



*
    ```scala
    4.0
    ```



## ExprSeqNumericOps
Operations that can be performed on `Expr[Seq[T]]` where `T` is numeric
### ExprSeqNumericOps.sum



```scala
Purchase.select.map(_.count).sum
```


*
    ```sql
    SELECT SUM(purchase0.count) as res FROM purchase purchase0
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
    SELECT MIN(purchase0.count) as res FROM purchase purchase0
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
    SELECT MAX(purchase0.count) as res FROM purchase purchase0
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
    SELECT AVG(purchase0.count) as res FROM purchase purchase0
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
    SELECT COUNT(1) as res FROM purchase purchase0
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
    SELECT SUM(purchase0.count) as res FROM purchase purchase0
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
    SELECT SUM(purchase0.count) as res FROM purchase purchase0
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
    SELECT SUM(purchase0.count) as res FROM purchase purchase0 WHERE ?
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
    SELECT MIN(purchase0.count) as res FROM purchase purchase0
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
    SELECT MIN(purchase0.count) as res FROM purchase purchase0
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
    SELECT MIN(purchase0.count) as res FROM purchase purchase0 WHERE ?
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
    SELECT MAX(purchase0.count) as res FROM purchase purchase0
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
    SELECT MAX(purchase0.count) as res FROM purchase purchase0
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
    SELECT MAX(purchase0.count) as res FROM purchase purchase0 WHERE ?
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
    SELECT AVG(purchase0.count) as res FROM purchase purchase0
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
    SELECT AVG(purchase0.count) as res FROM purchase purchase0
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
    SELECT AVG(purchase0.count) as res FROM purchase purchase0 WHERE ?
    ```



*
    ```scala
    Option.empty[Int]
    ```



## ExprStringOps
Operations that can be performed on `Expr[String]`
### ExprStringOps.plus



```scala
Expr("hello") + Expr("world")
```


*
    ```sql
    SELECT ? || ? as res
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
    SELECT ? LIKE ? as res
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
    SELECT LENGTH(?) as res
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
    SELECT OCTET_LENGTH(?) as res
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
    SELECT POSITION(? IN ?) as res
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
    SELECT LOWER(?) as res
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
    SELECT TRIM(?) as res
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
    SELECT LTRIM(?) as res
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
    SELECT RTRIM(?) as res
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
    SELECT SUBSTRING(?, ?, ?) as res
    ```



*
    ```scala
    "el"
    ```



## DataTypes
Basic operations on all the data types that ScalaSql supports mapping between Database types and Scala types
### DataTypes.constant



```scala
DataTypes.insert.values(
  _.myTinyInt := value.myTinyInt,
  _.mySmallInt := value.mySmallInt,
  _.myInt := value.myInt,
  _.myBigInt := value.myBigInt,
  _.myDouble := value.myDouble,
  _.myBoolean := value.myBoolean,
  _.myLocalDate := value.myLocalDate,
  _.myLocalTime := value.myLocalTime,
  _.myLocalDateTime := value.myLocalDateTime,
//          _.myZonedDateTime := value.myZonedDateTime,
  _.myInstant := value.myInstant
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
NonRoundTripTypes.insert.values(
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
Queries using columns that may be `NULL`, `Expr[Option[T]]` or `Option[T] in Scala
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



```scala
OptCols.select
```


*
    ```sql
    SELECT
      opt_cols0.my_int as res__my_int,
      opt_cols0.my_int2 as res__my_int2
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



```scala
OptCols.select.groupBy(_.myInt)(_.maxByOpt(_.myInt2.get))
```


*
    ```sql
    SELECT opt_cols0.my_int as res__0, MAX(opt_cols0.my_int2) as res__1
    FROM opt_cols opt_cols0
    GROUP BY opt_cols0.my_int
    ```



*
    ```scala
    Seq(None -> Some(4), Some(1) -> Some(2), Some(3) -> None)
    ```



### Optional.isDefined



```scala
OptCols.select.filter(_.myInt.isDefined)
```


*
    ```sql
    SELECT
      opt_cols0.my_int as res__my_int,
      opt_cols0.my_int2 as res__my_int2
    FROM opt_cols opt_cols0
    WHERE opt_cols0.my_int IS NOT NULL
    ```



*
    ```scala
    Seq(OptCols[Id](Some(1), Some(2)), OptCols[Id](Some(3), None))
    ```



### Optional.isEmpty



```scala
OptCols.select.filter(_.myInt.isEmpty)
```


*
    ```sql
    SELECT
      opt_cols0.my_int as res__my_int,
      opt_cols0.my_int2 as res__my_int2
    FROM opt_cols opt_cols0
    WHERE opt_cols0.my_int IS NULL
    ```



*
    ```scala
    Seq(OptCols[Id](None, None), OptCols[Id](None, Some(4)))
    ```



### Optional.sqlEquals.nonOptionHit



```scala
OptCols.select.filter(_.myInt `=` 1)
```


*
    ```sql
    SELECT
      opt_cols0.my_int as res__my_int,
      opt_cols0.my_int2 as res__my_int2
    FROM opt_cols opt_cols0
    WHERE opt_cols0.my_int = ?
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
      opt_cols0.my_int as res__my_int,
      opt_cols0.my_int2 as res__my_int2
    FROM opt_cols opt_cols0
    WHERE opt_cols0.my_int = ?
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
      opt_cols0.my_int as res__my_int,
      opt_cols0.my_int2 as res__my_int2
    FROM opt_cols opt_cols0
    WHERE opt_cols0.my_int = ?
    ```



*
    ```scala
    Seq[OptCols[Id]]()
    ```



### Optional.scalaEquals.someHit



```scala
OptCols.select.filter(_.myInt === Option(1))
```


*
    ```sql
    SELECT
      opt_cols0.my_int as res__my_int,
      opt_cols0.my_int2 as res__my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int IS NULL AND ? IS NULL) OR opt_cols0.my_int = ?
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
      opt_cols0.my_int as res__my_int,
      opt_cols0.my_int2 as res__my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int IS NULL AND ? IS NULL) OR opt_cols0.my_int = ?
    ```



*
    ```scala
    Seq(OptCols[Id](None, None), OptCols[Id](None, Some(4)))
    ```



### Optional.map



```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + 10)))
```


*
    ```sql
    SELECT
      opt_cols0.my_int + ? as res__my_int,
      opt_cols0.my_int2 as res__my_int2
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
    SELECT opt_cols0.my_int + ? as res FROM opt_cols opt_cols0
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
      opt_cols0.my_int + opt_cols0.my_int2 + ? as res__my_int,
      opt_cols0.my_int2 as res__my_int2
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



```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + d.myInt2.get + 1)))
```


*
    ```sql
    SELECT
      opt_cols0.my_int + opt_cols0.my_int2 + ? as res__my_int,
      opt_cols0.my_int2 as res__my_int2
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
      opt_cols0.my_int + opt_cols0.my_int2 + ? as res__my_int,
      opt_cols0.my_int2 as res__my_int2
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
      COALESCE(opt_cols0.my_int, ?) as res__my_int,
      opt_cols0.my_int2 as res__my_int2
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
      COALESCE(opt_cols0.my_int, opt_cols0.my_int2) as res__my_int,
      opt_cols0.my_int2 as res__my_int2
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



```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.filter(_ < 2)))
```


*
    ```sql
    SELECT
      CASE
        WHEN opt_cols0.my_int < ? THEN opt_cols0.my_int
        ELSE NULL
      END as res__my_int,
      opt_cols0.my_int2 as res__my_int2
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



```scala
OptCols.select.sortBy(_.myInt).nullsLast
```


*
    ```sql
    SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
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
    SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
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
    SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
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
    SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
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
    SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
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
    SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
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
### PostgresDialect.ltrim2



```scala
Expr("xxHellox").ltrim("x")
```


*
    ```sql
    SELECT LTRIM(?, ?) as res
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
    SELECT RTRIM(?, ?) as res
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
    SELECT REVERSE(?) as res
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
    SELECT LPAD(?, ?, ?) as res
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
    SELECT RPAD(?, ?, ?) as res
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
    SELECT REVERSE(?) as res
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
    SELECT LPAD(?, ?, ?) as res
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
    SELECT RPAD(?, ?, ?) as res
    ```



*
    ```scala
    "Helloxyxyx"
    ```



### MySqlDialect.conflict.ignore



```scala
Buyer.insert
  .values(
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
  .values(
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
  .values(
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
    SELECT LTRIM(?, ?) as res
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
    SELECT RTRIM(?, ?) as res
    ```



*
    ```scala
    "xxHello"
    ```



## HsqlDbDialect
Operations specific to working with HsqlDb Databases
### HsqlDbDialect.ltrim2



```scala
Expr("xxHellox").ltrim("x")
```


*
    ```sql
    SELECT LTRIM(?, ?) as res
    ```



*
    ```scala
    "Hellox"
    ```



### HsqlDbDialect.rtrim2



```scala
Expr("xxHellox").rtrim("x")
```


*
    ```sql
    SELECT RTRIM(?, ?) as res
    ```



*
    ```scala
    "xxHello"
    ```



### HsqlDbDialect.reverse



```scala
Expr("Hello").reverse
```


*
    ```sql
    SELECT REVERSE(?) as res
    ```



*
    ```scala
    "olleH"
    ```



### HsqlDbDialect.lpad



```scala
Expr("Hello").lpad(10, "xy")
```


*
    ```sql
    SELECT LPAD(?, ?, ?) as res
    ```



*
    ```scala
    "xyxyxHello"
    ```



### HsqlDbDialect.rpad



```scala
Expr("Hello").rpad(10, "xy")
```


*
    ```sql
    SELECT RPAD(?, ?, ?) as res
    ```



*
    ```scala
    "Helloxyxyx"
    ```



## H2Dialect
Operations specific to working with H2 Databases
### H2Dialect.ltrim2



```scala
Expr("xxHellox").ltrim("x")
```


*
    ```sql
    SELECT LTRIM(?, ?) as res
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
    SELECT RTRIM(?, ?) as res
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
    SELECT LPAD(?, ?, ?) as res
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
    SELECT RPAD(?, ?, ?) as res
    ```



*
    ```scala
    "Helloxxxxx"
    ```


