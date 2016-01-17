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
package io.deftrade

import scala.language.{ postfixOps, implicitConversions }
import scala.util.control.NonFatal

import ImplicitConversions._

/**
 * An Enumeration mixin which enables construction of new values from previously undefined names.
 * Intended to allow more graceful error handling and reporting.
 * Has the side effect of altering (and possibly corrupting) the values and maxId collections.
 * Client code should be proactive in dealing with ERROR values and shutdown/restart at the
 * earliest opportunity.
 *
 * ISSUE: THREAD SAFETY: the only thread which will create new (error) enum values is the Reader
 * thread which is managed by the IbConnection FSM actor. Anything state updates associated with the
 * new (erroneous) enums *happens before* the sending of a message containing these new enums,
 * which in turn *happens before* that message is received. (Akka guarantees this).
 * So the instances should be stable by the time any client coode reads them.
 * If other threads create erroneous enums, *all bets are off*.
 * How could this happen? Multiple IbConnection instantiations would be a problem. Restricted to one
 * IB connection per classloader. TODO: revisit this limitation; advertise prominently!
 */

/*
 * When mixed in to Enumeration, creates an Undefined value with id == 0.
 */
private[deftrade] trait HasUndefined { this: Enumeration =>
  val Undefined: Value = Value(0, "")
}

/**
 *  Dynamically adds (unexpected) new values to the enumeration
 *  when deserialized from IB API connection.
 */
private[deftrade] trait ResiliantRx { _: Enumeration =>
  // throws if unexpected value is encountered
  protected def deserialize(s: String): Value
  // handle unexpected value
  protected def unexpected(s: String): Value
  private[deftrade] final implicit def s2v(s: String): Value =
    try { deserialize(s) } catch { case NonFatal(_) => synchronized { unexpected(s) } }
}
private[deftrade] object ResiliantRx {
  val PREFIX = "UNEXPECTED_ENUM_VALUE: "
}

private[deftrade] trait RxOrdinal extends ResiliantRx { _: Enumeration =>
  override final protected def deserialize(s: String) = apply(s2i(s))
  override final protected def unexpected(s: String): Value = {
    val id = s2i(s) // if it throws during int parse... so be it. Big problem, kill the thread
    Value(id, s"${ResiliantRx.PREFIX}${id}")
  }
}

private[deftrade] trait TxApi { _: Enumeration =>
  private[deftrade] implicit def v2s(v: Value): String
}

private[deftrade] trait TxOrdinal extends TxApi { _: Enumeration =>
  private[deftrade] override final implicit def v2s(v: Value): String = i2s(v.id)
}

private[deftrade] trait RxTxOrdinal extends RxOrdinal with TxOrdinal { _: Enumeration => }

private[deftrade] trait RxString extends ResiliantRx { _: Enumeration =>
  import RxString._
  override final protected def deserialize(s: String) =
    if (isEmptyStringOrAlias(s)) withName("") // treat '?' or "0" as empty string -> IB sends this, dunno why
    else withName(s)
  // id is negative for errors. OK because id is a map key, not an array index
  private[this] var errId = 0 // only for use within unexpected() method.
  override final protected def unexpected(s: String): Value = {
    val errName = s"${ResiliantRx.PREFIX}${s}"
    try { withName(errName) } // did we encounter this error before?
    catch {
      case (_: NoSuchElementException) =>
        errId -= 1
        Value(errId, errName)
    }
  }
}
private[deftrade] object RxString {
  /**
   * Values observed to be equivalent based on messages received from the IB server.
   */
  def isEmptyStringOrAlias(s: String): Boolean = s == "" || s == "?" || s == "0"
}

private[deftrade] trait TxString extends TxApi { _: Enumeration =>
  private[deftrade] override final implicit def v2s(v: Value): String = v.toString
}

private[deftrade] trait RxTxString extends RxString with TxString { _: Enumeration => }

/*
 * Enum objects
 */
object Action extends Enumeration with HasUndefined with RxTxString {
  type Action = Value
  val BUY, SELL, SSHORT = Value
  // val SSHORTX = Value // commented out - not in use, 9.65 release notes notwithstanding
}

