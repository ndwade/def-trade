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

import akka.actor.ActorRef
import org.reactivestreams.Subscriber
import java.io.{ DataInputStream, IOException }

private[deftrade] object IncomingMessages {

  private var _nrdz: Int = 0

  def resetNrdz(): Unit = { _nrdz = 0 }
  def nrdz = _nrdz

  // TODO: revisit and optimize this - builder reuse was removed to get around multithreading 
  //	issues but this should really be optimized.
  //    val sb = StringBuilder.newBuilder

  def rdz(implicit input: DataInputStream): String = {
    val sb = StringBuilder.newBuilder
    //      sb.clear
    while ({
      val c = input.readByte()
      if (c != 0) { sb += c.asInstanceOf[Char]; true } else false
    }) {}
    //    println(s"rdz: ${nrdz} => ${sb.result}")
    _nrdz += 1 // omg... testing overhead incurred in production... OK, over it. Whew.
    sb.result
  }

  def rdzIf(pred: Boolean)(implicit input: DataInputStream): String = if (pred) rdz else ""

  def readList[T](rdT: => T)(implicit input: DataInputStream): List[T] = {
    import ImplicitConversions.s2i
    @annotation.tailrec
    def step(n: Int, acc: List[T]): List[T] = n match {
      case 0 => acc
      case i => step(i - 1, rdT :: acc)
    }
    step(rdz, Nil).reverse
  }
}

// FIXME: nonDefaultNamedValues everywhere!!!
// indent: need to use "; " as sep rather than ';' due to use of semicolon in tradingHours param

// todo: different exchanges have different accepted orders for the same contract. Results in 
// multiple ContractDetailsCont messages for the same security. Different liquid hours as well.

trait IncomingMessages { self: SubscriptionsComponent with StreamsComponent with OutgoingMessages =>

  import NonDefaultNamedValues.nonDefaultNamedValues

  import ImplicitConversions._

  import IncomingMessages._

  /**
   * Marker trait for all messages which may be subscribed to from the subs EventBus.
   */
  sealed trait IncomingMessage

  /**
   * A message sent from the TWS API. Instances of these messages can read from the socket using
   * the read method in the companion objects.
   */
  trait ApiMessage extends IncomingMessage

  /**
   * Marker for _any_ message with error semantics. Mixed into ConnectionMessage and ApiMessage.
   */
  trait ErrorMessage extends IncomingMessage
  object ErrorMessage {
    val ReasonEx = "Exception"
    val ReasonUpdateTWS = "Please update TWS"
  }

  /*
   * general categories that API messages are grouped in.
   */
  trait SystemMessage extends ApiMessage
  trait MarketDataMessage extends ApiMessage
  trait HistoricalDataMessage extends ApiMessage
  trait ReferenceDataMessage extends ApiMessage
  trait OrderManagementMessage extends ApiMessage
  trait AccountMessage extends ApiMessage

  // anything that can be sent in response to a TickType specified in ReqMktData
  trait RawTickMessage extends MarketDataMessage

  trait MarketDepthMessage extends MarketDataMessage // (for regular and L2)

  /**
   * Signals TWS connection status change - or attempted change.
   */
  trait IbConnectionMessage extends SystemMessage

  /*
   * TODO: revisit naming; symmetry is misleading on IbConnectionMessage s 
   * - Conn/Dis X OK / Err...
   * - BUT: only one results in connection; 3 others leave you in disconnected state.  
   */
  /**
   * Signals that the connection to IB was made.
   */
  case class IbConnectOk(
    sender: ActorRef,
    msg: IbConnect,
    serverVersion: Int,
    twsTime: String)
      extends IbConnectionMessage {
  }

  /**
   * Signals an error when attempting to connect to the TWS API.
   * - already connected
   * - exception thrown connecting
   */
  case class IbConnectError(
    reason: String,
    sender: ActorRef,
    msg: IbConnect,
    ex: Option[Throwable] = None)
      extends IbConnectionMessage with ErrorMessage {
  }
  object IbConnectError {
    val Redundant = "Already connected"
    val BadSequence = "Connnect addtempt while disconnect pending"
  }

  case class IbDisconnectOk(
    reason: String,
    sender: ActorRef) // the sender of the EDisconnect message
      extends IbConnectionMessage {
  }

  /**
   * Signals an abnormal disconnect from the TWS API.
   * - already disconnected
   * - abnormal disconnect: IOException throw by Reader
   * - server termination API code (-1)
   * - abnormal disconnect: exception thrown while writing (OutgoingError also sent)
   */
  case class IbDisconnectError(
    reason: String, // let's always have one, OK?
    sender: Option[ActorRef] = None,
    msg: Option[OutgoingMessage] = None, // the OutgoingMessage that caused the disconnect
    ex: Option[Throwable] = None) // the exception that caused the disconnect
      extends IbConnectionMessage with ErrorMessage {
  }
  object IbDisconnectError {
    val Redundant = "Already disconnected"
    val BadSequence = "Disconnect attempt while connect pending"
  }

  /**
   * Signals errors which occur when sending messages to the TWS API socket.
   * - tried to send while not connected
   * - validation error (bad params given versions)
   * - thrown exception sending - disconnects (IbDisconnectError also sent)
   */
  case class OutgoingError(
    reason: String,
    sender: ActorRef,
    msg: OutgoingMessage,
    ex: Option[Throwable] = None)
      extends ErrorMessage { // Neither ConnectionMessage nor ApiMessage
  }

