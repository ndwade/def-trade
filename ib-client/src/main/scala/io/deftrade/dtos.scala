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

import NonDefaultNamedValues.nonDefaultNamedValues

/**
 * Data Transfer Objects for the IB API.
 *
 * Superficially isomorphic to the IB API Java classes, but in this library the DTOs (Contract,
 * Order etc) are immutable (case classes) and thus better suited as messages in an Actor system.
 *
 * Some refactoring is required on the original IB DTOs to respect the 22 member limit on
 * case classes in Scala.
 *
 * The default values for each member are (in general) those which would result from
 * conversion from an empty String - the object is constructed "as if" the instance were read
 * from a stream consisting of empty (zero length) strings.
 *
 * For certain fields which are never received but which _are_ transmitted, other defaults
 * may make sense.
 *
 * ISSUE: testing - the mapping of ordinals to semantics with the enums isn't tested, not sure
 * it would be testable without very deep integration testing which is likely impossible with
 * the IB API. Encourage users to check this. Really in no worse shape than the existing API
 * since they'd be using raw constants. Should use IB defined constants wherever possible.
 *
 */

import Right._, Action._, SecType._, SecIdType._, OpenClose._, ShortSaleSlot._, OrderType._
import TimeInForce._, OcaType._, TriggerMethod._, Rule80A._, OrderInstOpenClose._, Origin._
import AuctionStrategy._, VolType._, ReferencePriceType._, HedgeType._, ClearingIntent._
import AllocationMethod._, AlgoStrategy._, OrderStatusEnum._, ExecAction._
import Currency._

import ImplicitConversions._

/*    ``` 
     *  Usage of each of the Data Transfer Object classes:
     *  
     *  Class            Transmit/Receive
     *  =================================
     *  CommissionReport   	//    RX
     * 	ContractDetails		  //    RX
     * 		Contract, 		    // TX/RX
     * 		  V[ComboLeg], 	  // TX
     * 		  O[UnderComp],	  // TX/RX
     * 	Order, 				      // TX/RX
     * 		V[TagValue]		    // TX/RX	// algo params
     * 		V[TagValue]		    // TX/RX	// SMART combo routing params
     *   	V[OCL]			      // TX/RX
     * 	OrderState			    //    RX
     *  OrderComboLeg		    // TX/RX
     *  Execution			      //    RX
     * 	ExecutionFilter		  // TX
     * 	ScannerSubscription // TX		// n.b. this one needs getter/setter...
     * ```
     */

sealed trait DTO

case class UnderComp(
    conId: ConId = "",
    delta: Double = "",
    price: Double = "") extends DTO {

  override def toString: String = nonDefaultNamedValues

}
/*
   *  Contract field usage:
   *
   * EClientSocket:
   *   reqContractDetails(): all but comboLegs* and underComp
   *   reqFunamentalData(): symbol, secType, exchange, primaryExch, currency, localSymbol
   *   reqHistoricalData(): all except id, secIdType, incl Combo except short sale stuff and openClose
   *   reqMktData(): everything?!
   *   reqMktDepth(): like reqFundamentalData()
   *   reqRealTimeBars(); like reqFundamentalData()
   *
   * EClientWrapper:
   * 	Contract is input as a field in ContractDetails, in:
   * 	scannerData(), contractData(), bondContractData()
   */
case class Contract(

    conId: ConId = "",
    symbol: String = "",
    secType: SecType = "",
    expiry: String = "",
    strike: Double = "",
    right: Right = "",
    multiplier: String = "",
    exchange: String = "",

    currency: Currency = "",
    localSymbol: String = "",
    tradingClass: String = "",
    primaryExch: String = "",
    includeExpired: Boolean = "",

    secIdType: SecIdType = SecIdType.Undefined,
    secId: String = "",

    comboLegsDescrip: String = "",
    comboLegs: List[ComboLeg] = Nil,

    underComp: Option[UnderComp] = None) extends DTO {

  override def toString: String = nonDefaultNamedValues
}

object Contract {
  /**
   * Convenience method for creating Contract objects with combo legs.
   */
  def combo(
    symbol: String,
    exchange: String = "SMART",
    currency: Currency = Currency.USD)(
      legs: ComboLeg*): Contract = Contract(
    symbol = symbol, exchange = exchange, currency = currency,
    secType = BAG, comboLegs = legs.toList)
}

/**
 * ComboLeg.
 */
case class ComboLeg(
    conId: ConId = "",
    ratio: Int = "",
    action: Action = "",
    exchange: String = "",
    openClose: OpenClose = "",
    shortSaleSlot: ShortSaleSlot = "",
    designatedLocation: String = "",
    exemptCode: Int = -1) extends DTO {

  override def toString: String = nonDefaultNamedValues

}

