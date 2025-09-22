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

## DbCountOps
COUNT and COUNT(DISTINCT) aggregations
### DbCountOps.countBy



```scala
Purchase.select.countBy(_.productId)
```


*
    ```sql
    SELECT COUNT(purchase0.product_id) AS res FROM purchase purchase0
    ```



*
    ```scala
    7
    ```


