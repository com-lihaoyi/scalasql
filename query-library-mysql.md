# mysql
## scalasql.mysql.CompoundSelectTests
### scalasql.mysql.CompoundSelectTests.sort.simple

**ScalaSql Query**
```scala
Product.select.sortBy(_.price).map(_.name)
```

**Generated Sql**
```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price
```


**Results**
```scala
Seq("Cookie", "Socks", "Face Mask", "Skate Board", "Guitar", "Camera")
```


### scalasql.mysql.CompoundSelectTests.sort.twice

**ScalaSql Query**
```scala
Purchase.select.sortBy(_.productId).asc.sortBy(_.shippingInfoId).desc
```

**Generated Sql**
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


**Results**
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


### scalasql.mysql.CompoundSelectTests.sort.sortLimit

**ScalaSql Query**
```scala
Product.select.sortBy(_.price).map(_.name).take(2)
```

**Generated Sql**
```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2
```


**Results**
```scala
Seq("Cookie", "Socks")
```


### scalasql.mysql.CompoundSelectTests.sort.sortOffset

**ScalaSql Query**
```scala
Product.select.sortBy(_.price).map(_.name).drop(2)
```

**Generated Sql**
```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2147483647 OFFSET 2
```


**Results**
```scala
Seq("Face Mask", "Skate Board", "Guitar", "Camera")
```


### scalasql.mysql.CompoundSelectTests.sort.sortLimitTwiceHigher

**ScalaSql Query**
```scala
Product.select.sortBy(_.price).map(_.name).take(2).take(3)
```

**Generated Sql**
```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2
```


**Results**
```scala
Seq("Cookie", "Socks")
```


### scalasql.mysql.CompoundSelectTests.sort.sortLimitTwiceLower

**ScalaSql Query**
```scala
Product.select.sortBy(_.price).map(_.name).take(2).take(1)
```

**Generated Sql**
```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1
```


**Results**
```scala
Seq("Cookie")
```


### scalasql.mysql.CompoundSelectTests.sort.sortLimitOffset

**ScalaSql Query**
```scala
Product.select.sortBy(_.price).map(_.name).drop(2).take(2)
```

**Generated Sql**
```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2
```


**Results**
```scala
Seq("Face Mask", "Skate Board")
```


### scalasql.mysql.CompoundSelectTests.sort.sortLimitOffsetTwice

**ScalaSql Query**
```scala
Product.select.sortBy(_.price).map(_.name).drop(2).drop(2).take(1)
```

**Generated Sql**
```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 4
```


**Results**
```scala
Seq("Guitar")
```


### scalasql.mysql.CompoundSelectTests.sort.sortOffsetLimit

**ScalaSql Query**
```scala
Product.select.sortBy(_.price).map(_.name).drop(2).take(2)
```

**Generated Sql**
```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2
```


**Results**
```scala
Seq("Face Mask", "Skate Board")
```


### scalasql.mysql.CompoundSelectTests.sort.sortLimitOffset

**ScalaSql Query**
```scala
Product.select.sortBy(_.price).map(_.name).take(2).drop(1)
```

**Generated Sql**
```sql
SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 1
```


**Results**
```scala
Seq("Socks")
```


### scalasql.mysql.CompoundSelectTests.distinct

**ScalaSql Query**
```scala
Purchase.select.sortBy(_.total).desc.take(3).map(_.shippingInfoId).distinct
```

**Generated Sql**
```sql
SELECT DISTINCT subquery0.res as res
FROM (SELECT purchase0.shipping_info_id as res
  FROM purchase purchase0
  ORDER BY purchase0.total DESC
  LIMIT 3) subquery0
```


**Results**
```scala
Seq(1, 2)
```


### scalasql.mysql.CompoundSelectTests.flatMap

**ScalaSql Query**
```scala
Purchase.select.sortBy(_.total).desc.take(3).flatMap { p =>
  Product.select.filter(_.id === p.productId).map(_.name)
}
```

**Generated Sql**
```sql
SELECT product1.name as res
FROM (SELECT purchase0.product_id as res__product_id, purchase0.total as res__total
  FROM purchase purchase0
  ORDER BY res__total DESC
  LIMIT 3) subquery0, product product1
WHERE product1.id = subquery0.res__product_id
```


**Results**
```scala
Seq("Camera", "Face Mask", "Guitar")
```


### scalasql.mysql.CompoundSelectTests.sumBy

**ScalaSql Query**
```scala
Purchase.select.sortBy(_.total).desc.take(3).sumBy(_.total)
```

**Generated Sql**
```sql
SELECT SUM(subquery0.res__total) as res
FROM (SELECT purchase0.total as res__total
  FROM purchase purchase0
  ORDER BY res__total DESC
  LIMIT 3) subquery0
```


**Results**
```scala
11788.0
```


### scalasql.mysql.CompoundSelectTests.aggregate

**ScalaSql Query**
```scala
Purchase.select.sortBy(_.total).desc.take(3).aggregate(p => (p.sumBy(_.total), p.avgBy(_.total)))
```

**Generated Sql**
```sql
SELECT SUM(subquery0.res__total) as res__0, AVG(subquery0.res__total) as res__1
FROM (SELECT purchase0.total as res__total
  FROM purchase purchase0
  ORDER BY res__total DESC
  LIMIT 3) subquery0
```


**Results**
```scala
(11788.0, 3929.0)
```


### scalasql.mysql.CompoundSelectTests.union

**ScalaSql Query**
```scala
Product.select.map(_.name.toLowerCase).union(Product.select.map(_.kebabCaseName.toLowerCase))
```

**Generated Sql**
```sql
SELECT LOWER(product0.name) as res
FROM product product0
UNION
SELECT LOWER(product0.kebab_case_name) as res
FROM product product0
```


**Results**
```scala
Seq("camera", "cookie", "face mask", "face-mask", "guitar", "skate board", "skate-board", "socks")
```


### scalasql.mysql.CompoundSelectTests.unionAll

**ScalaSql Query**
```scala
Product.select.map(_.name.toLowerCase).unionAll(Product.select.map(_.kebabCaseName.toLowerCase))
```

**Generated Sql**
```sql
SELECT LOWER(product0.name) as res
FROM product product0
UNION ALL
SELECT LOWER(product0.kebab_case_name) as res
FROM product product0
```


**Results**
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


### scalasql.mysql.CompoundSelectTests.intersect

**ScalaSql Query**
```scala
Product.select.map(_.name.toLowerCase).intersect(Product.select.map(_.kebabCaseName.toLowerCase))
```

**Generated Sql**
```sql
SELECT LOWER(product0.name) as res
FROM product product0
INTERSECT
SELECT LOWER(product0.kebab_case_name) as res
FROM product product0
```


**Results**
```scala
Seq("camera", "cookie", "guitar", "socks")
```


### scalasql.mysql.CompoundSelectTests.except

**ScalaSql Query**
```scala
Product.select.map(_.name.toLowerCase).except(Product.select.map(_.kebabCaseName.toLowerCase))
```

**Generated Sql**
```sql
SELECT LOWER(product0.name) as res
FROM product product0
EXCEPT
SELECT LOWER(product0.kebab_case_name) as res
FROM product product0
```


**Results**
```scala
Seq("face mask", "skate board")
```


### scalasql.mysql.CompoundSelectTests.unionAllUnionSort

**ScalaSql Query**
```scala
Product.select.map(_.name.toLowerCase).unionAll(Buyer.select.map(_.name.toLowerCase))
  .union(Product.select.map(_.kebabCaseName.toLowerCase)).sortBy(identity)
```

**Generated Sql**
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


**Results**
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


### scalasql.mysql.CompoundSelectTests.unionAllUnionSortLimit

**ScalaSql Query**
```scala
Product.select.map(_.name.toLowerCase).unionAll(Buyer.select.map(_.name.toLowerCase))
  .union(Product.select.map(_.kebabCaseName.toLowerCase)).sortBy(identity).drop(4).take(4)
```

**Generated Sql**
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


**Results**
```scala
Seq("guitar", "james bond", "li haoyi", "skate board")
```


### scalasql.mysql.CompoundSelectTests.exceptAggregate

**ScalaSql Query**
```scala
Product.select.map(p => (p.name.toLowerCase, p.price))
  // `p.name.toLowerCase` and  `p.kebabCaseName.toLowerCase` are not eliminated, because
  // they are important to the semantics of EXCEPT (and other non-UNION-ALL operators)
  .except(Product.select.map(p => (p.kebabCaseName.toLowerCase, p.price)))
  .aggregate(ps => (ps.maxBy(_._2), ps.minBy(_._2)))
```

**Generated Sql**
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