  object OutgoingError {
    val NotConnected = "Not connected."
    type MsgType = OutgoingMessage with Product
    def mkReason(msg: MsgType, detail: String): String = s"${msg.productPrefix}: $detail"
  }

  /**
   * Codes and messages should be considered part of the API specification. Note this is the only
   * place where the TWS API notion of "error codes" is retained.
   */
  case class Error(eid: Either[OrderId, ReqId], errorCode: Int, errorMsg: String)
      extends Throwable(s"$errorCode: $errorMsg") with SystemMessage with ErrorMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object Error {
    // TODO: add moar codes
    //    val UnknownId = (505, "Fatal Error: Unknown message id.")
    //    val UnknownContract = (517, "Unknown contract. Verify the contract details supplied.")
    val code = 4
    def read(implicit input: DataInputStream): Unit = {
      val version: Int = rdz
      val id = if (version < 2) "-1" else rdz
      val err =
        if (version < 2) Error(eid = id, errorCode = -1, errorMsg = rdz)
        else Error(eid = id, errorCode = rdz, errorMsg = rdz)
      // FIXME: this is pretty broken for id == -1
      streams get id foreach { s => s onError err; streams - id }
      subs publish err
    }
  }

  /**
   *
   */
  case class NextValidId(orderId: OrderId) extends SystemMessage {
    override def toString: String = nonDefaultNamedValues
  }
  object NextValidId {
    val code = 9
    def read(implicit input: DataInputStream): Unit = {
      rdz // version field unused
      subs publish NextValidId(orderId = rdz)
    }
  }

  /**
   *
   */
  case class CurrentTime(time: Long) extends SystemMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object CurrentTime {
    val code = 49
    def read(implicit input: DataInputStream): Unit = {
      rdz // unused version field
      subs publish CurrentTime(time = rdz)
    }
  }

  import NewsType.NewsType
  case class UpdateNewsBulletin(msgId: Int, msgType: NewsType, message: String,
      origExchange: String) extends SystemMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object UpdateNewsBulletin {
    val code = 14
    def read(implicit input: DataInputStream): Unit = {
      rdz // version is unused
      subs publish UpdateNewsBulletin(msgId = rdz,
        msgType = rdz, message = rdz, origExchange = rdz)
    }
  }

  import MktDataType._
  /**
   * This message signals a switch between real-time and frozen data.
   * It is delivered per data subscription and should be folded in to the stream response,
   * to be acted on programmatically per stream.
   */
  case class MarketDataType(reqId: ReqId, marketDataType: MktDataType) extends RawTickMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object MarketDataType {
    val code = 58
    def read(implicit input: DataInputStream): Unit = {
      rdz // unused version field
      subs publish MarketDataType(reqId = rdz, marketDataType = rdz)
    }
  }

  /**
   * 
   */
  case class ScannerParameters(xml: String) extends SystemMessage {
    override def toString: String = nonDefaultNamedValues
  }
  object ScannerParameters {
    val code = 19
    def read(implicit input: DataInputStream): Unit = {
      rdz //unused version
      subs publish ScannerParameters(xml = rdz)
    }
  }

  import TickType._
  /**
   *
   */
  case class TickPrice(tickerId: ReqId, field: TickType, price: Double, canAutoExecute: Boolean)
      extends RawTickMessage {
    override def toString: String = nonDefaultNamedValues
  }
  object TickPrice {
    val code = 1
    def read(implicit input: DataInputStream): Unit = {
      val version: Int = rdz
      val tickerId, tickType, price = rdz
      val size = rdzIf(version >= 2)
      val canAutoExecute = rdzIf(version >= 3)
      val tp = TickPrice(tickerId = tickerId, field = tickType, price = price, canAutoExecute = canAutoExecute)
      val os = streams get tickerId
      os foreach { _ onNext tp }
      subs publish tp
      if (version >= 2) {
        val sizeTickType = toSizeTickType(tickType)
        if (sizeTickType != INVALID) {
          val ts = TickSize(tickerId = tickerId, field = sizeTickType, size = size)
          os foreach { _ onNext ts }
          subs publish ts
        }
      }
    }
    private val toSizeTickType =
      Map(BID -> BID_SIZE, ASK -> ASK_SIZE, LAST -> LAST_SIZE) withDefaultValue INVALID
  }

  case class TickSize(tickerId: ReqId, field: TickType, size: Int) extends RawTickMessage {
    override def toString: String = nonDefaultNamedValues
  }
  object TickSize {
    val code = 2
    def read(implicit input: DataInputStream): Unit = {
      rdz // unused version field
      subs publish TickSize(tickerId = rdz, field = rdz, size = rdz)
    }
  }

  case class TickGeneric(tickerId: ReqId, tickType: TickType, value: Double) extends RawTickMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object TickGeneric {
    val code = 45
    def read(implicit input: DataInputStream): Unit = {
      rdz // unused version field
      subs publish TickGeneric(tickerId = rdz, tickType = rdz, value = rdz)
    }
  }

