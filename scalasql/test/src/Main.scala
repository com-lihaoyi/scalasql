package scalasql


object Main {

  def main(args: Array[String]): Unit = {
//    // org.xerial:sqlite-jdbc:3.43.0.0
//    val conn = java.sql.DriverManager
//      .getConnection(
//        s"${TestClients.postgres.getJdbcUrl}&user=${TestClients.postgres.getUsername}&password=${TestClients.postgres.getPassword}"
//      )
//    val statement = conn.createStatement()
//    statement.executeUpdate(
//      "CREATE TABLE thing ( id SERIAL PRIMARY KEY, date DATE);" +
//        "INSERT INTO thing (date) VALUES ('2012-04-05')"
//    )
//    statement.close()
//
//    val prepped = conn.prepareStatement("SELECT * from thing WHERE date = ?")
////    prepped.setDate(1, java.sql.LocalDate.parse("2012-04-05"))
//    prepped.setString(1, "2012-04-05")
//
//    val result = prepped.executeQuery()
//    println(result.next())
  }
}
