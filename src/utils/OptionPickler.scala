package usql.utils

import java.sql.Date

object OptionPickler extends upickle.AttributeTagged {
  override implicit def OptionWriter[T: Writer]: Writer[Option[T]] =
    implicitly[Writer[T]].comap[Option[T]] {
      case None => null.asInstanceOf[T]
      case Some(x) => x
    }

  override implicit def OptionReader[T: Reader]: Reader[Option[T]] = {
    new Reader.Delegate[Any, Option[T]](implicitly[Reader[T]].map(Some(_))) {
      override def visitNull(index: Int) = None
    }
  }

  override implicit val BooleanReader: Reader[Boolean] = new SimpleReader[Boolean] {
    override def expectedMsg = "expected boolean"

    override def visitTrue(index: Int) = true

    override def visitFalse(index: Int) = false

    override def visitString(s: CharSequence, index: Int) = s match {
      case "0" | "f" | "false" | "FALSE" => false
      case "1" | "t" | "true" | "TRUE" => true
    }

  }

  implicit val DateReader: Reader[Date] = new SimpleReader[Date] {
    override def expectedMsg = "expected date"

    override def visitString(s: CharSequence, index: Int) = {
      val str = s.toString
      if (str.forall(_.isDigit)) new Date(str.toLong)
      else Date.valueOf(str)
    }
  }
}
