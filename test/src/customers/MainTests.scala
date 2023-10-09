package usql.customers

import utest._
import usql._
import ExprOps._
import pprint.PPrinter

object MainTests extends TestSuite {
  def camelToSnake(s: String) = {
    s.replaceAll("([A-Z])", "#$1").split('#').map(_.toLowerCase).mkString("_").stripPrefix("_")
  }

  def snakeToCamel(s: String) = {
    val out = new StringBuilder()
    val chunks = s.split("_", -1)
    for(i <- Range(0, chunks.length)){
      val chunk = chunks(i)
      if (i == 0) out.append(chunk)
      else{
        out.append(chunk(0).toUpper)
        out.append(chunk.drop(1))
      }
    }
    out.toString()
  }
  val db = new DatabaseApi(
    java.sql.DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", ""),
    tableNameMapper = camelToSnake,
    tableNameUnMapper = snakeToCamel,
    columnNameMapper = camelToSnake,
    columnNameUnMapper = snakeToCamel
  )
  db.runRaw(os.read(os.pwd / "test" / "resources" / "customers.sql"))

  case class Check[T, V](query: T)(implicit qr: Queryable[T, V]){
    def expect(sql: String, value: V) = {
      val sqlResult = db.toSqlQuery(query)
      val expectedSql = sql.trim.replaceAll("\\s+", " ")
      assert(sqlResult == expectedSql)

      val result = db.run(query)
      lazy val pprinter: PPrinter = PPrinter.Color.copy(
        additionalHandlers = { case v: Val[_] => pprinter.treeify(v.apply(), false, true) }
      )
      pprinter.log(result)
      assert(result == value)
    }
  }