case class ContractDetails(
    summary: Contract = Contract(), // looks weird but intentional - aids homologation.
    marketName: String = "",
    minTick: Double = "",
    priceMagnifier: Int = "",
    orderTypes: String = "", // parse into EnumSet? NO - put util in Enum class 
    validExchanges: String = "",
    underConId: ConId = "",
    longName: String = "",
    contractMonth: String = "",
    industry: String = "",
    category: String = "",
    subcategory: String = "",
    timeZoneId: String = "", // TODO: enum candidate?
    tradingHours: String = "", // TODO: TimeType? or whatnot...
    liquidHours: String = "",
    evRule: String = "",
    evMultiplier: Double = "",
    secIdList: List[(String, String)] = Nil,
    cusip: String = "",
    ratings: String = "",
    descAppend: String = "",
    bondType: String = "",
    couponType: String = "",
    callable: Boolean = "",
    putable: Boolean = "",
    coupon: Double = "",
    convertible: Boolean = "",
    maturity: String = "",
    issueDate: String = "",
    nextOptionDate: String = "",
    nextOptionType: String = "",
    nextOptionPartial: Boolean = "",
    notes: String = "") extends DTO {

  override def toString: String = nonDefaultNamedValues
}

import Order._

/**
 * Order
 */
case class Order(

    // main order fields
    orderId: OrderId = "",
    clientId: Int = "",
    permId: Int = "",
    action: Action = "",
    totalQuantity: Int = "",
    orderType: OrderType = "",
    lmtPrice: Option[Double] = "",
    auxPrice: Option[Double] = "",

    // extended order fields
    tif: TimeInForce = "",
    activeStartTime: String = "",
    activeStopTime: String = "",
    ocaGroup: String = "",
    ocaType: OcaType = "",
    orderRef: String = "",
    transmit: Boolean = true,
    parentId: OrderId = OrderId(0),
    blockOrder: Boolean = "",
    sweepToFill: Boolean = "",
    displaySize: Int = "",
    triggerMethod: TriggerMethod = "",
    outsideRth: Boolean = "",
    hidden: Boolean = "",
    goodAfterTime: String = "", // FORMAT: 20060505 08:00:00 {time zone}
    goodTillDate: String = "", // FORMAT: 20060505 08:00:00 {time zone}
    overridePercentageConstraints: Boolean = "",
    rule80A: Rule80A = "",
    allOrNone: Boolean = "",
    minQty: Option[Int] = "",
    percentOffset: Option[Double] = "",
    trailStopPrice: Option[Double] = "",
    trailingPercent: Option[Double] = "",

    // Financial advisors only 
    faGroup: String = "",
    faProfile: String = "",
    faMethod: AllocationMethod = "",
    faPercentage: String = "",

    // Institutional orders only
    openClose: OrderInstOpenClose = OrderInstOpenClose.OPEN,
    origin: Origin = Customer,
    shortSaleSlot: ShortSaleSlot = NA,
    designatedLocation: String = "", // set when slot=2 only.
    exemptCode: Int = -1,

    // Institutional orders only
    discretionaryAmt: Double = "",
    eTradeOnly: Boolean = "",
    firmQuoteOnly: Boolean = "",
    nbboPriceCap: Option[Double] = "",
    optOutSmartRouting: Boolean = "",

    // BOX or VOL ORDERS ONLY
    auctionStrategy: AuctionStrategy = "",

    // BOX ORDERS ONLY
    startingPrice: Option[Double] = "",
    stockRefPrice: Option[Double] = "",
    delta: Option[Double] = "",

    // pegged to stock or VOL orders
    stockRangeLower: Option[Double] = "",
    stockRangeUpper: Option[Double] = "",

    // VOLATILITY ORDERS ONLY
    volatility: Option[Double] = "",
    volatilityType: VolType = "", // subtle bug in ib code: int on rec but intMax on send
    continuousUpdate: Int = "",
    referencePriceType: ReferencePriceType = "", // same ib bug as VolType above

    deltaNeutralOrderType: OrderType = "",
    deltaNeutralAuxPrice: Option[Double] = "",
    deltaNeutralConId: ConId = "",
    deltaNeutralSettlingFirm: String = "",
    deltaNeutralClearingAccount: String = "",
    deltaNeutralClearingIntent: String = "",
    deltaNeutralOpenClose: String = "", // TODO: no clear documentation on acceptable values
    deltaNeutralShortSale: Boolean = "",
    deltaNeutralShortSaleSlot: ShortSaleSlot = NA,
    deltaNeutralDesignatedLocation: String = "",

    // COMBO ORDERS ONLY
    basisPoints: Option[Double] = "",
    basisPointsType: Option[Int] = "", // TODO: should be enum. IB documentation?!

    // SCALE ORDERS ONLY
    scaleInitLevelSize: Option[Int] = "",
    scaleSubsLevelSize: Option[Int] = "",
    scalePriceIncrement: Option[Double] = "",
    scalePriceAdjustValue: Option[Double] = "",
    scalePriceAdjustInterval: Option[Int] = "",
    scaleProfitOffset: Option[Double] = "",
    scaleAutoReset: Boolean = "",
    scaleInitPosition: Option[Int] = "",
    scaleInitFillQty: Option[Int] = "",
    scaleRandomPercent: Boolean = "",
    scaleTable: String = "",

    // HEDGE ORDERS ONLY
    hedgeType: HedgeType = "",
    hedgeParam: String = "", // TODO: IB has this as String, should be double?!

    // Clearing info
    account: String = "",
    settlingFirm: String = "",
    clearingAccount: String = "",
    clearingIntent: ClearingIntent = "",

    // ALGO ORDERS ONLY
    algoStrategy: AlgoStrategy = "",
    algoParams: List[(String, String)] = Nil,

    whatIf: Boolean = "",
    notHeld: Boolean = "",

    // Smart combo routing params
    smartComboRoutingParams: List[(String, String)] = Nil,

    orderComboLegs: List[OrderComboLeg] = Nil) extends DTO {

  override def toString: String = nonDefaultNamedValues
}

