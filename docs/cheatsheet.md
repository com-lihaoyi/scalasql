# ScalaSql Cheat Sheet


### Call Styles

**Scala Queries**
```scala
val str = "hello"

// Select Query
db.run(Foo.select.filter(_.myStr === str)): Seq[Foo[Id]]

// Update Query
db.run(Foo.update(_.myStr === str).set(_.myInt := 123)): Int
```

**SQL Queries**
```scala
// SQL Select Query
db.runSql[Seq[Foo[Id]]](sql"SELECT * FROM foo WHERE foo.my_str = $str"): Seq[Foo[Id]]

// SQL Update Query
db.updateSql(sql"UPDATE foo SET my_int = 123 WHERE foo.my_str = $str"): Int
```

**Raw Queries**

```scala
// Raw Select Query
db.runRaw[Foo[Id]]("SELECT * FROM foo WHERE foo.my_str = ?", Seq(str)): Seq[Foo[Id]]

// Raw Update Query
db.updateRaw("UPDATE foo SET my_int = 123 WHERE foo.my_str = ?", Seq(str)): Int
```

**Streaming Queries**

```scala
// Streaming Select Query
db.stream(Foo.select.filter(_.myStr === str)): Generator[Foo[Id]]

// Streaming SQL Select Query
db.streamSql[Foo[Id]](sql"SELECT * FROM foo WHERE foo.my_str = $str"): Generator[Foo[Id]]

// Streaming SQL Select Query
db.streamRaw[Foo[Id]]("SELECT * FROM foo WHERE foo.my_str = ?", Seq(str)): Generator[Foo[Id]]
```

### Selects

```scala
Foo.select                                                          // Seq[Foo[Id]]           
// SELECT * FROM foo

Foo.select.map(_.myStr)                                             // Seq[String]
// SELECT my_str FROM foo

Foo.select.map(t => (t.myStr, t.myInt))                             // Seq[(String, Int)]
// SELECT my_str, my_int FROM foo 

Foo.select.sumBy(_.myInt)                                           // Int
// SELECT SUM(my_int) FROM foo

Foo.select.sumByOpt(_.myInt)                                        // Option[Int]
// SELECT SUM(my_int) FROM foo

Foo.select.size                                                     // Int
// SELECT COUNT(1) FROM foo

Foo.select.aggregate(fs => (fs.sumBy(_.myInt), fs.maxBy(_.myInt)))  // (Int, Int)
// SELECT SUM(my_int), MAX(my_int) FROM foo

Foo.select.filter(_.myStr === "hello")                              // Seq[Foo[Id]]
// SELECT * FROM foo WHERE my_str = "hello"

Foo.select.filter(_.myStr === Expr("hello"))                        // Seq[Foo[Id]]
// SELECT * FROM foo WHERE my_str = "hello"

Foo.select.filter(_.myStr === "hello").single                       // Foo[Id]
// SELECT * FROM foo WHERE my_str = "hello"

Foo.select.map(_.myInt).sorted.asc                                  // Seq[Foo[Id]]
// SELECT * FROM foo ORDER BY my_int ASC

Foo.select.sortBy(_.myInt).asc.take(20).drop(5)                     // Seq[Foo[Id]] 
// SELECT * FROM foo ORDER BY my_int ASC LIMIT 15 OFFSET 5

Foo.select.map(_.myInt.cast[String])                                // Seq[String]
// SELECT CAST(my_int AS VARCHAR) FROM foo

Foo.select.join(Bar)(_.id === _.fooId)                              // Seq[(Foo[Id], Bar[Id])] 
// SELECT * FROM foo JOIN bar ON foo.id = foo2.foo_id

Foo.select.leftJoin(Bar)(_.id === _.fooId) // Seq[(Foo[Id], Option[Bar[Id]])]
// SELECT * FROM foo LEFT JOIN bar ON foo.id = foo2.foo_id

Foo.select.rightJoin(Bar)(_.id === _.fooId) // Seq[(Option[Foo[Id]], Bar[Id])]
// SELECT * FROM foo RIGHT JOIN bar ON foo.id = foo2.foo_id

Foo.select.outerJoin(Bar)(_.id === _.fooId) // Seq[(Option[Foo[Id]], Option[Bar[Id]])]
// SELECT * FROM foo FULL OUTER JOIN bar ON foo.id = foo2.foo_id

for(f <- Foo.select; b <- Bar.join(f.id === _.fooId)) yield (f, b)  // Seq[(Foo[Id], Bar[Id])] 
// SELECT * FROM foo JOIN bar ON foo.id = foo2.foo_id
```

