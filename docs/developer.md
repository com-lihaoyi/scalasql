
# Developer Docs

## Precondition
Before running the unit tests, ensure the following steps are completed:

1. **Start Docker on Your Machine:**
   Ensure that Docker is running. You can verify this by executing `docker info` in your terminal. If Docker is not running, start it using your system's preferred method (e.g., using the Docker desktop application or by running `systemctl start docker` on Linux).

## Running Unit Tests
To facilitate efficient testing, you can choose from several commands based on your testing needs:

### Run all Tests
* Running all unit tests: 
  ```bash
    ./mill -i -w "__.test"
  ```

### Quick Database-Specific Tests
* Running all unit tests on one database. This
  is much faster than running all tests, and useful for quick iteration for changes that
  are not database specific:
  ```bash
  ./mill -i -w "__.test scalasql.sqlite"
  ```

### Full Test Suite with Documentation Generation
* Re-generating docs:
  ```bash
  ./mill -ij1 "__.test" && ./mill -ij1 generateTutorial + generateReference
  ```
    * Note that ScalaSql's reference docs are extracted from the test suite, and thus we need
      to make sure to run the test suite before re-generating them.

### Code Formatting and Auto-Fixes
* Fix all auto-generating and auto-formatting issues at once via
  ```bash
  ./mill -i -w __.fix + mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources + "scalasql[2.13.12].test" + generateTutorial + generateReference
  ```

## Benchmarking
* You can run ad-hoc benchmarks using any test case via the `SCALASQL_RUN_BENCHMARK` 
environment variable, e.g.
    ```bash
    SCALASQL_RUN_BENCHMARK=5000 ./mill -i -w __.test scalasql.sqlite.SubQueryTests.deeplyNested
    ```

## ScalaSql Modules Overview
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