**Results**
```scala
(123.45, 8.88)
```


### scalasql.mysql.CompoundSelectTests.unionAllAggregate

**ScalaSql Query**
```scala
Product.select.map(p => (p.name.toLowerCase, p.price))
  // `p.name.toLowerCase` and  `p.kebabCaseName.toLowerCase` get eliminated,
  // as they are not selected by the enclosing query, and cannot affect the UNION ALL
  .unionAll(Product.select.map(p => (p.kebabCaseName.toLowerCase, p.price)))
  .aggregate(ps => (ps.maxBy(_._2), ps.minBy(_._2)))
```

**Generated Sql**
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


**Results**
```scala
(1000.0, 0.1)
```


## scalasql.mysql.DataTypesTests
### scalasql.mysql.DataTypesTests.constant

**ScalaSql Query**
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



**Results**
```scala
1
```


### scalasql.mysql.DataTypesTests.constant

**ScalaSql Query**
```scala
DataTypes.select
```



**Results**
```scala
Seq(value)
```


### scalasql.mysql.DataTypesTests.nonRoundTrip

**ScalaSql Query**
```scala
NonRoundTripTypes.insert
  .values(_.myOffsetDateTime := value.myOffsetDateTime, _.myZonedDateTime := value.myZonedDateTime)
```



**Results**
```scala
1
```


### scalasql.mysql.DataTypesTests.nonRoundTrip

**ScalaSql Query**
```scala
NonRoundTripTypes.select
```



**Results**
```scala
Seq(normalize(value))
```


## scalasql.mysql.DeleteTests
### scalasql.mysql.DeleteTests.single

**ScalaSql Query**
```scala
Purchase.delete(_.id `=` 2)
```

**Generated Sql**
```sql
DELETE FROM purchase WHERE purchase.id = ?
```


**Results**
```scala
1
```


### scalasql.mysql.DeleteTests.single

**ScalaSql Query**
```scala
Purchase.select
```



**Results**
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


### scalasql.mysql.DeleteTests.multiple

**ScalaSql Query**
```scala
Purchase.delete(_.id <> 2)
```

**Generated Sql**
```sql
DELETE FROM purchase WHERE purchase.id <> ?
```


**Results**
```scala
6
```


### scalasql.mysql.DeleteTests.multiple

**ScalaSql Query**
```scala
Purchase.select
```



**Results**
```scala
Seq(Purchase[Id](id = 2, shippingInfoId = 1, productId = 2, count = 3, total = 900.0))
```


### scalasql.mysql.DeleteTests.all

**ScalaSql Query**
```scala
Purchase.delete(_ => true)
```

**Generated Sql**
```sql
DELETE FROM purchase WHERE ?
```


**Results**
```scala
7
```


### scalasql.mysql.DeleteTests.all

**ScalaSql Query**
```scala
Purchase.select
```



**Results**
```scala
Seq[Purchase[Id]](
  // all Deleted
)
```


## scalasql.mysql.ExprBooleanOpsTests
### scalasql.mysql.ExprBooleanOpsTests.and

**ScalaSql Query**
```scala
Expr(true) && Expr(true)
```

**Generated Sql**
```sql
SELECT ? AND ? as res
```


**Results**
```scala
true
```


### scalasql.mysql.ExprBooleanOpsTests.or

**ScalaSql Query**
```scala
Expr(false) || Expr(false)
```

**Generated Sql**
```sql
SELECT ? OR ? as res
```


**Results**
```scala
false
```


### scalasql.mysql.ExprBooleanOpsTests.or

**ScalaSql Query**
```scala
!Expr(false)
```

**Generated Sql**
```sql
SELECT NOT ? as res
```


**Results**
```scala
true
```


## scalasql.mysql.ExprExprIntOpsTests
### scalasql.mysql.ExprExprIntOpsTests.plus

**ScalaSql Query**
```scala
Expr(6) + Expr(2)
```

**Generated Sql**
```sql
SELECT ? + ? as res
```


**Results**
```scala
8
```


### scalasql.mysql.ExprExprIntOpsTests.minus

**ScalaSql Query**
```scala
Expr(6) - Expr(2)
```

**Generated Sql**
```sql
SELECT ? - ? as res
```


**Results**
```scala
4
```


### scalasql.mysql.ExprExprIntOpsTests.times

**ScalaSql Query**
```scala
Expr(6) * Expr(2)
```

**Generated Sql**
```sql
SELECT ? * ? as res
```


**Results**
```scala
12
```


### scalasql.mysql.ExprExprIntOpsTests.divide

**ScalaSql Query**
```scala
Expr(6) / Expr(2)
```

**Generated Sql**
```sql
SELECT ? / ? as res
```


**Results**
```scala
3
```


### scalasql.mysql.ExprExprIntOpsTests.modulo

**ScalaSql Query**
```scala
Expr(6) % Expr(2)
```

**Generated Sql**
```sql
SELECT MOD(?, ?) as res
```


**Results**
```scala
0
```


### scalasql.mysql.ExprExprIntOpsTests.bitwiseAnd

**ScalaSql Query**
```scala
Expr(6) & Expr(2)
```

**Generated Sql**
```sql
SELECT ? & ? as res
```


**Results**
```scala
2
```


### scalasql.mysql.ExprExprIntOpsTests.bitwiseOr

**ScalaSql Query**
```scala
Expr(6) | Expr(3)
```

**Generated Sql**
```sql
SELECT ? | ? as res
```


**Results**
```scala
7
```


### scalasql.mysql.ExprExprIntOpsTests.between

**ScalaSql Query**
```scala
Expr(4).between(Expr(2), Expr(6))
```

**Generated Sql**
```sql
SELECT ? BETWEEN ? AND ? as res
```


**Results**
```scala
true
```


### scalasql.mysql.ExprExprIntOpsTests.unaryPlus

**ScalaSql Query**
```scala
+Expr(-4)
```

**Generated Sql**
```sql
SELECT +? as res
```


**Results**
```scala
-4
```


### scalasql.mysql.ExprExprIntOpsTests.unaryMinus

**ScalaSql Query**
```scala
-Expr(-4)
```

**Generated Sql**
```sql
SELECT -? as res
```


**Results**
```scala
4
```


### scalasql.mysql.ExprExprIntOpsTests.unaryTilde

**ScalaSql Query**
```scala
~Expr(-4)
```

**Generated Sql**
```sql
SELECT ~? as res
```


**Results**
```scala
3
```


### scalasql.mysql.ExprExprIntOpsTests.abs

**ScalaSql Query**
```scala
Expr(-4).abs
```

**Generated Sql**
```sql
SELECT ABS(?) as res
```


**Results**
```scala
4
```


### scalasql.mysql.ExprExprIntOpsTests.mod

**ScalaSql Query**
```scala
Expr(8).mod(Expr(3))
```

**Generated Sql**
```sql
SELECT MOD(?, ?) as res
```


**Results**
```scala
2
```


### scalasql.mysql.ExprExprIntOpsTests.ceil

**ScalaSql Query**
```scala
Expr(4.3).ceil
```

**Generated Sql**
```sql
SELECT CEIL(?) as res
```


**Results**
```scala
5.0
```


### scalasql.mysql.ExprExprIntOpsTests.floor

**ScalaSql Query**
```scala
Expr(4.7).floor
```

**Generated Sql**
```sql
SELECT FLOOR(?) as res
```


**Results**
```scala
4.0
```


### scalasql.mysql.ExprExprIntOpsTests.floor

**ScalaSql Query**
```scala
Expr(4.7).floor
```

**Generated Sql**
```sql
SELECT FLOOR(?) as res
```


**Results**
```scala
4.0
```


## scalasql.mysql.ExprSeqNumericOpsTests
### scalasql.mysql.ExprSeqNumericOpsTests.sum

**ScalaSql Query**
```scala
Purchase.select.map(_.count).sum
```

**Generated Sql**
```sql
SELECT SUM(purchase0.count) as res FROM purchase purchase0
```


**Results**
```scala
140
```


### scalasql.mysql.ExprSeqNumericOpsTests.min

**ScalaSql Query**
```scala
Purchase.select.map(_.count).min
```

**Generated Sql**
```sql
SELECT MIN(purchase0.count) as res FROM purchase purchase0
```


**Results**
```scala
3
```


### scalasql.mysql.ExprSeqNumericOpsTests.max

**ScalaSql Query**
```scala
Purchase.select.map(_.count).max
```

**Generated Sql**
```sql
SELECT MAX(purchase0.count) as res FROM purchase purchase0
```


**Results**
```scala
100
```


### scalasql.mysql.ExprSeqNumericOpsTests.avg

**ScalaSql Query**
```scala
Purchase.select.map(_.count).avg
```

