/*
 * Copyright 2014 Panavista Technologies, LLC
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

import scala.language.postfixOps
import org.scalatest.{ FlatSpec, Assertions }
import org.scalactic.Equality
import com.ib.client.IApiEnum
import com.ib.controller.{ Types, OrderType => IbOrderType, NewTickType }
import com.ib.controller.OrderStatus
//import com.ib.controller.NewComboLeg

/**
 *  Checks conformance of IB enum spec with this implementation
 */
class EnumSpec extends FlatSpec with Assertions {

  /**
   * For comparing Scala emumns to java enums. Casts are horrible but there are enough
   * tight dependent type checks to catch errors at compile time.
   */
  implicit class SEnum[E <: Enumeration](val e: E) {

    /** Only used within [[JEnum]].conformsTo, where Values obtained directly from an Enumeration */
    def stringFrom(v: E#Value) = v.toString()

  }

  /** Implicit class holding the Java enum and mappings. */
  implicit class JEnum[E <: Enum[E]](es: Array[E]) { self =>

    import JEnum._

    protected def patch: Patch = identity

    protected def stringFrom(e: E) = e.toString()

    protected class SubClass extends JEnum[E](es) {
      override def patch = self.patch
      override def stringFrom(e: E) = self stringFrom e
    }

    def withPatch(p: Patch): JEnum[E] = new SubClass {
      override def patch = p
    }

    def withStrings(fn: E => String): JEnum[E] = new SubClass {
      override def stringFrom(e: E) = fn(e)
    }

    def withUndefinedAs(undefined: String) = withStrings { e =>
      e.toString() match {
        case `undefined` => ""
        case s => s
      }
    }

    def withApiStrings = withStrings { e => e.asInstanceOf[IApiEnum].getApiString() }

    /**
     *  What "conforms to" means:
     *  - same set of ordinals, and
     *  - strings associated with each ordinal match per the given string mappings.
     */
    final def conformsTo[SE <: Enumeration](senum: SEnum[SE]): Unit = {
      import senum.{ e => se }
      val m = patch((es map { e => (e.ordinal() -> stringFrom(e)) }).toMap)
      val seIds = se.values map { _.id }
      val jeIds = m.keySet
      if (jeIds !== seIds) fail {
        s"""|ids present in java but not scala: ${jeIds &~ seIds} 
            |ids present in scala but not java: ${seIds &~ jeIds}
            |""".stripMargin
      }
      m foreach { case (o, s) => assert(s === senum.stringFrom(se(o)), se.toString) }
    }
  }

  object JEnum {
    type Patch = Map[Int, String] => Map[Int, String]
    val prependUndefined: Patch = { m =>
      (m map { case (o, s) => (o + 1, s) }) updated (0, "")
    }
  }

  import JEnum.prependUndefined

  "Enumerations" should "have same ordinal and API serialization" in {

    (Types.Right.values withApiStrings) conformsTo Right

    (Types.Action.values withPatch prependUndefined) conformsTo Action

    (Types.SecType.values withApiStrings) conformsTo SecType

    (Types.ExerciseType.values withUndefinedAs "None") conformsTo ExerciseType // ordinal

    (Types.SecIdType.values withApiStrings) conformsTo SecIdType

    // checked once; OpenClose is not accessible from outside NewComboLeg. TODO: request patch
    // NewComboLeg.OpenClose.values conformsTo OpenClose

    (Types.TimeInForce.values withPatch prependUndefined) conformsTo TimeInForce

    (Types.OcaType.values withUndefinedAs "None") conformsTo OcaType

    Types.TriggerMethod.values withPatch {
      _ map {
        case (o, s) => // "ordinal" is the api string
          (Types.TriggerMethod.values()(o).getApiString.toInt, s)
      }
    }

    (Types.Rule80A.values withApiStrings) conformsTo Rule80A

    (Types.VolatilityType.values withUndefinedAs "None") conformsTo VolType

    (Types.ReferencePriceType.values withUndefinedAs "None") conformsTo ReferencePriceType

    (Types.HedgeType.values withApiStrings) conformsTo HedgeType

    // these are currently not implemented in com.ib.controller because
    // controller.NewOrder doesn't take institutional params.
    // ShortSaleSlot
    // OrderInstOpenClose 
    // Origin
    // AuctionStrategy

    (Types.Method.values withApiStrings) conformsTo AllocationMethod

    (Types.AlgoStrategy.values withApiStrings) conformsTo AlgoStrategy

    Types.AlgoParam.values conformsTo AlgoParam

    // check that we got the same set of params
    for (as <- Types.AlgoStrategy.values) {
      val strategy = as.getApiString()
      val jps = as.params()
      val sps = AlgoStrategy.paramsFor(AlgoStrategy.withName(strategy))
      assert(jps.length === sps.size)
      (jps zip sps) foreach { case (j, s) => assert(j.toString === s.toString) }
    }

    (Types.MktDataType.values withUndefinedAs "Unknown") conformsTo MktDataType

    (OrderStatus.values withPatch prependUndefined) conformsTo OrderStatusEnum

    assert(OrderStatusEnum.Cancelled nonActive) // just checking this works
    OrderStatus.values foreach { os =>
      val ose = OrderStatusEnum.withName(os.toString)
      assert(os.isActive() === ose.isActive)
    }

    Types.DeepType.values conformsTo DeepType

    Types.DeepSide.values conformsTo DeepSide

    (Types.NewsType.values withUndefinedAs "UNKNOWN") conformsTo NewsType

    (IbOrderType.values withApiStrings) withPatch { m =>
      m updated (m.size, "None") // observed value for deltaNeutral
    } conformsTo OrderType

    NewTickType.values withPatch {
      _ + (-1 -> "INVALID", 61 -> "REGULATORY_IMBALANCE")
    } conformsTo TickType

    // no com.ib.controller homolog for GenericTickType enum

    (Types.FundamentalType.values withStrings (_.getApiString())) conformsTo FundamentalType

  }
}