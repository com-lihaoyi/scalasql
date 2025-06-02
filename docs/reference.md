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

>**A note for users of `SimpleTable`**: The examples in this document assume usage of
>`Table`, with a higher kinded type parameter on a case class. If you are using
>`SimpleTable`, then the same code snippets should work by dropping `[Sc]`.

## DataTypes
Basic operations on all the data types that ScalaSql supports mapping between Database types and Scala types
### DataTypes.constant

This example demonstrates a range of different data types being written
and read back via ScalaSQL

```scala
object MyEnum extends Enumeration {
  val foo, bar, baz = Value

  implicit def make: String => Value = withName
}
case class DataTypes[T[_]](
    myTinyInt: T[Byte],
    mySmallInt: T[Short],
    myInt: T[Int],
    myBigInt: T[Long],
    myDouble: T[Double],
    myBoolean: T[Boolean],
    myLocalDate: T[LocalDate],
    myLocalTime: T[LocalTime],
    myLocalDateTime: T[LocalDateTime],
    myUtilDate: T[Date],
    myInstant: T[Instant],
    myVarBinary: T[geny.Bytes],
    myUUID: T[java.util.UUID],
    myEnum: T[MyEnum.Value]
)

object DataTypes extends Table[DataTypes]

val value = DataTypes[Sc](
  myTinyInt = 123.toByte,
  mySmallInt = 12345.toShort,
  myInt = 12345678,
  myBigInt = 12345678901L,
  myDouble = 3.14,
  myBoolean = true,
  myLocalDate = LocalDate.parse("2023-12-20"),
  myLocalTime = LocalTime.parse("10:15:30"),
  myLocalDateTime = LocalDateTime.parse("2011-12-03T10:15:30"),
  myUtilDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse("2011-12-03T10:15:30.000"),
  myInstant = Instant.parse("2011-12-03T10:15:30Z"),
  myVarBinary = new geny.Bytes(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8)),
  myUUID = new java.util.UUID(1234567890L, 9876543210L),
  myEnum = MyEnum.bar
)

db.run(
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
    _.myUtilDate := value.myUtilDate,
    _.myInstant := value.myInstant,
    _.myVarBinary := value.myVarBinary,
    _.myUUID := value.myUUID,
    _.myEnum := value.myEnum
  )
) ==> 1

db.run(DataTypes.select) ==> Seq(value)
```






### DataTypes.nonRoundTrip

In general, databases do not store timezones and offsets together with their timestamps:
"TIMESTAMP WITH TIMEZONE" is a lie and it actually stores UTC and renders to whatever
timezone the client queries it from. Thus values of type `OffsetDateTime` can preserve
their instant, but cannot be round-tripped preserving the offset.

```scala
case class NonRoundTripTypes[T[_]](
    myZonedDateTime: T[ZonedDateTime],
    myOffsetDateTime: T[OffsetDateTime]
)

object NonRoundTripTypes extends Table[NonRoundTripTypes]

val value = NonRoundTripTypes[Sc](
  myZonedDateTime = ZonedDateTime.parse("2011-12-03T10:15:30+01:00[Europe/Paris]"),
  myOffsetDateTime = OffsetDateTime.parse("2011-12-03T10:15:30+00:00")
)

def normalize(v: NonRoundTripTypes[Sc]) = v.copy[Sc](
  myZonedDateTime = v.myZonedDateTime.withZoneSameInstant(ZoneId.systemDefault),
  myOffsetDateTime = v.myOffsetDateTime.withOffsetSameInstant(OffsetDateTime.now.getOffset)
)

db.run(
  NonRoundTripTypes.insert.columns(
    _.myOffsetDateTime := value.myOffsetDateTime,
    _.myZonedDateTime := value.myZonedDateTime
  )
) ==> 1

db.run(NonRoundTripTypes.select).map(normalize) ==> Seq(normalize(value))
```






### DataTypes.enclosing

