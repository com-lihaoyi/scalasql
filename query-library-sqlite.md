# sqlite
## scalasql.sqlite.CompoundSelectTests
### CompoundSelectTests.sort.simple

```scala
Product.select.sortBy(_.price).map(_.name)
```

```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price
```


```scala
Seq("Cookie", "Socks", "Face Mask", "Skate Board", "Guitar", "Camera")
```


### CompoundSelectTests.sort.twice

```scala
Purchase.select.sortBy(_.productId).asc.sortBy(_.shippingInfoId).desc
```

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


### CompoundSelectTests.sort.sortLimit

```scala
Product.select.sortBy(_.price).map(_.name).take(2)
```

```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2
```


```scala
Seq("Cookie", "Socks")
```


### CompoundSelectTests.sort.sortOffset

```scala
Product.select.sortBy(_.price).map(_.name).drop(2)
```

```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2147483647 OFFSET 2
```


```scala
Seq("Face Mask", "Skate Board", "Guitar", "Camera")
```


### CompoundSelectTests.sort.sortLimitTwiceHigher

```scala
Product.select.sortBy(_.price).map(_.name).take(2).take(3)
```

```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2
```


```scala
Seq("Cookie", "Socks")
```


### CompoundSelectTests.sort.sortLimitTwiceLower

```scala
Product.select.sortBy(_.price).map(_.name).take(2).take(1)
```

```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1
```


```scala
Seq("Cookie")
```


### CompoundSelectTests.sort.sortLimitOffset

```scala
Product.select.sortBy(_.price).map(_.name).drop(2).take(2)
```

```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2
```


```scala
Seq("Face Mask", "Skate Board")
```


### CompoundSelectTests.sort.sortLimitOffsetTwice

```scala
Product.select.sortBy(_.price).map(_.name).drop(2).drop(2).take(1)
```

```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 4
```


```scala
Seq("Guitar")
```


### CompoundSelectTests.sort.sortOffsetLimit

```scala
Product.select.sortBy(_.price).map(_.name).drop(2).take(2)
```

```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2
```


```scala
Seq("Face Mask", "Skate Board")
```


### CompoundSelectTests.sort.sortLimitOffset

```scala
Product.select.sortBy(_.price).map(_.name).take(2).drop(1)
```

```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 1
```


```scala
Seq("Socks")
```


### CompoundSelectTests.distinct

```scala
Purchase.select.sortBy(_.total).desc.take(3).map(_.shippingInfoId).distinct
```

```sql
SELECT DISTINCT subquery0.res as res
FROM (SELECT purchase0.shipping_info_id as res
  FROM purchase purchase0
  ORDER BY purchase0.total DESC
  LIMIT 3) subquery0
```


```scala
Seq(1, 2)
```


### CompoundSelectTests.flatMap

```scala
Purchase.select.sortBy(_.total).desc.take(3).flatMap { p =>
  Product.select.filter(_.id === p.productId).map(_.name)
}
```

```sql
SELECT product1.name as res
FROM (SELECT purchase0.product_id as res__product_id, purchase0.total as res__total
  FROM purchase purchase0
  ORDER BY res__total DESC
  LIMIT 3) subquery0, product product1
WHERE product1.id = subquery0.res__product_id
```


```scala
Seq("Camera", "Face Mask", "Guitar")
```


### CompoundSelectTests.sumBy

```scala
Purchase.select.sortBy(_.total).desc.take(3).sumBy(_.total)
```

```sql
SELECT SUM(subquery0.res__total) as res
FROM (SELECT purchase0.total as res__total
  FROM purchase purchase0
  ORDER BY res__total DESC
  LIMIT 3) subquery0
```


```scala
11788.0
```


### CompoundSelectTests.aggregate

```scala
Purchase.select.sortBy(_.total).desc.take(3).aggregate(p => (p.sumBy(_.total), p.avgBy(_.total)))
```

```sql
SELECT SUM(subquery0.res__total) as res__0, AVG(subquery0.res__total) as res__1
FROM (SELECT purchase0.total as res__total
  FROM purchase purchase0
  ORDER BY res__total DESC
  LIMIT 3) subquery0
```


```scala
(11788.0, 3929.0)
```


### CompoundSelectTests.union

```scala
Product.select.map(_.name.toLowerCase).union(Product.select.map(_.kebabCaseName.toLowerCase))
```

```sql
SELECT LOWER(product0.name) as res
FROM product product0
UNION
SELECT LOWER(product0.kebab_case_name) as res
FROM product product0
```


```scala
Seq("camera", "cookie", "face mask", "face-mask", "guitar", "skate board", "skate-board", "socks")
```


### CompoundSelectTests.unionAll

```scala
Product.select.map(_.name.toLowerCase).unionAll(Product.select.map(_.kebabCaseName.toLowerCase))
```

```sql
SELECT LOWER(product0.name) as res
FROM product product0
UNION ALL
SELECT LOWER(product0.kebab_case_name) as res
FROM product product0
```


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


### CompoundSelectTests.intersect

```scala
Product.select.map(_.name.toLowerCase).intersect(Product.select.map(_.kebabCaseName.toLowerCase))
```

```sql
SELECT LOWER(product0.name) as res
FROM product product0
INTERSECT
SELECT LOWER(product0.kebab_case_name) as res
FROM product product0
```


```scala
Seq("camera", "cookie", "guitar", "socks")
```


### CompoundSelectTests.except

```scala
Product.select.map(_.name.toLowerCase).except(Product.select.map(_.kebabCaseName.toLowerCase))
```

```sql
SELECT LOWER(product0.name) as res
FROM product product0
EXCEPT
SELECT LOWER(product0.kebab_case_name) as res
FROM product product0
```


```scala
Seq("face mask", "skate board")
```


### CompoundSelectTests.unionAllUnionSort

```scala
Product.select.map(_.name.toLowerCase).unionAll(Buyer.select.map(_.name.toLowerCase))
  .union(Product.select.map(_.kebabCaseName.toLowerCase)).sortBy(identity)
```

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


### CompoundSelectTests.unionAllUnionSortLimit

```scala
Product.select.map(_.name.toLowerCase).unionAll(Buyer.select.map(_.name.toLowerCase))
  .union(Product.select.map(_.kebabCaseName.toLowerCase)).sortBy(identity).drop(4).take(4)
```

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


```scala
Seq("guitar", "james bond", "li haoyi", "skate board")
```


### CompoundSelectTests.exceptAggregate

```scala
Product.select.map(p => (p.name.toLowerCase, p.price))
  // `p.name.toLowerCase` and  `p.kebabCaseName.toLowerCase` are not eliminated, because
  // they are important to the semantics of EXCEPT (and other non-UNION-ALL operators)
  .except(Product.select.map(p => (p.kebabCaseName.toLowerCase, p.price)))
  .aggregate(ps => (ps.maxBy(_._2), ps.minBy(_._2)))
```

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


```scala
(123.45, 8.88)
```


### CompoundSelectTests.unionAllAggregate

```scala
Product.select.map(p => (p.name.toLowerCase, p.price))
  // `p.name.toLowerCase` and  `p.kebabCaseName.toLowerCase` get eliminated,
  // as they are not selected by the enclosing query, and cannot affect the UNION ALL
  .unionAll(Product.select.map(p => (p.kebabCaseName.toLowerCase, p.price)))
  .aggregate(ps => (ps.maxBy(_._2), ps.minBy(_._2)))
```

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