/*
 * http://www.interactivebrokers.com.hk/php/webhelp/Making_Trades/baskets/manualBasket.htm
 */
object SecType extends Enumeration with HasUndefined with RxTxString {
  type SecType = Value
  val STK, OPT, FUT, CASH, BOND, CFD, FOP, WAR, IOPT, FWD, BAG, IND, BILL, FUND, FIXED, SLB, NEWS, CMDTY, BSK, ICU, ICS = Value

}

object Right extends Enumeration with HasUndefined with RxTxString {
  type Right = Value
  val Put = Value("P")
  val Call = Value("C")
}

object ExerciseType extends Enumeration with HasUndefined with TxOrdinal {
  type ExerciseType = Value
  val Exercise = Value(1)
  val Lapse = Value(2)
}

object SecIdType extends Enumeration with HasUndefined with TxString {
  type SecIdType = Value
  val CUSIP, SEDOL, ISIN, RIC = Value
}

object OpenClose extends Enumeration with RxTxOrdinal {
  type OpenClose = Value
  val Same = Value(0)
  val Open = Value(1)
  val Close = Value(2)
  val Unknown = Value(3)
}

/** Applies to institutional orders only */
object ShortSaleSlot extends Enumeration with RxTxOrdinal {
  type ShortSaleSlot = Value
  val NA, ClearingBroker, ThirdParty = Value
}

/**
 *  Type of order to enter.
 *  Values here are from com.ib.controller.OrderType.
 *  TODO: revisit the idea of making this a UDT (some may simply want to use String).
 */
object OrderType extends Enumeration with HasUndefined with RxTxString {
  type OrderType = Value
  val MKT = Value("MKT")
  val LMT = Value("LMT")
  val STP = Value("STP")
  val STP_LMT = Value("STP LMT")
  val REL = Value("REL")
  val TRAIL = Value("TRAIL")
  val BOX_TOP = Value("BOX TOP")
  val FIX_PEGGED = Value("FIX PEGGED")
  val LIT = Value("LIT")
  val LMT_PLUS_MKT = Value("LMT + MKT")
  val LOC = Value("LOC")
  val MIT = Value("MIT")
  val MKT_PRT = Value("MKT PRT")
  val MOC = Value("MOC")
  val MTL = Value("MTL")
  val PASSV_REL = Value("PASSV REL")
  val PEG_BENCH = Value("PEG BENCH")
  val PEG_MID = Value("PEG MID")
  val PEG_MKT = Value("PEG MKT")
  val PEG_PRIM = Value("PEG PRIM")
  val PEG_STK = Value("PEG STK")
  val REL_PLUS_LMT = Value("REL + LMT")
  val REL_PLUS_MKT = Value("REL + MKT")
  val STP_PRT = Value("STP PRT")
  val TRAIL_LIMIT = Value("TRAIL LIMIT")
  val TRAIL_LIT = Value("TRAIL LIT")
  val TRAIL_LMT_PLUS_MKT = Value("TRAIL LMT + MKT")
  val TRAIL_MIT = Value("TRAIL MIT")
  val TRAIL_REL_PLUS_MKT = Value("TRAIL REL + MKT")
  val VOL = Value("VOL")
  val VWAP = Value("VWAP")
  val NONE = Value("None") // observed for Order.deltaNeutralOrderType
}

object TimeInForce extends Enumeration with HasUndefined with RxTxString {
  type TimeInForce = Value
  val DAY, GTC, OPG, IOC, GTD, GTT, AUC, FOK, GTX, DTC = Value
}

object OcaType extends Enumeration with HasUndefined with RxTxOrdinal {
  type OcaType = Value
  val CancelWithBlocking, ReduceWithBlocking, ReduceWithoutBlocking = Value
}

object TriggerMethod extends Enumeration with RxTxOrdinal {
  type TriggerMethod = Value
  val Default = Value(0)
  val DoubleBidAsk = Value(1)
  val Last = Value(2)
  val DoubleLast = Value(3)
  val BidAsk = Value(4)
  val LastOrBidAsk = Value(7)
  val Midpoint = Value(8)
}
object Rule80A extends Enumeration with HasUndefined with RxTxString {
  type Rule80A = Value
  val IndivArb = Value("J")
  val IndivBigNonArb = Value("K")
  val IndivSmallNonArb = Value("I")
  val InstArb = Value("U")
  val InstBigNonArb = Value("Y")
  val InstSmallNonArb = Value("A")
}