You can nest `case class`es in other `case class`es to DRY up common sets of
table columns. These nested `case class`es have their columns flattened out
into the enclosing `case class`'s columns, such that at the SQL level it is
all flattened out without nesting.

```scala
// case class Nested[T[_]](
//   fooId: T[Int],
//   myBoolean: T[Boolean],
// )
// object Nested extends Table[Nested]
//
// case class Enclosing[T[_]](
//     barId: T[Int],
//     myString: T[String],
//     foo: Nested[T]
// )
// object Enclosing extends Table[Enclosing]
val value1 = Enclosing[Sc](
  barId = 1337,
  myString = "hello",
  foo = Nested[Sc](
    fooId = 271828,
    myBoolean = true
  )
)
val value2 = Enclosing[Sc](
  barId = 31337,
  myString = "world",
  foo = Nested[Sc](
    fooId = 1618,
    myBoolean = false
  )
)

val insertColumns = Enclosing.insert.columns(
  _.barId := value1.barId,
  _.myString := value1.myString,
  _.foo.fooId := value1.foo.fooId,
  _.foo.myBoolean := value1.foo.myBoolean
)
db.renderSql(insertColumns) ==>
  "INSERT INTO enclosing (bar_id, my_string, foo_id, my_boolean) VALUES (?, ?, ?, ?)"

db.run(insertColumns) ==> 1

val insertValues = Enclosing.insert.values(value2)
db.renderSql(insertValues) ==>
  "INSERT INTO enclosing (bar_id, my_string, foo_id, my_boolean) VALUES (?, ?, ?, ?)"

db.run(insertValues) ==> 1

db.renderSql(Enclosing.select) ==> """
          SELECT
            enclosing0.bar_id AS bar_id,
            enclosing0.my_string AS my_string,
            enclosing0.foo_id AS foo_id,
            enclosing0.my_boolean AS my_boolean
          FROM enclosing enclosing0
        """

db.run(Enclosing.select) ==> Seq(value1, value2)
```






### DataTypes.JoinNullable proper type mapping



```scala
case class A[T[_]](id: T[Int], bId: T[Option[Int]])
object A extends Table[A]

object Custom extends Enumeration {
  val Foo, Bar = Value

  implicit def make: String => Value = withName
}

case class B[T[_]](id: T[Int], custom: T[Custom.Value])
object B extends Table[B]
db.run(A.insert.columns(_.id := 1, _.bId := None))
val result = db.run(A.select.leftJoin(B)(_.id === _.id).single)
result._2 ==> None
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
      opt_cols0.my_int AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Sc](None, None),
      OptCols[Sc](Some(1), Some(2)),
      OptCols[Sc](Some(3), None),
      OptCols[Sc](None, Some(4))
    )
    ```



### Optional.groupByMaxGet

Some aggregates return `Expr[Option[V]]`s, et.c. `.maxByOpt`

```scala
OptCols.select.groupBy(_.myInt)(_.maxByOpt(_.myInt2.get))
```


*
    ```sql
    SELECT opt_cols0.my_int AS res_0, MAX(opt_cols0.my_int2) AS res_1
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
      opt_cols0.my_int AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int IS NOT NULL)
    ```



*
    ```scala
    Seq(OptCols[Sc](Some(1), Some(2)), OptCols[Sc](Some(3), None))
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
      opt_cols0.my_int AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int IS NULL)
    ```



*
    ```scala
    Seq(OptCols[Sc](None, None), OptCols[Sc](None, Some(4)))
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
      opt_cols0.my_int AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int = ?)
    ```



*
    ```scala
    Seq(OptCols[Sc](Some(1), Some(2)))
    ```



### Optional.sqlEquals.nonOptionMiss



```scala
OptCols.select.filter(_.myInt `=` 2)
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int = ?)
    ```



*
    ```scala
    Seq[OptCols[Sc]]()
    ```



### Optional.sqlEquals.optionMiss



```scala
OptCols.select.filter(_.myInt `=` Option.empty[Int])
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int = ?)
    ```



*
    ```scala
    Seq[OptCols[Sc]]()
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
      opt_cols0.my_int AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int <=> ?)
    ```