  case class TickString(tickerId: ReqId, tickType: TickType, value: String) extends RawTickMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object TickString {
    val code = 46
    def read(implicit input: DataInputStream): Unit = {
      rdz // unused version field
      subs publish TickString(tickerId = rdz, tickType = rdz, value = rdz)
    }
  }

  case class TickOptionComputation(
      tickerId: ReqId,
      field: TickType,
      impliedVol: Option[Double],
      delta: Option[Double],
      optPrice: Option[Double],
      pvDividend: Option[Double],
      gamma: Option[Double],
      vega: Option[Double],
      theta: Option[Double],
      undPrice: Option[Double]) extends MarketDataMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object TickOptionComputation {

    val code = 21

    type Constraint = Double => Boolean
    private val nonNeg: Constraint = _ >= 0.0
    private val magSubOne: Constraint = d => (-1.0) < d && d < 1.0

    def read(implicit input: DataInputStream): Unit = {

      def rdzConstrained(constraint: Constraint) = {
        val d: Double = rdz
        if (constraint(d)) Some(d) else None
      }
      def rdzConstrainedIf(pred: Boolean)(constraint: Constraint) =
        if (pred) rdzConstrained(constraint) else None

      def rdzNonNeg = rdzConstrained(nonNeg)
      def rdzMagSubOne = rdzConstrained(magSubOne)

      def rdzNonNegIf(pred: Boolean) = rdzConstrainedIf(pred)(nonNeg)
      def rdzMagSubOneIf(pred: Boolean) = rdzConstrainedIf(pred)(magSubOne)

      val version: Int = rdz // unused version field
      val tickerId = rdz
      val tickType: TickType = rdz

      subs publish TickOptionComputation(
        tickerId = tickerId,
        field = tickType,
        impliedVol = rdzNonNeg,
        delta = rdzMagSubOne,
        optPrice = rdzNonNegIf(version >= 6 || tickType == TickType.MODEL_OPTION),
        pvDividend = rdzNonNegIf(version >= 6 || tickType == TickType.MODEL_OPTION),
        gamma = rdzMagSubOneIf(version >= 6),
        vega = rdzMagSubOneIf(version >= 6),
        theta = rdzMagSubOneIf(version >= 6),
        undPrice = rdzNonNegIf(version >= 6))
    }
  }
  /**
   *
   */
  case class TickEFP(tickerId: ReqId, tickType: TickType, basisPoints: Double,
      formattedBasisPoints: String, impliedFuture: Double, holdDays: Int,
      futureExpiry: String, dividendImpact: Double, dividendsToExpiry: Double) extends RawTickMessage {
    override def toString: String = nonDefaultNamedValues
  }
  case object TickEFP {
    val code = 47
    def read(implicit input: DataInputStream): Unit = {
      rdz // unused version
      subs publish TickEFP(tickerId = rdz, tickType = rdz, basisPoints = rdz,
        formattedBasisPoints = rdz, impliedFuture = rdz, holdDays = rdz, futureExpiry = rdz,
        dividendImpact = rdz, dividendsToExpiry = rdz)
    }
  }

  /**
   *
   */
  case class TickSnapshotEnd(reqId: ReqId) extends RawTickMessage {
    override def toString: String = nonDefaultNamedValues
  }
  case object TickSnapshotEnd {
    val code = 17
    def read(implicit input: DataInputStream): Unit = {
      rdz // unused version
      subs publish TickSnapshotEnd(rdz)
    }
  }

  /**
   *
   */
  case class RealTimeBar(tickerId: ReqId, time: Long,
      open: Double, high: Double, low: Double, close: Double,
      volume: Long, wap: Double, count: Int) extends MarketDataMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object RealTimeBar {
    val code = 50
    def read(implicit input: DataInputStream): Unit = {
      rdz // unused version field
      subs publish RealTimeBar(tickerId = rdz, time = rdz,
        open = rdz, high = rdz, low = rdz, close = rdz,
        volume = rdz, wap = rdz, count = rdz)
    }
  }

  import DeepType.DeepType
  import DeepSide.DeepSide

  case class UpdateMktDepth(tickerId: ReqId, position: Int, operation: DeepType,
      side: DeepSide, price: Double, size: Int) extends MarketDepthMessage {
    override def toString: String = nonDefaultNamedValues
  }
  object UpdateMktDepth {
    val code = 12
    def read(implicit input: DataInputStream): Unit = {
      rdz // version is unused
      subs publish UpdateMktDepth(
        tickerId = rdz, position = rdz, operation = rdz, side = rdz, price = rdz, size = rdz)
    }
  }

  case class UpdateMktDepthL2(tickerId: ReqId, position: Int, marketMaker: String,
      operation: DeepType, side: DeepSide, price: Double, size: Int) extends MarketDepthMessage {
    override def toString: String = nonDefaultNamedValues
  }
  object UpdateMktDepthL2 {
    val code = 13
    def read(implicit input: DataInputStream): Unit = {
      rdz // version is unused
      subs publish UpdateMktDepthL2(tickerId = rdz,
        position = rdz, marketMaker = rdz, operation = rdz, side = rdz, price = rdz, size = rdz)
    }
  }