object OrderInstOpenClose extends Enumeration with HasUndefined with RxTxString {
  type OrderInstOpenClose = Value
  val OPEN = Value("O")
  val CLOSE = Value("C")
}

object Origin extends Enumeration with RxTxOrdinal {
  type Origin = Value
  val Customer, Firm = Value
}

object AuctionStrategy extends Enumeration with HasUndefined with RxTxOrdinal {
  type AuctionStrategy = Value
  val AuctionMatch, AuctionImprovement, AuctionTransparent = Value
}

object VolType extends Enumeration with HasUndefined with RxTxOrdinal {
  type VolType = Value
  val Daily = Value(1)
  val Annual = Value(2)
}

object ReferencePriceType extends Enumeration with HasUndefined with RxTxOrdinal {
  type ReferencePriceType = Value
  val Midpoint, BidOrAsk = Value
}

object HedgeType extends Enumeration with HasUndefined with RxTxString {
  type HedgeType = Value
  val Delta = Value("D")
  val Beta = Value("B")
  val Fx = Value("F")
  val Pair = Value("P")
}

object ClearingIntent extends Enumeration with HasUndefined with RxTxString {
  type ClearingIntent = Value
  val IB, Away, PTA = Value
}

object AllocationMethod extends Enumeration with HasUndefined with RxTxString {
  type AllocationMethod = Value
  val EqualQuantity, AvailableEquity, NetLiq, PctChange = Value
}

object AlgoParam extends Enumeration with RxTxString {
  type AlgoParam = Value
  val startTime, endTime, allowPastEndTime, maxPctVol, pctVol, strategyType = Value
  val noTakeLiq, riskAversion, forceCompletion, displaySize, getDone, noTradeAhead = Value
  val useOddLots, componentSize, timeBetweenOrders, randomizeTime20 = Value
  val randomizeSize55, giveUp, catchUp, waitForFill = Value
}

object AlgoStrategy extends Enumeration with HasUndefined with RxTxString {
  type AlgoStrategy = Value
  val Vwap, Twap, ArrivalPx, DarkIce, PctVol, AD = Value

  import AlgoParam.{ ValueSet => Params, _ }
  lazy val paramsFor = Map(
    Undefined -> Params(),
    Vwap -> Params(startTime, endTime, maxPctVol, noTakeLiq, getDone, noTradeAhead, useOddLots),
    Twap -> Params(startTime, endTime, allowPastEndTime, strategyType),
    ArrivalPx -> Params(startTime, endTime, allowPastEndTime, maxPctVol, riskAversion, forceCompletion),
    DarkIce -> Params(startTime, endTime, allowPastEndTime, displaySize),
    PctVol -> Params(startTime, endTime, pctVol, noTakeLiq),
    AD -> Params(startTime, endTime, componentSize, timeBetweenOrders, randomizeTime20, randomizeSize55, giveUp, catchUp, waitForFill))
}

object MktDataType extends Enumeration with HasUndefined with RxTxOrdinal {
  type MktDataType = Value
  val Realtime, Frozen = Value
}

object OrderStatusEnum extends Enumeration with HasUndefined with RxString {
  type OrderStatusEnum = Value
  val ApiPending, ApiCancelled, PreSubmitted, PendingCancel, Cancelled = Value
  val Submitted, Filled, Inactive, PendingSubmit = Value
  val Unknown = Value // not sure why IB has this but retaining for now.
  implicit class MaybeActive(status: OrderStatusEnum) {
    def isActive: Boolean = status match {
      case PreSubmitted | PendingCancel | Submitted | PendingSubmit => true
      case _ => false
    }
    def nonActive = !isActive
  }
}

object DeepType extends Enumeration with RxOrdinal {
  type DeepType = Value
  val INSERT = Value(0)
  val UPDATE = Value(1)
  val DELETE = Value(2)
}
object DeepSide extends Enumeration with RxOrdinal {
  type DeepSide = Value
  val SELL = Value(0)
  val BUY = Value(1)
}