```scala
(1000.0, 0.1)
```


## scalasql.sqlite.DataTypesTests
### DataTypesTests.constant

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



```scala
1
```


### DataTypesTests.constant

```scala
DataTypes.select
```



```scala
Seq(value)
```


### DataTypesTests.nonRoundTrip

```scala
NonRoundTripTypes.insert
  .values(_.myOffsetDateTime := value.myOffsetDateTime, _.myZonedDateTime := value.myZonedDateTime)
```



```scala
1
```


### DataTypesTests.nonRoundTrip

```scala
NonRoundTripTypes.select
```



```scala
Seq(normalize(value))
```


## scalasql.sqlite.DeleteTests
### DeleteTests.single

```scala
Purchase.delete(_.id `=` 2)
```

```sql
DELETE FROM purchase WHERE purchase.id = ?
```


```scala
1
```


### DeleteTests.single

```scala
Purchase.select
```



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


### DeleteTests.multiple

```scala
Purchase.delete(_.id <> 2)
```

```sql
DELETE FROM purchase WHERE purchase.id <> ?
```


```scala
6
```


### DeleteTests.multiple

```scala
Purchase.select
```



```scala
Seq(Purchase[Id](id = 2, shippingInfoId = 1, productId = 2, count = 3, total = 900.0))
```


### DeleteTests.all

```scala
Purchase.delete(_ => true)
```

```sql
DELETE FROM purchase WHERE ?
```


```scala
7
```


### DeleteTests.all

```scala
Purchase.select
```



```scala
Seq[Purchase[Id]](
  // all Deleted
)
```


## scalasql.sqlite.ExprBooleanOpsTests
### ExprBooleanOpsTests.and

```scala
Expr(true) && Expr(true)
```

```sql
SELECT ? AND ? as res
```


```scala
true
```


### ExprBooleanOpsTests.or

```scala
Expr(false) || Expr(false)
```

```sql
SELECT ? OR ? as res
```


```scala
false
```


### ExprBooleanOpsTests.or

```scala
!Expr(false)
```

```sql
SELECT NOT ? as res
```


```scala
true
```


## scalasql.sqlite.ExprIntOpsTests
### ExprIntOpsTests.plus

```scala
Expr(6) + Expr(2)
```

```sql
SELECT ? + ? as res
```


```scala
8
```


### ExprIntOpsTests.minus

```scala
Expr(6) - Expr(2)
```

```sql
SELECT ? - ? as res
```


```scala
4
```


### ExprIntOpsTests.times

```scala
Expr(6) * Expr(2)
```

```sql
SELECT ? * ? as res
```


```scala
12
```


### ExprIntOpsTests.divide

```scala
Expr(6) / Expr(2)
```

```sql
SELECT ? / ? as res
```


```scala
3
```


### ExprIntOpsTests.modulo

```scala
Expr(6) % Expr(2)
```

```sql
SELECT MOD(?, ?) as res
```


```scala
0
```


### ExprIntOpsTests.bitwiseAnd

```scala
Expr(6) & Expr(2)
```

```sql
SELECT ? & ? as res
```


```scala
2
```


### ExprIntOpsTests.bitwiseOr

```scala
Expr(6) | Expr(3)
```

```sql
SELECT ? | ? as res
```


```scala
7
```


### ExprIntOpsTests.between

```scala
Expr(4).between(Expr(2), Expr(6))
```

```sql
SELECT ? BETWEEN ? AND ? as res
```


```scala
true
```


### ExprIntOpsTests.unaryPlus

```scala
+Expr(-4)
```

```sql
SELECT +? as res
```


```scala
-4
```


### ExprIntOpsTests.unaryMinus

```scala
-Expr(-4)
```

```sql
SELECT -? as res
```


```scala
4
```


### ExprIntOpsTests.unaryTilde

```scala
~Expr(-4)
```

```sql
SELECT ~? as res
```


```scala
3
```


### ExprIntOpsTests.abs

```scala
Expr(-4).abs
```

```sql
SELECT ABS(?) as res
```


```scala
4
```


### ExprIntOpsTests.mod

```scala
Expr(8).mod(Expr(3))
```

```sql
SELECT MOD(?, ?) as res
```


```scala
2
```


### ExprIntOpsTests.ceil

```scala
Expr(4.3).ceil
```

```sql
SELECT CEIL(?) as res
```


```scala
5.0
```


### ExprIntOpsTests.floor

```scala
Expr(4.7).floor
```

```sql
SELECT FLOOR(?) as res
```


```scala
4.0
```


### ExprIntOpsTests.floor

```scala
Expr(4.7).floor
```

```sql
SELECT FLOOR(?) as res
```


```scala
4.0
```


## scalasql.sqlite.ExprSeqNumericOpsTests
### ExprSeqNumericOpsTests.sum

```scala
Purchase.select.map(_.count).sum
```

```sql
SELECT SUM(purchase0.count) as res FROM purchase purchase0
```


```scala
140
```


### ExprSeqNumericOpsTests.min

```scala
Purchase.select.map(_.count).min
```

```sql
SELECT MIN(purchase0.count) as res FROM purchase purchase0
```


```scala
3
```


### ExprSeqNumericOpsTests.max

```scala
Purchase.select.map(_.count).max
```

```sql
SELECT MAX(purchase0.count) as res FROM purchase purchase0
```


```scala
100
```


### ExprSeqNumericOpsTests.avg

```scala
Purchase.select.map(_.count).avg
```

```sql
SELECT AVG(purchase0.count) as res FROM purchase purchase0
```


```scala
20
```


## scalasql.sqlite.ExprSeqOpsTests
### ExprSeqOpsTests.size

```scala
Purchase.select.size
```

```sql
SELECT COUNT(1) as res FROM purchase purchase0
```


```scala
7
```


### ExprSeqOpsTests.sumBy.simple

```scala
Purchase.select.sumBy(_.count)
```

```sql
SELECT SUM(purchase0.count) as res FROM purchase purchase0
```


```scala
140
```


### ExprSeqOpsTests.sumBy.some

```scala
Purchase.select.sumByOpt(_.count)
```

```sql
SELECT SUM(purchase0.count) as res FROM purchase purchase0
```


```scala
Option(140)
```


### ExprSeqOpsTests.sumBy.none

```scala
Purchase.select.filter(_ => false).sumByOpt(_.count)
```

```sql
SELECT SUM(purchase0.count) as res FROM purchase purchase0 WHERE ?
```


```scala
Option.empty[Int]
```


### ExprSeqOpsTests.minBy.simple

```scala
Purchase.select.minBy(_.count)
```

```sql
SELECT MIN(purchase0.count) as res FROM purchase purchase0
```


```scala
3
```


### ExprSeqOpsTests.minBy.some

```scala
Purchase.select.minByOpt(_.count)
```

```sql
SELECT MIN(purchase0.count) as res FROM purchase purchase0
```


```scala
Option(3)
```


### ExprSeqOpsTests.minBy.none

```scala
Purchase.select.filter(_ => false).minByOpt(_.count)
```