  /**
   * Scanner data is deemed to be Market Data because it is derived from current market quotes,
   * in the much same sense as real time bars are derived from market data.
   */
  case class ScannerData(val reqId: ReqId, rank: Int, contractDetails: ContractDetails,
    distance: String, benchmark: String, projection: String, legsStr: String)
      extends MarketDataMessage {
    override def toString: String = nonDefaultNamedValues
  }

  case class ScannerDataEnd(reqId: ReqId) extends MarketDataMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object ScannerData {
    val code = 20
    def read(implicit input: DataInputStream): Unit = {
      val version: Int = rdz
      val reqId = rdz
      val n: Int = rdz

      misc.repeat(n) {
        val rank = rdz
        val conId = rdzIf(version >= 3)
        val symbol, secType, expiry, strike, right, exchange, currency, localSymbol = rdz
        val marketName = rdz
        val tradingClass = rdz
        subs publish ScannerData(reqId = reqId, rank = rank,
          contractDetails = ContractDetails(
            summary = Contract(conId = conId, symbol = symbol, secType = secType, expiry = expiry,
              strike = strike, right = right, exchange = exchange, currency = currency,
              localSymbol = localSymbol, tradingClass = tradingClass),
            marketName = marketName),
          distance = rdz, benchmark = rdz, projection = rdz,
          legsStr = rdzIf(version >= 2))
      }
      subs publish ScannerDataEnd(reqId)
    }
  }

  /**
   *
   */
  case class HistoricalData(
      reqId: ReqId, date: String,
      open: Double, high: Double, low: Double, close: Double,
      volume: Int, count: Int, WAP: Double, hasGaps: Boolean) extends HistoricalDataMessage {
    override def toString: String = nonDefaultNamedValues
  }

  /**
   * Modification to the IB library protocol, which simply calls historicalData again with
   * some magic values. This change makes HistoricalData behave more like ScannerData.
   */
  case class HistoricalDataEnd(reqId: ReqId, startDate: String, endDate: String) extends HistoricalDataMessage {
    override def toString: String = nonDefaultNamedValues
  }
  object HistoricalData {
    val code = 17
    def read(implicit input: DataInputStream): Unit = {
      val version: Int = rdz
      val reqId = rdz
      val startDate = rdzIf(version >= 2)
      val endDate = rdzIf(version >= 2)
      val n: Int = rdz
      misc.repeat(n) {
        subs publish HistoricalData(reqId = reqId, date = rdz,
          open = rdz, high = rdz, low = rdz, close = rdz,
          volume = rdz, WAP = rdz,
          hasGaps = (rdz equalsIgnoreCase "true"),
          count = if (version >= 3) rdz else "-1")
      }
      subs publish HistoricalDataEnd(reqId, startDate, endDate)
    }
  }

  /**
   * Fundamental data is deemed to be historical data because it is derived from historical
   * (not current) market prices.
   */
  case class FundamentalData(reqId: ReqId, data: String) extends HistoricalDataMessage {
    override def toString: String = nonDefaultNamedValues
  }
  case object FundamentalData {
    val code = 51
    def read(implicit input: DataInputStream): Unit = {
      rdz // unused version
      subs publish FundamentalData(reqId = rdz, data = rdz)
    }
  }

  /**
   *
   */
  case class ContractDetailsCont(reqId: ReqId, contractDetails: ContractDetails)
      extends ReferenceDataMessage {
    override def toString: String = nonDefaultNamedValues
  }
  object ContractDetailsCont {
    val code = 10
    def read(implicit input: DataInputStream): Unit = {
      val version: Int = rdz
      val reqId = if (version >= 3) rdz else "-1"
      val symbol, secType, expiry, strike, right, exchange, currency, localSymbol, marketName = rdz
      val tradingClass, conId, minTick, multiplier, orderTypes, validExchanges = rdz
      val priceMagnifier = rdzIf(version >= 2)
      val underConId = rdzIf(version >= 4)
      val longName, primaryExch = rdzIf(version >= 5)
      val contractMonth, industry, category, subcategory, timeZoneId, tradingHours, liquidHours = rdzIf(version >= 6)
      val evRule, evMultiplier = rdzIf(version >= 8)
      val secIdList: List[(String, String)] = if (version >= 7) readList { (rdz, rdz) } else Nil

      subs publish ContractDetailsCont(reqId, ContractDetails(
        summary = Contract(
          symbol = symbol, secType = secType, expiry = expiry, strike = strike, right = right,
          exchange = exchange, currency = currency, localSymbol = localSymbol,
          tradingClass = tradingClass,
          conId = conId, multiplier = multiplier, primaryExch = primaryExch),
        marketName = marketName, minTick = minTick,
        orderTypes = orderTypes, validExchanges = validExchanges, priceMagnifier = priceMagnifier,
        underConId = underConId, longName = longName, contractMonth = contractMonth,
        industry = industry, category = category, subcategory = subcategory,
        timeZoneId = timeZoneId, tradingHours = tradingHours, liquidHours = liquidHours,
        evRule = evRule, evMultiplier = evMultiplier, secIdList = secIdList))
    }
  }

  case class ContractDetailsEnd(reqId: ReqId) extends ReferenceDataMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object ContractDetailsEnd {
    val code = 52
    def read(implicit input: DataInputStream): Unit = {
      rdz // version field unused
      subs publish ContractDetailsEnd(reqId = rdz)
    }
  }