*
    ```scala
    Seq(OptCols[Sc](Some(1), Some(2)))
    ```



### Optional.scalaEquals.noneHit



```scala
OptCols.select.filter(_.myInt === Option.empty[Int])
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    WHERE (opt_cols0.my_int <=> ?)
    ```



*
    ```scala
    Seq(OptCols[Sc](None, None), OptCols[Sc](None, Some(4)))
    ```



### Optional.scalaEquals.notEqualsSome



```scala
OptCols.select.filter(_.myInt !== Option(1))
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    WHERE (NOT (opt_cols0.my_int <=> ?))
    ```



*
    ```scala
    Seq(
      OptCols[Sc](None, None),
      OptCols[Sc](Some(3), None),
      OptCols[Sc](None, Some(value = 4))
    )
    ```



### Optional.scalaEquals.notEqualsNone



```scala
OptCols.select.filter(_.myInt !== Option.empty[Int])
```


*
    ```sql
    SELECT
      opt_cols0.my_int AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    WHERE (NOT (opt_cols0.my_int <=> ?))
    ```



*
    ```scala
    Seq(
      OptCols[Sc](Some(1), Some(2)),
      OptCols[Sc](Some(3), None)
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
      (opt_cols0.my_int + ?) AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Sc](None, None),
      OptCols[Sc](Some(11), Some(2)),
      OptCols[Sc](Some(13), None),
      OptCols[Sc](None, Some(4))
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
      ((opt_cols0.my_int + opt_cols0.my_int2) + ?) AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Sc](None, None),
      OptCols[Sc](Some(13), Some(2)),
      // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
      OptCols[Sc](None, None),
      OptCols[Sc](None, Some(4))
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
      ((opt_cols0.my_int + opt_cols0.my_int2) + ?) AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Sc](None, None),
      OptCols[Sc](Some(4), Some(2)),
      // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
      OptCols[Sc](None, None),
      OptCols[Sc](None, Some(4))
    )
    ```



### Optional.rawGet



```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.get + d.myInt2.get + 1))
```


*
    ```sql
    SELECT
      ((opt_cols0.my_int + opt_cols0.my_int2) + ?) AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Sc](None, None),
      OptCols[Sc](Some(4), Some(2)),
      // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
      OptCols[Sc](None, None),
      OptCols[Sc](None, Some(4))
    )
    ```



### Optional.getOrElse



```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.getOrElse(-1)))
```


*
    ```sql
    SELECT
      COALESCE(opt_cols0.my_int, ?) AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Sc](Some(-1), None),
      OptCols[Sc](Some(1), Some(2)),
      OptCols[Sc](Some(3), None),
      OptCols[Sc](Some(-1), Some(4))
    )
    ```



### Optional.orElse



```scala
OptCols.select.map(d => d.copy[Expr](myInt = d.myInt.orElse(d.myInt2)))
```


*
    ```sql
    SELECT
      COALESCE(opt_cols0.my_int, opt_cols0.my_int2) AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Sc](None, None),
      OptCols[Sc](Some(1), Some(2)),
      OptCols[Sc](Some(3), None),
      OptCols[Sc](Some(4), Some(4))
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
      END AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols[Sc](None, None),
      OptCols[Sc](Some(1), Some(2)),
      OptCols[Sc](None, None),
      OptCols[Sc](None, Some(4))
    )
    ```



### Optional.sorting.nullsLast

`.nullsLast` and `.nullsFirst` translate to SQL `NULLS LAST` and `NULLS FIRST` clauses

```scala
OptCols.select.sortBy(_.myInt).nullsLast
```


*
    ```sql
    SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ORDER BY my_int IS NULL ASC, my_int
    ```



*
    ```scala
    Seq(
      OptCols[Sc](Some(1), Some(2)),
      OptCols[Sc](Some(3), None),
      OptCols[Sc](None, None),
      OptCols[Sc](None, Some(4))
    )
    ```



### Optional.sorting.nullsFirst



