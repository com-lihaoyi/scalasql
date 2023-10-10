package usql

import mainargs.{ParserForMethods, main}
import ExprOps._


object Main {
  @main
  def main() = {
    val createTableSql =
      """CREATE TABLE product (
        id INT PRIMARY KEY,
        sku VARCHAR(256),
        name VARCHAR(256),
        price DECIMAL(20, 2)
    --     UNIQUE KEY UNIQUE_PRODUCT_SKU (sku)
    );

    CREATE TABLE customer (
        id INT PRIMARY KEY,
        name VARCHAR(256),
        birthdate DATE
    );


    CREATE TABLE purchase_order (
        id INT PRIMARY KEY,
        customer_id INT,
        order_date DATE,
        FOREIGN KEY(customer_id) REFERENCES customer(id)
    );

    CREATE TABLE item (
        id INT PRIMARY KEY,
        order_id INT,
        product_id INT,
        quantity INT,
        total DECIMAL(20, 2),
        FOREIGN KEY(order_id) REFERENCES purchase_order(id),
        FOREIGN KEY(product_id) REFERENCES product(id)
    );

    INSERT INTO product (id, sku, name, price) VALUES (1, 'keyboard', 'Keyboard', 7.99);
    INSERT INTO product (id, sku, name, price) VALUES (2, 'tv', 'Television', 351.96);
    INSERT INTO product (id, sku, name, price) VALUES (3, 'shirt', 'Shirt', 3.57);
    INSERT INTO product (id, sku, name, price) VALUES (4, 'bed', 'Bed', 131.00);
    INSERT INTO product (id, sku, name, price) VALUES (5, 'cell-phone', 'Cell Phone', 1000.00);
    INSERT INTO product (id, sku, name, price) VALUES (6, 'spoon', 'Spoon', 1.00);

    INSERT INTO customer (id, name, birthdate) VALUES (1, 'John Doe', '1960-10-30');
    INSERT INTO customer (id, name, birthdate) VALUES (2, 'Pepito Pérez', '1954-07-15');
    INSERT INTO customer (id, name, birthdate) VALUES (3, 'Cosme Fulanito', '1956-05-12');

    INSERT INTO purchase_order (id, customer_id, order_date) VALUES (1, 2, '2018-01-04');
    INSERT INTO purchase_order (id, customer_id, order_date) VALUES (2, 1, '2018-02-13');
    INSERT INTO purchase_order (id, customer_id, order_date) VALUES (3, 2, '2018-02-25');

    INSERT INTO item (id, order_id, product_id, quantity, total) VALUES (1, 1, 1, 10, 79.90);
    INSERT INTO item (id, order_id, product_id, quantity, total) VALUES (2, 1, 2, 2, 703.92);
    INSERT INTO item (id, order_id, product_id, quantity, total) VALUES (3, 1, 3, 7, 24.99);
    INSERT INTO item (id, order_id, product_id, quantity, total) VALUES (4, 2, 4, 2, 262.00);
    INSERT INTO item (id, order_id, product_id, quantity, total) VALUES (5, 2, 5, 15, 15000.00);
    INSERT INTO item (id, order_id, product_id, quantity, total) VALUES (6, 3, 1, 7, 55.93);
    INSERT INTO item (id, order_id, product_id, quantity, total) VALUES (7, 3, 6, 18, 18.00);"""

    val connection = DatabaseSetup.getConnection()
    try {

      val statement = connection.createStatement()
      statement.executeUpdate(createTableSql)

      println("Table 'users' created successfully.")
    } catch {
      case e: Exception => e.printStackTrace()
    } finally {
      connection.close()
    }

  }

  def main(args: Array[String]): Unit = ParserForMethods(this).runOrExit(args)
}


import java.sql.{Connection, DriverManager, Statement}

object DatabaseSetup {
  private var connection: Connection = _

  def getConnection(): Connection = {
    if (connection == null) {
      try {
        // Load the SQLite JDBC driver
        Class.forName("org.sqlite.JDBC")

        // Create a connection to an in-memory database
        connection = DriverManager.getConnection("jdbc:sqlite::memory:")
      } catch {
        case e: Exception => e.printStackTrace()
      }
    }
    connection
  }
}