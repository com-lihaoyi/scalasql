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

## OnConflict
Queries using `ON CONFLICT DO UPDATE` or `ON CONFLICT DO NOTHING`
### OnConflict.ignore

ScalaSql's `.onConflictIgnore` translates into SQL's `ON CONFLICT DO NOTHING`

Note that H2 and HsqlExpr do not support `onConflictIgnore` and `onConflictUpdate`, while
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



----

with `insert.values`

```scala
Buyer.insert
  .values(
    Buyer[Sc](
      id = 1,
      name = "test buyer",
      dateOfBirth = LocalDate.parse("2023-09-09")
    )
  )
  .onConflictIgnore(_.id)
```


*
    ```sql
    INSERT INTO buyer (id, name, date_of_birth) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING
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



----

with `insert.values`

```scala
Buyer.insert
  .values(
    Buyer[Sc](
      id = 1,
      name = "test buyer",
      dateOfBirth = LocalDate.parse("2023-09-09")
    )
  )
  .onConflictIgnore(_.id)
  .returning(_.name)
```


*
    ```sql
    INSERT INTO buyer (id, name, date_of_birth) VALUES (?, ?, ?)
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
    _.id := 4
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



----

with `insert.values`

```scala
Buyer.insert
  .values(
    Buyer[Sc](
      id = 5,
      name = "test buyer",
      dateOfBirth = LocalDate.parse("2023-09-09")
    )
  )
  .onConflictIgnore(_.id)
  .returning(_.name)
```


*
    ```sql
    INSERT INTO buyer (id, name, date_of_birth) VALUES (?, ?, ?)
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

with `insert.values`

```scala
Buyer.insert
  .values(
    Buyer[Sc](
      id = 1,
      name = "test buyer",
      dateOfBirth = LocalDate.parse("2023-09-09")
    )
  )
  .onConflictUpdate(_.id)(_.dateOfBirth := LocalDate.parse("2023-10-10"))
```


*
    ```sql
    INSERT INTO buyer (id, name, date_of_birth) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET date_of_birth = ?
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
      Buyer[Sc](1, "TEST BUYER CONFLICT", LocalDate.parse("2023-10-10")),
      Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
      Buyer[Sc](3, "Li Haoyi", LocalDate.parse("1965-08-09"))
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

with `insert.values`

```scala
Buyer.insert
  .values(
    Buyer[Sc](
      id = 3,
      name = "test buyer",
      dateOfBirth = LocalDate.parse("2023-09-09")
    )
  )
  .onConflictUpdate(_.id)(v => v.name := v.name.toUpperCase)
```


*
    ```sql
    INSERT INTO buyer (id, name, date_of_birth) VALUES (?, ?, ?) ON CONFLICT (id) DO UPDATE SET name = UPPER(buyer.name)
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
      Buyer[Sc](1, "JAMES BOND", LocalDate.parse("2001-02-03")),
      Buyer[Sc](2, "叉烧包", LocalDate.parse("1923-11-12")),
      Buyer[Sc](3, "LI HAOYI", LocalDate.parse("1965-08-09"))
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



----

with `insert.values`

```scala
Buyer.insert
  .values(
    Buyer[Sc](
      id = 1,
      name = "test buyer",
      dateOfBirth = LocalDate.parse("2023-09-09")
    )
  )
  .onConflictUpdate(_.id)(v => v.name := v.name.toLowerCase)
  .returning(_.name)
  .single
```


*
    ```sql
    INSERT INTO buyer (id, name, date_of_birth) VALUES (?, ?, ?)
    ON CONFLICT (id) DO UPDATE
    SET name = LOWER(buyer.name)
    RETURNING buyer.name AS res
    ```



*
    ```scala
    "james bond"
    ```