object ExecAction extends Enumeration with HasUndefined with RxString {
  type ExecAction = Value
  val BOT, SLD = Value
}

// FIXME: LIVE_EXCH / DEAD_EXCH is backwards from what is specified in the API guide.
object NewsType extends Enumeration with HasUndefined with RxOrdinal {
  type NewsType = Value
  val BBS, LIVE_EXCH, DEAD_EXCH, HTML, POPUP_TEXT, POPUP_HTML = Value
}

/**
 * TickType reference: com.ib.client.TickType.java
 */
object TickType extends Enumeration with RxOrdinal {
  type TickType = Value
  val INVALID = Value(-1)
  val BID_SIZE = Value(0)
  val BID = Value(1)
  val ASK = Value(2)
  val ASK_SIZE = Value(3)
  val LAST = Value(4)
  val LAST_SIZE = Value(5)
  val HIGH = Value(6)
  val LOW = Value(7)
  val VOLUME = Value(8)
  val CLOSE = Value(9)
  val BID_OPTION = Value(10)
  val ASK_OPTION = Value(11)
  val LAST_OPTION = Value(12)
  val MODEL_OPTION = Value(13)
  val OPEN = Value(14)
  val LOW_13_WEEK = Value(15)
  val HIGH_13_WEEK = Value(16)
  val LOW_26_WEEK = Value(17)
  val HIGH_26_WEEK = Value(18)
  val LOW_52_WEEK = Value(19)
  val HIGH_52_WEEK = Value(20)
  val AVG_VOLUME = Value(21)
  val OPEN_INTEREST = Value(22)
  val OPTION_HISTORICAL_VOL = Value(23)
  val OPTION_IMPLIED_VOL = Value(24)
  val OPTION_BID_EXCH = Value(25)
  val OPTION_ASK_EXCH = Value(26)
  val OPTION_CALL_OPEN_INTEREST = Value(27)
  val OPTION_PUT_OPEN_INTEREST = Value(28)
  val OPTION_CALL_VOLUME = Value(29)
  val OPTION_PUT_VOLUME = Value(30)
  val INDEX_FUTURE_PREMIUM = Value(31)
  val BID_EXCH = Value(32)
  val ASK_EXCH = Value(33)
  val AUCTION_VOLUME = Value(34)
  val AUCTION_PRICE = Value(35)
  val AUCTION_IMBALANCE = Value(36)
  val MARK_PRICE = Value(37)
  val BID_EFP_COMPUTATION = Value(38)
  val ASK_EFP_COMPUTATION = Value(39)
  val LAST_EFP_COMPUTATION = Value(40)
  val OPEN_EFP_COMPUTATION = Value(41)
  val HIGH_EFP_COMPUTATION = Value(42)
  val LOW_EFP_COMPUTATION = Value(43)
  val CLOSE_EFP_COMPUTATION = Value(44)
  val LAST_TIMESTAMP = Value(45)
  val SHORTABLE = Value(46)
  val FUNDAMENTAL_RATIOS = Value(47)
  val RT_VOLUME = Value(48)
  val HALTED = Value(49)
  val BID_YIELD = Value(50)
  val ASK_YIELD = Value(51)
  val LAST_YIELD = Value(52)
  val CUST_OPTION_COMPUTATION = Value(53)
  val TRADE_COUNT = Value(54)
  val TRADE_RATE = Value(55)
  val VOLUME_RATE = Value(56)
  val LAST_RTH_TRADE = Value(57)
  val RT_HISTORICAL_VOL = Value(58)
  val REGULATORY_IMBALANCE = Value(61)

}
/**
 * reference:
 * https://www.interactivebrokers.com/en/software/api/apiguide/tables/generic_tick_types.htm
 */
object GenericTickType extends Enumeration with HasUndefined with TxOrdinal {
  type GenericTickType = Value
  val OptionVolume = Value(100)
  val OptionOpenInterest = Value(101)
  val HistoricalVolatility = Value(104)
  val OptionImpliedVolatility = Value(106)
  val IndexFuturePremium = Value(162)
  val MiscellaneousStats = Value(165)
  val MarkPrice = Value(221)
  val AuctionValues = Value(225)
  val RTVolume = Value(233)
  val Shortable = Value(236)
  val Inventory = Value(256)
  val FundamentalRatios = Value(258)
  val RealtimeHistoricalVolatility = Value(411)
  val IBDividends = Value(456)
}

