package usql
import usql._

import java.sql.Date

case class Product[+T[_]](id: T[Int], kebabCaseName: T[String], name: T[String], price: T[Double])
object Product extends Table[Product] {
  val metadata = initMetadata()
}

case class Buyer[+T[_]](id: T[Int], name: T[String], dateOfBirth: T[Date])
object Buyer extends Table[Buyer] {
  val metadata = initMetadata()
}

case class ShippingInfo[+T[_]](id: T[Int], buyerId: T[Int], shippingDate: T[Date])
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