**Generated Sql**
```sql
SELECT AVG(purchase0.count) as res FROM purchase purchase0
```


**Results**
```scala
20
```


## scalasql.mysql.ExprSeqOpsTests
### scalasql.mysql.ExprSeqOpsTests.size

**ScalaSql Query**
```scala
Purchase.select.size
```

**Generated Sql**
```sql
SELECT COUNT(1) as res FROM purchase purchase0
```


**Results**
```scala
7
```


### scalasql.mysql.ExprSeqOpsTests.sumBy.simple

**ScalaSql Query**
```scala
Purchase.select.sumBy(_.count)
```

**Generated Sql**
```sql
SELECT SUM(purchase0.count) as res FROM purchase purchase0
```


**Results**
```scala
140
```


### scalasql.mysql.ExprSeqOpsTests.sumBy.some

**ScalaSql Query**
```scala
Purchase.select.sumByOpt(_.count)
```

**Generated Sql**
```sql
SELECT SUM(purchase0.count) as res FROM purchase purchase0
```


**Results**
```scala
Option(140)
```


### scalasql.mysql.ExprSeqOpsTests.sumBy.none

**ScalaSql Query**
```scala
Purchase.select.filter(_ => false).sumByOpt(_.count)
```

**Generated Sql**
```sql
SELECT SUM(purchase0.count) as res FROM purchase purchase0 WHERE ?
```


**Results**
```scala
Option.empty[Int]
```


### scalasql.mysql.ExprSeqOpsTests.minBy.simple

**ScalaSql Query**
```scala
Purchase.select.minBy(_.count)
```

**Generated Sql**
```sql
SELECT MIN(purchase0.count) as res FROM purchase purchase0
```


**Results**
```scala
3
```


### scalasql.mysql.ExprSeqOpsTests.minBy.some

**ScalaSql Query**
```scala
Purchase.select.minByOpt(_.count)
```

**Generated Sql**
```sql
SELECT MIN(purchase0.count) as res FROM purchase purchase0
```


**Results**
```scala
Option(3)
```


### scalasql.mysql.ExprSeqOpsTests.minBy.none

**ScalaSql Query**
```scala
Purchase.select.filter(_ => false).minByOpt(_.count)
```

**Generated Sql**
```sql
SELECT MIN(purchase0.count) as res FROM purchase purchase0 WHERE ?
```


**Results**
```scala
Option.empty[Int]
```


### scalasql.mysql.ExprSeqOpsTests.maxBy.simple

**ScalaSql Query**
```scala
Purchase.select.maxBy(_.count)
```

**Generated Sql**
```sql
SELECT MAX(purchase0.count) as res FROM purchase purchase0
```


**Results**
```scala
100
```


### scalasql.mysql.ExprSeqOpsTests.maxBy.some

**ScalaSql Query**
```scala
Purchase.select.maxByOpt(_.count)
```

**Generated Sql**
```sql
SELECT MAX(purchase0.count) as res FROM purchase purchase0
```


**Results**
```scala
Option(100)
```


### scalasql.mysql.ExprSeqOpsTests.maxBy.none

**ScalaSql Query**
```scala
Purchase.select.filter(_ => false).maxByOpt(_.count)
```

**Generated Sql**
```sql
SELECT MAX(purchase0.count) as res FROM purchase purchase0 WHERE ?
```


**Results**
```scala
Option.empty[Int]
```


### scalasql.mysql.ExprSeqOpsTests.avgBy.simple

**ScalaSql Query**
```scala
Purchase.select.avgBy(_.count)
```

**Generated Sql**
```sql
SELECT AVG(purchase0.count) as res FROM purchase purchase0
```


**Results**
```scala
20
```


### scalasql.mysql.ExprSeqOpsTests.avgBy.some

**ScalaSql Query**
```scala
Purchase.select.avgByOpt(_.count)
```

**Generated Sql**
```sql
SELECT AVG(purchase0.count) as res FROM purchase purchase0
```


**Results**
```scala
Option(20)
```


### scalasql.mysql.ExprSeqOpsTests.avgBy.none

**ScalaSql Query**
```scala
Purchase.select.filter(_ => false).avgByOpt(_.count)
```

**Generated Sql**
```sql
SELECT AVG(purchase0.count) as res FROM purchase purchase0 WHERE ?
```


**Results**
```scala
Option.empty[Int]
```


## scalasql.mysql.ExprStringOpsTests
### scalasql.mysql.ExprStringOpsTests.plus

**ScalaSql Query**
```scala
Expr("hello") + Expr("world")
```

**Generated Sql**
```sql
SELECT CONCAT(?, ?) as res
```


**Results**
```scala
"helloworld"
```


### scalasql.mysql.ExprStringOpsTests.like

**ScalaSql Query**
```scala
Expr("hello").like("he%")
```

**Generated Sql**
```sql
SELECT ? LIKE ? as res
```


**Results**
```scala
true
```


### scalasql.mysql.ExprStringOpsTests.length

**ScalaSql Query**
```scala
Expr("hello").length
```

**Generated Sql**
```sql
SELECT LENGTH(?) as res
```


**Results**
```scala
5
```


### scalasql.mysql.ExprStringOpsTests.octetLength

**ScalaSql Query**
```scala
Expr("叉烧包").octetLength
```

**Generated Sql**
```sql
SELECT OCTET_LENGTH(?) as res
```


**Results**
```scala
9
```


### scalasql.mysql.ExprStringOpsTests.position

**ScalaSql Query**
```scala
Expr("hello").indexOf("ll")
```

**Generated Sql**
```sql
SELECT POSITION(? IN ?) as res
```


**Results**
```scala
3
```


### scalasql.mysql.ExprStringOpsTests.toLowerCase

**ScalaSql Query**
```scala
Expr("Hello").toLowerCase
```

**Generated Sql**
```sql
SELECT LOWER(?) as res
```


**Results**
```scala
"hello"
```


### scalasql.mysql.ExprStringOpsTests.trim

**ScalaSql Query**
```scala
Expr("  Hello ").trim
```

**Generated Sql**
```sql
SELECT TRIM(?) as res
```


**Results**
```scala
"Hello"
```


### scalasql.mysql.ExprStringOpsTests.ltrim

**ScalaSql Query**
```scala
Expr("  Hello ").ltrim
```

**Generated Sql**
```sql
SELECT LTRIM(?) as res
```


**Results**
```scala
"Hello "
```


### scalasql.mysql.ExprStringOpsTests.rtrim

**ScalaSql Query**
```scala
Expr("  Hello ").rtrim
```

**Generated Sql**
```sql
SELECT RTRIM(?) as res
```


**Results**
```scala
"  Hello"
```


### scalasql.mysql.ExprStringOpsTests.substring

**ScalaSql Query**
```scala
Expr("Hello").substring(2, 2)
```

**Generated Sql**
```sql
SELECT SUBSTRING(?, ?, ?) as res
```


**Results**
```scala
"el"
```


## scalasql.mysql.InsertTests
### scalasql.mysql.InsertTests.single.simple

**ScalaSql Query**
```scala
Buyer.insert
  .values(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"), _.id := 4)
```

**Generated Sql**
```sql
INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?)
```


**Results**
```scala
1
```


### scalasql.mysql.InsertTests.single.simple

**ScalaSql Query**
```scala
Buyer.select.filter(_.name `=` "test buyer")
```



**Results**
```scala
Seq(Buyer[Id](4, "test buyer", LocalDate.parse("2023-09-09")))
```


### scalasql.mysql.InsertTests.single.partial

**ScalaSql Query**
```scala
Buyer.insert.values(_.name := "test buyer", _.dateOfBirth := LocalDate.parse("2023-09-09"))
```

**Generated Sql**
```sql
INSERT INTO buyer (name, date_of_birth) VALUES (?, ?)
```


**Results**
```scala
1
```


### scalasql.mysql.InsertTests.single.partial

**ScalaSql Query**
```scala
Buyer.select.filter(_.name `=` "test buyer")
```



**Results**
```scala
Seq(Buyer[Id](4, "test buyer", LocalDate.parse("2023-09-09")))
```


### scalasql.mysql.InsertTests.batch.simple

**ScalaSql Query**
```scala
Buyer.insert.batched(_.name, _.dateOfBirth, _.id)(
  ("test buyer A", LocalDate.parse("2001-04-07"), 4),
  ("test buyer B", LocalDate.parse("2002-05-08"), 5),
  ("test buyer C", LocalDate.parse("2003-06-09"), 6)
)
```

**Generated Sql**
```sql
INSERT INTO buyer (name, date_of_birth, id)
VALUES
  (?, ?, ?),
  (?, ?, ?),
  (?, ?, ?)
```