### Insert/Update/Delete

```scala
Foo.insert.values(_.myStr := "hello", _.myInt := 123)         // 1
// INSERT INTO foo (my_str, my_int) VALUES ("hello", 123)

Foo.insert.batched(_.myStr, _.myInt)(("a", 1), ("b", 2))      // 2
// INSERT INTO foo (my_str, my_int) VALUES ("a", 1), ("b", 2)

Foo.update(_.myStr === "hello").set(_.myInt := 123)           // Int
// UPDATE foo SET my_int = 123 WHERE foo.my_str = "hello"

Foo.update(_.myStr === "a").set(t => t.myInt := t.myInt + 1)   // Int
// UPDATE foo SET my_int = foo.my_int + 1 WHERE foo.my_str = "a"

Foo
  .update(_.myStr === "a")
  .set(t => t.myInt := t.myInt + 1)
  .returning(f => (f.id, f.myInt))   // Seq[(Int, Int)]
// UPDATE foo SET my_int = foo.my_int + 1 WHERE foo.my_str = "a" RETURNING foo.id, foo.my_int

Foo.delete(_.myStr === "hello")                               // Int
// DELETE FROM foo WHERE foo.my_str = "hello"
```


### Type Mapping

|                   Scala |                   Postgres |               MySql |                   Sqlite |                           H2 |
|------------------------:|---------------------------:|--------------------:|-------------------------:|-----------------------------:|
|                `String` |               `VARCHAR(n)` |        `VARCHAR(n)` |             `VARCHAR(n)` |                 `VARCHAR(n)` |
|                  `Byte` |                 `SMALLINT` |          `SMALLINT` |               `SMALLINT` |                    `TINYINT` |
|                 `Short` |                 `SMALLINT` |          `SMALLINT` |               `SMALLINT` |                   `SMALLINT` |
|                   `Int` |                  `INTEGER` |           `INTEGER` |                `INTEGER` |                    `INTEGER` |
|                  `Long` |                   `BIGINT` |            `BIGINT` |                 `BIGINT` |                     `BIGINT` |
|                `Double` |         `DOUBLE PRECISION` |  `DOUBLE PRECISION` |       `DOUBLE PRECISION` |                     `DOUBLE` |
|               `Boolean` |                  `BOOLEAN` |           `BOOLEAN` |                `BOOLEAN` |                    `BOOLEAN` |
|             `LocalDate` |                     `DATE` |              `DATE` |                   `DATE` |                       `DATE` |
|             `LocalTime` |                     `TIME` |              `TIME` |                   `TIME` |                       `TIME` |
|         `LocalDateTime` |                `TIMESTAMP` |         `TIMESTAMP` |              `TIMESTAMP` |                  `TIMESTAMP` |
|               `Instant` | `TIMESTAMP WITH TIME ZONE` |          `DATETIME` |               `DATETIME` |   `TIMESTAMP WITH TIME ZONE` |
|            `geny.Bytes` |                    `BYTEA` |      `VARBINARY(n)` |              `VARBINARY` |             `VARBINARY(256)` |
|        `java.util.UUID` |                     `UUID` |          `CHAR(36)` |             `BINARY(16)` |                       `UUID` |
| `scala.Enumeration` (1) |                     `ENUM` |        `VARCHAR(n)` |             `VARCHAR(n)` |                 `VARCHAR(n)` |

(1) Your subclass of `Enumeration` needs to define a `implicit def make: String => Value = withName`
to allow ScalaSql to work with it


### Common ScalaSql Functions

**Expression Functions**

