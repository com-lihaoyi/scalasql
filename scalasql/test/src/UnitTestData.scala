package scalasql
import scalasql._

import java.time.LocalDate

case class Product[T[_]](id: T[Int], kebabCaseName: T[String], name: T[String], price: T[Double])
object Product extends Table[Product]

case class Buyer[T[_]](id: T[Int], name: T[String], dateOfBirth: T[LocalDate])
object Buyer extends Table[Buyer]

case class Invoice[T[_]](id: T[Int], total: T[Double], vendor_name: T[String])
object Invoice extends Table[Invoice] {
  override def schemaName = "otherschema"
}

case class ShippingInfo[T[_]](id: T[Int], buyerId: T[Int], shippingDate: T[LocalDate])
object ShippingInfo extends Table[ShippingInfo]

case class Purchase[T[_]](
    id: T[Int],
    shippingInfoId: T[Int],
    productId: T[Int],
    count: T[Int],
    total: T[Double]
)
object Purchase extends Table[Purchase]
