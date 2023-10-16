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

    CREATE TABLE buyer (
        id INT PRIMARY KEY,
        name VARCHAR(256),
        birthdate DATE
    );


    CREATE TABLE shipping_info (
        id INT PRIMARY KEY,
        buyer_id INT,
        shipping_date DATE,
        FOREIGN KEY(buyer_id) REFERENCES buyer(id)
    );

    CREATE TABLE purchase (
        id INT PRIMARY KEY,
        shipping_info_id INT,
        product_id INT,
        count INT,
        total DECIMAL(20, 2),
        FOREIGN KEY(shipping_info_id) REFERENCES shipping_info(id),
        FOREIGN KEY(product_id) REFERENCES product(id)
    );

    INSERT INTO product (id, sku, name, price) VALUES (1, 'face-mask', 'Face Mask', 8.88);
    INSERT INTO product (id, sku, name, price) VALUES (2, 'guitar', 'Guitar', 300);
    INSERT INTO product (id, sku, name, price) VALUES (3, 'socks', 'Socks', 3.14);
    INSERT INTO product (id, sku, name, price) VALUES (4, 'skate-board', 'Skate Board', 123.45);
    INSERT INTO product (id, sku, name, price) VALUES (5, 'camera', 'Camera', 1000.00);
    INSERT INTO product (id, sku, name, price) VALUES (6, 'cookie', 'Cookie', 1.00);

    INSERT INTO buyer (id, name, birthdate) VALUES (1, 'James Bond', '2001-02-03');
    INSERT INTO buyer (id, name, birthdate) VALUES (2, '叉烧包', '1923-11-12');
    INSERT INTO buyer (id, name, birthdate) VALUES (3, 'Li Haoyi', '1965-08-09');

    INSERT INTO shipping_info (id, buyer_id, shipping_date) VALUES (1, 2, '2010-02-03');
    INSERT INTO shipping_info (id, buyer_id, shipping_date) VALUES (2, 1, '2012-04-05');
    INSERT INTO shipping_info (id, buyer_id, shipping_date) VALUES (3, 2, '2012-05-06');

    INSERT INTO purchase (id, shipping_info_id, product_id, count, total) VALUES (1, 1, 1, 10, 79.90);
    INSERT INTO purchase (id, shipping_info_id, product_id, count, total) VALUES (2, 1, 2, 2, 703.92);
    INSERT INTO purchase (id, shipping_info_id, product_id, count, total) VALUES (3, 1, 3, 7, 24.99);
    INSERT INTO purchase (id, shipping_info_id, product_id, count, total) VALUES (4, 2, 4, 2, 262.00);
    INSERT INTO purchase (id, shipping_info_id, product_id, count, total) VALUES (5, 2, 5, 15, 15000.00);
    INSERT INTO purchase (id, shipping_info_id, product_id, count, total) VALUES (6, 3, 1, 7, 55.93);
    INSERT INTO purchase (id, shipping_info_id, product_id, count, total) VALUES (7, 3, 6, 18, 18.00);"""

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