```scala
OptCols.select.sortBy(_.myInt).nullsFirst
```


*
    ```sql
    SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ORDER BY my_int IS NULL DESC, my_int
    ```



*
    ```scala
    Seq(
      OptCols[Sc](None, None),
      OptCols[Sc](None, Some(4)),
      OptCols[Sc](Some(1), Some(2)),
      OptCols[Sc](Some(3), None)
    )
    ```



### Optional.sorting.ascNullsLast



```scala
OptCols.select.sortBy(_.myInt).asc.nullsLast
```


*
    ```sql
    SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ORDER BY my_int IS NULL ASC, my_int ASC
    ```



*
    ```scala
    Seq(
      OptCols[Sc](Some(1), Some(2)),
      OptCols[Sc](Some(3), None),
      OptCols[Sc](None, None),
      OptCols[Sc](None, Some(4))
    )
    ```



### Optional.sorting.ascNullsFirst



```scala
OptCols.select.sortBy(_.myInt).asc.nullsFirst
```


*
    ```sql
    SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ORDER BY my_int ASC
    ```



*
    ```scala
    Seq(
      OptCols[Sc](None, None),
      OptCols[Sc](None, Some(4)),
      OptCols[Sc](Some(1), Some(2)),
      OptCols[Sc](Some(3), None)
    )
    ```



### Optional.sorting.descNullsLast



```scala
OptCols.select.sortBy(_.myInt).desc.nullsLast
```


*
    ```sql
    SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ORDER BY my_int DESC
    ```



*
    ```scala
    Seq(
      OptCols[Sc](Some(3), None),
      OptCols[Sc](Some(1), Some(2)),
      OptCols[Sc](None, None),
      OptCols[Sc](None, Some(4))
    )
    ```



### Optional.sorting.descNullsFirst



```scala
OptCols.select.sortBy(_.myInt).desc.nullsFirst
```


*
    ```sql
    SELECT opt_cols0.my_int AS my_int, opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ORDER BY my_int IS NULL DESC, my_int DESC
    ```



*
    ```scala
    Seq(
      OptCols[Sc](None, None),
      OptCols[Sc](None, Some(4)),
      OptCols[Sc](Some(3), None),
      OptCols[Sc](Some(1), Some(2))
    )
    ```



### Optional.sorting.roundTripOptionalValues

This example demonstrates a range of different data types being written
as options, both with Some(v) and None values

```scala
object MyEnum extends Enumeration {
  val foo, bar, baz = Value

  implicit def make: String => Value = withName
}
case class OptDataTypes[T[_]](
    myTinyInt: T[Option[Byte]],
    mySmallInt: T[Option[Short]],
    myInt: T[Option[Int]],
    myBigInt: T[Option[Long]],
    myDouble: T[Option[Double]],
    myBoolean: T[Option[Boolean]],
    myLocalDate: T[Option[LocalDate]],
    myLocalTime: T[Option[LocalTime]],
    myLocalDateTime: T[Option[LocalDateTime]],
    myUtilDate: T[Option[Date]],
    myInstant: T[Option[Instant]],
    myVarBinary: T[Option[geny.Bytes]],
    myUUID: T[Option[java.util.UUID]],
    myEnum: T[Option[MyEnum.Value]]
)

object OptDataTypes extends Table[OptDataTypes] {
  override def tableName: String = "data_types"
}

val rowSome = OptDataTypes[Sc](
  myTinyInt = Some(123.toByte),
  mySmallInt = Some(12345.toShort),
  myInt = Some(12345678),
  myBigInt = Some(12345678901L),
  myDouble = Some(3.14),
  myBoolean = Some(true),
  myLocalDate = Some(LocalDate.parse("2023-12-20")),
  myLocalTime = Some(LocalTime.parse("10:15:30")),
  myLocalDateTime = Some(LocalDateTime.parse("2011-12-03T10:15:30")),
  myUtilDate = Some(
    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse("2011-12-03T10:15:30.000")
  ),
  myInstant = Some(Instant.parse("2011-12-03T10:15:30Z")),
  myVarBinary = Some(new geny.Bytes(Array[Byte](1, 2, 3, 4, 5, 6, 7, 8))),
  myUUID = Some(new java.util.UUID(1234567890L, 9876543210L)),
  myEnum = Some(MyEnum.bar)
)

val rowNone = OptDataTypes[Sc](
  myTinyInt = None,
  mySmallInt = None,
  myInt = None,
  myBigInt = None,
  myDouble = None,
  myBoolean = None,
  myLocalDate = None,
  myLocalTime = None,
  myLocalDateTime = None,
  myUtilDate = None,
  myInstant = None,
  myVarBinary = None,
  myUUID = None,
  myEnum = None
)

db.run(
  OptDataTypes.insert.values(rowSome, rowNone)
) ==> 2

db.run(OptDataTypes.select) ==> Seq(rowSome, rowNone)
```






