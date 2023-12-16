
# Developer Docs

* Running all unit tests: `./mill -i -w "__.test"`. 

* Running all unit tests on one database: `./mill -i -w "__.test scalasql.sqlite"`. This
  is much faster than running all tests, and useful for quick iteration for changes that
  are not database specific.

* Re-generating docs: `./mill -i "__.test" + generateTutorial + generateReference`
    * Note that ScalaSql's reference docs are extracted from the test suite, and thus we need
      to make sure to run the test suite before re-generating them.

* Fix all auto-generating and auto-formatting issues at once via
```
./mill -i -w __.fix + mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources + "scalasql[2.13.12].test" + generateTutorial + generateReference
```

* You can run ad-hoc benchmarks using any test case via the `SCALASQL_RUN_BENCHMARK` 
  environment variable, e.g.

```
SCALASQL_RUN_BENCHMARK=5000 ./mill -i -w __.test scalasql.sqlite.SubQueryTests.deeplyNested
```

* ScalaSql comprises 4 main submodules:
    * `scalasql.core`: the core functionality of evaluating `SqlStr` queries, but without any typed
      helpers to construct them
    * `scalasql.operations`: extension methods on `Expr[T]` values representing operations on typed
      SQL expressions, like `LOWER(str)` or `a || b`/`CONCAT(a, b)`
    * `scalasql.query`: builders for entire SQL queries, `INSERT`, `SELECT`, `UPDATE`, `DELETE`.
    * `scalasql`: the main user-facing ScalaSql module, contains the `package object` defining
      what a user sees when they do `import scalasql._`, as well as the various database `*Dialect`s
      that provide the relevant set of `query`s and `operations` for each respective database

* ScalaSql's tests are concentrated within a single `scalasql.test` module, with subfolders
  corresponding to the various ScalaSql sub-modules they are intended to cover

```
             scalasql.core
              |        |
        +-----+        +-----+
        |                    |
scalasql.operations   scalasql.query
        |                    |
        +------+      +------+
               |      |
               scalasql
                      |
                      +------+
                             |
                        scalasql.test
```