**Results**
```scala
3
```


### scalasql.mysql.InsertTests.batch.simple

**ScalaSql Query**
```scala
Buyer.select
```



**Results**
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


### scalasql.mysql.InsertTests.batch.partial

**ScalaSql Query**
```scala
Buyer.insert.batched(_.name, _.dateOfBirth)(
  ("test buyer A", LocalDate.parse("2001-04-07")),
  ("test buyer B", LocalDate.parse("2002-05-08")),
  ("test buyer C", LocalDate.parse("2003-06-09"))
)
```

**Generated Sql**
```sql
INSERT INTO buyer (name, date_of_birth)
VALUES (?, ?), (?, ?), (?, ?)
```


**Results**
```scala
3
```


### scalasql.mysql.InsertTests.batch.partial

**ScalaSql Query**
```scala
Buyer.select
```



**Results**
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


### scalasql.mysql.InsertTests.select.caseclass

**ScalaSql Query**
```scala
Buyer.insert.select(
  identity,
  Buyer.select.filter(_.name <> "Li Haoyi").map(b => b.copy(id = b.id + Buyer.select.maxBy(_.id)))
)
```

**Generated Sql**
```sql
INSERT INTO buyer (id, name, date_of_birth)
SELECT
  buyer0.id + (SELECT MAX(buyer0.id) as res FROM buyer buyer0) as res__id,
  buyer0.name as res__name,
  buyer0.date_of_birth as res__date_of_birth
FROM buyer buyer0
WHERE buyer0.name <> ?
```


**Results**
```scala
2
```


### scalasql.mysql.InsertTests.select.caseclass

**ScalaSql Query**
```scala
Buyer.select
```



**Results**
```scala
Seq(
  Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
  Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
  Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")),
  Buyer[Id](4, "James Bond", LocalDate.parse("2001-02-03")),
  Buyer[Id](5, "叉烧包", LocalDate.parse("1923-11-12"))
)
```


### scalasql.mysql.InsertTests.select.simple

**ScalaSql Query**
```scala
Buyer.insert.select(
  x => (x.name, x.dateOfBirth),
  Buyer.select.map(x => (x.name, x.dateOfBirth)).filter(_._1 <> "Li Haoyi")
)
```

**Generated Sql**
```sql
INSERT INTO buyer (name, date_of_birth)
SELECT buyer0.name as res__0, buyer0.date_of_birth as res__1
FROM buyer buyer0
WHERE buyer0.name <> ?
```


**Results**
```scala
2
```


### scalasql.mysql.InsertTests.select.simple

**ScalaSql Query**
```scala
Buyer.select
```



**Results**
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


## scalasql.mysql.JoinTests
### scalasql.mysql.JoinTests.joinFilter

**ScalaSql Query**
```scala
Buyer.select.joinOn(ShippingInfo)(_.id `=` _.buyerId).filter(_._1.name `=` "叉烧包")
```

**Generated Sql**
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


**Results**
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


### scalasql.mysql.JoinTests.joinSelectFilter

**ScalaSql Query**
```scala
Buyer.select.joinOn(ShippingInfo)(_.id `=` _.buyerId).filter(_._1.name `=` "叉烧包")
```

**Generated Sql**
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


**Results**
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


### scalasql.mysql.JoinTests.joinFilterMap

**ScalaSql Query**
```scala
Buyer.select.joinOn(ShippingInfo)(_.id `=` _.buyerId).filter(_._1.name `=` "James Bond")
  .map(_._2.shippingDate)
```

**Generated Sql**
```sql
SELECT shipping_info1.shipping_date as res
FROM buyer buyer0
JOIN shipping_info shipping_info1 ON buyer0.id = shipping_info1.buyer_id
WHERE buyer0.name = ?
```


**Results**
```scala
Seq(LocalDate.parse("2012-04-05"))
```


### scalasql.mysql.JoinTests.selfJoin

**ScalaSql Query**
```scala
Buyer.select.joinOn(Buyer)(_.id `=` _.id)
```

**Generated Sql**
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


**Results**
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


### scalasql.mysql.JoinTests.selfJoin2

**ScalaSql Query**
```scala
Buyer.select.joinOn(Buyer)(_.id <> _.id)
```

**Generated Sql**
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


**Results**
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


### scalasql.mysql.JoinTests.flatMap

**ScalaSql Query**
```scala
Buyer.select.flatMap(c => ShippingInfo.select.map((c, _))).filter { case (c, p) =>
  c.id `=` p.buyerId && c.name `=` "James Bond"
}.map(_._2.shippingDate)
```

**Generated Sql**
```sql
SELECT shipping_info1.shipping_date as res
FROM buyer buyer0, shipping_info shipping_info1
WHERE buyer0.id = shipping_info1.buyer_id
AND buyer0.name = ?
```


**Results**
```scala
Seq(LocalDate.parse("2012-04-05"))
```


### scalasql.mysql.JoinTests.flatMap2

**ScalaSql Query**
```scala
Buyer.select
  .flatMap(c => ShippingInfo.select.filter { p => c.id `=` p.buyerId && c.name `=` "James Bond" })
  .map(_.shippingDate)
```

**Generated Sql**
```sql
SELECT shipping_info1.shipping_date as res
FROM buyer buyer0, shipping_info shipping_info1
WHERE buyer0.id = shipping_info1.buyer_id
AND buyer0.name = ?
```


**Results**
```scala
Seq(LocalDate.parse("2012-04-05"))
```


### scalasql.mysql.JoinTests.leftJoin

**ScalaSql Query**
```scala
Buyer.select.leftJoin(ShippingInfo)(_.id `=` _.buyerId)
```

**Generated Sql**
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


**Results**
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


### scalasql.mysql.JoinTests.rightJoin

**ScalaSql Query**
```scala
ShippingInfo.select.rightJoin(Buyer)(_.buyerId `=` _.id)
```

**Generated Sql**
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


**Results**
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


### scalasql.mysql.JoinTests.outerJoin

**ScalaSql Query**
```scala
ShippingInfo.select.outerJoin(Buyer)(_.buyerId `=` _.id)
```

