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

import NonDefaultNamedValues.nonDefaultNamedValues

private[deftrade] object MinServerVer {
  val SecIdType = 45
  val DeltaNeutralOpenClose = 66
  val AcctSummary = 67
  val TradingClass = 68
  val ScaleTable = 69
}

private[deftrade] object OutgoingMessages { 
  
  // TODO: support FA messages

  import java.io.{ DataInputStream, OutputStream, IOException }

  @inline def _wrz(s: String)(implicit os: OutputStream) {
    os write s.getBytes("US-ASCII")
    os write (0: Byte) // putting the z in wrz
  }

  def wrz(ss: String*)(implicit os: OutputStream) {
    ss foreach _wrz
  }

  def wrz(codeAndVersion: (Int, Int))(implicit os: OutputStream) {
    import ImplicitConversions.i2s
    codeAndVersion match { case (code, version) => wrz(code, version) }
  }
  def wrzList[T](xs: List[T])(writeT: T => Unit)(implicit os: OutputStream): Unit = {
    import ImplicitConversions.i2s
    wrz(xs.size)
    xs foreach writeT
  }

  @inline def isEmpty[A](a: A)(implicit ev: String => A): Boolean = a == ("": A)
  @inline def nonEmpty[A](a: A)(implicit ev: String => A): Boolean = a != ("": A)

  class OutgoingMessageValidationException(message: String)
    extends RuntimeException(message) with util.control.ControlThrowable

  def fail(errmsg: String) = throw new OutgoingMessageValidationException(errmsg)

}

trait OutgoingMessages { _: DomainTypesComponent with DTOs with ConfigSettings =>

  import java.io.{ DataInputStream, OutputStream, IOException }
  import ImplicitConversions._
  import MoneyType.implicitConversions._
  import CurrencyType.implicitConversions._

  import OutgoingMessages._

  case class IbConnect(
      host: String = settings.ibc.host, 
      port: Int = settings.ibc.port, 
      clientId: Int = settings.ibc.clientId)

  case class IbDisconnect(why: String)

  /**
   * Base trait for all TWS API messages sent to the server. {{{OutgoingMessage}}}s can write
   * themselves to an output stream.
   */
  sealed trait OutgoingMessage {
    def write(implicit out: OutputStream, serverVersion: Int): Unit
  }

