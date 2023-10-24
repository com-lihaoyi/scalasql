package scalasql
import scalasql._

import java.sql.Date
import java.time.{
  Instant,
  LocalDate,
  LocalDateTime,
  LocalTime,
  OffsetDateTime,
  OffsetTime,
  ZonedDateTime
}

case class Product[+T[_]](id: T[Int], kebabCaseName: T[String], name: T[String], price: T[Double])
object Product extends Table[Product] {
  val metadata = initMetadata()
}

case class Buyer[+T[_]](id: T[Int], name: T[String], dateOfBirth: T[LocalDate])
object Buyer extends Table[Buyer] {
  val metadata = initMetadata()
}

case class ShippingInfo[+T[_]](id: T[Int], buyerId: T[Int], shippingDate: T[LocalDate])
object ShippingInfo extends Table[ShippingInfo] {
  val metadata = initMetadata()
}

case class Purchase[+T[_]](
    id: T[Int],
    shippingInfoId: T[Int],
    productId: T[Int],
    count: T[Int],
    total: T[Double]
)
object Purchase extends Table[Purchase] {
  val metadata = initMetadata()
}

case class DataTypes[+T[_]](
    myTinyInt: T[Byte],
    mySmallInt: T[Short],
    myInt: T[Int],
    myBigInt: T[Long],
    myDouble: T[Double],
    myBoolean: T[Boolean],
    myLocalDate: T[LocalDate],
    myLocalTime: T[LocalTime],
    myLocalDateTime: T[LocalDateTime],
//    myZonedDateTime: T[ZonedDateTime],
//  myInstant: T[Instant],
//  myOffsetTime: T[OffsetTime],
  myOffsetDateTime: T[OffsetDateTime],
)

object DataTypes extends Table[DataTypes] {
  val metadata = initMetadata()
}
