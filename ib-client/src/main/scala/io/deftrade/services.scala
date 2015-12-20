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
import scala.collection.concurrent
import scala.xml
import org.reactivestreams.{ Publisher, Subscriber, Subscription }

trait StreamsMap {
  def streams: concurrent.Map[Int, Subscriber[Any]]
}

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
trait Services extends StreamsMap { self: IbConnectionComponent with OutgoingMessages with ConfigSettings =>

  def connect(): Either[IbConnectError, IbConnectOk]

  def scannerParameters: xml.Document

  def news: Publisher[UpdateNewsBulletin]

  // TODO: idea: a message bus for exchanges; subscribe to an exchange and get available / not available
  // also: for data farms

  /*
 * ReferenceData 
 */

  // Will often be used with .toFuture, but want to allow for streaming directly into DB
  def contractDetails(contract: Contract): Publisher[ContractDetails]

  // TODO: verify this really has RPC semantics.
  // cancel if timeout?
  // careful not to parse XML on socket receiver thread
  def fundamentals(contract: Contract): Future[xml.Document]

  /*
   * MarketData 
   */

  case class TockedTick[T <: RawTickMessage](val ts: Long, tick: T) // maybe just store in db?
  /*
   * use cases:
   * - not every use of a tick requires a timestamp - e.g. a trading algo based on raw ticks
   * only has a sense of "now" - so adding a timestamp to all ticks is needless delay
   * - however, reconstructing some derived streams (e.g bars from ticks) from persisted data
   * will require the persistence of a timestamp per tick (e.g. a TockedTick
   */

  def ticks(contract: Contract,
    genericTickList: List[GenericTickType.GenericTickType],
    snapshot: Boolean = false): Publisher[RawTickMessage] = {

    val reqId = ReqId.next
    IbPublisher(ReqMktData(reqId, contract, genericTickList, snapshot), CancelMktData(reqId))
  }

  import WhatToShow.WhatToShow
  def bars(contract: Contract, whatToShow: WhatToShow): Publisher[RealTimeBar] = {
    val reqId = ReqId.next()
    IbPublisher(ReqRealTimeBars(reqId, contract, 5, whatToShow, true), CancelRealTimeBars(reqId))
  }

  def optionPrice(contract: Contract, volatility: Double): Publisher[TickOptionComputation] = ???

  def impliedVolatility(contract: Contract, optionPrice: Double): Publisher[TickOptionComputation] = ???

  def depth(contract: Contract, rows: Int): Publisher[MarketDepthMessage] = ???

  def scan(params: ScannerParameters): Future[List[ScannerData]] // TODO: verify RPC semantics

  // TODO: deal with requesting news. How is news returned?  
  // See https://www.interactivebrokers.com/en/software/api/apiguide/tables/requestingnews.htm
  def mdNews(): Publisher[Null] = ???

  /*
   * HistoricalData 
   */
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
    regularHoursOnly: Boolean = true): Publisher[HistoricalData] = {

    val reqId = ReqId.next()
    IbPublisher(
      ReqHistoricalData(reqId, contract,
        end.toString, // FIXME LMAO
        duration.toString, // FIXME
        barSize,
        whatToShow,
        if (regularHoursOnly) 1 else 0,
        DateFormatType.SecondsSinceEpoch),
      CancelHistoricalData(reqId))

  }

  // implementation note: the formatDate field is hardwired to 2 (epoch seconds count)
  // the HistoricalData responce data should use DateTimes, not strings. 
  // the connection logic can reply with both (legacy to the EventBus for logging and
  // testability; and an "opinionated" version for the higher level interface

  // IB measures the effectiveness of client orders through the Order Efficiency Ratio (OER).  
  // This ratio compares aggregate daily order activity relative to that portion of activity 
  // which results in an execution and is determined as follows:
  // OER = (Order Submissions + Order Revisions + Order Cancellations) / (Executed Orders + 1)
  // see http://ibkb.interactivebrokers.com/article/1765

  /*
 * OrderManager {
 */
  def cancelAll(): Unit

  /*
   * 
   */
  import scala.language.existentials

  type Msg = OutgoingMessage with HasReqId

  override val streams: concurrent.Map[Int, Subscriber[Any]] = concurrent.TrieMap.empty[Int, Subscriber[Any]]

  case class IbPublisher[T](req: Msg, cnc: Msg) extends Publisher[T] {

    import java.util.concurrent.atomic.AtomicBoolean
    
    val subscribed = new AtomicBoolean(false) // no anticipation of race but why take chances
    override def subscribe(subscriber: Subscriber[_ >: T]): Unit = {
      val first = !subscribed.getAndSet(true)
      if (first) subscriber onSubscribe IbSubscription(subscriber.asInstanceOf[Subscriber[Any]], req, cnc)
    }
  }

  case class IbSubscription(subscriber: Subscriber[Any], req: Msg, cnc: Msg) extends Subscription {
    require(req.reqId == cnc.reqId)

    private var r = (n: Long) => {
      require(n == Long.MaxValue) // FIXME: log this in production, no assertion  
      streams += ((req.reqId.raw, subscriber))
      conn ! req
    }

    override def request(n: Long): Unit = { r(n); r = _ => () }

    private var c = () => {
      streams -= req.reqId.raw
      conn ! cnc
    }
    override def cancel(): Unit = { c(); c = () => () }
  }

  import akka.stream.ActorMaterializer
  import org.reactivestreams.Publisher
  import akka.stream.scaladsl.{ Source, Flow, Sink }

  implicit val _system = self.system
  implicit val exectutor = _system.dispatcher
  implicit val materializer = ActorMaterializer()

  implicit class PublisherToFuture[M](publisher: Publisher[M]) {
    def toFuture: Future[List[M]] = {
      Source(publisher).fold(List.empty[M]) { (u, t) => t :: u } map (_.reverse) runWith Sink.head
    }

    // def toFutureSeq: Future[Seq[M]] = Source(publisher).grouped(Int.MaxValue) runWith Sink.head
  }

  object TicksToBar {
    import java.time._

    val zdt: ZonedDateTime = ???
    val dur: Duration = ???
  }

  /*
   * 
   */

}