  /**
   *
   */
  case class ReqIds(numIds: Int) extends OutgoingMessage {
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(ReqIds.api)
      wrz(numIds)
    }
    override def toString: String = nonDefaultNamedValues
  }
  object ReqIds { val api = (8, 1) }

  import GenericTickType._
  /**
   *
   */
  case class ReqMktData(
    tickerId: ReqId,
    contract: Contract,
    genericTickList: List[GenericTickType],
    snapshot: Boolean) extends OutgoingMessage {

    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {

      if (serverVersion < MinServerVer.TradingClass && !contract.tradingClass.isEmpty) {
        fail("tradingClass parameter not supported by this server version")
      }
      wrz(ReqMktData.api)
      wrz(tickerId)
      wrz(contract.conId)
      wrz(contract.symbol, contract.secType, contract.expiry, contract.strike, contract.right)
      wrz(contract.multiplier)
      wrz(contract.exchange, contract.primaryExch)
      wrz(contract.currency)
      wrz(contract.localSymbol)
      if (serverVersion >= MinServerVer.TradingClass) wrz(contract.tradingClass)
      if (contract.secType == SecType.BAG) {
        wrzList(contract.comboLegs) { cl => wrz(cl.conId, cl.ratio, cl.action, cl.exchange) }
      }
      contract.underComp match {
        case Some(uc) => wrz(true, uc.conId, uc.delta, uc.price)
        case None => wrz(false)
      }
      wrz((genericTickList map (v => (v: String))) mkString ",")
      wrz(snapshot)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object ReqMktData { val api = (1, 10) }

  case class CancelMktData(tickerId: ReqId) extends OutgoingMessage {
    import CancelMktData._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(tickerId)
    }
    override def toString: String = nonDefaultNamedValues
  }
  object CancelMktData {
    val api = (2, 1)
  }

  case class ReqMktDepth(tickerId: ReqId, contract: Contract, numRows: Int) extends OutgoingMessage {
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      import contract._
      if (serverVersion < MinServerVer.TradingClass) {
        if (!tradingClass.isEmpty || conId.id > 0) {
          fail("conId and tradingClass parameters not supported by this server version")
        }
      }
      val tc = serverVersion >= MinServerVer.TradingClass
      wrz(ReqMktDepth.api)
      wrz(tickerId)
      if (tc) wrz(conId)
      wrz(symbol, secType, expiry, strike, right, multiplier, exchange, currency, localSymbol)
      if (tc) wrz(tradingClass)
      wrz(numRows)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object ReqMktDepth {
    val api = (10, 4)
  }

  case class CancelMktDepth(tickerId: ReqId) extends OutgoingMessage {
    import CancelMktDepth._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(tickerId)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object CancelMktDepth {
    val api = (11, 1)
  }

  case class ReqNewsBulletins(allMsgs: Boolean) extends OutgoingMessage {
    import ReqNewsBulletins._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(allMsgs)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object ReqNewsBulletins {
    val api = (12, 1)
  }

  case class CancelNewsBulletins() extends OutgoingMessage {
    import CancelNewsBulletins._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object CancelNewsBulletins {
    val api = (13, 1)
  }

  case class SetServerLogLevel(logLevel: ServerLogLevel.ServerLogLevel) extends OutgoingMessage {
    import SetServerLogLevel._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(logLevel)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object SetServerLogLevel {
    val api = (14, 1)
  }

  /**
   *
   */
  case class ReqContractDetails(reqId: ReqId, contract: Contract) extends OutgoingMessage {

    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {

      import ReqContractDetails._
      import contract._

      if (serverVersion < MinServerVer.TradingClass && !tradingClass.isEmpty) {
        fail("tradingClass parameter not supported by this server version")
      }

      wrz(api)

      wrz(reqId)
      wrz(conId)

      wrz(symbol, secType, expiry, strike, right, multiplier, exchange, currency, localSymbol)

      if (serverVersion >= MinServerVer.TradingClass) wrz(tradingClass)

      wrz(includeExpired, secIdType, secId)
    }

    override def toString: String = nonDefaultNamedValues
  }

  object ReqContractDetails {
    val api = (9, 7)
  }

  case class PlaceOrder(id: OrderId = "", contract: Contract, order: Order)
    extends OutgoingMessage {

    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {

      if (serverVersion < MinServerVer.TradingClass && !contract.tradingClass.isEmpty) {
        fail("tradingClass parameter not supported by this server version")
      }
      if (serverVersion < MinServerVer.ScaleTable) {
        if (!order.scaleTable.isEmpty ||
          !order.activeStartTime.isEmpty ||
          !order.activeStopTime.isEmpty) {
          fail("scaleTable, activeStartTime and activeStopTime parameters are not supported by this server version")
        }
      }

      wrz(PlaceOrder.api)
      wrz(id)

      wrz(contract.conId, contract.symbol, contract.secType, contract.expiry, contract.strike,
        contract.right, contract.multiplier, contract.exchange, contract.primaryExch,
        contract.currency, contract.localSymbol)

      if (serverVersion >= MinServerVer.TradingClass) wrz(contract.tradingClass)

      wrz(contract.secIdType, contract.secId)
      wrz(order.action, order.totalQuantity, order.orderType)
      wrz(order.lmtPrice)
      wrz(order.auxPrice)

      wrz(order.tif, order.ocaGroup)
      wrz(order.account, order.openClose, order.origin)
      wrz(order.orderRef, order.transmit, order.parentId)
      wrz(order.blockOrder, order.sweepToFill, order.displaySize, order.triggerMethod, order.outsideRth, order.hidden)

      if (contract.secType == SecType.BAG) {
        wrzList(contract.comboLegs) { cl =>
          wrz(cl.conId, cl.ratio, cl.action, cl.exchange, cl.openClose,
            cl.shortSaleSlot, cl.designatedLocation,
            cl.exemptCode)
        }
        wrzList(order.orderComboLegs) { ocl => wrz(ocl.price) }
        wrzList(order.smartComboRoutingParams) { case (tag, value) => wrz(tag, value) }
      }
      wrz("") // empty deprecated sharesAllocation field
      wrz(order.discretionaryAmt)
      wrz(order.goodAfterTime, order.goodTillDate)
      wrz(order.faGroup, order.faMethod, order.faPercentage, order.faProfile)
      wrz(order.shortSaleSlot, order.designatedLocation)
      wrz(order.exemptCode)
      wrz(order.ocaType)
      wrz(order.rule80A, order.settlingFirm, order.allOrNone, order.minQty,
        order.percentOffset, order.eTradeOnly, order.firmQuoteOnly,
        order.nbboPriceCap, order.auctionStrategy,
        order.startingPrice, order.stockRefPrice, order.delta)
      wrz(order.stockRangeLower, order.stockRangeUpper)
      wrz(order.overridePercentageConstraints)
      wrz(order.volatility, order.volatilityType)
      wrz(order.deltaNeutralOrderType, order.deltaNeutralAuxPrice)
      if (nonEmpty(order.deltaNeutralOrderType)) {
        wrz(order.deltaNeutralConId, order.deltaNeutralSettlingFirm,
          order.deltaNeutralClearingAccount, order.deltaNeutralClearingIntent)
        wrz(order.deltaNeutralOpenClose, order.deltaNeutralShortSale,
          order.deltaNeutralShortSaleSlot, order.deltaNeutralDesignatedLocation)
      }
      wrz(order.continuousUpdate)
      wrz(order.referencePriceType)
      wrz(order.trailStopPrice)
      wrz(order.trailingPercent)

      wrz(order.scaleInitLevelSize, order.scaleSubsLevelSize)
      wrz(order.scalePriceIncrement)
      import MoneyType.ordering._
      if (order.scalePriceIncrement map (_ > MoneyType.zero) getOrElse false) {
        wrz(order.scalePriceAdjustValue, order.scalePriceAdjustInterval, order.scaleProfitOffset,
          order.scaleAutoReset, order.scaleInitPosition,
          order.scaleInitFillQty, order.scaleRandomPercent)
      }
      if (serverVersion >= MinServerVer.ScaleTable) {
        wrz(order.scaleTable, order.activeStartTime, order.activeStopTime)
      }
      wrz(order.hedgeType)
      if (order.hedgeType != HedgeType.Undefined) wrz(order.hedgeParam)
      wrz(order.optOutSmartRouting)
      wrz(order.clearingAccount, order.clearingIntent)
      wrz(order.notHeld)
      contract.underComp match {
        case Some(uc) => wrz(true, uc.conId, uc.delta, uc.price)
        case None => wrz(false)
      }
      wrz(order.algoStrategy)
      if (order.algoStrategy != AlgoStrategy.Undefined)
        wrzList(order.algoParams) {
          case (tag, value) =>
            wrz(tag, value)
        }
      wrz(order.whatIf)
    }

    override def toString: String = nonDefaultNamedValues
  }
  object PlaceOrder {
    val api = (3, 41)
  }

  case class CancelOrder(orderId: OrderId) extends OutgoingMessage {
    import CancelOrder._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(orderId)
    }
    override def toString: String = nonDefaultNamedValues
  }
  object CancelOrder {
    val api = (4, 1)
  }

  case class ReqOpenOrders() extends OutgoingMessage {
    import ReqOpenOrders._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
    }
    override def toString: String = nonDefaultNamedValues
  }
  object ReqOpenOrders {
    val api = (5, 1)
  }
  case class ReqAccountUpdates(subscribe: Boolean, acctCode: String = "") extends OutgoingMessage {
    import ReqAccountUpdates._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(subscribe)
      wrz(acctCode)
    }
    override def toString: String = nonDefaultNamedValues
  }
  object ReqAccountUpdates {
    val api = (6, 2)
  }

  case class ReqExecutions(reqId: ReqId, filter: ExecutionFilter = ExecutionFilter.all) extends OutgoingMessage {
    import ReqExecutions._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(reqId)
      import filter._
      wrz(clientId, acctCode, time, symbol, secType, exchange, side)
    }
    override def toString: String = nonDefaultNamedValues
  }
  object ReqExecutions {
    val api = (7, 3)
  }

  case class ReqAutoOpenOrders(bAutoBind: Boolean) extends OutgoingMessage {
    import ReqAutoOpenOrders._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(bAutoBind)
    }
    override def toString: String = nonDefaultNamedValues
  }
  object ReqAutoOpenOrders {
    val api = (15, 1)
  }
  case class ReqAllOpenOrders() extends OutgoingMessage {
    import ReqAllOpenOrders._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
    }
    override def toString: String = nonDefaultNamedValues
  }
  object ReqAllOpenOrders {
    val api = (16, 1)
  }

  case class ReqHistoricalData(reqId: ReqId, contract: Contract,
    endDateTime: String, durationStr: String,
    barSizeSetting: String, whatToShow: String,
    useRTH: Int,
    formatDate: DateFormatType.DateFormatType) extends OutgoingMessage {
    import ReqHistoricalData._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      import contract._
      if (serverVersion < MinServerVer.TradingClass) {
        if (!tradingClass.isEmpty || conId.id > 0) {
          fail("conId and tradingClass parameters not supported by this server version")
        }
      }
      val tc = serverVersion >= MinServerVer.TradingClass
      wrz(api)
      wrz(reqId)
      if (tc) wrz(conId)
      wrz(symbol, secType, expiry, strike, right, multiplier, exchange, primaryExch, currency,
        localSymbol)
      if (tc) wrz(tradingClass)
      wrz(includeExpired)
      wrz(endDateTime, barSizeSetting)
      wrz(durationStr, useRTH, whatToShow, formatDate)
      if (secType == SecType.BAG) wrzList(comboLegs) { leg => import leg._; wrz(conId, ratio, action, exchange) }
    }
    override def toString: String = nonDefaultNamedValues
  }

  object ReqHistoricalData {
    val api = (20, 5)
  }

  case class CancelHistoricalData(reqId: ReqId) extends OutgoingMessage {
    import CancelHistoricalData._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(reqId)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object CancelHistoricalData {
    val api = (25, 1)
  }

  case class ExerciseOptions(reqId: ReqId, contract: Contract,
    exerciseAction: ExerciseType.ExerciseType, exerciseQuantity: Int,
    account: String, overrideDefaults: Boolean) extends OutgoingMessage {
    import ExerciseOptions._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      import contract._
      if (serverVersion < MinServerVer.TradingClass) {
        if (!tradingClass.isEmpty || conId.id > 0) {
          fail("conId and tradingClass parameters not supported by this server version")
        }
      }
      val tc = serverVersion >= MinServerVer.TradingClass
      wrz(api)
      wrz(reqId)
      if (tc) wrz(conId)
      wrz(symbol, secType, expiry, strike, right, multiplier, exchange, currency, localSymbol)
      if (tc) wrz(tradingClass)
      wrz(exerciseAction, exerciseQuantity, account, overrideDefaults)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object ExerciseOptions {
    val api = (21, 2)
  }

  case class ReqScannerSubscription(reqId: ReqId, subscription: ScannerSubscription) extends OutgoingMessage {
    import ReqScannerSubscription._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {

      wrz(api)
      wrz(reqId)
      import subscription._
      wrz(if (numberOfRows == 0) Int.MaxValue else numberOfRows) // sic - wierd special case
      wrz(instrument, locationCode, scanCode)
      wrz(abovePrice, belowPrice, aboveVolume, marketCapAbove, marketCapBelow)
      wrz(moodyRatingAbove, moodyRatingBelow, spRatingAbove, spRatingBelow)
      wrz(maturityDateAbove, maturityDateBelow, couponRateAbove, couponRateBelow, excludeConvertible)
      wrz(averageOptionVolumeAbove, scannerSettingPairs)
      wrz(stockTypeFilter)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object ReqScannerSubscription {
    val api = (22, 3)
  }

  case class CancelScannerSubscription(reqId: ReqId) extends OutgoingMessage {
    import CancelScannerSubscription._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(reqId)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object CancelScannerSubscription {
    val api = (23, 1)
  }

  case class ReqScannerParameters() extends OutgoingMessage {
    import ReqScannerParameters._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object ReqScannerParameters {
    val api = (24, 1)
  }

  case class ReqCurrentTime() extends OutgoingMessage {
    import ReqCurrentTime._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object ReqCurrentTime {
    val api = (49, 1)
  }

  /**
   *
   */
  case class ReqRealTimeBars(tickerId: ReqId,
    contract: Contract,
    barSize: Int,
    whatToShow: String,
    useRTH: Boolean) extends OutgoingMessage {

    import ReqRealTimeBars._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      import contract._
      if (serverVersion < MinServerVer.TradingClass) {
        if (!tradingClass.isEmpty || conId.id > 0) {
          fail("conId and tradingClass parameters not supported by this server version")
        }
      }
      val tc = serverVersion >= MinServerVer.TradingClass
      wrz(api)
      wrz(tickerId)
      if (tc) wrz(conId)
      wrz(symbol, secType, expiry, strike, right, multiplier,
        exchange, primaryExch, currency, localSymbol)
      if (tc) wrz(tradingClass)
      wrz(barSize, whatToShow, useRTH)
    }

    override def toString: String = nonDefaultNamedValues
  }

  object ReqRealTimeBars {
    val api = (50, 2)
  }

  case class CancelRealTimeBars(tickerId: ReqId) extends OutgoingMessage {

    import CancelRealTimeBars._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(tickerId)
    }

    override def toString: String = nonDefaultNamedValues
  }

  object CancelRealTimeBars {
    val api = (51, 1)
  }

  case class ReqFundamentalData(reqId: ReqId,
    contract: Contract,
    reportType: FundamentalType.FundamentalType) extends OutgoingMessage {

    import ReqFundamentalData._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      import contract._
      if (serverVersion < MinServerVer.TradingClass && conId.id > 0) {
        fail("conId parameter not supported by this server version")
      }
      wrz(api)
      wrz(reqId)
      if (serverVersion >= MinServerVer.TradingClass) wrz(conId)
      wrz(symbol, secType, exchange, primaryExch, currency, localSymbol)
      wrz(reportType)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object ReqFundamentalData {
    val api = (52, 2)
  }

  case class CancelFundamentalData(reqId: ReqId) extends OutgoingMessage {

    import CancelFundamentalData._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(reqId)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object CancelFundamentalData {
    val api = (53, 1)
  }

  case class CalculateImpliedVolatility(reqId: ReqId,
    contract: Contract,
    optionPrice: MoneyType,
    underPrice: MoneyType) extends OutgoingMessage {

    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      import contract._
      if (serverVersion < MinServerVer.TradingClass && !tradingClass.isEmpty) {
        fail("tradingClass parameter not supported by this server version")
      }
      wrz(CalculateImpliedVolatility.api)
      wrz(reqId)
      wrz(conId, symbol, secType, expiry, strike, right, multiplier,
        exchange, primaryExch, currency, localSymbol)
      if (serverVersion >= MinServerVer.TradingClass) wrz(tradingClass)
      wrz(optionPrice, underPrice)
    }

    override def toString: String = nonDefaultNamedValues
  }

  object CalculateImpliedVolatility {
    val api = (54, 2)
  }

  case class CancelCalculateImpliedVolatility(reqId: ReqId) extends OutgoingMessage {

    import CancelCalculateImpliedVolatility._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(reqId)
    }

    override def toString: String = nonDefaultNamedValues
  }

  object CancelCalculateImpliedVolatility {
    val api = (56, 1)
  }

  case class CalculateOptionPrice(reqId: ReqId,
    contract: Contract,
    volatility: Double,
    underPrice: MoneyType) extends OutgoingMessage {

    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      import contract._
      if (serverVersion < MinServerVer.TradingClass && !tradingClass.isEmpty) {
        fail("tradingClass parameter not supported by this server version")
      }
      wrz(CalculateOptionPrice.api)
      wrz(reqId)
      wrz(conId, symbol, secType, expiry, strike, right, multiplier,
        exchange, primaryExch, currency, localSymbol)
      if (serverVersion >= MinServerVer.TradingClass) wrz(tradingClass)
      wrz(volatility, underPrice)
    }

    override def toString: String = nonDefaultNamedValues
  }

  object CalculateOptionPrice {
    val api = (55, 2)
  }

  case class CancelCalculateOptionPrice(reqId: ReqId) extends OutgoingMessage {

    import CancelCalculateOptionPrice._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(reqId)
    }

    override def toString: String = nonDefaultNamedValues
  }

  object CancelCalculateOptionPrice {
    val api = (57, 1)
  }

  case class ReqMarketDataType(marketDataType: MktDataType.MktDataType) extends OutgoingMessage {
    import ReqMarketDataType._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
      wrz(marketDataType)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object ReqMarketDataType {
    val api = (59, 1)
  }

  case class ReqGlobalCancel() extends OutgoingMessage {
    import ReqGlobalCancel._
    override def write(implicit out: OutputStream, serverVersion: Int): Unit = {
      wrz(api)
    }
    override def toString: String = nonDefaultNamedValues
  }

  object ReqGlobalCancel {
    val api = (58, 1)
  }

}