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

## Schema
Additional tests to ensure schema mapping produces valid SQL
### Schema.schema.select

If your table belongs to a schema other than the default schema of your database,
you can specify this in your table definition with table.schemaName

```scala
Invoice.select
```


*
    ```sql
    SELECT invoice0.id AS id, invoice0.total AS total, invoice0.vendor_name AS vendor_name
    FROM otherschema.invoice invoice0
    ```



*
    ```scala
    Seq(
      Invoice[Sc](id = 1, total = 150.4, vendor_name = "Siemens"),
      Invoice[Sc](id = 2, total = 213.3, vendor_name = "Samsung"),
      Invoice[Sc](id = 3, total = 407.2, vendor_name = "Shell")
    )
    ```



### Schema.schema.insert.columns

If your table belongs to a schema other than the default schema of your database,
you can specify this in your table definition with table.schemaName

```scala
Invoice.insert.columns(
  _.total := 200.3,
  _.vendor_name := "Huawei"
)
```


*
    ```sql
    INSERT INTO otherschema.invoice (total, vendor_name) VALUES (?, ?)
    ```



*
    ```scala
    1
    ```



### Schema.schema.insert.values

If your table belongs to a schema other than the default schema of your database,
you can specify this in your table definition with table.schemaName

```scala
Invoice.insert
  .values(
    Invoice[Sc](
      id = 0,
      total = 200.3,
      vendor_name = "Huawei"
    )
  )
  .skipColumns(_.id)
```


*
    ```sql
    INSERT INTO otherschema.invoice (total, vendor_name) VALUES (?, ?)
    ```



*
    ```scala
    1
    ```



### Schema.schema.update

If your table belongs to a schema other than the default schema of your database,
you can specify this in your table definition with table.schemaName

```scala
Invoice
  .update(_.id === 1)
  .set(
    _.total := 200.3,
    _.vendor_name := "Huawei"
  )
```


*
    ```sql
    UPDATE otherschema.invoice
                SET
                  total = ?,
                  vendor_name = ?
                WHERE
                  (invoice.id = ?)
    ```



*
    ```scala
    1
    ```



### Schema.schema.delete

If your table belongs to a schema other than the default schema of your database,
you can specify this in your table definition with table.schemaName

```scala
Invoice.delete(_.id === 1)
```


*
    ```sql
    DELETE FROM otherschema.invoice WHERE (invoice.id = ?)
    ```



*
    ```scala
    1
    ```



### Schema.schema.insert into

If your table belongs to a schema other than the default schema of your database,
you can specify this in your table definition with table.schemaName

```scala
Invoice.insert.select(
  i => (i.total, i.vendor_name),
  Invoice.select.map(i => (i.total, i.vendor_name))
)
```


*
    ```sql
    INSERT INTO
                  otherschema.invoice (total, vendor_name)
                SELECT
                  invoice0.total AS res_0,
                  invoice0.vendor_name AS res_1
                FROM
                  otherschema.invoice invoice0
    ```



*
    ```scala
    4
    ```



### Schema.schema.join

If your table belongs to a schema other than the default schema of your database,
you can specify this in your table definition with table.schemaName

```scala
Invoice.select.join(Invoice)(_.id `=` _.id).map(_._1.id)
```


*
    ```sql
    SELECT
                  invoice0.id AS res
                FROM
                  otherschema.invoice invoice0
                JOIN otherschema.invoice invoice1 ON (invoice0.id = invoice1.id)
    ```



*
    ```scala
    Seq(2, 3, 4, 5, 6, 7, 8, 9)
    ```


