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

import java.{ time => jt }
import scala.concurrent.Future
import scala.xml
import org.reactivestreams.Publisher

/*
 * prologue:
 * connect (?)
 * ReqCurrentTime() -> just log diff between this and now()
 * ReqIds() -> route response directly to OrderManagementSystem
 * ReqOpenOrders() / ReqExecutions()
 * SetServerLogLevel (based on config params) 
 * ReqMarketDataType (based on config params)
 * ReqNewsBullitins (based on config params)
 * ReqScannerParameters (based on config params) - hold in a well know location as XML
 * ReqAccountUpdates() etc (based on config params) - sets up service for Account, Positions etc.
 */
trait SystemManager { self: DTOs with IncomingMessages =>
  implicit class PublisherToFuture[Msg](p: Publisher[Msg]) {
    def toFuture: Future[List[Msg]] = {
      // start a SubscriberActor which will receive stream, fulfill Promise on completion 
      ???
    }
  }
  
  def scannerParameters: xml.Document
  
  def news: Publisher[UpdateNewsBulletin]
  
  // TODO: idea: a message bus for exchanges; subscribe to an exchange and get available / not available
  // also: for data farms
  
}

trait ReferenceData { self: DTOs with IncomingMessages =>
  
  // Will often be used with .toFuture, but want to allow for streaming directly into DB
  def contractDetails(contract: Contract): Publisher[ContractDetails]
  
  // TODO: verify this really has RPC semantics.
  // cancel if timeout?
  // careful not to parse XML on socket receiver thread
  def fundamentals(contract: Contract): Future[xml.Document]
}
trait MarketData { self: DTOs with IncomingMessages =>

  def ticks(contract: Contract, genericTickList: List[GenericTickType.GenericTickType], snapshot: Boolean = false): Publisher[RawTickMessage]

  def bars(contract: Contract): Publisher[RealTimeBar]

  def optionPrice(contract: Contract, volatility: Double): Publisher[TickOptionComputation]

  def impliedVolatility(contract: Contract, optionPrice: Double): Publisher[TickOptionComputation]
  
  def depth(contract: Contract, rows: Int): Publisher[MarketDepthMessage]
  
  def scan(params: ScannerParameters): Future[List[ScannerData]] // TODO: verify RPC semantics

  // TODO: deal with requesting news. How is news returned?  
  // See https://www.interactivebrokers.com/en/software/api/apiguide/tables/requestingnews.htm
  def news(): Publisher[Nothing] = ???
}

trait HistoricalData { self: DTOs with IncomingMessages =>
  import BarSize._
  import WhatToShow._

  /*
   * High level historical data service. Lower level API limitations and rate limiting is 
   * handled within the service; just request what you want.
   */
  def hdBars(contract: Contract,
    end: jt.ZonedDateTime,
    duration: jt.Duration,
    barSize: BarSize,
    whatToShow: WhatToShow,
    regularHoursOnly: Boolean = true): Publisher[HistoricalData]

  // implementation note: the formatDate field is hardwired to 2 (epoch seconds count)
  // the HistoricalData responce data should use DateTimes, not strings. 
  // the connection logic can reply with both (legacy to the EventBus for logging and
  // testability; and an "opinionated" version for the higher level interface
}


// TODO: should calculate the 1/20 ratio of executed orders per order/order-mod
// see http://ibkb.interactivebrokers.com/article/1765
trait OrderManager {
  def cancelAll(): Unit
}