  case class BondContractDetails(reqId: ReqId, contract: ContractDetails) extends ReferenceDataMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object BondContractDetails {
    val code = 18
    def read(implicit input: DataInputStream, serverVersion: Int): Unit = {
      val version: Int = rdz
      val reqId = if (version >= 3) rdz else "-1"
      val symbol, secType = rdz
      val cusip, coupon, maturity, issueDate, ratings, bondType, couponType, convertible = rdz
      val callable, putable, descAppend, exchange, currency, marketName, tradingClass = rdz
      val conId, minTick, orderTypes, validExchanges = rdz
      val nextOptionDate, nextOptionType, nextOptionPartial, notes = rdzIf(version >= 2)
      val longName = rdzIf(version >= 4)
      val evRule, evMultiplier = rdzIf(version >= 6)
      val secIdList = if (version >= 5) readList { (rdz, rdz) } else Nil
      subs publish BondContractDetails(reqId = reqId,
        contract = ContractDetails(
          marketName = marketName,
          minTick = minTick,
          orderTypes = orderTypes,
          validExchanges = validExchanges,
          longName = longName,
          evRule = evRule, evMultiplier = evMultiplier,
          secIdList = secIdList,
          summary = Contract(
            symbol = symbol, secType = secType, conId = conId,
            exchange = exchange, currency = currency, tradingClass = tradingClass),
          cusip = cusip, coupon = coupon, maturity = maturity, issueDate = issueDate,
          ratings = ratings, bondType = bondType, couponType = couponType,
          convertible = convertible, callable = callable, putable = putable,
          descAppend = descAppend,
          nextOptionDate = nextOptionDate, nextOptionType = nextOptionType,
          nextOptionPartial = nextOptionPartial, notes = notes))
    }
  }

