# ScalaSql Reference Library

This page contains example queries for the ScalaSql, taken from the
ScalaSql test suite. You can use this as a reference to see what kinds
of operations ScalaSql supports and how these operations are translated
into raw SQL to be sent to the database for execution.

Note that ScalaSql may generate different SQL in certain cases for different
databases, due to differences in how each database parses SQL. These differences
are typically minor, and as long as you use the right `Dialect` for your database
ScalaSql should do the right thing for you.

## MySqlDialect
Operations specific to working with MySql Databases
### reverse



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



## ExprExprIntOps
Operations that can be performed on `Expr[T]` when `T` is numeric
### plus



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



## PostgresDialect
Operations specific to working with Postgres Databases
### ltrim2



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



## Returning
Queries using `INSERT` or `UPDATE` with `RETURNING`
### insert.single



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



## OnConflict
Queries using `ON CONFLICT DO UPDATE` or `ON CONFLICT DO NOTHING`
### ignore



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



## SqliteDialect
Operations specific to working with Sqlite Databases
### ltrim2



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



## HsqlDbDialect
Operations specific to working with HsqlDb Databases
### ltrim2



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



## DbApi
Basic usage of `db.*` operations such as `db.run`
### run

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






## Select
Basic `SELECT`` operations: map, filter, join, etc.
### constant

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



## Join
inner `JOIN`s, `JOIN ON`s, self-joins, `LEFT`/`RIGHT`/`OUTER` `JOIN`s
### joinFilter



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



## Insert
Basic `INSERT` operations
### single.simple



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



## Update
Basic `UPDATE` queries
### update



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



## Delete
Basic `DELETE` operations
### single



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



## CompoundSelect
Compound `SELECT` operations: sort, take, drop, union, unionAll, etc.
### sort.simple



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



## SubQuery
Queries that explicitly use subqueries (e.g. for `JOIN`s) or require subqueries to preserve the Scala semantics of the various operators
### sortTakeJoin



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



## UpdateJoin
`UPDATE` queries that use `JOIN`s
### update



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



## UpdateSubQuery
`UPDATE` queries that use Subqueries
### setSubquery



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



## ExprBooleanOps
Operations that can be performed on `Expr[Boolean]`
### and



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



## ExprIntOps
Operations that can be performed on `Expr[T]` when `T` is numeric
### plus



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



## ExprSeqNumericOps
Operations that can be performed on `Expr[Seq[T]]` where `T` is numeric
### sum



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



## ExprSeqOps
Operations that can be performed on `Expr[Seq[_]]`
### size



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



## ExprStringOps
Operations that can be performed on `Expr[String]`
### plus



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



## H2Dialect
Operations specific to working with H2 Databases
### ltrim2



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



## DataTypes
Basic operations on all the data types that ScalaSql supports mapping between Database types and Scala types
### constant



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



## Optional
Queries using columns that may be `NULL`, `Expr[Option[T]]` or `Option[T] in Scala
----



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



## Transaction
Usage of transactions, rollbacks, and savepoints
### simple.commit

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





