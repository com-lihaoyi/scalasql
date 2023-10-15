package usql.customers
import usql._

case class Product[T[_]](id: T[Int],
                         sku: T[String],
                         name: T[String],
                         price: T[Double])
object Product extends Table[Product]{
  val metadata = initMetadata()
}

case class Customer[T[_]](id: T[Int],
                          name: T[String],
                          birthdate: T[String])
object Customer extends Table[Customer]{
  val metadata = initMetadata()
}


case class PurchaseOrder[T[_]](id: T[Int],
                               customerId: T[Int],
                               orderDate: T[String])
object PurchaseOrder extends Table[PurchaseOrder]{
  val metadata = initMetadata()
}


case class Item[T[_]](id: T[Int],
                      orderId: T[Int],
                      productId: T[Int],
                      quantity: T[Int],
                      total: T[Double])
object Item extends Table[Item]{
  val metadata = initMetadata()
}