  /**
   *
   */
  case class OpenOrder(orderId: OrderId, contract: Contract, order: Order, orderState: OrderState)
      extends OrderManagementMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object OpenOrder {
    val code = 5
    def read(implicit input: DataInputStream, serverVersion: Int): Unit = {
      val version: Int = rdz
      val orderId = rdz
      val conId = rdzIf(version >= 17)
      val symbol, secType, expiry, strike, right = rdz
      val multiplier = rdzIf(version >= 32)
      val exchange, currency = rdz
      val localSymbol = rdzIf(version >= 2)
      val tradingClass = rdzIf(version >= 32)
      val action, totalQuantity, orderType = rdz
      def rdzEmptyAsZero: String = {
        val v = rdz
        if (v.isEmpty) "0" else v
      }
      val lmtPrice = if (version < 29) rdzEmptyAsZero else rdz
      val auxPrice = if (version < 30) rdzEmptyAsZero else rdz
      val tif, ocaGroup, account, openClose, origin, orderRef = rdz
      val clientId = rdzIf(version >= 3)
      var permId, outsideRth, discretionaryAmt = ""
      var hidden: Boolean = false
      if (version >= 4) {
        permId = rdz
        if (version < 18) rdz else outsideRth = rdz
        hidden = (rdz: Int) == 1 // (sic)
        discretionaryAmt = rdz
      }
      val goodAfterTime = rdzIf(version >= 5)
      rdzIf(version >= 6) // sharesAllocation - deprecated
      var faGroup, faMethod, faPercentage, faProfile = ""
      if (version >= 7) {
        faGroup = rdz; faMethod = rdz; faPercentage = rdz; faProfile = rdz
      }

      val goodTillDate = rdzIf(version >= 8)
      var rule80A, percentOffset, settlingFirm, shortSaleSlot, designatedLocation = ""
      var exemptCode = "-1"
      var auctionStrategy, startingPrice, stockRefPrice, delta = ""
      var stockRangeLower, stockRangeUpper, displaySize = ""
      var blockOrder, sweepToFill, allOrNone, minQty, ocaType, eTradeOnly, firmQuoteOnly, nbboPriceCap = ""
      if (version >= 9) {
        rule80A = rdz; percentOffset = rdz; settlingFirm = rdz
        shortSaleSlot = rdz; designatedLocation = rdz
        // TODO: revisit support for serverVersion this far back
        if (serverVersion == 51) rdz else if (version >= 23) exemptCode = rdz
        auctionStrategy = rdz; startingPrice = rdz; stockRefPrice = rdz; delta = rdz
        stockRangeLower = rdz; stockRangeUpper = rdz; displaySize = rdz
        rdzIf(version < 18) // rthOnly - deprecated
        blockOrder = rdz; sweepToFill = rdz; allOrNone = rdz; minQty = rdz; ocaType = rdz
        eTradeOnly = rdz; firmQuoteOnly = rdz; nbboPriceCap = rdz
      }
      val parentId, triggerMethod = rdzIf(version >= 10)
      var volatility, volatilityType, deltaNeutralOrderType, deltaNeutralAuxPrice = ""
      var deltaNeutralConId, deltaNeutralSettlingFirm, deltaNeutralClearingAccount = ""
      var deltaNeutralClearingIntent, deltaNeutralOpenClose, deltaNeutralShortSale = ""
      var deltaNeutralShortSaleSlot, deltaNeutralDesignatedLocation = ""
      var continuousUpdate, referencePriceType = ""
      if (version >= 11) {
        volatility = rdz; volatilityType = rdz
        if (version == 11) { deltaNeutralOrderType = if ((rdz: Int) == 0) "NONE" else "MKT" } else {
          // version >= 12
          deltaNeutralOrderType = rdz; deltaNeutralAuxPrice = rdz
          if (version >= 27 && !deltaNeutralOrderType.isEmpty) {
            deltaNeutralConId = rdz; deltaNeutralSettlingFirm = rdz
            deltaNeutralClearingAccount = rdz; deltaNeutralClearingIntent = rdz
          }
          if (version >= 31 && !deltaNeutralOrderType.isEmpty) {
            deltaNeutralOpenClose = rdz; deltaNeutralShortSale = rdz
            deltaNeutralShortSaleSlot = rdz; deltaNeutralDesignatedLocation = rdz
          }
        }
        continuousUpdate = rdz
        // n.b. serverVersion == 26 is not supported
        referencePriceType = rdz
      }
      val trailStopPrice = rdzIf(version >= 13)
      val trailingPercent = rdzIf(version >= 30)
      val basisPoints, basisPointsType, comboLegsDescrip = rdzIf(version >= 14)
      val comboLegs = if (version >= 29) readList {
        ComboLeg(conId = rdz, ratio = rdz, action = rdz, exchange = rdz, openClose = rdz,
          shortSaleSlot = rdz, designatedLocation = rdz, exemptCode = rdz)
      }
      else Nil
      val orderComboLegs = if (version >= 29) readList { OrderComboLeg(price = rdz) } else Nil
      val smartComboRoutingParams = if (version >= 26) readList { (rdz, rdz) } else Nil
      var scaleInitLevelSize, scaleSubsLevelSize, scalePriceIncrement = ""
      if (version >= 15) {
        if (version >= 20) { scaleInitLevelSize = rdz; scaleSubsLevelSize = rdz }
        else { rdz; scaleInitLevelSize = rdz }
        scalePriceIncrement = rdz
      }
      val _c1 = version >= 28 && ((scalePriceIncrement: Option[Double]) map {
        _ > 0.0
      } getOrElse false)
      val scalePriceAdjustValue, scalePriceAdjustInterval, scaleProfitOffset = rdzIf(_c1)
      val scaleAutoReset, scaleInitPosition, scaleInitFillQty, scaleRandomPercent = rdzIf(_c1)
      val hedgeType = rdzIf(version >= 24)
      val hedgeParam = rdzIf(version >= 24 && !hedgeType.isEmpty)
      val optOutSmartRouting = rdzIf(version >= 25)
      val clearingAccount, clearingIntent = rdzIf(version >= 19)
      val notHeld = rdzIf(version >= 22)
      val underComp = if (version >= 20 && rdz) Some(UnderComp(conId = rdz, delta = rdz, price = rdz)) else None
      val algoStrategy = rdzIf(version >= 21)
      val algoParams = if (!algoStrategy.isEmpty) readList { (rdz, rdz) } else Nil

      val whatIf, status, initMargin, maintMargin, equityWithLoan = rdzIf(version >= 16)

      // arguably a bug in the com.ib.client code to initialize commission fields to zero.
      // will be bug for bug compatible for now.
      val commission, minCommission, maxCommission = if (version >= 16) rdz else "0"
      val commissionCurrency, warningText = rdzIf(version >= 16)

      subs publish OpenOrder(
        orderId = orderId,
        contract = Contract(conId = conId, symbol = symbol, secType = secType, expiry = expiry,
          strike = strike, right = right, multiplier = multiplier, exchange = exchange,
          currency = currency, localSymbol = localSymbol, tradingClass = tradingClass,
          comboLegsDescrip = comboLegsDescrip, comboLegs = comboLegs, underComp = underComp),
        order = Order(
          orderId = orderId, clientId = clientId, permId = permId, action = action,
          totalQuantity = totalQuantity, orderType = orderType, lmtPrice = lmtPrice,
          auxPrice = auxPrice,
          tif = tif, ocaGroup = ocaGroup, ocaType = ocaType, orderRef = orderRef,
          parentId = parentId, blockOrder = blockOrder, sweepToFill = sweepToFill,
          displaySize = displaySize, triggerMethod = triggerMethod, outsideRth = outsideRth,
          hidden = hidden, goodAfterTime = goodAfterTime, goodTillDate = goodTillDate,
          rule80A = rule80A, allOrNone = allOrNone, minQty = minQty,
          percentOffset = percentOffset, trailStopPrice = trailStopPrice,
          trailingPercent = trailingPercent, notHeld = notHeld,
          faGroup = faGroup, faProfile = faProfile, faMethod = faMethod,
          faPercentage = faPercentage,
          openClose = openClose, origin = origin, shortSaleSlot = shortSaleSlot,
          designatedLocation = designatedLocation, exemptCode = exemptCode,
          discretionaryAmt = discretionaryAmt, eTradeOnly = eTradeOnly,
          firmQuoteOnly = firmQuoteOnly, nbboPriceCap = nbboPriceCap,
          optOutSmartRouting = optOutSmartRouting,
          auctionStrategy = auctionStrategy, startingPrice = startingPrice,
          stockRefPrice = stockRefPrice, delta = delta,
          stockRangeLower = stockRangeLower, stockRangeUpper = stockRangeUpper,
          volatility = volatility, volatilityType = volatilityType,
          continuousUpdate = continuousUpdate, referencePriceType = referencePriceType,
          deltaNeutralOrderType = deltaNeutralOrderType,
          deltaNeutralAuxPrice = deltaNeutralAuxPrice,
          deltaNeutralConId = deltaNeutralConId,
          deltaNeutralSettlingFirm = deltaNeutralSettlingFirm,
          deltaNeutralClearingAccount = deltaNeutralClearingAccount,
          deltaNeutralClearingIntent = deltaNeutralClearingIntent,
          deltaNeutralOpenClose = deltaNeutralOpenClose,
          deltaNeutralShortSale = deltaNeutralShortSale,
          deltaNeutralShortSaleSlot = deltaNeutralShortSaleSlot,
          deltaNeutralDesignatedLocation = deltaNeutralDesignatedLocation,
          scaleInitLevelSize = scaleInitLevelSize,
          scaleSubsLevelSize = scaleSubsLevelSize,
          scalePriceIncrement = scalePriceIncrement,
          scalePriceAdjustValue = scalePriceAdjustValue,
          scalePriceAdjustInterval = scalePriceAdjustInterval,
          scaleProfitOffset = scaleProfitOffset,
          scaleAutoReset = scaleAutoReset,
          scaleInitPosition = scaleInitPosition,
          scaleInitFillQty = scaleInitFillQty,
          scaleRandomPercent = scaleRandomPercent,
          basisPoints = basisPoints,
          basisPointsType = basisPointsType,
          smartComboRoutingParams = smartComboRoutingParams,
          orderComboLegs = orderComboLegs,
          hedgeType = hedgeType, hedgeParam = hedgeParam,
          account = account, settlingFirm = settlingFirm,
          clearingAccount = clearingAccount, clearingIntent = clearingIntent,
          algoStrategy = algoStrategy, algoParams = algoParams,
          whatIf = whatIf),
        orderState = OrderState(status = status, initMargin = initMargin, maintMargin = maintMargin,
          equityWithLoan = equityWithLoan, commission = commission, minCommission = minCommission,
          maxCommission = maxCommission, commissionCurrency = commissionCurrency,
          warningText = warningText))
    }
  }