**Generated Sql**
```sql
SELECT
  shipping_info0.id as res__0__id,
  shipping_info0.buyer_id as res__0__buyer_id,
  shipping_info0.shipping_date as res__0__shipping_date,
  buyer1.id as res__1__id,
  buyer1.name as res__1__name,
  buyer1.date_of_birth as res__1__date_of_birth
FROM shipping_info shipping_info0
LEFT JOIN buyer buyer1 ON shipping_info0.buyer_id = buyer1.id
UNION
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


**Results**
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


## scalasql.mysql.MySqlDialectTests
### scalasql.mysql.MySqlDialectTests.reverse

**ScalaSql Query**
```scala
Expr("Hello").reverse
```

**Generated Sql**
```sql
SELECT REVERSE(?) as res
```


**Results**
```scala
"olleH"
```


### scalasql.mysql.MySqlDialectTests.lpad

**ScalaSql Query**
```scala
Expr("Hello").lpad(10, "xy")
```

**Generated Sql**
```sql
SELECT LPAD(?, ?, ?) as res
```


**Results**
```scala
"xyxyxHello"
```


### scalasql.mysql.MySqlDialectTests.rpad

**ScalaSql Query**
```scala
Expr("Hello").rpad(10, "xy")
```

**Generated Sql**
```sql
SELECT RPAD(?, ?, ?) as res
```


**Results**
```scala
"Helloxyxyx"
```


### scalasql.mysql.MySqlDialectTests.conflict.ignore

**ScalaSql Query**
```scala
Buyer.insert.values(
  _.name := "test buyer",
  _.dateOfBirth := LocalDate.parse("2023-09-09"),
  _.id := 1 // This should cause a primary key conflict
).onConflictUpdate(x => x.id := x.id)
```

**Generated Sql**
```sql
INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE id = buyer.id
```


**Results**
```scala
1
```


### scalasql.mysql.MySqlDialectTests.conflict.update

**ScalaSql Query**
```scala
Buyer.insert.values(
  _.name := "test buyer",
  _.dateOfBirth := LocalDate.parse("2023-09-09"),
  _.id := 1 // This should cause a primary key conflict
).onConflictUpdate(_.name := "TEST BUYER CONFLICT")
```

**Generated Sql**
```sql
INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE name = ?
```


**Results**
```scala
2
```


### scalasql.mysql.MySqlDialectTests.conflict.update

**ScalaSql Query**
```scala
Buyer.select
```



**Results**
```scala
Seq(
  Buyer[Id](1, "TEST BUYER CONFLICT", LocalDate.parse("2001-02-03")),
  Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
  Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
)
```


### scalasql.mysql.MySqlDialectTests.conflict.updateComputed

**ScalaSql Query**
```scala
Buyer.insert.values(
  _.name := "test buyer",
  _.dateOfBirth := LocalDate.parse("2023-09-09"),
  _.id := 1 // This should cause a primary key conflict
).onConflictUpdate(v => v.name := v.name.toUpperCase)
```

**Generated Sql**
```sql
INSERT INTO buyer (name, date_of_birth, id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE name = UPPER(buyer.name)
```


**Results**
```scala
2
```


### scalasql.mysql.MySqlDialectTests.conflict.updateComputed

**ScalaSql Query**
```scala
Buyer.select
```



**Results**
```scala
Seq(
  Buyer[Id](1, "JAMES BOND", LocalDate.parse("2001-02-03")),
  Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")),
  Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
)
```


## scalasql.mysql.OptionalTests
### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.selectAll

**ScalaSql Query**
```scala
OptCols.select
```

**Generated Sql**
```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


**Results**
```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](Some(3), None),
  OptCols[Id](None, Some(4))
)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.groupByMaxGet

**ScalaSql Query**
```scala
OptCols.select.groupBy(_.myInt)(_.maxByOpt(_.myInt2.get))
```

**Generated Sql**
```sql
SELECT opt_cols0.my_int as res__0, MAX(opt_cols0.my_int2) as res__1
FROM opt_cols opt_cols0
GROUP BY opt_cols0.my_int
```


**Results**
```scala
Seq(None -> Some(4), Some(1) -> Some(2), Some(3) -> None)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.isDefined

**ScalaSql Query**
```scala
OptCols.select.filter(_.myInt.isDefined)
```

**Generated Sql**
```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
WHERE opt_cols0.my_int IS NOT NULL
```


**Results**
```scala
Seq(OptCols[Id](Some(1), Some(2)), OptCols[Id](Some(3), None))
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.isEmpty

**ScalaSql Query**
```scala
OptCols.select.filter(_.myInt.isEmpty)
```

**Generated Sql**
```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
WHERE opt_cols0.my_int IS NULL
```


**Results**
```scala
Seq(OptCols[Id](None, None), OptCols[Id](None, Some(4)))
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.sqlEquals.nonOptionHit

**ScalaSql Query**
```scala
OptCols.select.filter(_.myInt `=` 1)
```

**Generated Sql**
```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
WHERE opt_cols0.my_int = ?
```


**Results**
```scala
Seq(OptCols[Id](Some(1), Some(2)))
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.sqlEquals.nonOptionMiss

**ScalaSql Query**
```scala
OptCols.select.filter(_.myInt `=` 2)
```

**Generated Sql**
```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
WHERE opt_cols0.my_int = ?
```


**Results**
```scala
Seq[OptCols[Id]]()
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.sqlEquals.optionMiss

**ScalaSql Query**
```scala
OptCols.select.filter(_.myInt `=` Option.empty[Int])
```

**Generated Sql**
```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
WHERE opt_cols0.my_int = ?
```


**Results**
```scala
Seq[OptCols[Id]]()
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.scalaEquals.someHit

**ScalaSql Query**
```scala
OptCols.select.filter(_.myInt === Option(1))
```

**Generated Sql**
```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
WHERE (opt_cols0.my_int IS NULL AND ? IS NULL) OR opt_cols0.my_int = ?
```


**Results**
```scala
Seq(OptCols[Id](Some(1), Some(2)))
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.scalaEquals.noneHit

**ScalaSql Query**
```scala
OptCols.select.filter(_.myInt === Option.empty[Int])
```

**Generated Sql**
```sql
SELECT
  opt_cols0.my_int as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
WHERE (opt_cols0.my_int IS NULL AND ? IS NULL) OR opt_cols0.my_int = ?
```


**Results**
```scala
Seq(OptCols[Id](None, None), OptCols[Id](None, Some(4)))
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.map

**ScalaSql Query**
```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + 10)))
```

**Generated Sql**
```sql
SELECT
  opt_cols0.my_int + ? as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


**Results**
```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](Some(11), Some(2)),
  OptCols[Id](Some(13), None),
  OptCols[Id](None, Some(4))
)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.map2

**ScalaSql Query**
```scala
OptCols.select.map(_.myInt.map(_ + 10))
```

**Generated Sql**
```sql
SELECT opt_cols0.my_int + ? as res FROM opt_cols opt_cols0
```


**Results**
```scala
Seq(None, Some(11), Some(13), None)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.flatMap

**ScalaSql Query**
```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.flatMap(v => d.myInt2.map(v2 => v + v2 + 10))))
```

**Generated Sql**
```sql
SELECT
  opt_cols0.my_int + opt_cols0.my_int2 + ? as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


**Results**
```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](Some(13), Some(2)),
  // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4))
)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.mapGet

**ScalaSql Query**
```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.map(_ + d.myInt2.get + 1)))
```

**Generated Sql**
```sql
SELECT
  opt_cols0.my_int + opt_cols0.my_int2 + ? as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


**Results**
```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](Some(4), Some(2)),
  // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4))
)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.rawGet

**ScalaSql Query**
```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.get + d.myInt2.get + 1))
```

**Generated Sql**
```sql
SELECT
  opt_cols0.my_int + opt_cols0.my_int2 + ? as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


**Results**
```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](Some(4), Some(2)),
  // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4))
)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.getOrElse

**ScalaSql Query**
```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.getOrElse(-1)))
```

**Generated Sql**
```sql
SELECT
  COALESCE(opt_cols0.my_int, ?) as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


**Results**
```scala
Seq(
  OptCols[Id](Some(-1), None),
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](Some(3), None),
  OptCols[Id](Some(-1), Some(4))
)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.orElse

**ScalaSql Query**
```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.orElse(d.myInt2)))
```

**Generated Sql**
```sql
SELECT
  COALESCE(opt_cols0.my_int, opt_cols0.my_int2) as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


**Results**
```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](Some(3), None),
  OptCols[Id](Some(4), Some(4))
)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.filter

**ScalaSql Query**
```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.filter(_ < 2)))
```

**Generated Sql**
```sql
SELECT
  CASE
    WHEN opt_cols0.my_int < ? THEN opt_cols0.my_int
    ELSE NULL
  END as res__my_int,
  opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
```


**Results**
```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4))
)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.sorting.nullsLast

**ScalaSql Query**
```scala
OptCols.select.sortBy(_.myInt).nullsLast
```

**Generated Sql**
```sql
SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
ORDER BY res__my_int IS NULL ASC, res__my_int
```


**Results**
```scala
Seq(
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](Some(3), None),
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4))
)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.sorting.nullsFirst

**ScalaSql Query**
```scala
OptCols.select.sortBy(_.myInt).nullsFirst
```

**Generated Sql**
```sql
SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
ORDER BY res__my_int IS NULL DESC, res__my_int
```


**Results**
```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4)),
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](Some(3), None)
)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.sorting.ascNullsLast

**ScalaSql Query**
```scala
OptCols.select.sortBy(_.myInt).asc.nullsLast
```

**Generated Sql**
```sql
SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
ORDER BY res__my_int IS NULL ASC, res__my_int ASC
```


**Results**
```scala
Seq(
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](Some(3), None),
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4))
)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.sorting.ascNullsFirst

**ScalaSql Query**
```scala
OptCols.select.sortBy(_.myInt).asc.nullsFirst
```

**Generated Sql**
```sql
SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
ORDER BY res__my_int ASC
```


**Results**
```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4)),
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](Some(3), None)
)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.sorting.descNullsLast

**ScalaSql Query**
```scala
OptCols.select.sortBy(_.myInt).desc.nullsLast
```

**Generated Sql**
```sql
SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
ORDER BY res__my_int DESC
```


**Results**
```scala
Seq(
  OptCols[Id](Some(3), None),
  OptCols[Id](Some(1), Some(2)),
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4))
)
```


### scalasql.mysql.OptionalTests

**ScalaSql Query**
```scala
OptCols.insert
  .batched(_.myInt, _.myInt2)((None, None), (Some(1), Some(2)), (Some(3), None), (None, Some(4)))
