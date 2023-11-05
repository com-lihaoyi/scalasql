# hsqldb Reference Query Library

This page contains example queries for the `hsqldb` database, taken
from the ScalaSql test suite. You can use this as a reference to see
what kinds of operations ScalaSql supports when working on `hsqldb`,
and how these operations are translated into raw SQL to be sent to
the database for execution.

## DbApi
Basic usage of `db.*` operations such as `db.run`
### DbApi.run

()

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

()

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

()

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

()

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

()

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

()

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






## Select
Basic `SELECT`` operations: map, filter, join, etc.
### Select.constant

()

```scala
Expr(1) + Expr(2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    3
    ```



### Select.table

()

```scala
Buyer.select
```


*
    ```sql
    ()
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

()

```scala
ShippingInfo.select.filter(_.buyerId `=` 2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(
      ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03")),
      ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06"))
    )
    ```



### Select.filter.multiple

()

```scala
ShippingInfo.select
  .filter(_.buyerId `=` 2)
  .filter(_.shippingDate `=` LocalDate.parse("2012-05-06"))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(ShippingInfo[Id](id = 3, buyerId = 2, shippingDate = LocalDate.parse("2012-05-06")))
    ```



### Select.filter.dotSingle.pass

()

```scala
ShippingInfo.select
  .filter(_.buyerId `=` 2)
  .filter(_.shippingDate `=` LocalDate.parse("2012-05-06"))
  .single
```


*
    ```sql
    ()
    ```



*
    ```scala
    ShippingInfo[Id](id = 3, buyerId = 2, shippingDate = LocalDate.parse("2012-05-06"))
    ```



### Select.filter.combined

()

```scala
ShippingInfo.select
  .filter(p => p.buyerId `=` 2 && p.shippingDate `=` LocalDate.parse("2012-05-06"))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06")))
    ```



### Select.map.single

()

```scala
Buyer.select.map(_.name)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("James Bond", "叉烧包", "Li Haoyi")
    ```



### Select.map.filterMap

()

```scala
Product.select.filter(_.price < 100).map(_.name)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("Face Mask", "Socks", "Cookie")
    ```



### Select.map.tuple2

()

```scala
Buyer.select.map(c => (c.name, c.id))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(("James Bond", 1), ("叉烧包", 2), ("Li Haoyi", 3))
    ```



### Select.map.tuple3

()

```scala
Buyer.select.map(c => (c.name, c.id, c.dateOfBirth))
```


*
    ```sql
    ()
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

()

```scala
Product.select.map(_.price * 2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(17.76, 600, 6.28, 246.9, 2000.0, 0.2)
    ```



### Select.map.heterogenousTuple

()

```scala
Buyer.select.map(c => (c.id, c))
```


*
    ```sql
    ()
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

()

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
    ()
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

()

```scala
Buyer.select.subquery.map(_.name)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("James Bond", "叉烧包", "Li Haoyi")
    ```



### Select.aggregate.single

()

```scala
Purchase.select.sumBy(_.total)
```


*
    ```sql
    ()
    ```



*
    ```scala
    12343.2
    ```



### Select.aggregate.multiple

()

```scala
Purchase.select.aggregate(q => (q.sumBy(_.total), q.maxBy(_.total)))
```


*
    ```sql
    ()
    ```



*
    ```scala
    (12343.2, 10000.0)
    ```



### Select.groupBy.simple

()

```scala
Purchase.select.groupBy(_.productId)(_.sumBy(_.total))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq((1, 932.4), (2, 900.0), (3, 15.7), (4, 493.8), (5, 10000.0), (6, 1.30))
    ```



### Select.groupBy.having

()

```scala
Purchase.select.groupBy(_.productId)(_.sumBy(_.total)).filter(_._2 > 100).filter(_._1 > 1)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq((2, 900.0), (4, 493.8), (5, 10000.0))
    ```



### Select.groupBy.filterHaving

()

```scala
Purchase.select
  .filter(_.count > 5)
  .groupBy(_.productId)(_.sumBy(_.total))
  .filter(_._2 > 100)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq((1, 888.0), (5, 10000.0))
    ```



### Select.distinct.nondistinct

()

```scala
Purchase.select.map(_.shippingInfoId)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(1, 1, 1, 2, 2, 3, 3)
    ```



### Select.distinct.distinct

()

```scala
Purchase.select.map(_.shippingInfoId).distinct
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(1, 2, 3)
    ```



### Select.contains

()

```scala
Buyer.select.filter(b => ShippingInfo.select.map(_.buyerId).contains(b.id))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(
      Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
      Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
    )
    ```



### Select.nonEmpty

()

```scala
Buyer.select
  .map(b => (b.name, ShippingInfo.select.filter(_.buyerId `=` b.id).map(_.id).nonEmpty))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(("James Bond", true), ("叉烧包", true), ("Li Haoyi", false))
    ```



### Select.isEmpty

()

```scala
Buyer.select
  .map(b => (b.name, ShippingInfo.select.filter(_.buyerId `=` b.id).map(_.id).isEmpty))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(("James Bond", false), ("叉烧包", false), ("Li Haoyi", true))
    ```



### Select.case.when

()

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
    ()
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

()

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
    ()
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

()

```scala
Buyer.select.joinOn(ShippingInfo)(_.id `=` _.buyerId).filter(_._1.name `=` "叉烧包")
```


*
    ```sql
    ()
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



### Join.joinSelectFilter

()

```scala
Buyer.select.joinOn(ShippingInfo)(_.id `=` _.buyerId).filter(_._1.name `=` "叉烧包")
```


*
    ```sql
    ()
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

()

```scala
Buyer.select
  .joinOn(ShippingInfo)(_.id `=` _.buyerId)
  .filter(_._1.name `=` "James Bond")
  .map(_._2.shippingDate)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(LocalDate.parse("2012-04-05"))
    ```



### Join.selfJoin

()

```scala
Buyer.select.joinOn(Buyer)(_.id `=` _.id)
```


*
    ```sql
    ()
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

()

```scala
Buyer.select.joinOn(Buyer)(_.id <> _.id)
```


*
    ```sql
    ()
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

()

```scala
Buyer.select
  .flatMap(c => ShippingInfo.select.map((c, _)))
  .filter { case (c, p) => c.id `=` p.buyerId && c.name `=` "James Bond" }
  .map(_._2.shippingDate)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(LocalDate.parse("2012-04-05"))
    ```



### Join.flatMap2

()

```scala
Buyer.select
  .flatMap(c => ShippingInfo.select.filter { p => c.id `=` p.buyerId && c.name `=` "James Bond" })
  .map(_.shippingDate)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(LocalDate.parse("2012-04-05"))
    ```



### Join.leftJoin

()

```scala
Buyer.select.leftJoin(ShippingInfo)(_.id `=` _.buyerId)
```


*
    ```sql
    ()
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

()

```scala
ShippingInfo.select.rightJoin(Buyer)(_.buyerId `=` _.id)
```


*
    ```sql
    ()
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

()

```scala
ShippingInfo.select.outerJoin(Buyer)(_.buyerId `=` _.id)
```


*
    ```sql
    ()
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

()

```scala
Buyer.insert.values(
  _.name := "test buyer",
  _.dateOfBirth := LocalDate.parse("2023-09-09"),
  _.id := 4
)
```


*
    ```sql
    ()
    ```



*
    ```scala
    1
    ```



----

()

```scala
Buyer.select.filter(_.name `=` "test buyer")
```




*
    ```scala
    Seq(Buyer[Id](4, "test buyer", LocalDate.parse("2023-09-09")))
    ```



### Insert.single.partial

()

```scala
Buyer.insert
  .values(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"))
```


*
    ```sql
    ()
    ```



*
    ```scala
    1
    ```



----

()

```scala
Buyer.select.filter(_.name `=` "test buyer")
```




*
    ```scala
    Seq(Buyer[Id](4, "test buyer", LocalDate.parse("2023-09-09")))
    ```



### Insert.batch.simple

()

```scala
Buyer.insert.batched(_.name, _.dateOfBirth, _.id)(
  ("test buyer A", LocalDate.parse("2001-04-07"), 4),
  ("test buyer B", LocalDate.parse("2002-05-08"), 5),
  ("test buyer C", LocalDate.parse("2003-06-09"), 6)
)
```


*
    ```sql
    ()
    ```



*
    ```scala
    3
    ```



----

()

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

()

```scala
Buyer.insert.batched(_.name, _.dateOfBirth)(
  ("test buyer A", LocalDate.parse("2001-04-07")),
  ("test buyer B", LocalDate.parse("2002-05-08")),
  ("test buyer C", LocalDate.parse("2003-06-09"))
)
```


*
    ```sql
    ()
    ```



*
    ```scala
    3
    ```



----

()

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

()

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
    ()
    ```



*
    ```scala
    2
    ```



----

()

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

()

```scala
Buyer.insert.select(
  x => (x.name, x.dateOfBirth),
  Buyer.select.map(x => (x.name, x.dateOfBirth)).filter(_._1 <> "Li Haoyi")
)
```


*
    ```sql
    ()
    ```



*
    ```scala
    2
    ```



----

()

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

()

```scala
Buyer
  .update(_.name `=` "James Bond")
  .set(_.dateOfBirth := LocalDate.parse("2019-04-07"))
```


*
    ```sql
    ()
    ```



*
    ```scala
    1
    ```



----

()

```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth).single
```




*
    ```scala
    LocalDate.parse("2019-04-07")
    ```



----

()

```scala
Buyer.select.filter(_.name `=` "Li Haoyi").map(_.dateOfBirth).single
```




*
    ```scala
    LocalDate.parse("1965-08-09" /* not updated */ )
    ```



### Update.bulk

()

```scala
Buyer.update(_ => true).set(_.dateOfBirth := LocalDate.parse("2019-04-07"))
```


*
    ```sql
    ()
    ```



*
    ```scala
    3
    ```



----

()

```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth).single
```




*
    ```scala
    LocalDate.parse("2019-04-07")
    ```



----

()

```scala
Buyer.select.filter(_.name `=` "Li Haoyi").map(_.dateOfBirth).single
```




*
    ```scala
    LocalDate.parse("2019-04-07")
    ```



### Update.multiple

()

```scala
Buyer
  .update(_.name `=` "James Bond")
  .set(_.dateOfBirth := LocalDate.parse("2019-04-07"), _.name := "John Dee")
```


*
    ```sql
    ()
    ```



*
    ```scala
    1
    ```



----

()

```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```




*
    ```scala
    Seq[LocalDate]( /* not found due to rename */ )
    ```



----

()

```scala
Buyer.select.filter(_.name `=` "John Dee").map(_.dateOfBirth)
```




*
    ```scala
    Seq(LocalDate.parse("2019-04-07"))
    ```



### Update.dynamic

()

```scala
Buyer.update(_.name `=` "James Bond").set(c => c.name := c.name.toUpperCase)
```


*
    ```sql
    ()
    ```



*
    ```scala
    1
    ```



----

()

```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```




*
    ```scala
    Seq[LocalDate]( /* not found due to rename */ )
    ```



----

()

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

()

```scala
Purchase.delete(_.id `=` 2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    1
    ```



----

()

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

()

```scala
Purchase.delete(_.id <> 2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    6
    ```



----

()

```scala
Purchase.select
```




*
    ```scala
    Seq(Purchase[Id](id = 2, shippingInfoId = 1, productId = 2, count = 3, total = 900.0))
    ```



### Delete.all

()

```scala
Purchase.delete(_ => true)
```


*
    ```sql
    ()
    ```



*
    ```scala
    7
    ```



----

()

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

()

```scala
Product.select.sortBy(_.price).map(_.name)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("Cookie", "Socks", "Face Mask", "Skate Board", "Guitar", "Camera")
    ```



### CompoundSelect.sort.twice

()

```scala
Purchase.select.sortBy(_.productId).asc.sortBy(_.shippingInfoId).desc
```


*
    ```sql
    ()
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

()

```scala
Product.select.sortBy(_.price).map(_.name).take(2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("Cookie", "Socks")
    ```



### CompoundSelect.sort.sortOffset

()

```scala
Product.select.sortBy(_.price).map(_.name).drop(2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("Face Mask", "Skate Board", "Guitar", "Camera")
    ```



### CompoundSelect.sort.sortLimitTwiceHigher

()

```scala
Product.select.sortBy(_.price).map(_.name).take(2).take(3)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("Cookie", "Socks")
    ```



### CompoundSelect.sort.sortLimitTwiceLower

()

```scala
Product.select.sortBy(_.price).map(_.name).take(2).take(1)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("Cookie")
    ```



### CompoundSelect.sort.sortLimitOffset

()

```scala
Product.select.sortBy(_.price).map(_.name).drop(2).take(2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("Face Mask", "Skate Board")
    ```



### CompoundSelect.sort.sortLimitOffsetTwice

()

```scala
Product.select.sortBy(_.price).map(_.name).drop(2).drop(2).take(1)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("Guitar")
    ```



### CompoundSelect.sort.sortOffsetLimit

()

```scala
Product.select.sortBy(_.price).map(_.name).drop(2).take(2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("Face Mask", "Skate Board")
    ```



### CompoundSelect.distinct

()

```scala
Purchase.select.sortBy(_.total).desc.take(3).map(_.shippingInfoId).distinct
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(1, 2)
    ```



### CompoundSelect.flatMap

()

```scala
Purchase.select.sortBy(_.total).desc.take(3).flatMap { p =>
  Product.select.filter(_.id === p.productId).map(_.name)
}
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("Camera", "Face Mask", "Guitar")
    ```



### CompoundSelect.sumBy

()

```scala
Purchase.select.sortBy(_.total).desc.take(3).sumBy(_.total)
```


*
    ```sql
    ()
    ```



*
    ```scala
    11788.0
    ```



### CompoundSelect.aggregate

()

```scala
Purchase.select
  .sortBy(_.total)
  .desc
  .take(3)
  .aggregate(p => (p.sumBy(_.total), p.avgBy(_.total)))
```


*
    ```sql
    ()
    ```



*
    ```scala
    (11788.0, 3929.0)
    ```



### CompoundSelect.union

()

```scala
Product.select
  .map(_.name.toLowerCase)
  .union(Product.select.map(_.kebabCaseName.toLowerCase))
```


*
    ```sql
    ()
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

()

```scala
Product.select
  .map(_.name.toLowerCase)
  .unionAll(Product.select.map(_.kebabCaseName.toLowerCase))
```


*
    ```sql
    ()
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

()

```scala
Product.select
  .map(_.name.toLowerCase)
  .intersect(Product.select.map(_.kebabCaseName.toLowerCase))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("camera", "cookie", "guitar", "socks")
    ```



### CompoundSelect.except

()

```scala
Product.select
  .map(_.name.toLowerCase)
  .except(Product.select.map(_.kebabCaseName.toLowerCase))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("face mask", "skate board")
    ```



### CompoundSelect.unionAllUnionSort

()

```scala
Product.select
  .map(_.name.toLowerCase)
  .unionAll(Buyer.select.map(_.name.toLowerCase))
  .union(Product.select.map(_.kebabCaseName.toLowerCase))
  .sortBy(identity)
```


*
    ```sql
    ()
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

()

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
    ()
    ```



*
    ```scala
    Seq("guitar", "james bond", "li haoyi", "skate board")
    ```



### CompoundSelect.exceptAggregate

()

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
    ()
    ```



*
    ```scala
    (123.45, 8.88)
    ```



### CompoundSelect.unionAllAggregate

()

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
    ()
    ```



*
    ```scala
    (1000.0, 0.1)
    ```



## SubQuery
Queries that explicitly use subqueries (e.g. for `JOIN`s) or require subqueries to preserve the Scala semantics of the various operators
### SubQuery.sortTakeJoin

()

```scala
Purchase.select
  .joinOn(Product.select.sortBy(_.price).desc.take(1))(_.productId `=` _.id)
  .map { case (purchase, product) => purchase.total }
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(10000.0)
    ```



### SubQuery.sortTakeFrom

()

```scala
Product.select.sortBy(_.price).desc.take(1).joinOn(Purchase)(_.id `=` _.productId).map {
  case (product, purchase) => purchase.total
}
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(10000.0)
    ```



### SubQuery.sortTakeFromAndJoin

()

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
    ()
    ```



*
    ```scala
    Seq(("Camera", 10))
    ```



### SubQuery.sortLimitSortLimit

()

```scala
Product.select.sortBy(_.price).desc.take(4).sortBy(_.price).asc.take(2).map(_.name)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("Face Mask", "Skate Board")
    ```



### SubQuery.sortGroupBy

()

```scala
Purchase.select.sortBy(_.count).take(5).groupBy(_.productId)(_.sumBy(_.total))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq((1, 44.4), (2, 900.0), (3, 15.7), (4, 493.8), (5, 10000.0))
    ```



### SubQuery.groupByJoin

()

```scala
Purchase.select.groupBy(_.productId)(_.sumBy(_.total)).joinOn(Product)(_._1 `=` _.id).map {
  case ((productId, total), product) => (product.name, total)
}
```


*
    ```sql
    ()
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

()

```scala
Buyer.select.filter(c => ShippingInfo.select.filter(p => c.id `=` p.buyerId).size `=` 0)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")))
    ```



### SubQuery.subqueryInMap

()

```scala
Buyer.select.map(c => (c, ShippingInfo.select.filter(p => c.id `=` p.buyerId).size))
```


*
    ```sql
    ()
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

()

```scala
Buyer.select.map(c => (c, ShippingInfo.select.filter(p => c.id `=` p.buyerId).size `=` 1))
```


*
    ```sql
    ()
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

()

```scala
Buyer.select
  .map(_.name.toLowerCase)
  .take(2)
  .unionAll(Product.select.map(_.kebabCaseName.toLowerCase))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("james bond", "叉烧包", "face-mask", "guitar", "socks", "skate-board", "camera", "cookie")
    ```



### SubQuery.selectUnionSelectLimit

()

```scala
Buyer.select
  .map(_.name.toLowerCase)
  .unionAll(Product.select.map(_.kebabCaseName.toLowerCase).take(2))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq("james bond", "叉烧包", "li haoyi", "face-mask", "guitar")
    ```



## UpdateSubQuery
`UPDATE` queries that use Subqueries
### UpdateSubQuery.setSubquery

()

```scala
Product.update(_ => true).set(_.price := Product.select.maxBy(_.price))
```


*
    ```sql
    ()
    ```



*
    ```scala
    6
    ```



----

()

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

()

```scala
Product.update(_.price `=` Product.select.maxBy(_.price)).set(_.price := 0)
```


*
    ```sql
    ()
    ```



*
    ```scala
    1
    ```



----

()

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



## ExprBooleanOps
Operations that can be performed on `Expr[Boolean]`
### ExprBooleanOps.and

()

```scala
Expr(true) && Expr(true)
```


*
    ```sql
    ()
    ```



*
    ```scala
    true
    ```



----

()

```scala
Expr(false) && Expr(true)
```


*
    ```sql
    ()
    ```



*
    ```scala
    false
    ```



### ExprBooleanOps.or

()

```scala
Expr(false) || Expr(false)
```


*
    ```sql
    ()
    ```



*
    ```scala
    false
    ```



----

()

```scala
!Expr(false)
```


*
    ```sql
    ()
    ```



*
    ```scala
    true
    ```



## ExprIntOps
Operations that can be performed on `Expr[T]` when `T` is numeric
### ExprIntOps.plus

()

```scala
Expr(6) + Expr(2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    8
    ```



### ExprIntOps.minus

()

```scala
Expr(6) - Expr(2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    4
    ```



### ExprIntOps.times

()

```scala
Expr(6) * Expr(2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    12
    ```



### ExprIntOps.divide

()

```scala
Expr(6) / Expr(2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    3
    ```



### ExprIntOps.modulo

()

```scala
Expr(6) % Expr(2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    0
    ```



### ExprIntOps.bitwiseAnd

()

```scala
Expr(6) & Expr(2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    2
    ```



### ExprIntOps.bitwiseOr

()

```scala
Expr(6) | Expr(3)
```


*
    ```sql
    ()
    ```



*
    ```scala
    7
    ```



### ExprIntOps.between

()

```scala
Expr(4).between(Expr(2), Expr(6))
```


*
    ```sql
    ()
    ```



*
    ```scala
    true
    ```



### ExprIntOps.unaryPlus

()

```scala
+Expr(-4)
```


*
    ```sql
    ()
    ```



*
    ```scala
    -4
    ```



### ExprIntOps.unaryMinus

()

```scala
-Expr(-4)
```


*
    ```sql
    ()
    ```



*
    ```scala
    4
    ```



### ExprIntOps.unaryTilde

()

```scala
~Expr(-4)
```


*
    ```sql
    ()
    ```



*
    ```scala
    3
    ```



### ExprIntOps.abs

()

```scala
Expr(-4).abs
```


*
    ```sql
    ()
    ```



*
    ```scala
    4
    ```



### ExprIntOps.mod

()

```scala
Expr(8).mod(Expr(3))
```


*
    ```sql
    ()
    ```



*
    ```scala
    2
    ```



### ExprIntOps.ceil

()

```scala
Expr(4.3).ceil
```


*
    ```sql
    ()
    ```



*
    ```scala
    5.0
    ```



### ExprIntOps.floor

()

```scala
Expr(4.7).floor
```


*
    ```sql
    ()
    ```



*
    ```scala
    4.0
    ```



----

()

```scala
Expr(4.7).floor
```


*
    ```sql
    ()
    ```



*
    ```scala
    4.0
    ```



## ExprSeqNumericOps
Operations that can be performed on `Expr[Seq[T]]` where `T` is numeric
### ExprSeqNumericOps.sum

()

```scala
Purchase.select.map(_.count).sum
```


*
    ```sql
    ()
    ```



*
    ```scala
    140
    ```



### ExprSeqNumericOps.min

()

```scala
Purchase.select.map(_.count).min
```


*
    ```sql
    ()
    ```



*
    ```scala
    3
    ```



### ExprSeqNumericOps.max

()

```scala
Purchase.select.map(_.count).max
```


*
    ```sql
    ()
    ```



*
    ```scala
    100
    ```



### ExprSeqNumericOps.avg

()

```scala
Purchase.select.map(_.count).avg
```


*
    ```sql
    ()
    ```



*
    ```scala
    20
    ```



## ExprSeqOps
Operations that can be performed on `Expr[Seq[_]]`
### ExprSeqOps.size

()

```scala
Purchase.select.size
```


*
    ```sql
    ()
    ```



*
    ```scala
    7
    ```



### ExprSeqOps.sumBy.simple

()

```scala
Purchase.select.sumBy(_.count)
```


*
    ```sql
    ()
    ```



*
    ```scala
    140
    ```



### ExprSeqOps.sumBy.some

()

```scala
Purchase.select.sumByOpt(_.count)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Option(140)
    ```



### ExprSeqOps.sumBy.none

()

```scala
Purchase.select.filter(_ => false).sumByOpt(_.count)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Option.empty[Int]
    ```



### ExprSeqOps.minBy.simple

()

```scala
Purchase.select.minBy(_.count)
```


*
    ```sql
    ()
    ```



*
    ```scala
    3
    ```



### ExprSeqOps.minBy.some

()

```scala
Purchase.select.minByOpt(_.count)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Option(3)
    ```



### ExprSeqOps.minBy.none

()

```scala
Purchase.select.filter(_ => false).minByOpt(_.count)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Option.empty[Int]
    ```



### ExprSeqOps.maxBy.simple

()

```scala
Purchase.select.maxBy(_.count)
```


*
    ```sql
    ()
    ```



*
    ```scala
    100
    ```



### ExprSeqOps.maxBy.some

()

```scala
Purchase.select.maxByOpt(_.count)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Option(100)
    ```



### ExprSeqOps.maxBy.none

()

```scala
Purchase.select.filter(_ => false).maxByOpt(_.count)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Option.empty[Int]
    ```



### ExprSeqOps.avgBy.simple

()

```scala
Purchase.select.avgBy(_.count)
```


*
    ```sql
    ()
    ```



*
    ```scala
    20
    ```



### ExprSeqOps.avgBy.some

()

```scala
Purchase.select.avgByOpt(_.count)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Option(20)
    ```



### ExprSeqOps.avgBy.none

()

```scala
Purchase.select.filter(_ => false).avgByOpt(_.count)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Option.empty[Int]
    ```



## ExprStringOps
Operations that can be performed on `Expr[String]`
### ExprStringOps.plus

()

```scala
Expr("hello") + Expr("world")
```


*
    ```sql
    ()
    ```



*
    ```scala
    "helloworld"
    ```



### ExprStringOps.like

()

```scala
Expr("hello").like("he%")
```


*
    ```sql
    ()
    ```



*
    ```scala
    true
    ```



### ExprStringOps.length

()

```scala
Expr("hello").length
```


*
    ```sql
    ()
    ```



*
    ```scala
    5
    ```



### ExprStringOps.octetLength

()

```scala
Expr("叉烧包").octetLength
```


*
    ```sql
    ()
    ```



*
    ```scala
    9
    ```



### ExprStringOps.position

()

```scala
Expr("hello").indexOf("ll")
```


*
    ```sql
    ()
    ```



*
    ```scala
    3
    ```



### ExprStringOps.toLowerCase

()

```scala
Expr("Hello").toLowerCase
```


*
    ```sql
    ()
    ```



*
    ```scala
    "hello"
    ```



### ExprStringOps.trim

()

```scala
Expr("  Hello ").trim
```


*
    ```sql
    ()
    ```



*
    ```scala
    "Hello"
    ```



### ExprStringOps.ltrim

()

```scala
Expr("  Hello ").ltrim
```


*
    ```sql
    ()
    ```



*
    ```scala
    "Hello "
    ```



### ExprStringOps.rtrim

()

```scala
Expr("  Hello ").rtrim
```


*
    ```sql
    ()
    ```



*
    ```scala
    "  Hello"
    ```



### ExprStringOps.substring

()

```scala
Expr("Hello").substring(2, 2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    "el"
    ```



## HsqlDbDialect
Operations specific to working with HsqlDb Databases
### HsqlDbDialect.ltrim2

()

```scala
Expr("xxHellox").ltrim("x")
```


*
    ```sql
    ()
    ```



*
    ```scala
    "Hellox"
    ```



### HsqlDbDialect.rtrim2

()

```scala
Expr("xxHellox").rtrim("x")
```


*
    ```sql
    ()
    ```



*
    ```scala
    "xxHello"
    ```



### HsqlDbDialect.reverse

()

```scala
Expr("Hello").reverse
```


*
    ```sql
    ()
    ```



*
    ```scala
    "olleH"
    ```



### HsqlDbDialect.lpad

()

```scala
Expr("Hello").lpad(10, "xy")
```


*
    ```sql
    ()
    ```



*
    ```scala
    "xyxyxHello"
    ```



### HsqlDbDialect.rpad

()

```scala
Expr("Hello").rpad(10, "xy")
```


*
    ```sql
    ()
    ```



*
    ```scala
    "Helloxyxyx"
    ```



## DataTypes
Basic operations on all the data types that ScalaSql supports mapping between Database types and Scala types
### DataTypes.constant

()

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

()

```scala
DataTypes.select
```




*
    ```scala
    Seq(value)
    ```



### DataTypes.nonRoundTrip

()

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

()

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

()

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

()

```scala
OptCols.select
```


*
    ```sql
    ()
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

()

```scala
OptCols.select.groupBy(_.myInt)(_.maxByOpt(_.myInt2.get))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(None -> Some(4), Some(1) -> Some(2), Some(3) -> None)
    ```



### Optional.isDefined

()

```scala
OptCols.select.filter(_.myInt.isDefined)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(OptCols[Id](Some(1), Some(2)), OptCols[Id](Some(3), None))
    ```



### Optional.isEmpty

()

```scala
OptCols.select.filter(_.myInt.isEmpty)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(OptCols[Id](None, None), OptCols[Id](None, Some(4)))
    ```



### Optional.sqlEquals.nonOptionHit

()

```scala
OptCols.select.filter(_.myInt `=` 1)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(OptCols[Id](Some(1), Some(2)))
    ```



### Optional.sqlEquals.nonOptionMiss

()

```scala
OptCols.select.filter(_.myInt `=` 2)
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq[OptCols[Id]]()
    ```



### Optional.sqlEquals.optionMiss

()

```scala
OptCols.select.filter(_.myInt `=` Option.empty[Int])
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq[OptCols[Id]]()
    ```



### Optional.scalaEquals.someHit

()

```scala
OptCols.select.filter(_.myInt === Option(1))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(OptCols[Id](Some(1), Some(2)))
    ```



### Optional.scalaEquals.noneHit

()

```scala
OptCols.select.filter(_.myInt === Option.empty[Int])
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(OptCols[Id](None, None), OptCols[Id](None, Some(4)))
    ```



### Optional.map

()

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + 10)))
```


*
    ```sql
    ()
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

()

```scala
OptCols.select.map(_.myInt.map(_ + 10))
```


*
    ```sql
    ()
    ```



*
    ```scala
    Seq(None, Some(11), Some(13), None)
    ```



### Optional.flatMap

()

```scala
OptCols.select
  .map(d => d.copy[Expr](myInt = d.myInt.flatMap(v => d.myInt2.map(v2 => v + v2 + 10))))
```


*
    ```sql
    ()
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

()

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + d.myInt2.get + 1)))
```


*
    ```sql
    ()
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

()

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.get + d.myInt2.get + 1))
```


*
    ```sql
    ()
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

()

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.getOrElse(-1)))
```


*
    ```sql
    ()
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

()

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.orElse(d.myInt2)))
```


*
    ```sql
    ()
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

()

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.filter(_ < 2)))
```


*
    ```sql
    ()
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

()

```scala
OptCols.select.sortBy(_.myInt).nullsLast
```


*
    ```sql
    ()
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

()

```scala
OptCols.select.sortBy(_.myInt).nullsFirst
```


*
    ```sql
    ()
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

()

```scala
OptCols.select.sortBy(_.myInt).asc.nullsLast
```


*
    ```sql
    ()
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

()

```scala
OptCols.select.sortBy(_.myInt).asc.nullsFirst
```


*
    ```sql
    ()
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

()

```scala
OptCols.select.sortBy(_.myInt).desc.nullsLast
```


*
    ```sql
    ()
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

()

```scala
OptCols.select.sortBy(_.myInt).desc.nullsFirst
```


*
    ```sql
    ()
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



## Transaction
Usage of transactions, rollbacks, and savepoints
### Transaction.simple.commit

()

```scala
dbClient.transaction { implicit db =>
  db.run(Purchase.select.size) ==> 7

  db.run(Purchase.delete(_ => true)) ==> 7

  db.run(Purchase.select.size) ==> 0
}

dbClient.autoCommit.run(Purchase.select.size) ==> 0
```






### Transaction.simple.rollback

()

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

()

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

()

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

()

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

()

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

()

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