object ServerLogLevel extends Enumeration with HasUndefined with TxOrdinal {
  type ServerLogLevel = Value
  val SYSTEM = Value(1)
  val ERROR = Value(2)
  val WARNING = Value(3)
  val INFORMATION = Value(4)
  val DETAIL = Value(5)
}

object DateFormatType extends Enumeration with HasUndefined with TxOrdinal {
  type DateFormatType = Value
  val YYYMMDD = Value(1)
  val SecondsSinceEpoch = Value(2)
}

object FundamentalType extends Enumeration with TxString {
  type FundamentalType = Value
  // TODO: integration testing needs to check this - com.ib.controller values
  // are at odds with other docs
  //  val Estimates = Value("estimates")
  //  val FinancialStatements = Value("finstat")
  //  val Summary = Value("snapshot")
  //  val ReportSnapshot = Value("Company overview")
  //  val ReportsFinSummary = Value("Financial summary")
  //  val ReporRatios = Value("Financial ratios")
  //  val ReportsFinStatements = Value("Financial statements")
  //  val RESC = Value("Analyst estimates")
  //  val CalendarReport = Value("Company calendar")
  val ReportSnapshot = Value
  val ReportsFinSummary = Value
  val ReportRatios = Value
  val ReportsFinStatements = Value
  val RESC = Value
  val CalendarReport = Value
}

object WhatToShow extends Enumeration with HasUndefined with TxString {
  type WhatToShow = Value
  val TRADES, MIDPOINT, BID, ASK = Value // << only these are valid for real-time bars
  val BID_ASK, HISTORICAL_VOLATILITY, OPTION_IMPLIED_VOLATILITY, YIELD_ASK, YIELD_BID, YIELD_BID_ASK, YIELD_LAST = Value
}

// TODO: these are used with Historical data - larger issue of how to do time (Joda?)
// may want to leave this for the service level
// tricky issues - just do manually?
//object DurationUnit
//	public static enum DurationUnit {
//		SECOND, DAY, WEEK, MONTH, YEAR;
//	}

object BarSize extends Enumeration with TxString {
  class XVal(name: String, val secs: Int) extends Val(name)
  def Value(name: String, secs: Int) = new XVal(name, secs)
  type BarSize = Value
  val _1_secs = Value("1 secs", 1)
  val _5_secs = Value("5 secs", 5)
  val _10_secs = Value("10 secs", 10)
  val _15_secs = Value("15 secs", 15)
  val _30_secs = Value("30 secs", 30)
  val _1_min = Value("1 min", 1 * 60)
  val _2_mins = Value("2 mins", 2 * 60)
  val _3_mins = Value("3 mins", 3 * 60)
  val _5_mins = Value("5 mins", 5 * 60)
  val _10_mins = Value("10 mins", 10 * 60)
  val _15_mins = Value("15 mins", 15 * 60)
  val _20_mins = Value("20 mins", 20 * 60)
  val _30_mins = Value("30 mins", 30 * 60)
  val _1_hour = Value("1 hour", 60 * 60)
  val _4_hours = Value("4 hours", 4 * 60 * 60)
  val _1_day = Value("1 day") // sic - don't count seconds for these last two; can vary
  val _1_week = Value("1 week")
}

object Currency extends Enumeration with HasUndefined with RxTxString {
  type Currency = Value
  val USD, AUD, CAD, CHF, CNY, DKK, EUR, GBP, HKD, HUF, ILS, JPY, MXN, NOK, NZD, SEK, SGD, KRW = Value
}

object TimeZones extends Enumeration with RxTxString {
  val GMT = Value // Greenwich Mean Time
  val EST = Value // Eastern Standard Time
  val MST = Value // Mountain Standard Time
  val PST = Value // Pacific Standard Time
  val AST = Value // Atlantic Standard Time
  val JST = Value // Japan Standard Time
  val AET = Value // Australian Standard Time
}

// TODO: object ComboParam

// TODO: object FADataType // FA not yet supported