```sql
SELECT MIN(purchase0.count) as res FROM purchase purchase0 WHERE ?
```


```scala
Option.empty[Int]
```


### ExprSeqOpsTests.maxBy.simple

```scala
Purchase.select.maxBy(_.count)
```

```sql
SELECT MAX(purchase0.count) as res FROM purchase purchase0
```


```scala
100
```


### ExprSeqOpsTests.maxBy.some

```scala
Purchase.select.maxByOpt(_.count)
```

```sql
SELECT MAX(purchase0.count) as res FROM purchase purchase0
```


```scala
Option(100)
```


### ExprSeqOpsTests.maxBy.none

```scala
Purchase.select.filter(_ => false).maxByOpt(_.count)
```

```sql
SELECT MAX(purchase0.count) as res FROM purchase purchase0 WHERE ?
```


```scala
Option.empty[Int]
```


### ExprSeqOpsTests.avgBy.simple

```scala
Purchase.select.avgBy(_.count)
```

```sql
SELECT AVG(purchase0.count) as res FROM purchase purchase0
```


```scala
20
```


### ExprSeqOpsTests.avgBy.some

```scala
Purchase.select.avgByOpt(_.count)
```

```sql
SELECT AVG(purchase0.count) as res FROM purchase purchase0
```


```scala
Option(20)
```


### ExprSeqOpsTests.avgBy.none

```scala
Purchase.select.filter(_ => false).avgByOpt(_.count)
```

```sql
SELECT AVG(purchase0.count) as res FROM purchase purchase0 WHERE ?
```


```scala
Option.empty[Int]
```


## scalasql.sqlite.ExprStringOpsTests
### ExprStringOpsTests.plus

```scala
Expr("hello") + Expr("world")
```

```sql
SELECT ? || ? as res
```


```scala
"helloworld"
```


### ExprStringOpsTests.like

```scala
Expr("hello").like("he%")
```

```sql
SELECT ? LIKE ? as res
```


```scala
true
```


### ExprStringOpsTests.length

```scala
Expr("hello").length
```

```sql
SELECT LENGTH(?) as res
```


```scala
5
```


### ExprStringOpsTests.octetLength

```scala
Expr("叉烧包").octetLength
```

```sql
SELECT OCTET_LENGTH(?) as res
```


```scala
9
```


### ExprStringOpsTests.position

```scala
Expr("hello").indexOf("ll")
```

```sql
SELECT INSTR(?, ?) as res
```


```scala
3
```


### ExprStringOpsTests.toLowerCase

```scala
Expr("Hello").toLowerCase
```

```sql
SELECT LOWER(?) as res
```


```scala
"hello"
```


### ExprStringOpsTests.trim

```scala
Expr("  Hello ").trim
```

```sql
SELECT TRIM(?) as res
```


```scala
"Hello"
```


### ExprStringOpsTests.ltrim

```scala
Expr("  Hello ").ltrim
```

```sql
SELECT LTRIM(?) as res
```


```scala
"Hello "
```


### ExprStringOpsTests.rtrim

```scala
Expr("  Hello ").rtrim
```

```sql
SELECT RTRIM(?) as res
```


```scala
"  Hello"
```


### ExprStringOpsTests.substring

```scala
Expr("Hello").substring(2, 2)
```

```sql
SELECT SUBSTRING(?, ?, ?) as res
```


```scala
"el"
```


## scalasql.sqlite.InsertTests
### InsertTests.single.simple

```scala
Buyer.insert
  .values(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"), _.id := 4)
```

```sql
INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)
```


```scala
1
```


### InsertTests.single.simple

```scala
Buyer.select.filter(_.name `=` "test buyer")
```



```scala
Seq(Buyer[Id](4, "test buyer", LocalDate.parse("2023-09-09")))
```


### InsertTests.single.partial

```scala
Buyer.insert.values(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"))
```

```sql
INSERT INTO buyer (name, date_of_birth) VALUES (?, ?)
```


```scala
1
```


### InsertTests.single.partial

```scala
Buyer.select.filter(_.name `=` "test buyer")
```



```scala
Seq(Buyer[Id](4, "test buyer", LocalDate.parse("2023-09-09")))
```


### InsertTests.batch.simple

```scala
Buyer.insert.batched(_.name, _.dateOfBirth, _.id)(
  ("test buyer A", LocalDate.parse("2001-04-07"), 4),
  ("test buyer B", LocalDate.parse("2002-05-08"), 5),
  ("test buyer C", LocalDate.parse("2003-06-09"), 6)
)
```

```sql
INSERT INTO buyer (name, date_of_birth, id)
VALUES
  (?, ?, ?),
  (?, ?, ?),
  (?, ?, ?)
```


```scala
3
```


### InsertTests.batch.simple

```scala
Buyer.select
```



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


### InsertTests.batch.partial

```scala
Buyer.insert.batched(_.name, _.dateOfBirth)(
  ("test buyer A", LocalDate.parse("2001-04-07")),
  ("test buyer B", LocalDate.parse("2002-05-08")),
  ("test buyer C", LocalDate.parse("2003-06-09"))
)
```

```sql
INSERT INTO buyer (name, date_of_birth)
VALUES (?, ?), (?, ?), (?, ?)
```


```scala
3
```


### InsertTests.batch.partial

```scala
Buyer.select
```



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


### InsertTests.select.caseclass

```scala
Buyer.insert.select(
  identity,
  Buyer.select.filter(_.name <> "Li Haoyi").map(b => b.copy(id = b.id + Buyer.select.maxBy(_.id)))
)
```

```sql
INSERT INTO buyer (id, name, date_of_birth)
SELECT
  buyer0.id + (SELECT MAX(buyer0.id) as res FROM buyer buyer0) as res__id,
  buyer0.name as res__name,
  buyer0.date_of_birth as res__date_of_birth
FROM buyer buyer0
WHERE buyer0.name <> ?
```


```scala
2
```


### InsertTests.select.caseclass

```scala
Buyer.select
```



```scala
Seq(
  Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
  Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
  Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
  Buyer[Id](4, "James Bond", LocalDate.parse("2001-02-03")),
  Buyer[Id](5, "叉烧包", LocalDate.parse("1923-11-12"))
)
```


### InsertTests.select.simple

```scala
Buyer.insert.select(
  x => (x.name, x.dateOfBirth),
  Buyer.select.map(x => (x.name, x.dateOfBirth)).filter(_._1 <> "Li Haoyi")
)
```

```sql
INSERT INTO buyer (name, date_of_birth)
SELECT buyer0.name as res__0, buyer0.date_of_birth as res__1
FROM buyer buyer0
WHERE buyer0.name <> ?
```


```scala
2
```


### InsertTests.select.simple

```scala
Buyer.select
```



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


## scalasql.sqlite.JoinTests
### JoinTests.joinFilter

```scala
Buyer.select.joinOn(ShippingInfo)(_.id `=` _.buyerId).filter(_._1.name `=` "叉烧包")
```

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


### JoinTests.joinSelectFilter

```scala
Buyer.select.joinOn(ShippingInfo)(_.id `=` _.buyerId).filter(_._1.name `=` "叉烧包")
```

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


### JoinTests.joinFilterMap