```



**Results**
```scala
4
```


### scalasql.mysql.OptionalTests.sorting.descNullsFirst

**ScalaSql Query**
```scala
OptCols.select.sortBy(_.myInt).desc.nullsFirst
```

**Generated Sql**
```sql
SELECT opt_cols0.my_int as res__my_int, opt_cols0.my_int2 as res__my_int2
FROM opt_cols opt_cols0
ORDER BY res__my_int IS NULL DESC, res__my_int DESC
```


**Results**
```scala
Seq(
  OptCols[Id](None, None),
  OptCols[Id](None, Some(4)),
  OptCols[Id](Some(3), None),
  OptCols[Id](Some(1), Some(2))
)
```


## scalasql.mysql.SelectTests
### scalasql.mysql.SelectTests.constant

**ScalaSql Query**
```scala
Expr(1)
```

**Generated Sql**
```sql
SELECT ? as res
```


**Results**
```scala
1
```


### scalasql.mysql.SelectTests.table

**ScalaSql Query**
```scala
Buyer.select
```

**Generated Sql**
```sql
SELECT
  buyer0.id as res__id,
  buyer0.name as res__name,
  buyer0.date_of_birth as res__date_of_birth
FROM buyer buyer0
```


**Results**
```scala
Seq(
  Buyer[Id](id = 1, name = "James Bond", dateOfBirth = LocalDate.parse("2001-02-03")),
  Buyer[Id](id = 2, name = "叉烧包", dateOfBirth = LocalDate.parse("1923-11-12")),
  Buyer[Id](id = 3, name = "Li Haoyi", dateOfBirth = LocalDate.parse("1965-08-09"))
)
```


### scalasql.mysql.SelectTests.filter.single

**ScalaSql Query**
```scala
ShippingInfo.select.filter(_.buyerId `=` 2)
```

**Generated Sql**
```sql
SELECT
  shipping_info0.id as res__id,
  shipping_info0.buyer_id as res__buyer_id,
  shipping_info0.shipping_date as res__shipping_date
FROM shipping_info shipping_info0
WHERE shipping_info0.buyer_id = ?
```


**Results**
```scala
Seq(
  ShippingInfo[Id](1, 2, LocalDate.parse("2010-02-03")),
  ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06"))
)
```


### scalasql.mysql.SelectTests.filter.multiple

**ScalaSql Query**
```scala
ShippingInfo.select.filter(_.buyerId `=` 2).filter(_.shippingDate `=` LocalDate.parse("2012-05-06"))
```

**Generated Sql**
```sql
SELECT
  shipping_info0.id as res__id,
  shipping_info0.buyer_id as res__buyer_id,
  shipping_info0.shipping_date as res__shipping_date
FROM shipping_info shipping_info0
WHERE shipping_info0.buyer_id = ?
AND shipping_info0.shipping_date = ?
```


**Results**
```scala
Seq(ShippingInfo[Id](id = 3, buyerId = 2, shippingDate = LocalDate.parse("2012-05-06")))
```


### scalasql.mysql.SelectTests.filter.dotSingle.pass

**ScalaSql Query**
```scala
ShippingInfo.select.filter(_.buyerId `=` 2).filter(_.shippingDate `=` LocalDate.parse("2012-05-06"))
  .single
```

**Generated Sql**
```sql
SELECT
  shipping_info0.id as res__id,
  shipping_info0.buyer_id as res__buyer_id,
  shipping_info0.shipping_date as res__shipping_date
FROM shipping_info shipping_info0
WHERE shipping_info0.buyer_id = ?
AND shipping_info0.shipping_date = ?
```


**Results**
```scala
ShippingInfo[Id](id = 3, buyerId = 2, shippingDate = LocalDate.parse("2012-05-06"))
```


### scalasql.mysql.SelectTests.filter.combined

**ScalaSql Query**
```scala
ShippingInfo.select.filter(p => p.buyerId `=` 2 && p.shippingDate `=` LocalDate.parse("2012-05-06"))
```

**Generated Sql**
```sql
SELECT
  shipping_info0.id as res__id,
  shipping_info0.buyer_id as res__buyer_id,
  shipping_info0.shipping_date as res__shipping_date
FROM shipping_info shipping_info0
WHERE shipping_info0.buyer_id = ?
AND shipping_info0.shipping_date = ?
```


**Results**
```scala
Seq(ShippingInfo[Id](3, 2, LocalDate.parse("2012-05-06")))
```


### scalasql.mysql.SelectTests.map.single

**ScalaSql Query**
```scala
Buyer.select.map(_.name)
```

**Generated Sql**
```sql
SELECT buyer0.name as res FROM buyer buyer0
```


**Results**
```scala
Seq("James Bond", "叉烧包", "Li Haoyi")
```


### scalasql.mysql.SelectTests.map.tuple2

**ScalaSql Query**
```scala
Buyer.select.map(c => (c.name, c.id))
```

**Generated Sql**
```sql
SELECT buyer0.name as res__0, buyer0.id as res__1 FROM buyer buyer0
```


**Results**
```scala
Seq(("James Bond", 1), ("叉烧包", 2), ("Li Haoyi", 3))
```


### scalasql.mysql.SelectTests.map.tuple3

**ScalaSql Query**
```scala
Buyer.select.map(c => (c.name, c.id, c.dateOfBirth))
```

**Generated Sql**
```sql
SELECT
  buyer0.name as res__0,
  buyer0.id as res__1,
  buyer0.date_of_birth as res__2
FROM buyer buyer0
```


**Results**
```scala
Seq(
  ("James Bond", 1, LocalDate.parse("2001-02-03")),
  ("叉烧包", 2, LocalDate.parse("1923-11-12")),
  ("Li Haoyi", 3, LocalDate.parse("1965-08-09"))
)
```


### scalasql.mysql.SelectTests.map.interpolateInMap

**ScalaSql Query**
```scala
Product.select.map(_.price * 2)
```

**Generated Sql**
```sql
SELECT product0.price * ? as res FROM product product0
```


**Results**
```scala
Seq(17.76, 600, 6.28, 246.9, 2000.0, 0.2)
```


### scalasql.mysql.SelectTests.map.heterogenousTuple

**ScalaSql Query**
```scala
Buyer.select.map(c => (c.id, c))
```

**Generated Sql**
```sql
SELECT
  buyer0.id as res__0,
  buyer0.id as res__1__id,
  buyer0.name as res__1__name,
  buyer0.date_of_birth as res__1__date_of_birth
FROM buyer buyer0
```


**Results**
```scala
Seq(
  (1, Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03"))),
  (2, Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))),
  (3, Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")))
)
```


### scalasql.mysql.SelectTests.exprQuery

**ScalaSql Query**
```scala
Product.select.map(p =>
  (
    p.name,
    Purchase.select.filter(_.productId === p.id).sortBy(_.total).desc.take(1).map(_.total).exprQuery
  )
)
```

**Generated Sql**
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


**Results**
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


### scalasql.mysql.SelectTests.subquery

**ScalaSql Query**
```scala
Buyer.select.subquery.map(_.name)
```

**Generated Sql**
```sql
SELECT subquery0.res__name as res
FROM (SELECT buyer0.name as res__name FROM buyer buyer0) subquery0
```


**Results**
```scala
Seq("James Bond", "叉烧包", "Li Haoyi")
```


### scalasql.mysql.SelectTests.filterMap

**ScalaSql Query**
```scala
Product.select.filter(_.price < 100).map(_.name)
```

**Generated Sql**
```sql
SELECT product0.name as res FROM product product0 WHERE product0.price < ?
```


**Results**
```scala
Seq("Face Mask", "Socks", "Cookie")
```


### scalasql.mysql.SelectTests.aggregate.single

**ScalaSql Query**
```scala
Purchase.select.aggregate(_.sumBy(_.total))
```

**Generated Sql**
```sql
SELECT SUM(purchase0.total) as res FROM purchase purchase0
```


**Results**
```scala
12343.2
```


### scalasql.mysql.SelectTests.aggregate.multiple

**ScalaSql Query**
```scala
Purchase.select.aggregate(q => (q.sumBy(_.total), q.maxBy(_.total)))
```

**Generated Sql**
```sql
SELECT SUM(purchase0.total) as res__0, MAX(purchase0.total) as res__1 FROM purchase purchase0
```


**Results**
```scala
(12343.2, 10000.0)
```


### scalasql.mysql.SelectTests.groupBy.simple

**ScalaSql Query**
```scala
Purchase.select.groupBy(_.productId)(_.sumBy(_.total))
```

**Generated Sql**
```sql
SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
FROM purchase purchase0
GROUP BY purchase0.product_id
```


**Results**
```scala
Seq((1, 932.4), (2, 900.0), (3, 15.7), (4, 493.8), (5, 10000.0), (6, 1.30))
```


### scalasql.mysql.SelectTests.groupBy.having

**ScalaSql Query**
```scala
Purchase.select.groupBy(_.productId)(_.sumBy(_.total)).filter(_._2 > 100).filter(_._1 > 1)
```

**Generated Sql**
```sql
SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
FROM purchase purchase0
GROUP BY purchase0.product_id
HAVING SUM(purchase0.total) > ? AND purchase0.product_id > ?
```


**Results**
```scala
Seq((2, 900.0), (4, 493.8), (5, 10000.0))
```


### scalasql.mysql.SelectTests.groupBy.filterHaving

**ScalaSql Query**
```scala
Purchase.select.filter(_.count > 5).groupBy(_.productId)(_.sumBy(_.total)).filter(_._2 > 100)
```

**Generated Sql**
```sql
SELECT purchase0.product_id as res__0, SUM(purchase0.total) as res__1
FROM purchase purchase0
WHERE purchase0.count > ?
GROUP BY purchase0.product_id
HAVING SUM(purchase0.total) > ?
```


**Results**
```scala
Seq((1, 888.0), (5, 10000.0))
```


### scalasql.mysql.SelectTests.distinct.nondistinct

**ScalaSql Query**
```scala
Purchase.select.map(_.shippingInfoId)
```

**Generated Sql**
```sql
SELECT purchase0.shipping_info_id as res FROM purchase purchase0
```


**Results**
```scala
Seq(1, 1, 1, 2, 2, 3, 3)
```


### scalasql.mysql.SelectTests.distinct.distinct

**ScalaSql Query**
```scala
Purchase.select.map(_.shippingInfoId).distinct
```

**Generated Sql**
```sql
SELECT DISTINCT purchase0.shipping_info_id as res FROM purchase purchase0
```


**Results**
```scala
Seq(1, 2, 3)
```


### scalasql.mysql.SelectTests.contains

**ScalaSql Query**
```scala
Buyer.select.filter(b => ShippingInfo.select.map(_.buyerId).contains(b.id))
```

**Generated Sql**
```sql
SELECT buyer0.id as res__id, buyer0.name as res__name, buyer0.date_of_birth as res__date_of_birth
FROM buyer buyer0
WHERE buyer0.id in (SELECT shipping_info0.buyer_id as res FROM shipping_info shipping_info0)
```


**Results**
```scala
Seq(
  Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")),
  Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12"))
)
```


### scalasql.mysql.SelectTests.nonEmpty

**ScalaSql Query**
```scala
Buyer.select.map(b => (b.name, ShippingInfo.select.filter(_.buyerId `=` b.id).map(_.id).nonEmpty))
```

**Generated Sql**
```sql
SELECT
  buyer0.name as res__0,
  EXISTS (SELECT
    shipping_info0.id as res
    FROM shipping_info shipping_info0
    WHERE shipping_info0.buyer_id = buyer0.id) as res__1
