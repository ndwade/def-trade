/*
 * Copyright 2014-2016 Panavista Technologies LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.deftrade

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
    def +/- (d: Duration): (R, R) = (r minus d, r plus d)
    def +/  (d: Duration): (R, R) = (r        , r plus d)
    def  /- (d: Duration): (R, R) = (r minus d, r       )
    def within(interval: (R, R))(implicit o: Ordering[R]): Boolean = interval match {
      case(t1, t2) => o.lteq(t1, r) && o.lt(r, t2)
    }
    def adjustWith(ta: TemporalAdjuster): R = r `with` ta
    def adjustWith(tf: TemporalField, n: Long): R = r `with` (tf, n)
  }

  object ZonedDateTimeEx {
    def unapply(zdt: ZonedDateTime): Option[(LocalDateTime, ZoneId)] =
      Some((zdt.toLocalDateTime(), zdt.getZone()))
  }

  object OffsetDateTimeEx {
    def unapply(odt: OffsetDateTime): Option[(LocalDateTime, ZoneOffset)] =
      Some((odt.toLocalDateTime(), odt.getOffset()))
  }

  object LocalDateTimeEx {
    def unapply(ldt: LocalDateTime): Option[(LocalDate, LocalTime)] =
      Some((ldt.toLocalDate(), ldt.toLocalTime()))
  }

  object LocalDateEx {
    def unapply(ld: LocalDate): Option[(Int, Month, Int)] =
      Some((ld.getYear(), ld.getMonth(), ld.getDayOfMonth()))
  }

  object LocalTimeEx {
    def unapply(lt: LocalTime): Option[(Int, Int, Int, Int)] =
      Some((lt.getHour, lt.getMinute, lt.getSecond, lt.getNano))
  }

  implicit class DurationOps(val d: Duration) extends AnyVal {
    def apply(tu: TemporalUnit): Long = d get tu
    def +(other: Duration) : Duration = d plus other
    def -(other: Duration) : Duration = d minus other
    def *(l: Long): Duration = d multipliedBy l
    def /(l: Long): Duration = d dividedBy l
    def unary_- : Duration = d.negated()
  }

  implicit class LongToDuration(val n: Long) extends AnyVal {
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

  object DurationEx {
    def unapply(d: Duration): Option[(Long, Int)] = Some((d.getSeconds(), d.getNano()))
  }

  implicit class PeriodOps(val p: Period) extends AnyVal {
    def apply(tu: TemporalUnit): Long = p get tu
    def +(other: Period) : Period = p plus other
    def -(other: Period) : Period = p minus other
    def * (i: Int): Period = p multipliedBy i
    def unary_- : Period = p.negated()
  }

  implicit class IntToPeriod(val n: Int) extends AnyVal {
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
  object PeriodEx {
    def unapply(p: Period): Option[(Int, Int, Int)] =
      Some((p.getYears(), p.getMonths(), p.getDays()))
  }

  implicit def chronoLocalDateOrdering[D <: ChronoLocalDate] = Ordering.fromLessThan[D] {
    (a, b) => a isBefore b
  }
  implicit def chronoLocalDateTimeOrdering[DT <: ChronoLocalDateTime[_ <: ChronoLocalDate]] = Ordering.fromLessThan[DT] {
    (a, b) => a isBefore b
  }
  implicit def chronoZonedDateTimeOrdering[DT <: ChronoZonedDateTime[_ <: ChronoLocalDate]] = Ordering.fromLessThan[DT] {
    (a, b) => a isBefore b
  }
  implicit val offsetDateTimeOrdering = Ordering.fromLessThan[OffsetDateTime] {
    (a, b) => a isBefore b
  }

  object Test { // test to see if this compiles

    val now = LocalDateTime.now

    val LocalDateTimeEx(
      LocalDateEx(year, month, day),
      LocalTimeEx(hour, minute, second, nano)
    ) = now

    val later = now + (5 weeks)

    val time = later |> TemporalQueries.localTime()

    val minutes = time(ChronoField.MINUTE_OF_HOUR)

    val tomorrow = now + 1.day

    val jiffy = 3.seconds
    val neggy = -jiffy
    val DurationEx(secs, nanos) = 2.hours + 42.minutes + 1.second - 1000 * 186282.nanos

    import Ordering.Implicits.infixOrderingOps

    val b = now < later
    val maybe = later equiv later

    val ot = OffsetTime.now
    val otLater = ot + 1.minute

    val bot = ot < otLater

    val y1999 = Year of 1999
    val y2k = y1999 + 1.year
    val b2 = y1999 < y2k

    val nowIsh = now +/- 5.minutes
    val ok = (now + 1.nano) within nowIsh

  }
}
