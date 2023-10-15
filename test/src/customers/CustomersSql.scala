package usql.buyers
import usql._

case class Product[T[_]](id: T[Int],
                         sku: T[String],
                         name: T[String],
                         price: T[Double])
object Product extends Table[Product]{
  val metadata = initMetadata()
}

case class Buyer[T[_]](id: T[Int],
                          name: T[String],
                          birthdate: T[String])
object Buyer extends Table[Buyer]{
  val metadata = initMetadata()
}


case class ShippingInfo[T[_]](id: T[Int],
                               buyerId: T[Int],
                               shippingDate: T[String])
object ShippingInfo extends Table[ShippingInfo]{
  val metadata = initMetadata()
}


case class Purchase[T[_]](id: T[Int],
                      shippingInfoId: T[Int],
                      productId: T[Int],
                      count: T[Int],
                      total: T[Double])
object Purchase extends Table[Purchase]{
  val metadata = initMetadata()
}