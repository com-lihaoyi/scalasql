package scalasql

import java.sql.DriverManager
import scalasql.H2Dialect._
object Main {

  case class Example[T[_]](bytes: T[geny.Bytes])

  object Example extends Table[Example]

  // The example H2 database comes from the library `com.h2database:h2:2.2.224`
  val conn = DriverManager.getConnection("jdbc:h2:mem:mydb")

  def main(args: Array[String]): Unit = {
    conn
      .createStatement()
      .executeUpdate(
        """
      CREATE TABLE data_types (
          my_var_binary VARBINARY(256)
      );
      """
      )

    val prepared = conn.prepareStatement("INSERT INTO data_types (my_var_binary) VALUES (?)")
    prepared.setBytes(1, Array[Byte](1, 2, 3, 4))
    prepared.executeUpdate()

    val results = conn
      .createStatement()
      .executeQuery(
        "SELECT data_types0.my_var_binary AS my_var_binary FROM data_types data_types0"
      )

    results.next()
    pprint.log(results.getBytes(1))
  }
}