FROM buyer buyer0
```


**Results**
```scala
Seq(("James Bond", true), ("叉烧包", true), ("Li Haoyi", false))
```


### scalasql.mysql.SelectTests.isEmpty

**ScalaSql Query**
```scala
Buyer.select.map(b => (b.name, ShippingInfo.select.filter(_.buyerId `=` b.id).map(_.id).isEmpty))
```

**Generated Sql**
```sql
SELECT
  buyer0.name as res__0,
  NOT EXISTS (SELECT
    shipping_info0.id as res
    FROM shipping_info shipping_info0
    WHERE shipping_info0.buyer_id = buyer0.id) as res__1
FROM buyer buyer0
```


**Results**
```scala
Seq(("James Bond", false), ("叉烧包", false), ("Li Haoyi", true))
```


### scalasql.mysql.SelectTests.case.when

**ScalaSql Query**
```scala
Product.select.map(p =>
  caseWhen(
    (p.price > 200) -> (p.name + " EXPENSIVE"),
    (p.price > 5) -> (p.name + " NORMAL"),
    (p.price <= 5) -> (p.name + " CHEAP")
  )
)
```

**Generated Sql**
```sql
SELECT
  CASE
    WHEN product0.price > ? THEN CONCAT(product0.name, ?)
    WHEN product0.price > ? THEN CONCAT(product0.name, ?)
    WHEN product0.price <= ? THEN CONCAT(product0.name, ?)
  END as res
FROM product product0
```


**Results**
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


### scalasql.mysql.SelectTests.case.else

**ScalaSql Query**
```scala
Product.select.map(p =>
  caseWhen((p.price > 200) -> (p.name + " EXPENSIVE"), (p.price > 5) -> (p.name + " NORMAL"))
    .`else` { p.name + " UNKNOWN" }
)
```

**Generated Sql**
```sql
SELECT
  CASE
    WHEN product0.price > ? THEN CONCAT(product0.name, ?)
    WHEN product0.price > ? THEN CONCAT(product0.name, ?)
    ELSE CONCAT(product0.name, ?)
  END as res
FROM product product0
```


**Results**
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


## scalasql.mysql.SubQueryTests
### scalasql.mysql.SubQueryTests.sortTakeJoin

**ScalaSql Query**
```scala
Purchase.select.joinOn(Product.select.sortBy(_.price).desc.take(1))(_.productId `=` _.id).map {
  case (purchase, product) => purchase.total
}
```

**Generated Sql**
```sql
SELECT purchase0.total as res
FROM purchase purchase0
JOIN (SELECT product0.id as res__id, product0.price as res__price
  FROM product product0
  ORDER BY res__price DESC
  LIMIT 1) subquery1
ON purchase0.product_id = subquery1.res__id
```


**Results**
```scala
Seq(10000.0)
```


### scalasql.mysql.SubQueryTests.sortTakeFrom

**ScalaSql Query**
```scala
Product.select.sortBy(_.price).desc.take(1).joinOn(Purchase)(_.id `=` _.productId).map {
  case (product, purchase) => purchase.total
}
```

**Generated Sql**
```sql
SELECT purchase1.total as res
FROM (SELECT product0.id as res__id, product0.price as res__price
  FROM product product0
  ORDER BY res__price DESC
  LIMIT 1) subquery0
JOIN purchase purchase1 ON subquery0.res__id = purchase1.product_id
```


**Results**
```scala
Seq(10000.0)
```


### scalasql.mysql.SubQueryTests.sortTakeFromAndJoin

**ScalaSql Query**
```scala
Product.select.sortBy(_.price).desc.take(3)
  .joinOn(Purchase.select.sortBy(_.count).desc.take(3))(_.id `=` _.productId).map {
    case (product, purchase) => (product.name, purchase.count)
  }
