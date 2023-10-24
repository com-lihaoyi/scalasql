package scalasql.utils

import java.sql.Date
import java.time.{
  Instant,
  LocalDate,
  LocalDateTime,
  LocalTime,
  OffsetDateTime,
  OffsetTime,
  ZoneId,
  ZonedDateTime
}

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

  implicit val LocalDateReader: Reader[LocalDate] = new SimpleReader[LocalDate] {
    override def expectedMsg = "expected local date"
  }
  implicit val LocalTimeReader: Reader[LocalTime] = new SimpleReader[LocalTime] {
    override def expectedMsg = "expected local time"
  }
  implicit val LocalDateTimeReader: Reader[LocalDateTime] = new SimpleReader[LocalDateTime] {
    override def expectedMsg = "expected local date time"
  }

  implicit val ZonedDateTimeReader: Reader[ZonedDateTime] = new SimpleReader[ZonedDateTime] {
    override def expectedMsg = "expected zoned time"
  }

  implicit val InstantReader: Reader[Instant] = new SimpleReader[Instant] {
    override def expectedMsg = "expected instant"
  }

  implicit val OffsetTimeReader: Reader[OffsetTime] = new SimpleReader[OffsetTime] {
    override def expectedMsg = "expected offset time"
  }

  implicit val OffsetDateTimeReader: Reader[OffsetDateTime] = new SimpleReader[OffsetDateTime] {
    override def expectedMsg = "expected offset datetime"
  }

}
