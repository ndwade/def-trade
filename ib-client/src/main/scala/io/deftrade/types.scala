/*
 * Copyright 2014-2016 Panavista Technologies, LLC
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
///*
// * Copyright 2014-2016 Panavista Technologies, LLC
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package io.deftrade
//
//import scala.language.{ implicitConversions => ics }
//
///**
// * These traits allow clients to choose basic types within the domain (such as Money or Currency)
// * while retaining compatibility with the IB wire protocol (which is, ultimately, the only
// * compatibility that matters.)
// *
// * These basic types are propagated through the DTOs, Message classes, and the rest of the client
// * code, while allowing clients maximum flexibility in specifying low level types,
// * their serialization and error handling.
// *
// * Existing conventions mapping IB wire protocol to Java in the IB API:
// * - reading from socket: String fields have the property that they signal zero length input
// * by returning null. Incidentally, this guarantees that non-null strings are non-empty
// * - writing to the socket: each field is tested for null. Nulls are serialized as a zero
// * length string.
// *
// * Given that the wire protocol makes null values meaningless (no way to distinguish
// * `null` from `""` on the wire). Nulls can be replaced with empty strings. (Once transmitted
// * over the wire, the server has no way to distinguish a `null` field from an (intentionally)
// * empty string. Similarly, the client cannot distinguish whether the server "intended" to send
// * an empty string as a value or "value not preset". So nothing is lost by using an empty string
// * (which reflects the wire protocol itself) for StringType values.
// *
// * If you can't live without `null`s in your code and desire to reproduce
// * the _exact_ behavior of the Java library with respect to `Strings` and `null`s, that can be
// * done with a custom StringTypeClass (exercise left for the reader).
// *
// * Also, rather than use `null`s, StringType can be something like Option[String], where None
// * indicates an empty string.
// *
// * There are other possibilities for StringType - consider simply using ByteStrings as-is. Or,
// * consider the Scalaz.Cord class.
// *
// * Wire conventions for numeric types such as Double.
// *
// * _Most_ fields are assumed to be non-empty. Fields which are typed as Java primitives
// * (e.g. int and double) cannot be null and are serialized as is.
// * On de-serialization, empty strings are converted to the zero value for the type. Client cannot
// * distinguish between an actual zero value representation on the wire and a "default" zero value
// * arising from de-serializing an empty string.
// *
// * _Some_ numeric fields are treated specially: there is a distinguished value for the type which
// * signals an "empty" value. On serialization, this empty value is converted to a zero length
// * (empty) string, and on de-serialization a zero length string is converted to this
// * distinguished "empty" value. The convention taken by the IB API is to use the max value for
// * the datatype (Integer.MAX_VALUE and Double.MAX_VALUE) as this distinguished "empty" value.
// * These fields now will use Option[Int] or Option[Double] rather than rely on the distinguished
// * `max` value.
// *
// * Throwing Exceptions
// *
// * - With respect to fields which are primitive numerics in the IB Java API - failure to parse these
// * currently kills the connection. Seems like a good idea to stick with this.
// *
// * - Could, if desired, meaningfully use Option[] for some of these (e.g. a client can
// * then distinguish between a zero value sent by the server, and an empty value (not present).
// * Due to existing API practice, the server likely does not expect an actual zero length string
// * in any slot where a numeric value should appear.
// *
// * - With respect to fields which are Strings in the IB Java API and are parsed into more refined
// * types (like `Enumeration`s or perhaps some kind of Date), then "Keep Calm and Carry On" may be
// * a better strategy, choosing e.g. Either[Throwable, A] for those types, or perhaps Try.
// *
// *
// * Notes from Spiewak talk
// * - use nested objects within traits in leui of namespaces (packages)
// * - refine abstract types with bounds, use self types
// * 	- interesting variation when need to use typeclasses instead of base traits
// *  to unify the polymorphic types implementing an abstract type
// * - prefer extends to self-typing in outer cakes
// * - lifecycle! abstract override / startup, shutdown - no dependencies! treat as order independent
// * - question: do I really need to abstract the DTO case classes? I think not.
// * - module nesting: composition. Motivation: lifecycle independence, has-a vs is-a
// * 	- when a module M has a dependency D and D is _larger_ in scope (space or time) than M => D.M
// *
// *
// */
//
//trait DomainTypesComponent {
//
//  /**
//   * Alternatives to Double: BigDecimal, or a custom class...
//   */
//  type MoneyType
//  val MoneyType: MoneyTypeClass[MoneyType]
//
//  /**
//   * Alternative to String: Enumeration (provided in library for the 17 currently supported
//   * currencies).
//   *
//   * Motivation: while an enum would be safest, it would necessitate a lib update and recompile
//   * if new currencies are added to IB platform.
//   */
//  type CurrencyType
//  val CurrencyType: CurrencyTypeClass[CurrencyType]
//
//}
//
//private[deftrade] trait DomainTypeClass[T] {
//
//  /**
//   * Empty value for serialization and deserialization. E.g. for String it would be "".
//   */
//  val empty: T
//  type ImplicitConversions
//  private[deftrade] val implicitConversions: ImplicitConversions
//
//}
//
//trait StringTypeClass extends DomainTypeClass[String] {
//  val empty: String = ""
//}
//
//trait MoneyTypeClass[MT] extends DomainTypeClass[MT] {
//  final lazy val empty: MT = fromDouble(0.0)
//  final lazy val zero: MT = empty
//  def fromDouble(d: Double): MT
//  val ordering: Ordering[MT]
//  private[deftrade] val implicitConversions: ImplicitConversions
//  trait ImplicitConversions {
//    implicit def mt2s(m: MT): String
//    implicit def s2mt(s: String): MT
//    implicit def s2omt(s: String): Option[MT] = if (s.isEmpty) None else Some(s)
//    implicit def omt2s(oi: Option[MT]): String = (oi map mt2s) getOrElse ""
//  }
//}
//
//trait CurrencyTypeClass[CT] extends DomainTypeClass[CT] {
//  val USD: CT
//  private[deftrade] val implicitConversions: ImplicitConversions
//  trait ImplicitConversions {
//    implicit def ct2s(c: CT): String
//    implicit def s2ct(s: String): CT
//  }
//}
//
//object DefaultTypeClasses {
//  import optional._
//  import ImplicitConversions._
//
//  implicit object MoneyDoubleClass extends MoneyTypeClass[Double] {
//    override def fromDouble(d: Double) = identity(d)
//    val ordering = implicitly[Ordering[Double]]
//    private[deftrade] val implicitConversions = new ImplicitConversions {
//      override implicit def mt2s(m: Double): String = d2s(m)
//      override implicit def s2mt(s: String): Double = s2d(s)
//    }
//  }
//
//  implicit object MoneyBigDecimalClass extends MoneyTypeClass[BigDecimal] {
//    override def fromDouble(d: Double) = BigDecimal(d)
//    val ordering = implicitly[Ordering[BigDecimal]]
//    private[deftrade] val implicitConversions = new ImplicitConversions {
//      override implicit def mt2s(m: BigDecimal): String = m.toString
//      override implicit def s2mt(s: String): BigDecimal = BigDecimal(s)
//    }
//  }
//
//  implicit object CurrencyEnumClass extends CurrencyTypeClass[Currency.Currency] {
//    import Currency._
//    val empty = Currency.Undefined
//    val USD = Currency.USD
//    private[deftrade] val implicitConversions = new ImplicitConversions {
//      override implicit def ct2s(c: Currency): String = c.toString
//      override implicit def s2ct(s: String): Currency = Currency.withName(s)
//    }
//  }
//
//  implicit object CurrencyStringClass extends StringTypeClass with CurrencyTypeClass[String] {
//    val USD = "USD"
//    private[deftrade] val implicitConversions = new ImplicitConversions {
//      override implicit def ct2s(c: String): String = c
//      override implicit def s2ct(s: String): String = s
//    }
//  }
//
//  def MoneyTypeClass[T](implicit ev: MoneyTypeClass[T]): MoneyTypeClass[T] = ev
//  def CurrencyTypeClass[T](implicit ev: CurrencyTypeClass[T]): CurrencyTypeClass[T] = ev
//}
//
///**
// * Implementation of DomainTypesComponent which provides types identical to those used in the
// * Interactive Brokers java API.
// */
//trait IbDomainTypesComponent extends DomainTypesComponent {
//
//  import DefaultTypeClasses._
//
//  type MoneyType = Double
//  val MoneyType = MoneyTypeClass[MoneyType]
//
//  type CurrencyType = String
//  val CurrencyType = CurrencyTypeClass[CurrencyType]
//
//}
//
//object Xtras {
//  
//    final class Money(val amount: BigDecimal) extends AnyVal {
//  
//    import Money._
//    
//    override def toString: String = if (amount < 0) amount formatted neg else amount formatted pos
//    def formatted(format: String ): String = amount formatted format
//
//    def + (that: Money): Money = Money(amount + that.amount)
//    def - (that: Money): Money = Money(amount - that.amount)
//
//    def * (that: Double): Money = Money(amount * that)
//    def / (that: Money): Double = (amount / that.amount).toDouble
//    
//    def unary_- : Money = Money(-amount)
//  }
//  
//  object Money {
//  
//    val zero = Money(0)
//    val one = Money(1)
//    
//    private val tot = 16
//    private val frac = 6
//    private val neg = s" %(,$tot.${frac}f"
//    private val pos = s"%,$tot.${frac}f "
//    
//    def apply(amount: BigDecimal): Money = new Money(amount)
//
//    implicit val ordering = Ordering by { (m: Money) => m.amount }
//
//    object Implicits {
//      implicit class FromInt(val i: Int) extends AnyVal { def money: Money = Money(i) }
//      implicit class FromBigDecimal(val i: BigDecimal) extends AnyVal { def money: Money = Money(i) }
//      implicit class FromDouble(val i: Double) extends AnyVal { def money: Money = Money(i) }
//      implicit class FromLong(val i: Long) extends AnyVal { def money: Money = Money(i) }
//    }
//    
//  }
//
//}