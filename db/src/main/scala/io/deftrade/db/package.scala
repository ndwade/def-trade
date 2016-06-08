package io.deftrade

import java.util.Comparator
import java.time._
import java.time.temporal._
import java.time.chrono._
import scala.language.{implicitConversions, postfixOps}

package object db {

  implicit class PipeFun[A](val a: A) extends AnyVal {
    def |> [B](f: A => B): B = f(a)
  }

  implicit class TemporalQueryOps[R](val rq: TemporalQuery[R]) extends (TemporalAccessor => R) {
    def apply(ta: TemporalAccessor): R = rq queryFrom ta
  }

  implicit class TemporalOps[R <: Temporal](val r: R) extends AnyVal {
    private implicit def roR(r: Temporal) = r.asInstanceOf[R]
    def apply(tf: TemporalField): Int = r get tf
    def +(ta: TemporalAmount): R = r plus ta
    def -(ta: TemporalAmount): R = r minus ta
    def adjustWith(ta: TemporalAdjuster): R = r `with` ta
    def adjustWith(tf: TemporalField, n: Long): R = r `with` (tf, n)
  }

  object Duration {
    def unapply(d: Duration): Option[(Long, Int)] = Some((d.getSeconds(), d.getNano()))
  }
  implicit class DurationOps(val d: Duration) extends AnyVal {
    def apply(tu: TemporalUnit): Long = d get tu
    def +(other: Duration) : Duration = d plus other
    def -(other: Duration) : Duration = d minus other
    def *(l: Long): Duration = d multipliedBy l
    def /(l: Long): Duration = d dividedBy l
    def unary_- : Duration = d.negated()
  }

  object Period {
    def unapply(p: Period): Option[(Int, Int, Int)] =
      Some((p.getYears(), p.getMonths(), p.getDays()))
  }

  implicit class PeriodOps(val p: Period) extends AnyVal {
    def apply(tu: TemporalUnit): Long = p get tu
    def +(other: Period) : Period = p plus other
    def -(other: Period) : Period = p minus other
    def * (i: Int): Period = p multipliedBy i
    def unary_- : Period = p.negated()
  }

  implicit class LongOps(val n: Long) extends AnyVal {
    import java.time.Duration._
    def nano: Duration = nanos
    def nanos: Duration = ofNanos(n)
    def milli: Duration = millis
    def millis: Duration = ofMillis(n)
    def second: Duration = seconds
    def seconds: Duration = ofSeconds(n)
    def minute: Duration = minutes
    def minutes: Duration = ofMinutes(n)
    def hour: Duration = hours
    def hours: Duration = ofHours(n)
    def * (d: Duration): Duration = d multipliedBy n
  }

  implicit class IntOps(val n: Int) extends AnyVal {
    import java.time.Period._
    def day: Period = days
    def days: Period = ofDays(n)
    def week: Period = weeks
    def weeks: Period = ofWeeks(n)
    def month: Period = months
    def months: Period = ofMonths(n)
    def year: Period = years
    def years: Period = ofYears(n)
  }

  implicit val offsetDateTimeComparator = OffsetDateTime.timeLineOrder()

  implicit def chronoLocalDateOrdering[D <: ChronoLocalDate] = Ordering.fromLessThan[D] {
    (a, b) => a isBefore b
  }

  implicit def chronoLocalDateTimeOrdering[DT <: ChronoLocalDateTime[_ <: ChronoLocalDate]] = Ordering.fromLessThan[DT] {
    (a, b) => a isBefore b
  }

  implicit def chronoZonedDateTimeOrdering[DT <: ChronoZonedDateTime[_ <: ChronoLocalDate]] = Ordering.fromLessThan[DT] {
    (a, b) => a isBefore b
  }

  object Test { // test to see if this compiles
    val now = LocalDateTime.now

    val later = now + (5 weeks)

    val time = later |> TemporalQueries.localTime()

    val minute = time(ChronoField.MINUTE_OF_HOUR)

    val tomorrow = now + 1.day

    val earlier = -(33.seconds)
    val Duration(secs, nanos) = 2.hours + 42.minutes + 1.second + 1000 * 186282.nanos

    import Ordering.Implicits._

    val b = now < later
    val maybe = later equiv later

    val ot = OffsetTime.now
    val otLater = ot + 1.minute

    val bot = ot < otLater

    val y1999 = Year of 1999
    val y2k = y1999 + 1.year
    val b2 = y1999 < y2k

  }
}