  def tests = Tests {
    test("constant") - Check(Expr(1)).expect(
      sql = "SELECT ? as res",
      value = 1
    )

    test("table") - Check(Customer.query).expect(
      sql = """
        SELECT
          customer0.id as res__id,
          customer0.name as res__name,
          customer0.birthdate as res__birthdate
        FROM customer customer0
      """,
      value = Vector(
        Customer(id = 1, name = "John Doe", birthdate = "1960-10-30"),
        Customer(id = 2, name = "Pepito Pérez", birthdate = "1954-07-15"),
        Customer(id = 3, name = "Cosme Fulanito", birthdate = "1956-05-12")
      )
    )

    test("filter"){
      test("single") - Check(PurchaseOrder.query.filter(_.customerId === 2)).expect(
        sql = """
        SELECT
          purchase_order0.id as res__id,
          purchase_order0.customer_id as res__customer_id,
          purchase_order0.order_date as res__order_date
        FROM purchase_order purchase_order0
        WHERE purchase_order0.customer_id = ?
      """,
        value = Vector(
          PurchaseOrder(id = 1, customerId = 2, orderDate = "2018-01-04"),
          PurchaseOrder(id = 3, customerId = 2, orderDate = "2018-02-25")
        )
      )

      test("multiple") - Check(
        PurchaseOrder.query.filter(_.customerId === 2).filter(_.orderDate === "2018-02-25")
      ).expect(
        sql = """
        SELECT
          purchase_order0.id as res__id,
          purchase_order0.customer_id as res__customer_id,
          purchase_order0.order_date as res__order_date
        FROM purchase_order purchase_order0
        WHERE purchase_order0.customer_id = ?
        AND purchase_order0.order_date = ?
      """,
        value = Vector(
          PurchaseOrder(id = 3, customerId = 2, orderDate = "2018-02-25")
        )
      )
      test("combined") - Check(
        PurchaseOrder.query.filter(p => p.customerId === 2 && p.orderDate === "2018-02-25")
      ).expect(
        sql = """
        SELECT
          purchase_order0.id as res__id,
          purchase_order0.customer_id as res__customer_id,
          purchase_order0.order_date as res__order_date
        FROM purchase_order purchase_order0
        WHERE purchase_order0.customer_id = ?
        AND purchase_order0.order_date = ?
      """,
        value = Vector(
          PurchaseOrder(id = 3, customerId = 2, orderDate = "2018-02-25")
        )
      )
    }

    test("map"){
      test("single") - Check(Customer.query.map(_.name)).expect(
        sql = "SELECT customer0.name as res FROM customer customer0",
        value = Vector("John Doe", "Pepito Pérez", "Cosme Fulanito")
      )

      test("tuple2") - Check(Customer.query.map(c => (c.name, c.id))).expect(
        sql = "SELECT customer0.name as res__0, customer0.id as res__1 FROM customer customer0",
        value =  Vector(("John Doe", 1), ("Pepito Pérez", 2), ("Cosme Fulanito", 3))
      )

      test("tuple3") - Check(Customer.query.map(c => (c.name, c.id, c.birthdate))).expect(
        sql = """
          SELECT
            customer0.name as res__0,
            customer0.id as res__1,
            customer0.birthdate as res__2
          FROM customer customer0
        """,
        value =  Vector(
          ("John Doe", 1, "1960-10-30"),
          ("Pepito Pérez", 2, "1954-07-15"),
          ("Cosme Fulanito", 3, "1956-05-12")
        )
      )

      test("interpolateInMap") - Check(Product.query.map(_.price * 2)).expect(
        sql = "SELECT product0.price * ? as res FROM product product0",
        value = Vector(15.98, 703.92, 7.14, 262.0, 2000.0, 2.0)
      )

      test("heterogenousTuple") - Check(Customer.query.map(c => (c.id, c))).expect(
        sql = """
          SELECT
            customer0.id as res__0,
            customer0.id as res__1__id,
            customer0.name as res__1__name,
            customer0.birthdate as res__1__birthdate
          FROM customer customer0
        """,
        value = Vector(
          (1, Customer(id = 1, name = "John Doe", birthdate = "1960-10-30")),
          (2, Customer(id = 2, name = "Pepito Pérez", birthdate = "1954-07-15")),
          (3, Customer(id = 3, name = "Cosme Fulanito", birthdate = "1956-05-12"))
        )
      )
    }

    test("filterMap") - Check(Product.query.filter(_.price < 100).map(_.name)).expect(
      sql = "SELECT product0.name as res FROM product product0 WHERE product0.price < ?",
      value = Vector("Keyboard", "Shirt", "Spoon")
    )

    test("aggregate") - Check(Item.query.sumBy(_.total)).expect(
      sql = "SELECT SUM(item0.total) as res FROM item item0",
      value = 16144.74
    )

    test("sort") {
      test("sort") - Check(Product.query.sortBy(_.price).map(_.name)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price",
        value = Vector("Spoon", "Shirt", "Keyboard", "Bed", "Television", "Cell Phone")
      )

      test("sortLimit") - Check(Product.query.sortBy(_.price).map(_.name).take(2)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2",
        value = Vector("Spoon", "Shirt")
      )

      test("sortLimitTwiceHigher") - Check(Product.query.sortBy(_.price).map(_.name).take(2).take(3)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2",
        value = Vector("Spoon", "Shirt")
      )

      test("sortLimitTwiceLower") - Check(Product.query.sortBy(_.price).map(_.name).take(2).take(1)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1",
        value = Vector("Spoon")
      )

      test("sortOffset") - Check(Product.query.sortBy(_.price).map(_.name).drop(2)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price OFFSET 2",
        value = Vector("Keyboard", "Bed", "Television", "Cell Phone")
      )

      test("sortOffsetTwice") - Check(Product.query.sortBy(_.price).map(_.name).drop(2).drop(2)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price OFFSET 4",
        value = Vector("Television", "Cell Phone")
      )

      test("sortOffsetLimit") - Check(Product.query.sortBy(_.price).map(_.name).drop(2).take(2)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 2 OFFSET 2",
        value = Vector("Keyboard", "Bed")
      )

      test("sortLimitOffset") - Check(Product.query.sortBy(_.price).map(_.name).take(2).drop(1)).expect(
        sql = "SELECT product0.name as res FROM product product0 ORDER BY product0.price LIMIT 1 OFFSET 1",
        value = Vector("Shirt")
      )
    }

    test("joins"){
      test("joinFilter") - Check(
        Customer.query.joinOn(PurchaseOrder.query)(_.id === _.customerId)
          .filter(_._1.name === "Pepito Pérez")
      ).expect(
        sql = """
          SELECT
            customer0.id as res__0__id,
            customer0.name as res__0__name,
            customer0.birthdate as res__0__birthdate,
            purchase_order1.id as res__1__id,
            purchase_order1.customer_id as res__1__customer_id,
            purchase_order1.order_date as res__1__order_date
          FROM customer customer0
          JOIN purchase_order purchase_order1 ON customer0.id = purchase_order1.customer_id
          WHERE customer0.name = ?
        """,
        value = Vector(
          (
            Customer(id = 2, name = "Pepito Pérez", birthdate = "1954-07-15"),
            PurchaseOrder(id = 1, customerId = 2, orderDate = "2018-01-04")
          ),
          (
            Customer(id = 2, name = "Pepito Pérez", birthdate = "1954-07-15"),
            PurchaseOrder(id = 3, customerId = 2, orderDate = "2018-02-25")
          )
        )
      )

      test("joinFilterMap") - Check(
        Customer.query.joinOn(PurchaseOrder.query)(_.id === _.customerId)
          .filter(_._1.name === "John Doe")
          .map(_._2.orderDate)
      ).expect(
        sql = """
          SELECT purchase_order1.order_date as res
          FROM customer customer0
          JOIN purchase_order purchase_order1 ON customer0.id = purchase_order1.customer_id
          WHERE customer0.name = ?
        """,
        value = Vector("2018-02-13")
      )

      test("flatMap") - Check(
        Customer.query.flatMap(c => PurchaseOrder.query.map((c, _)))
          .filter{case (c, p) => c.id === p.customerId && c.name === "John Doe"}
          .map(_._2.orderDate)
      ).expect(
        sql = """
          SELECT purchase_order1.order_date as res
          FROM customer customer0, purchase_order purchase_order1
          WHERE customer0.id = purchase_order1.customer_id
          AND customer0.name = ?
        """,
        value = Vector("2018-02-13")
      )
      test("flatMap") - Check(
        Customer.query.flatMap(c =>
          PurchaseOrder.query
            .filter { p => c.id === p.customerId && c.name === "John Doe" }
        ).map(_.orderDate)

      ).expect(
        sql = """
          SELECT purchase_order1.order_date as res
          FROM customer customer0, purchase_order purchase_order1
          WHERE customer0.id = purchase_order1.customer_id
          AND customer0.name = ?
        """,
        value = Vector("2018-02-13")
      )
    }
  }
}