```scala
Buyer.select.joinOn(ShippingInfo)(_.id `=` _.buyerId).filter(_._1.name `=` "James Bond")
  .map(_._2.shippingDate)
```

```sql
SELECT shipping_info1.shipping_date as res
FROM buyer buyer0
JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
WHERE buyer0.name = ?
```


```scala
Seq(LocalDate.parse("2012-04-05"))
```


### JoinTests.selfJoin

```scala
Buyer.select.joinOn(Buyer)(_.id `=` _.id)
```

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


### JoinTests.selfJoin2

```scala
Buyer.select.joinOn(Buyer)(_.id <> _.id)
```

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


### JoinTests.flatMap

```scala
Buyer.select.flatMap(c => ShippingInfo.select.map((c, _))).filter { case (c, p) =>
  c.id `=` p.buyerId && c.name `=` "James Bond"
}.map(_._2.shippingDate)
```

```sql
SELECT shipping_info1.shipping_date as res
FROM buyer buyer0, shipping_info shipping_info1
WHERE buyer0.id = shipping_info1.buyer_id
AND buyer0.name = ?
```


```scala
Seq(LocalDate.parse("2012-04-05"))
```


### JoinTests.flatMap2

```scala
Buyer.select
  .flatMap(c => ShippingInfo.select.filter { p => c.id `=` p.buyerId && c.name `=` "James Bond" })
  .map(_.shippingDate)
```

```sql
SELECT shipping_info1.shipping_date as res
FROM buyer buyer0, shipping_info shipping_info1
WHERE buyer0.id = shipping_info1.buyer_id
AND buyer0.name = ?
```


```scala
Seq(LocalDate.parse("2012-04-05"))
```


### JoinTests.leftJoin

```scala
Buyer.select.leftJoin(ShippingInfo)(_.id `=` _.buyerId)
```

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


### JoinTests.rightJoin

```scala
ShippingInfo.select.rightJoin(Buyer)(_.buyerId `=` _.id)
```

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


### JoinTests.outerJoin

```scala
ShippingInfo.select.outerJoin(Buyer)(_.buyerId `=` _.id)
```

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


## scalasql.sqlite.OnConflictTests
### OnConflictTests.ignore

```scala
Buyer.insert.values(
  _.name := "test buyer",
  _.dateOfBirth := LocalDate.parse("2023-09-09"),
  _.id := 1 // This should cause a primary key conflict
).onConflictIgnore(_.id)
```

```sql
INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING
```


```scala
0
```


### OnConflictTests.ignore.returningEmpty

```scala
Buyer.insert.values(
  _.name := "test buyer",
  _.dateOfBirth := LocalDate.parse("2023-09-09"),
  _.id := 1 // This should cause a primary key conflict
).onConflictIgnore(_.id).returning(_.name)
```

```sql
INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)
ON CONFLICT (id) DO NOTHING
RETURNING buyer.name as res
```


```scala
Seq.empty[String]
```


### OnConflictTests.ignore

```scala
Buyer.insert.values(
  _.name := "test buyer",
  _.dateOfBirth := LocalDate.parse("2023-09-09"),
  _.id := 1 // This should cause a primary key conflict
).onConflictIgnore(_.id)
```

```sql
INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING
```


```scala
0
```


### OnConflictTests.ignore.returningOne

```scala
Buyer.insert.values(
  _.name := "test buyer",
  _.dateOfBirth := LocalDate.parse("2023-09-09"),
  _.id := 4 // This should cause a primary key conflict
).onConflictIgnore(_.id).returning(_.name)
```

```sql
INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)
ON CONFLICT (id) DO NOTHING
RETURNING buyer.name as res
```


```scala
Seq("test buyer")
```


### OnConflictTests.update

```scala
Buyer.insert.values(
  _.name := "test buyer",
  _.dateOfBirth := LocalDate.parse("2023-09-09"),
  _.id := 1 // This should cause a primary key conflict
).onConflictUpdate(_.id)(_.name := "TEST BUYER CONFLICT")
```

```sql
INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET name = ?
```


```scala
1
```


### OnConflictTests.update

```scala
Buyer.select
```



```scala
Seq(
  Buyer[Id](1, "TEST BUYER CONFLICT", LocalDate.parse("2001-02-03")),
  Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
  Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
)
```


### OnConflictTests.computed

```scala
Buyer.insert.values(
  _.name := "test buyer",
  _.dateOfBirth := LocalDate.parse("2023-09-09"),
  _.id := 1 // This should cause a primary key conflict
).onConflictUpdate(_.id)(v => v.name := v.name.toUpperCase)
```

```sql
INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET name = UPPER(buyer.name)
```


```scala
1
```


### OnConflictTests.computed

```scala
Buyer.select
```



```scala
Seq(
  Buyer[Id](1, "JAMES BOND", LocalDate.parse("2001-02-03")),
  Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
  Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
)
```


### OnConflictTests.returning

```scala
Buyer.insert.values(
  _.name := "test buyer",
  _.dateOfBirth := LocalDate.parse("2023-09-09"),
  _.id := 1 // This should cause a primary key conflict
).onConflictUpdate(_.id)(v => v.name := v.name.toUpperCase).returning(_.name).single
```

```sql
INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)
ON CONFLICT (id) DO UPDATE
SET name = UPPER(buyer.name)
RETURNING buyer.name as res
```


```scala
"JAMES BOND"
```


## scalasql.sqlite.OptionalTests
### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.selectAll

```scala
OptCols.select
```

```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](Some(3), None),
  OptCols[Id](None, Some(4))
)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.groupByMaxGet

```scala
OptCols.select.groupBy(_.myInt)(_.maxByOpt(_.myInt2.get))
```

```sql
SELECT opt_cols0.my_int as res__0, MAX(opt_cols0.my_int2) as res__1
FROM opt_cols opt_cols0
GROUP BY opt_cols0.my_int
```


```scala
Seq(None -> Some(4), Some(1) -> Some(2), Some(3) -> None)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.isDefined

```scala
OptCols.select.filter(_.myInt.isDefined)
```

```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
WHERE opt_cols0.my_int IS NOT NULL
```


```scala
Seq(OptCols[Id](Some(1), Some(2)), OptCols[Id](Some(3), None))
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.isEmpty

```scala
OptCols.select.filter(_.myInt.isEmpty)
```

```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
WHERE opt_cols0.my_int IS NULL
```


```scala
Seq(OptCols[Id](None, None), OptCols[Id](None, Some(4)))
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.sqlEquals.nonOptionHit

```scala
OptCols.select.filter(_.myInt `=` 1)
```

```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
WHERE opt_cols0.my_int = ?
```


```scala
Seq(OptCols[Id](Some(1), Some(2)))
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.sqlEquals.nonOptionMiss

```scala
OptCols.select.filter(_.myInt `=` 2)
```

```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
WHERE opt_cols0.my_int = ?
```


```scala
Seq[OptCols[Id]]()
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.sqlEquals.optionMiss

```scala
OptCols.select.filter(_.myInt `=` Option.empty[Int])
```

```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
WHERE opt_cols0.my_int = ?
```