### Optional.filter - with SimpleTable

`.filter` follows normal Scala semantics, and translates to a `CASE`/`WHEN (foo)`/`ELSE NULL`

```scala
OptCols.select.map(d => d.updates(_.myInt(_.filter(_ < 2))))
```


*
    ```sql
    SELECT
      CASE
        WHEN (opt_cols0.my_int < ?) THEN opt_cols0.my_int
        ELSE NULL
      END AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols(None, None),
      OptCols(Some(1), Some(2)),
      OptCols(None, None),
      OptCols(None, Some(4))
    )
    ```



### Optional.getOrElse - with SimpleTable



```scala
OptCols.select.map(d => d.updates(_.myInt(_.getOrElse(-1))))
```


*
    ```sql
    SELECT
      COALESCE(opt_cols0.my_int, ?) AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols(Some(-1), None),
      OptCols(Some(1), Some(2)),
      OptCols(Some(3), None),
      OptCols(Some(-1), Some(4))
    )
    ```



### Optional.rawGet - with SimpleTable



```scala
OptCols.select.map(d => d.updates(_.myInt := d.myInt.get + d.myInt2.get + 1))
```


*
    ```sql
    SELECT
      ((opt_cols0.my_int + opt_cols0.my_int2) + ?) AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols(None, None),
      OptCols(Some(4), Some(2)),
      // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
      OptCols(None, None),
      OptCols(None, Some(4))
    )
    ```



### Optional.orElse - with SimpleTable



```scala
OptCols.select.map(d => d.updates(_.myInt(_.orElse(d.myInt2))))
```


*
    ```sql
    SELECT
      COALESCE(opt_cols0.my_int, opt_cols0.my_int2) AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols(None, None),
      OptCols(Some(1), Some(2)),
      OptCols(Some(3), None),
      OptCols(Some(4), Some(4))
    )
    ```



### Optional.flatMap - with SimpleTable



```scala
OptCols.select
  .map(d => d.updates(_.myInt(_.flatMap(v => d.myInt2.map(v2 => v + v2 + 10)))))
```


*
    ```sql
    SELECT
      ((opt_cols0.my_int + opt_cols0.my_int2) + ?) AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols(None, None),
      OptCols(Some(13), Some(2)),
      // because my_int2 is added to my_int, and my_int2 is null, my_int becomes null too
      OptCols(None, None),
      OptCols(None, Some(4))
    )
    ```



### Optional.map - with SimpleTable

You can use operators like `.map` and `.flatMap` to work with
your `Expr[Option[V]]` values. These roughly follow the semantics
that you would be familiar with from Scala.

```scala
OptCols.select.map(d => d.updates(_.myInt(_.map(_ + 10))))
```


*
    ```sql
    SELECT
      (opt_cols0.my_int + ?) AS my_int,
      opt_cols0.my_int2 AS my_int2
    FROM opt_cols opt_cols0
    ```



*
    ```scala
    Seq(
      OptCols(None, None),
      OptCols(Some(11), Some(2)),
      OptCols(Some(13), None),
      OptCols(None, Some(4))
    )
    ```