  /**
   *
   */
  case class OpenOrderEnd() extends OrderManagementMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object OpenOrderEnd {
    val code = 53
    def read(implicit input: DataInputStream): Unit = {
      rdz // unused version
      subs publish OpenOrderEnd()
    }
  }

  import OrderStatusEnum._
  /**
   *
   */
  case class OrderStatus( // FIXME: use some defaults here...
      orderId: OrderId, status: OrderStatusEnum, filled: Int,
      remaining: Int, avgFillPrice: Double,
      permId: Int,
      parentId: OrderId, lastFillPrice: Double, clientId: Int,
      whyHeld: String) extends OrderManagementMessage {
    // TODO: whyHeld as an enum?
    override def toString: String = nonDefaultNamedValues
  }
  object OrderStatus {
    val code = 3
    def read(implicit input: DataInputStream): Unit = {
      val version: Int = rdz
      subs publish OrderStatus(orderId = rdz,
        status = rdz, filled = rdz, remaining = rdz, avgFillPrice = rdz,
        permId = rdzIf(version >= 2), parentId = rdzIf(version >= 3),
        lastFillPrice = rdzIf(version >= 4), clientId = rdzIf(version >= 5),
        whyHeld = rdzIf(version >= 6))
    }
  }

  /**
   * DeltaNeutralValidation is deemed to be an OrderManagementMessage because it confirms
   * validation of an outstanding order.
   */
  case class DeltaNeutralValidation(reqId: ReqId, underComp: UnderComp) extends OrderManagementMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object DeltaNeutralValidation {
    val code = 56
    def read(implicit input: DataInputStream): Unit = {
      rdz // unused version field
      subs publish DeltaNeutralValidation(
        reqId = rdz,
        underComp = UnderComp(conId = rdz, delta = rdz, price = rdz))
    }
  }

  /**
   *
   */
  case class ExecDetails(reqId: ReqId, contract: Contract, exec: Execution) extends OrderManagementMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object ExecDetails {
    val code = 11
    def read(implicit input: DataInputStream, serverVersion: Int): Unit = {
      val version: Int = rdz
      val reqId = if (version >= 7) rdz else "-1"
      val orderId = rdz
      val conId = rdzIf(version >= 5)
      val symbol, secType, expiry, strike, right = rdz
      val multiplier = rdzIf(version >= 9)
      val exchange, currency, localSymbol = rdz
      val tradingClass = rdzIf(version >= 10)
      val execId, time, acctNumber, xExchange, side, shares, price = rdz
      val permId = rdzIf(version >= 2)
      val clientId = rdzIf(version >= 3)
      val liquidation = rdzIf(version >= 4)
      val cumQty, avgPrice = rdzIf(version >= 6)
      val orderRef = rdzIf(version >= 8)
      val evRule, evMultiplier = rdzIf(version >= 9)
      subs publish ExecDetails(
        reqId,
        Contract(
          conId = conId, symbol = symbol, secType = secType, expiry = expiry, strike = strike,
          right = right, multiplier = multiplier, exchange = exchange,
          currency = currency, localSymbol = localSymbol, tradingClass = tradingClass),
        Execution(
          orderId = orderId, execId = execId, time = time, acctNumber = acctNumber,
          exchange = xExchange, side = side, shares = shares, price = price, permId = permId,
          clientId = clientId, liquidation = liquidation, cumQty = cumQty, avgPrice = avgPrice,
          orderRef = orderRef, evRule = evRule, evMultiplier = evMultiplier))
    }

  }