object Order {

  def algoParams(ps: (String, String)*) = List(ps: _*)

}

case class OrderComboLeg(price: Option[Double]) extends DTO {
  override def toString: String = nonDefaultNamedValues
}

case class CommissionReport( //
    execId: String = "",
    commission: Double = "",
    currency: Currency = "",
    realizedPNL: Double = "",
    `yield`: Double = "", // TODO: revisit name - unfortunate keyword collision
    yieldRedemptionDate: Int // YYYYMMDD format	TODO: date type?
    ) extends DTO {
  override def toString: String = nonDefaultNamedValues
}

case class Execution(

    orderId: OrderId = "",
    clientId: Int = "", // TODO: clientId as a GenId?
    execId: String = "",
    time: String = "", // TODO: time format?
    acctNumber: String = "",
    exchange: String = "",
    side: ExecAction = "",
    shares: Int = "",
    price: Double = "",
    permId: Int = "", // FIXME: PermId should be GenId opaque
    liquidation: Int = "", // TODO: is this effectively a Boolean?
    cumQty: Int = "",
    avgPrice: Double = "",
    orderRef: String = "",
    evRule: String = "",
    evMultiplier: Double = "") extends DTO {

  override def toString: String = nonDefaultNamedValues
}

case class ExecutionFilter(
    clientId: Int = "",
    acctCode: String = "",
    time: String = "", // TODO: format? Time type?
    symbol: String = "",
    secType: SecType = "",
    exchange: String = "",
    side: Action = "") extends DTO {
  override def toString: String = nonDefaultNamedValues
}

object ExecutionFilter {
  lazy val all = ExecutionFilter()
}

/**
 * Order state is only a field of an IncomingMessage, so there are no default values.
 */
case class OrderState(
    status: OrderStatusEnum = "",
    initMargin: String = "",
    maintMargin: String = "",
    equityWithLoan: String = "",
    commission: Option[Double] = "",
    minCommission: Option[Double] = "",
    maxCommission: Option[Double] = "",
    commissionCurrency: Currency = "",
    warningText: String = "") extends DTO {
  override def toString: String = nonDefaultNamedValues
}

/**
 * Since ScannerSubscription is transmit only, defaults are not specified by conversion
 * from empty String. In most cases this works out to the same value though (e.g. [None]).
 *
 * TODO: Some of these seem ripe for Enumerations, e.g. spRating etc...
 * For StockTypeFilter:
 * Valid values are:
 * - CORP = Corporation
 * - ADR = American Depositary Receipt
 * - ETF = Exchange Traded Fund
 * - REIT = Real Estate Investment Trust
 * - CEF = Closed End Fund
 * ... but: can you use lists?
 */
case class ScannerSubscription(
    numberOfRows: Int = ScannerSubscription.NO_ROW_NUMBER_SPECIFIED,
    instrument: String = "",
    locationCode: String = "",
    scanCode: String = "",
    abovePrice: Option[Double] = None,
    belowPrice: Option[Double] = None,
    aboveVolume: Option[Int] = None,
    averageOptionVolumeAbove: Option[Int] = None,
    marketCapAbove: Option[Double] = None,
    marketCapBelow: Option[Double] = None,
    moodyRatingAbove: String = "",
    moodyRatingBelow: String = "",
    spRatingAbove: String = "",
    spRatingBelow: String = "",
    maturityDateAbove: String = "",
    maturityDateBelow: String = "",
    couponRateAbove: Option[Double] = None,
    couponRateBelow: Option[Double] = None,
    excludeConvertible: String = "",
    scannerSettingPairs: String = "",
    stockTypeFilter: String = "") extends DTO {

  override def toString: String = nonDefaultNamedValues
}

object ScannerSubscription {
  val NO_ROW_NUMBER_SPECIFIED = -1
}