```

**Generated Sql**
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


**Results**
```scala
Seq(("Camera", 10))
```


### scalasql.mysql.SubQueryTests.sortLimitSortLimit

**ScalaSql Query**
```scala
Product.select.sortBy(_.price).desc.take(4).sortBy(_.price).asc.take(2).map(_.name)
```

**Generated Sql**
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


**Results**
```scala
Seq("Face Mask", "Skate Board")
```


### scalasql.mysql.SubQueryTests.sortGroupBy

**ScalaSql Query**
```scala
Purchase.select.sortBy(_.count).take(5).groupBy(_.productId)(_.sumBy(_.total))
```

**Generated Sql**
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


**Results**
```scala
Seq((1, 44.4), (2, 900.0), (3, 15.7), (4, 493.8), (5, 10000.0))
```


### scalasql.mysql.SubQueryTests.groupByJoin

**ScalaSql Query**
```scala
Purchase.select.groupBy(_.productId)(_.sumBy(_.total)).joinOn(Product)(_._1 `=` _.id).map {
  case ((productId, total), product) => (product.name, total)
}
```

**Generated Sql**
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


**Results**
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


### scalasql.mysql.SubQueryTests.subqueryInFilter

**ScalaSql Query**
```scala
Buyer.select.filter(c => ShippingInfo.select.filter(p => c.id `=` p.buyerId).size `=` 0)
```

**Generated Sql**
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


**Results**
```scala
Seq(Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")))
```


### scalasql.mysql.SubQueryTests.subqueryInMap

**ScalaSql Query**
```scala
Buyer.select.map(c => (c, ShippingInfo.select.filter(p => c.id `=` p.buyerId).size))
```

**Generated Sql**
```sql
SELECT
  buyer0.id as res__0__id,
  buyer0.name as res__0__name,
  buyer0.date_of_birth as res__0__date_of_birth,
  (SELECT COUNT(1) as res FROM shipping_info shipping_info0 WHERE buyer0.id = shipping_info0.buyer_id) as res__1
FROM buyer buyer0
```


**Results**
```scala
Seq(
  (Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")), 1),
  (Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")), 2),
  (Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")), 0)
)
```


### scalasql.mysql.SubQueryTests.subqueryInMapNested

**ScalaSql Query**
```scala
Buyer.select.map(c => (c, ShippingInfo.select.filter(p => c.id `=` p.buyerId).size `=` 1))
```

**Generated Sql**
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


**Results**
```scala
Seq(
  (Buyer[Id](1, "James Bond", LocalDate.parse("2001-02-03")), true),
  (Buyer[Id](2, "叉烧包", LocalDate.parse("1923-11-12")), false),
  (Buyer[Id](3, "Li Haoyi", LocalDate.parse("1965-08-09")), false)
)
```


### scalasql.mysql.SubQueryTests.selectLimitUnionSelect

**ScalaSql Query**
```scala
Buyer.select.map(_.name.toLowerCase).take(2)
  .unionAll(Product.select.map(_.kebabCaseName.toLowerCase))
```

**Generated Sql**
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


**Results**
```scala
Seq("james bond", "叉烧包", "face-mask", "guitar", "socks", "skate-board", "camera", "cookie")
```


### scalasql.mysql.SubQueryTests.selectUnionSelectLimit

**ScalaSql Query**
```scala
Buyer.select.map(_.name.toLowerCase)
  .unionAll(Product.select.map(_.kebabCaseName.toLowerCase).take(2))
```

**Generated Sql**
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


**Results**
```scala
Seq("james bond", "叉烧包", "li haoyi", "face-mask", "guitar")
```


## scalasql.mysql.UpdateJoinTests
### scalasql.mysql.UpdateJoinTests.join

**ScalaSql Query**
```scala
Buyer.update(_.name `=` "James Bond").joinOn(ShippingInfo)(_.id `=` _.buyerId)
  .set(c => c._1.dateOfBirth := c._2.shippingDate)
```

**Generated Sql**
```sql
UPDATE buyer
JOIN shipping_info shipping_info0 ON buyer.id = shipping_info0.buyer_id
SET buyer.date_of_birth = shipping_info0.shipping_date
WHERE buyer.name = ?
```


**Results**
```scala
1
```


### scalasql.mysql.UpdateJoinTests.join

**ScalaSql Query**
```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



**Results**
```scala
Seq(LocalDate.parse("2012-04-05"))
```


### scalasql.mysql.UpdateJoinTests.multijoin

**ScalaSql Query**
```scala
Buyer.update(_.name `=` "James Bond").joinOn(ShippingInfo)(_.id `=` _.buyerId)
  .joinOn(Purchase)(_._2.id `=` _.shippingInfoId).joinOn(Product)(_._2.productId `=` _.id)
  .filter(t => t._2.name.toLowerCase `=` t._2.kebabCaseName.toLowerCase)
  .set(c => c._1._1._1.name := c._2.name)
```

**Generated Sql**
```sql
UPDATE buyer
JOIN shipping_info shipping_info0 ON buyer.id = shipping_info0.buyer_id
JOIN purchase purchase1 ON shipping_info0.id = purchase1.shipping_info_id
JOIN product product2 ON purchase1.product_id = product2.id
SET buyer.name = product2.name
WHERE buyer.name = ?
AND LOWER(product2.name) = LOWER(product2.kebab_case_name)
```


**Results**
```scala
1
```


### scalasql.mysql.UpdateJoinTests.multijoin

**ScalaSql Query**
```scala
Buyer.select.filter(_.id `=` 1).map(_.name)
```



**Results**
```scala
Seq("Camera")
```


### scalasql.mysql.UpdateJoinTests.joinSubquery

**ScalaSql Query**
```scala
Buyer.update(_.name `=` "James Bond")
  .joinOn(ShippingInfo.select.sortBy(_.id).asc.take(2))(_.id `=` _.buyerId)
  .set(c => c._1.dateOfBirth := c._2.shippingDate)
```

**Generated Sql**
```sql
UPDATE
  buyer
  JOIN (SELECT
      shipping_info0.id as res__id,
      shipping_info0.buyer_id as res__buyer_id,
      shipping_info0.shipping_date as res__shipping_date
    FROM shipping_info shipping_info0
    ORDER BY res__id ASC
    LIMIT 2) subquery0 ON buyer.id = subquery0.res__buyer_id
SET buyer.date_of_birth = subquery0.res__shipping_date
WHERE buyer.name = ?
```


**Results**
```scala
1
```


### scalasql.mysql.UpdateJoinTests.joinSubquery

**ScalaSql Query**
```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



**Results**
```scala
Seq(LocalDate.parse("2012-04-05"))
```


### scalasql.mysql.UpdateJoinTests.joinSubqueryEliminatedColumn

**ScalaSql Query**
```scala
Buyer.update(_.name `=` "James Bond")
  // Make sure the `SELECT shipping_info0.shipping_info_id as res__shipping_info_id`
  // column gets eliminated since it is not used outside the subquery
  .joinOn(ShippingInfo.select.sortBy(_.id).asc.take(2))(_.id `=` _.buyerId)
  .set(c => c._1.dateOfBirth := LocalDate.parse("2000-01-01"))
```

**Generated Sql**
```sql
UPDATE
  buyer
  JOIN (SELECT
      shipping_info0.id as res__id,
      shipping_info0.buyer_id as res__buyer_id
    FROM shipping_info shipping_info0
    ORDER BY res__id ASC
    LIMIT 2) subquery0 ON buyer.id = subquery0.res__buyer_id
SET buyer.date_of_birth = ?
WHERE buyer.name = ?
```


**Results**
```scala
1
```


### scalasql.mysql.UpdateJoinTests.joinSubqueryEliminatedColumn

**ScalaSql Query**
```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



**Results**
```scala
Seq(LocalDate.parse("2000-01-01"))
```


## scalasql.mysql.UpdateTests
### scalasql.mysql.UpdateTests.update

**ScalaSql Query**
```scala
Buyer.update(_.name `=` "James Bond").set(_.dateOfBirth := LocalDate.parse("2019-04-07"))
```

**Generated Sql**
```sql
UPDATE buyer SET buyer.date_of_birth = ? WHERE buyer.name = ?
```


**Results**
```scala
1
```


### scalasql.mysql.UpdateTests.update

**ScalaSql Query**
```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



**Results**
```scala
Seq(LocalDate.parse("2019-04-07"))
```


### scalasql.mysql.UpdateTests.update

**ScalaSql Query**
```scala
Buyer.select.filter(_.name `=` "Li Haoyi").map(_.dateOfBirth)
```



**Results**
```scala
Seq(LocalDate.parse("1965-08-09"))
```


### scalasql.mysql.UpdateTests.bulk

**ScalaSql Query**
```scala
Buyer.update(_ => true).set(_.dateOfBirth := LocalDate.parse("2019-04-07"))
```

**Generated Sql**
```sql
UPDATE buyer SET buyer.date_of_birth = ? WHERE ?
```


**Results**
```scala
3
```


### scalasql.mysql.UpdateTests.bulk

**ScalaSql Query**
```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



**Results**
```scala
Seq(LocalDate.parse("2019-04-07"))
```


### scalasql.mysql.UpdateTests.bulk

**ScalaSql Query**
```scala
Buyer.select.filter(_.name `=` "Li Haoyi").map(_.dateOfBirth)
```



**Results**
```scala
Seq(LocalDate.parse("2019-04-07"))
```


### scalasql.mysql.UpdateTests.multiple

**ScalaSql Query**
```scala
Buyer.update(_.name `=` "James Bond")
  .set(_.dateOfBirth := LocalDate.parse("2019-04-07"), _.name := "John Dee")
```

**Generated Sql**
```sql
UPDATE buyer SET buyer.date_of_birth = ?, buyer.name = ? WHERE buyer.name = ?
```


**Results**
```scala
1
```


### scalasql.mysql.UpdateTests.multiple

**ScalaSql Query**
```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



**Results**
```scala
Seq[LocalDate]()
```


### scalasql.mysql.UpdateTests.multiple

**ScalaSql Query**
```scala
Buyer.select.filter(_.name `=` "John Dee").map(_.dateOfBirth)
```



**Results**
```scala
Seq(LocalDate.parse("2019-04-07"))
```


### scalasql.mysql.UpdateTests.dynamic

**ScalaSql Query**
```scala
Buyer.update(_.name `=` "James Bond").set(c => c.name := c.name.toUpperCase)
```

**Generated Sql**
```sql
UPDATE buyer SET buyer.name = UPPER(buyer.name) WHERE buyer.name = ?
```


**Results**
```scala
1
```


### scalasql.mysql.UpdateTests.dynamic

**ScalaSql Query**
```scala
Buyer.select.filter(_.name `=` "James Bond").map(_.dateOfBirth)
```



**Results**
```scala
Seq[LocalDate]()
```


### scalasql.mysql.UpdateTests.dynamic

**ScalaSql Query**
```scala
Buyer.select.filter(_.name `=` "JAMES BOND").map(_.dateOfBirth)
```



**Results**
```scala
Seq(LocalDate.parse("2001-02-03"))
```

