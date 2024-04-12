package scalasql

import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet, SQLException}

object Main extends App {
  var connection: Connection = null
  var preparedStatement: PreparedStatement = null
  var resultSet: ResultSet = null

  try {
    // 1. Connect to the database
    connection = DriverManager.getConnection("jdbc:h2:~/test", "username", "password")

    // 2. Create the table if it doesn't exist
    val createTableSql =
      """
        |CREATE TABLE IF NOT EXISTS MY_TABLE (
        |    ID INT AUTO_INCREMENT PRIMARY KEY,
        |    COLUMN1 VARCHAR(255),
        |    COLUMN2 VARCHAR(255)
        |)
        |""".stripMargin
    preparedStatement = connection.prepareStatement(createTableSql)
    preparedStatement.execute()

    // 3. Prepare a statement with generated keys option
    val insertSql = "INSERT INTO MY_TABLE (COLUMN1, COLUMN2) VALUES (?, ?)"
    preparedStatement = connection.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS)

    // 4. Set values for placeholders
    preparedStatement.setString(1, "value1")
    preparedStatement.setString(2, "value2")

    // 5. Execute the insert statement
    val affectedRows = preparedStatement.executeUpdate()

    if (affectedRows == 0) {
      println("Insertion failed, no rows affected.")
    } else {
      // 6. Retrieve generated keys
      resultSet = preparedStatement.getGeneratedKeys()
      if (resultSet.next()) {
        println("Generated Key: " + resultSet.getLong(1))
      } else {
        println("No generated keys were retrieved.")
      }
    }
  } catch {
    case e: SQLException => e.printStackTrace()
  } finally {
    // 7. Close resources
    try {
      if (resultSet != null) resultSet.close()
      if (preparedStatement != null) preparedStatement.close()
      if (connection != null) connection.close()
    } catch {
      case e: SQLException => e.printStackTrace()
    }
  }
}