|   Scala |                        SQL | Scala |                   SQL |
|-----------------:|-----------------------------------------:|---------------:|------------------------------------:|
|        `a === b` |    `a IS NOT DISTINCT FROM b` or `a = b` |      `a !== b` | `a IS DISTINCT FROM b`  or `a <> b` |
|          `a < b` |                                  `a < b` |       `a <= b` |                            `a <= b` |
|          `a > b` |                                  `a > b` |       `a >= b` |                            `a >= b` |
|          `a = b` |                                  `a = b` |       `a <> b` |                            `a <> b` |
| `a.cast[String]` |                     `CAST(a AS VARCHAR)` |

**Numeric Functions**

| Scala | SQL | Scala | SQL |
|---------------:|------------------:|---------------:|------------------:|
|           `+a` |              `+a` |           `-a` |              `-a` |
|        `a + b` |           `a + b` |        `a - b` |           `a - b` |
|        `a * b` |           `a * b` |        `a / b` |           `a / b` |
|        `a % b` |       `MOD(a, b)` |        `a & b` |           `a & b` |
|        `a ^ b` |           `a ^ b` |       `a \| b` |          `a \| b` |
|       `a.ceil` |         `CEIL(a)` |      `a.floor` |        `FLOOR(a)` |
|    `a.expr(b)` |       `EXP(a, b)` |         `a.ln` |           `LN(a)` |
|     `a.pow(b)` |       `POW(a, b)` |       `a.sqrt` |         `SQRT(a)` |
| `a.abs` | `ABS(a)` |

**Boolean Functions**

| Scala | SQL | Scala | SQL |
|---------------:|------------------:|---------------:|------------------:|
|       `a && b` |         `a AND b` |     `a \|\| b` |          `a OR b` |
|           `!a` |           `NOT a` |

**String Functions**

|               Scala |                  SQL |           Scala |                          SQL |
|--------------------:|---------------------:|----------------:|-----------------------------:|
|             `a + b` |           `a \|\| b` |     `a.like(b)` |                   `a LIKE b` |
|  `a.toLowerCase(b)` |           `LOWER(a)` | `a.toUpperCase` |                   `UPPER(a)` |
|      `a.indexOf(b)` |     `POSITION(a, b)` |        `a.trim` |                    `TRIM(a)` |
|          `a.length` |          `LENGTH(a)` | `a.octetLength` |            `OCTET_LENGTH(a)` |
|           `a.ltrim` |           `LTRIM(a)` |       `a.rtrim` |                   `RTRIM(a)` |
| `a.substring(b, c)` | `SUBSTRING(a, b, c)` | `a.contains(b)` | `a LIKE '%' \|\| b \|\| '%'` |
|   `a.startsWith(b)` |  `a LIKE b \|\| '%'` | `a.endsWith(b)` |          `a LIKE '%' \|\| b` |

**Aggregate Functions**

|                                      Scala |       SQL |
|----------------------------------------------------:|------------------------:|
|                                            `a.size` |              `COUNT(1)` |
|                                   `a.mkString(sep)` |  `GROUP_CONCAT(a, sep)` |
|  `a.sum`, `a.sumBy(_.myInt)`, `a.sumByOpt(_.myInt)` |           `SUM(my_int)` |
|  `a.min`, `a.minBy(_.myInt)`, `a.minByOpt(_.myInt)` |           `MIN(my_int)` |
|  `a.max`, `a.maxBy(_.myInt)`, `a.maxByOpt(_.myInt)` |           `MAX(my_int)` |
|  `a.avg`, `a.avgBy(_.myInt)`, `a.avgByOpt(_.myInt)` |           `AVG(my_int)` |

**Select Functions**

|  Scala | SQL | Scala | SQL |
|----------------:|------------------:|---------------:|------------------:|
| `s.contains(a)` |          `a IN s` |
|    `s.nonEmpty` |        `EXISTS s` |    `s.isEmpty` |    `NOT EXISTS s` |

**Option/Nullable Functions**

|   Scala | SQL | Scala | SQL |
|-----------------:|------------------:|---------------:|------------------:|
|    `a.isDefined` |   `a IS NOT NULL` |    `a.isEmpty` |       `a IS NULL` |
|       `a.map(b)` |               `b` | `a.flatMap(b)` |               `b` |
| `a.getOrElse(b)` |  `COALESCE(a, b)` |  `a.orElse(b)` |  `COALESCE(a, b)` |