  /**
   *
   */
  case class ExecDetailsEnd(reqId: ReqId) extends OrderManagementMessage {
    override def toString: String = nonDefaultNamedValues
  }
  object ExecDetailsEnd {
    val code = 9
    def read(implicit input: DataInputStream): Unit = {
      rdz // version field unused
      subs publish ExecDetailsEnd(reqId = rdz)
    }
  }

  /**
   * CommissionReportMsg is deemed to be an OrderManagementMessage because it can be joined
   * (through the execId) to the OrderId that resulted in the commission charge.
   */
  case class CommissionReportMsg(commissionReport: CommissionReport) extends OrderManagementMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object CommissionReportMsg {
    val code = 59
    def read(implicit input: DataInputStream): Unit = {
      rdz
      subs publish CommissionReportMsg(commissionReport = CommissionReport(
        execId = rdz, commission = rdz, currency = rdz,
        realizedPNL = rdz, `yield` = rdz, yieldRedemptionDate = rdz))
    }
  }

  import Currency._
  /**
   *
   */
  case class UpdateAccountValue(key: String, value: String, currency: Currency,
      accountName: String) extends AccountMessage {
    override def toString: String = nonDefaultNamedValues
  }
  object UpdateAccountValue {
    val code = 6
    def read(implicit input: DataInputStream): Unit = {
      val version: Int = rdz
      subs publish UpdateAccountValue(key = rdz, value = rdz, currency = rdz,
        accountName = rdzIf(version >= 2))
    }
  }

  case class UpdatePortfolio(contract: Contract, position: Int, marketPrice: Double,
      marketValue: Double, averageCost: Double, unrealizedPNL: Double,
      realizedPNL: Double, accountName: String) extends AccountMessage {
    override def toString: String = nonDefaultNamedValues
  }
  object UpdatePortfolio {
    val code = 7
    def read(implicit input: DataInputStream, serverVersion: Int): Unit = {
      val version: Int = rdz
      subs publish UpdatePortfolio(
        contract = Contract(conId = rdzIf(version >= 6), symbol = rdz, secType = rdz,
          expiry = rdz, strike = rdz, right = rdz, multiplier = rdzIf(version >= 7),
          primaryExch = rdzIf(version >= 7), currency = rdz, localSymbol = rdzIf(version >= 2),
          tradingClass = rdzIf(version >= 8)),
        position = rdz, marketPrice = rdz, marketValue = rdz,
        averageCost = rdzIf(version >= 3),
        unrealizedPNL = rdzIf(version >= 3),
        realizedPNL = rdzIf(version >= 3),
        accountName = rdzIf(version >= 4))
      // n.b: in com.ib.client, the condition for reading primaryExchange at the end - 
      // if (version >= 6 && serverVersion == 39) will never hold because serverVersion 39 will
      // never be supported.
    }
  }

  case class UpdateAccountTime(timeStamp: String) extends AccountMessage {
    override def toString: String = nonDefaultNamedValues
  }
  object UpdateAccountTime {
    val code = 8
    def read(implicit input: DataInputStream): Unit = {
      rdz
      subs publish UpdateAccountTime(timeStamp = rdz)
    }
  }

  case class AccountDownloadEnd(accountName: String) extends AccountMessage {
    override def toString: String = nonDefaultNamedValues
  }

  object AccountDownloadEnd {
    val code = 54
    def read(implicit input: DataInputStream): Unit = {
      rdz // unused version field
      subs publish AccountDownloadEnd(accountName = rdz)
    }
  }

  case class ManageAccounts(accounts: String) extends AccountMessage {
    override def toString: String = nonDefaultNamedValues
  }
  object ManageAccounts {
    val code = 15
    def read(implicit input: DataInputStream): Unit = {
      rdz // unused version field
      subs publish ManageAccounts(accounts = rdz)
    }
  }

  case class Debug(msgs: String) extends IncomingMessage {
    override def toString: String = nonDefaultNamedValues
  }

  def dbg(msgs: String*) = msgs foreach { subs publish Debug(_) }

  /**
   * Used for debugging the deserialization logic. Import the names from this module
   * with a read() method and every zero-terminated string read from the socket input stream
   * will be published as a Debug message.
   */
  object Dbg {
    def rdz(implicit input: DataInputStream): String = {
      val s = IncomingMessages.rdz
      dbg(s)
      s
    }
    def rdzIf(pred: Boolean)(implicit input: DataInputStream): String = if (pred) rdz else ""

    def readList[T](rdT: => T)(implicit input: DataInputStream): List[T] = {
      import ImplicitConversions.s2i
      @annotation.tailrec
      def step(n: Int, acc: List[T]): List[T] = n match {
        case 0 => acc
        case i => step(i - 1, rdT :: acc)
      }
      step(rdz, Nil).reverse
    }
  }

}