```scala
Seq[OptCols[Id]]()
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.scalaEquals.someHit

```scala
OptCols.select.filter(_.myInt === Option(1))
```

```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
WHERE (opt_cols0.my_int IS NULL AND ? IS NULL) OR opt_cols0.my_int = ?
```


```scala
Seq(OptCols[Id](Some(1), Some(2)))
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.scalaEquals.noneHit

```scala
OptCols.select.filter(_.myInt === Option.empty[Int])
```

```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
WHERE (opt_cols0.my_int IS NULL AND ? IS NULL) OR opt_cols0.my_int = ?
```


```scala
Seq(OptCols[Id](None, None), OptCols[Id](None, Some(4)))
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.map

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + 10)))
```

```sql
SELECT
  opt_cols0.my_int + ? as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](Some(11), Some(2)),
  OptCols[Id](Some(13), None),
  OptCols[Id](None, Some(4))
)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.map2

```scala
OptCols.select.map(_.myInt.map(_ + 10))
```

```sql
SELECT opt_cols0.my_int + ? as res FROM opt_cols opt_cols0
```


```scala
Seq(None, Some(11), Some(13), None)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.flatMap

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.flatMap(v => d.myInt2.map(v2 => v + v2 + 10))))
```

```sql
SELECT
  opt_cols0.my_int + opt_cols0.my_int2 + ? as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](Some(13), Some(2)),
  // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4))
)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.mapGet

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + d.myInt2.get + 1)))
```

```sql
SELECT
  opt_cols0.my_int + opt_cols0.my_int2 + ? as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](Some(4), Some(2)),
  // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4))
)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.rawGet

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.get + d.myInt2.get + 1))
```

```sql
SELECT
  opt_cols0.my_int + opt_cols0.my_int2 + ? as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](Some(4), Some(2)),
  // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4))
)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.getOrElse

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.getOrElse(-1)))
```

```sql
SELECT
  COALESCE(opt_cols0.my_int, ?) as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


```scala
Seq(
  OptCols[Id](Some(-1), None),
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](Some(3), None),
  OptCols[Id](Some(-1), Some(4))
)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.orElse

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.orElse(d.myInt2)))
```

```sql
SELECT
  COALESCE(opt_cols0.my_int, opt_cols0.my_int2) as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](Some(3), None),
  OptCols[Id](Some(4), Some(4))
)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.filter

```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.filter(_ < 2)))
```

```sql
SELECT
  CASE
    WHEN opt_cols0.my_int < ? THEN opt_cols0.my_int
    ELSE NULL
  END as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4))
)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.sorting.nullsLast

```scala
OptCols.select.sortBy(_.myInt).nullsLast
```

```sql
SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
ORDER BY res__my_int NULLS LAST
```


```scala
Seq(
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](Some(3), None),
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4))
)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.sorting.nullsFirst

```scala
OptCols.select.sortBy(_.myInt).nullsFirst
```

```sql
SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
ORDER BY res__my_int NULLS FIRST
```


```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4)),
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](Some(3), None)
)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.sorting.ascNullsLast

```scala
OptCols.select.sortBy(_.myInt).asc.nullsLast
```

```sql
SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
ORDER BY res__my_int ASC NULLS LAST
```


```scala
Seq(
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](Some(3), None),
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4))
)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.sorting.ascNullsFirst

```scala
OptCols.select.sortBy(_.myInt).asc.nullsFirst
```

```sql
SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
ORDER BY res__my_int ASC NULLS FIRST
```


```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4)),
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](Some(3), None)
)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.sorting.descNullsLast

```scala
OptCols.select.sortBy(_.myInt).desc.nullsLast
```

```sql
SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
ORDER BY res__my_int DESC NULLS LAST
```


```scala
Seq(
  OptCols[Id](Some(3), None),
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4))
)
```


### OptionalTests

