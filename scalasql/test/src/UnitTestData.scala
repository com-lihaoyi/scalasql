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
  initTableMetadata()
}

case class Buyer[+T[_]](id: T[Int], name: T[String], dateOfBirth: T[LocalDate])
object Buyer extends Table[Buyer] {
  initTableMetadata()
}

case class ShippingInfo[+T[_]](id: T[Int], buyerId: T[Int], shippingDate: T[LocalDate])
object ShippingInfo extends Table[ShippingInfo] {
  initTableMetadata()
}

case class Purchase[+T[_]](
    id: T[Int],
    shippingInfoId: T[Int],
    productId: T[Int],
    count: T[Int],
    total: T[Double]
)
object Purchase extends Table[Purchase] {
  initTableMetadata()
}