```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



```scala
4
```


### OptionalTests.sorting.descNullsFirst

```scala
OptCols.select.sortBy(_.myInt).desc.nullsFirst
```

```sql
SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
ORDER BY res__my_int DESC NULLS FIRST
```


```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4)),
  OptCols[Id](Some(3), None),
  OptCols[Id](Some(1), Some(2))
)
```


## scalasql.sqlite.ReturningTests
### ReturningTests.insert.single

```scala
Buyer.insert.values(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"))
  .returning(_.id)
```

```sql
INSERT INTO buyer (name, date_of_birth) VALUES (?, ?) RETURNING buyer.id as res
```


```scala
Seq(4)
```


### ReturningTests.insert.single

```scala
Buyer.select.filter(_.name `=` "test buyer")
```



```scala
Seq(Buyer[Id](4, "test buyer", LocalDate.parse("2023-09-09")))
```


### ReturningTests.insert.dotSingle

```scala
Buyer.insert.values(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"))
  .returning(_.id).single
```

```sql
INSERT INTO buyer (name, date_of_birth) VALUES (?, ?) RETURNING buyer.id as res
```


```scala
4
```


### ReturningTests.insert.dotSingle

```scala
Buyer.select.filter(_.name `=` "test buyer")
```



```scala
Seq(Buyer[Id](4, "test buyer", LocalDate.parse("2023-09-09")))
```


### ReturningTests.insert.multiple

```scala
Buyer.insert.batched(_.name, _.dateOfBirth)(
  ("test buyer A", LocalDate.parse("2001-04-07")),
  ("test buyer B", LocalDate.parse("2002-05-08")),
  ("test buyer C", LocalDate.parse("2003-06-09"))
).returning(_.id)
```

```sql
INSERT INTO buyer (name, date_of_birth)
VALUES
  (?, ?),
  (?, ?),
  (?, ?)
RETURNING buyer.id as res
```


```scala
Seq(4, 5, 6)
```


### ReturningTests.insert.multiple

```scala
Buyer.select
```



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


### ReturningTests.insert.select

```scala
Buyer.insert.select(
  x => (x.name, x.dateOfBirth),
  Buyer.select.map(x => (x.name, x.dateOfBirth)).filter(_._1 <> "Li Haoyi")
).returning(_.id)
```

```sql
INSERT INTO buyer (name, date_of_birth)
SELECT
  buyer0.name as res__0,
  buyer0.date_of_birth as res__1
FROM buyer buyer0
WHERE buyer0.name <> ?
RETURNING buyer.id as res
```


```scala
Seq(4, 5)
```


### ReturningTests.insert.select

```scala
Buyer.select
```



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


### ReturningTests.update.single

```scala
Buyer.update(_.name `=` "James Bond").set(_.dateOfBirth := LocalDate.parse("2019-04-07"))
  .returning(_.id)
```

```sql
UPDATE buyer SET date_of_birth = ? WHERE buyer.name = ? RETURNING buyer.id as res
```


```scala
Seq(1)
```


### ReturningTests.update.single

```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



```scala
Seq(LocalDate.parse("2019-04-07"))
```


### ReturningTests.update.multiple

```scala
Buyer.update(_.name `=` "James Bond")
  .set(_.dateOfBirth := LocalDate.parse("2019-04-07"), _.name := "John Dee")
  .returning(c => (c.id, c.name, c.dateOfBirth))
```

```sql
UPDATE buyer
SET date_of_birth = ?, name = ? WHERE buyer.name = ?
RETURNING buyer.id as res__0, buyer.name as res__1, buyer.date_of_birth as res__2
```


```scala
Seq((1, "John Dee", LocalDate.parse("2019-04-07")))
```


### ReturningTests.delete

```scala
Purchase.delete(_.shippingInfoId `=` 1).returning(_.total)
```

```sql
DELETE FROM purchase WHERE purchase.shipping_info_id = ? RETURNING purchase.total as res
```


```scala
Seq(888.0, 900.0, 15.7)
```


### ReturningTests.delete

```scala
Purchase.select
```



```scala
Seq(
  // id=1,2,3 had shippingInfoId=1 and thus got deleted
  Purchase[Id](id = 4, shippingInfoId = 2, productId = 4, count = 4, total = 493.8),
  Purchase[Id](id = 5, shippingInfoId = 2, productId = 5, count = 10, total = 10000.0),
  Purchase[Id](id = 6, shippingInfoId = 3, productId = 1, count = 5, total = 44.4),
  Purchase[Id](id = 7, shippingInfoId = 3, productId = 6, count = 13, total = 1.3)
)
```


## scalasql.sqlite.SelectTests
### SelectTests.constant

```scala
Expr(1)
```

```sql
SELECT ? as res
```


```scala
1
```


### SelectTests.table

```scala
Buyer.select
```

```sql
SELECT
  buyer0.id as res__id,
  buyer0.name as res__name,
  buyer0.date_of_birth as res__date_of_birth
FROM buyer buyer0
```


```scala
Seq(
  Buyer[Id](id = 1, name = "James Bond", dateOfBirth = LocalDate.parse("2001-02-03")),
  Buyer[Id](id = 2, name = "叉烧包", dateOfBirth = LocalDate.parse("1923-11-12")),
  Buyer[Id](id = 3, name = "Li Haoyi", dateOfBirth = LocalDate.parse("1965-08-09"))
)
```


### SelectTests.filter.single

```scala
ShippingInfo.select.filter(_.buyerId `=` 2)
```

```sql
SELECT
  shipping_info0.id as res__id,
  shipping_info0.buyer_id as res__buyer_id,
  shipping_info0.shipping_date as res__shipping_date
FROM shipping_info shipping_info0
WHERE shipping_info0.buyer_id = ?
```


```scala
Seq(
  ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03")),
  ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06"))
)
```


### SelectTests.filter.multiple

```scala
ShippingInfo.select.filter(_.buyerId `=` 2).filter(_.shippingDate `=` LocalDate.parse("2012-05-06"))
```

```sql
SELECT
  shipping_info0.id as res__id,
  shipping_info0.buyer_id as res__buyer_id,
  shipping_info0.shipping_date as res__shipping_date
FROM shipping_info shipping_info0
WHERE shipping_info0.buyer_id = ?
AND shipping_info0.shipping_date = ?
```


```scala
Seq(ShippingInfo[Id](id = 3, buyerId = 2, shippingDate = LocalDate.parse("2012-05-06")))
```


### SelectTests.filter.dotSingle.pass

```scala
ShippingInfo.select.filter(_.buyerId `=` 2).filter(_.shippingDate `=` LocalDate.parse("2012-05-06"))
  .single
```

```sql
SELECT
  shipping_info0.id as res__id,
  shipping_info0.buyer_id as res__buyer_id,
  shipping_info0.shipping_date as res__shipping_date
FROM shipping_info shipping_info0
WHERE shipping_info0.buyer_id = ?
AND shipping_info0.shipping_date = ?
```


```scala
ShippingInfo[Id](id = 3, buyerId = 2, shippingDate = LocalDate.parse("2012-05-06"))
```


### SelectTests.filter.combined

```scala
ShippingInfo.select.filter(p => p.buyerId `=` 2 && p.shippingDate `=` LocalDate.parse("2012-05-06"))
```

```sql
SELECT
  shipping_info0.id as res__id,
  shipping_info0.buyer_id as res__buyer_id,
  shipping_info0.shipping_date as res__shipping_date
FROM shipping_info shipping_info0
WHERE shipping_info0.buyer_id = ?
AND shipping_info0.shipping_date = ?
```


```scala
Seq(ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06")))
```


### SelectTests.map.single

```scala
Buyer.select.map(_.name)
```

```sql
SELECT buyer0.name as res FROM buyer buyer0
```


```scala
Seq("James Bond", "叉烧包", "Li Haoyi")
```


### SelectTests.map.tuple2

```scala
Buyer.select.map(c => (c.name, c.id))
```

```sql
SELECT buyer0.name as res__0, buyer0.id as res__1 FROM buyer buyer0
```


```scala
Seq(("James Bond", 1), ("叉烧包", 2), ("Li Haoyi", 3))
```


### SelectTests.map.tuple3

```scala
Buyer.select.map(c => (c.name, c.id, c.dateOfBirth))
```

```sql
SELECT
  buyer0.name as res__0,
  buyer0.id as res__1,
  buyer0.date_of_birth as res__2
FROM buyer buyer0
```


```scala
Seq(
  ("James Bond", 1, LocalDate.parse("2001-02-03")),
  ("叉烧包", 2, LocalDate.parse("1923-11-12")),
  ("Li Haoyi", 3, LocalDate.parse("1965-08-09"))
)
```


### SelectTests.map.interpolateInMap

```scala
Product.select.map(_.price * 2)
```

```sql
SELECT product0.price * ? as res FROM product product0
```


```scala
Seq(17.76, 600, 6.28, 246.9, 2000.0, 0.2)
```


### SelectTests.map.heterogenousTuple

```scala
Buyer.select.map(c => (c.id, c))
```

```sql
SELECT
  buyer0.id as res__0,
  buyer0.id as res__1__id,
  buyer0.name as res__1__name,
  buyer0.date_of_birth as res__1__date_of_birth
FROM buyer buyer0
```


```scala
Seq(
  (1, Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03"))),
  (2, Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))),
  (3, Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")))
)
```


### SelectTests.exprQuery

```scala
Product.select.map(p =>
  (
    p.name,
    Purchase.select.filter(_.productId === p.id).sortBy(_.total).desc.take(1).map(_.total).exprQuery
  )
)
```

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


### SelectTests.subquery

```scala
Buyer.select.subquery.map(_.name)
```

```sql
SELECT subquery0.res__name as res
FROM (SELECT buyer0.name as res__name FROM buyer buyer0) subquery0
```


```scala
Seq("James Bond", "叉烧包", "Li Haoyi")
```


### SelectTests.filterMap

```scala
Product.select.filter(_.price < 100).map(_.name)
```

```sql
SELECT product0.name as res FROM product product0 WHERE product0.price < ?
```


```scala
Seq("Face Mask", "Socks", "Cookie")
```


### SelectTests.aggregate.single

```scala
Purchase.select.aggregate(_.sumBy(_.total))
```

```sql
SELECT SUM(purchase0.total) as res FROM purchase purchase0
```


```scala
12343.2
```


### SelectTests.aggregate.multiple

```scala
Purchase.select.aggregate(q => (q.sumBy(_.total), q.maxBy(_.total)))
```

```sql
SELECT SUM(purchase0.total) as res__0, MAX(purchase0.total) as res__1 FROM purchase purchase0
```


```scala
(12343.2, 10000.0)
```


### SelectTests.groupBy.simple

```scala
Purchase.select.groupBy(_.productId)(_.sumBy(_.total))
```

```sql
SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
FROM purchase purchase0
GROUP BY purchase0.product_id
```


```scala
Seq((1, 932.4), (2, 900.0), (3, 15.7), (4, 493.8), (5, 10000.0), (6, 1.30))
```


### SelectTests.groupBy.having

```scala
Purchase.select.groupBy(_.productId)(_.sumBy(_.total)).filter(_._2 > 100).filter(_._1 > 1)
```

```sql
SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
FROM purchase purchase0
GROUP BY purchase0.product_id
HAVING SUM(purchase0.total) > ? AND purchase0.product_id > ?
```


```scala
Seq((2, 900.0), (4, 493.8), (5, 10000.0))
```


### SelectTests.groupBy.filterHaving

```scala
Purchase.select.filter(_.count > 5).groupBy(_.productId)(_.sumBy(_.total)).filter(_._2 > 100)
```

```sql
SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
FROM purchase purchase0
WHERE purchase0.count > ?
GROUP BY purchase0.product_id
HAVING SUM(purchase0.total) > ?
```


```scala
Seq((1, 888.0), (5, 10000.0))
```


### SelectTests.distinct.nondistinct

```scala
Purchase.select.map(_.shippingInfoId)
```

```sql
SELECT purchase0.shipping_info_id as res FROM purchase purchase0
```


```scala
Seq(1, 1, 1, 2, 2, 3, 3)
```


### SelectTests.distinct.distinct

```scala
Purchase.select.map(_.shippingInfoId).distinct
```

```sql
SELECT DISTINCT purchase0.shipping_info_id as res FROM purchase purchase0
```


```scala
Seq(1, 2, 3)
```


### SelectTests.contains

```scala
Buyer.select.filter(b => ShippingInfo.select.map(_.buyerId).contains(b.id))
```

```sql
SELECT buyer0.id as res__id, buyer0.name as res__name, buyer0.date_of_birth as res__date_of_birth
FROM buyer buyer0
WHERE buyer0.id in (SELECT shipping_info0.buyer_id as res FROM shipping_info shipping_info0)
```


```scala
Seq(
  Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
  Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
)
```


### SelectTests.nonEmpty

```scala
Buyer.select.map(b => (b.name, ShippingInfo.select.filter(_.buyerId `=` b.id).map(_.id).nonEmpty))
```

```sql
SELECT
  buyer0.name as res__0,
  EXISTS (SELECT
    shipping_info0.id as res
    FROM shipping_info shipping_info0
    WHERE shipping_info0.buyer_id = buyer0.id) as res__1
FROM buyer buyer0
```


```scala
Seq(("James Bond", true), ("叉烧包", true), ("Li Haoyi", false))
```


### SelectTests.isEmpty

```scala
Buyer.select.map(b => (b.name, ShippingInfo.select.filter(_.buyerId `=` b.id).map(_.id).isEmpty))
```

```sql
SELECT
  buyer0.name as res__0,
  NOT EXISTS (SELECT
    shipping_info0.id as res
    FROM shipping_info shipping_info0
    WHERE shipping_info0.buyer_id = buyer0.id) as res__1
FROM buyer buyer0
```


```scala
Seq(("James Bond", false), ("叉烧包", false), ("Li Haoyi", true))
```


### SelectTests.case.when

```scala
Product.select.map(p =>
  caseWhen(
    (p.price > 200) -> (p.name + " EXPENSIVE"),
    (p.price > 5) -> (p.name + " NORMAL"),
    (p.price <= 5) -> (p.name + " CHEAP")
  )
)
```

```sql
SELECT
  CASE
    WHEN product0.price > ? THEN product0.name || ?
    WHEN product0.price > ? THEN product0.name || ?
    WHEN product0.price <= ? THEN product0.name || ?
  END as res
FROM product product0
```


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


### SelectTests.case.else

```scala
Product.select.map(p =>
  caseWhen((p.price > 200) -> (p.name + " EXPENSIVE"), (p.price > 5) -> (p.name + " NORMAL"))
    .`else` { p.name + " UNKNOWN" }
)
```

```sql
SELECT
  CASE
    WHEN product0.price > ? THEN product0.name || ?
    WHEN product0.price > ? THEN product0.name || ?
    ELSE product0.name || ?
  END as res
FROM product product0
```


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


## scalasql.sqlite.SqliteDialectTests
### SqliteDialectTests.ltrim2

```scala
Expr("xxHellox").ltrim("x")
```

```sql
SELECT LTRIM(?, ?) as res
```


```scala
"Hellox"
```


### SqliteDialectTests.rtrim2

```scala
Expr("xxHellox").rtrim("x")
```

```sql
SELECT RTRIM(?, ?) as res
```


```scala
"xxHello"
```


## scalasql.sqlite.SubQueryTests
### SubQueryTests.sortTakeJoin

```scala
Purchase.select.joinOn(Product.select.sortBy(_.price).desc.take(1))(_.productId `=` _.id).map {
  case (purchase, product) => purchase.total
}
```

```sql
SELECT purchase0.total as res
FROM purchase purchase0
JOIN (SELECT product0.id as res__id, product0.price as res__price
  FROM product product0
  ORDER BY res__price DESC
  LIMIT 1) subquery1
ON purchase0.product_id = subquery1.res__id
```


```scala
Seq(10000.0)
```


### SubQueryTests.sortTakeFrom

```scala
Product.select.sortBy(_.price).desc.take(1).joinOn(Purchase)(_.id `=` _.productId).map {
  case (product, purchase) => purchase.total
}
```

```sql
SELECT purchase1.total as res
FROM (SELECT product0.id as res__id, product0.price as res__price
  FROM product product0
  ORDER BY res__price DESC
  LIMIT 1) subquery0
JOIN purchase purchase1 ON subquery0.res__id = purchase1.product_id
```


```scala
Seq(10000.0)
```


### SubQueryTests.sortTakeFromAndJoin

```scala
Product.select.sortBy(_.price).desc.take(3)
  .joinOn(Purchase.select.sortBy(_.count).desc.take(3))(_.id `=` _.productId).map {
    case (product, purchase) => (product.name, purchase.count)
  }
```

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


```scala
Seq(("Camera", 10))
```


### SubQueryTests.sortLimitSortLimit

```scala
Product.select.sortBy(_.price).desc.take(4).sortBy(_.price).asc.take(2).map(_.name)
```

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


```scala
Seq("Face Mask", "Skate Board")
```


### SubQueryTests.sortGroupBy

```scala
Purchase.select.sortBy(_.count).take(5).groupBy(_.productId)(_.sumBy(_.total))
```

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


```scala
Seq((1, 44.4), (2, 900.0), (3, 15.7), (4, 493.8), (5, 10000.0))
```


### SubQueryTests.groupByJoin

```scala
Purchase.select.groupBy(_.productId)(_.sumBy(_.total)).joinOn(Product)(_._1 `=` _.id).map {
  case ((productId, total), product) => (product.name, total)
}
```

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


### SubQueryTests.subqueryInFilter

```scala
Buyer.select.filter(c => ShippingInfo.select.filter(p => c.id `=` p.buyerId).size `=` 0)
```

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


```scala
Seq(Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")))
```


### SubQueryTests.subqueryInMap

```scala
Buyer.select.map(c => (c, ShippingInfo.select.filter(p => c.id `=` p.buyerId).size))
```

```sql
SELECT
  buyer0.id as res__0__id,
  buyer0.name as res__0__name,
  buyer0.date_of_birth as res__0__date_of_birth,
  (SELECT COUNT(1) as res FROM shipping_info shipping_info0 WHERE buyer0.id = shipping_info0.buyer_id) as res__1
FROM buyer buyer0
```


```scala
Seq(
  (Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")), 1),
  (Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")), 2),
  (Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")), 0)
)
```


### SubQueryTests.subqueryInMapNested

```scala
Buyer.select.map(c => (c, ShippingInfo.select.filter(p => c.id `=` p.buyerId).size `=` 1))
```

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


```scala
Seq(
  (Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")), true),
  (Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")), false),
  (Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")), false)
)
```


### SubQueryTests.selectLimitUnionSelect

```scala
Buyer.select.map(_.name.toLowerCase).take(2)
  .unionAll(Product.select.map(_.kebabCaseName.toLowerCase))
```

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


```scala
Seq("james bond", "叉烧包", "face-mask", "guitar", "socks", "skate-board", "camera", "cookie")
```


### SubQueryTests.selectUnionSelectLimit

```scala
Buyer.select.map(_.name.toLowerCase)
  .unionAll(Product.select.map(_.kebabCaseName.toLowerCase).take(2))
```

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


```scala
Seq("james bond", "叉烧包", "li haoyi", "face-mask", "guitar")
```


## scalasql.sqlite.UpdateJoinTests
### UpdateJoinTests.join

```scala
Buyer.update(_.name `=` "James Bond").joinOn(ShippingInfo)(_.id `=` _.buyerId)
  .set(c => c._1.dateOfBirth := c._2.shippingDate)
```

```sql
UPDATE buyer
SET date_of_birth = shipping_info0.shipping_date
FROM shipping_info shipping_info0
WHERE buyer.id = shipping_info0.buyer_id AND buyer.name = ?
```


```scala
1
```


### UpdateJoinTests.join

```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



```scala
Seq(LocalDate.parse("2012-04-05"))
```


### UpdateJoinTests.multijoin

```scala
Buyer.update(_.name `=` "James Bond").joinOn(ShippingInfo)(_.id `=` _.buyerId)
  .joinOn(Purchase)(_._2.id `=` _.shippingInfoId).joinOn(Product)(_._2.productId `=` _.id)
  .filter(t => t._2.name.toLowerCase `=` t._2.kebabCaseName.toLowerCase)
  .set(c => c._1._1._1.name := c._2.name)
```

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


```scala
1
```


### UpdateJoinTests.multijoin

```scala
Buyer.select.filter(_.id `=` 1).map(_.name)
```



```scala
Seq("Camera")
```


### UpdateJoinTests.joinSubquery

```scala
Buyer.update(_.name `=` "James Bond")
  .joinOn(ShippingInfo.select.sortBy(_.id).asc.take(2))(_.id `=` _.buyerId)
  .set(c => c._1.dateOfBirth := c._2.shippingDate)
```

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


```scala
1
```


### UpdateJoinTests.joinSubquery

```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



```scala
Seq(LocalDate.parse("2012-04-05"))
```


### UpdateJoinTests.joinSubqueryEliminatedColumn

```scala
Buyer.update(_.name `=` "James Bond")
  // Make sure the `SELECT shipping_info0.shipping_info_id as res__shipping_info_id`
  // column gets eliminated since it is not used outside the subquery
  .joinOn(ShippingInfo.select.sortBy(_.id).asc.take(2))(_.id `=` _.buyerId)
  .set(c => c._1.dateOfBirth := LocalDate.parse("2000-01-01"))
```

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


```scala
1
```


### UpdateJoinTests.joinSubqueryEliminatedColumn

```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



```scala
Seq(LocalDate.parse("2000-01-01"))
```


## scalasql.sqlite.UpdateSubQueryTests
### UpdateSubQueryTests.setSubquery

```scala
Product.update(_ => true).set(_.price := Product.select.maxBy(_.price))
```

```sql
UPDATE product
SET price = (SELECT MAX(product0.price) as res FROM product product0)
WHERE ?
```


```scala
6
```


### UpdateSubQueryTests.setSubquery

```scala
Product.select.map(p => (p.id, p.name, p.price))
```



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


### UpdateSubQueryTests.whereSubquery

```scala
Product.update(_.price `=` Product.select.maxBy(_.price)).set(_.price := 0)
```

```sql
UPDATE product
SET price = ?
WHERE product.price = (SELECT MAX(product0.price) as res FROM product product0)
```


```scala
1
```


### UpdateSubQueryTests.whereSubquery

```scala
Product.select.map(p => (p.id, p.name, p.price))
```



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


## scalasql.sqlite.UpdateTests
### UpdateTests.update

```scala
Buyer.update(_.name `=` "James Bond").set(_.dateOfBirth := LocalDate.parse("2019-04-07"))
```

```sql
UPDATE buyer SET date_of_birth = ? WHERE buyer.name = ?
```


```scala
1
```


### UpdateTests.update

```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



```scala
Seq(LocalDate.parse("2019-04-07"))
```


### UpdateTests.update

```scala
Buyer.select.filter(_.name `=` "Li Haoyi").map(_.dateOfBirth)
```



```scala
Seq(LocalDate.parse("1965-08-09"))
```


### UpdateTests.bulk

```scala
Buyer.update(_ => true).set(_.dateOfBirth := LocalDate.parse("2019-04-07"))
```

```sql
UPDATE buyer SET date_of_birth = ? WHERE ?
```


```scala
3
```


### UpdateTests.bulk

```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



```scala
Seq(LocalDate.parse("2019-04-07"))
```


### UpdateTests.bulk

```scala
Buyer.select.filter(_.name `=` "Li Haoyi").map(_.dateOfBirth)
```



```scala
Seq(LocalDate.parse("2019-04-07"))
```


### UpdateTests.multiple

```scala
Buyer.update(_.name `=` "James Bond")
  .set(_.dateOfBirth := LocalDate.parse("2019-04-07"), _.name := "John Dee")
```

```sql
UPDATE buyer SET date_of_birth = ?, name = ? WHERE buyer.name = ?
```


```scala
1
```


### UpdateTests.multiple

```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



```scala
Seq[LocalDate]()
```


### UpdateTests.multiple

```scala
Buyer.select.filter(_.name `=` "John Dee").map(_.dateOfBirth)
```



```scala
Seq(LocalDate.parse("2019-04-07"))
```


### UpdateTests.dynamic

```scala
Buyer.update(_.name `=` "James Bond").set(c => c.name := c.name.toUpperCase)
```

```sql
UPDATE buyer SET name = UPPER(buyer.name) WHERE buyer.name = ?
```


```scala
1
```


### UpdateTests.dynamic

```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



```scala
Seq[LocalDate]()
```


### UpdateTests.dynamic

```scala
Buyer.select.filter(_.name `=` "JAMES BOND").map(_.dateOfBirth)
```



```scala
Seq(LocalDate.parse("2001-02-03"))
```

