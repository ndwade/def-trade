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

trait StreamsComponent {
  type CMap <: collection.Map[Int, Subscriber[Any]] // some kind of concurrent map
  protected def streams: CMap
}

trait StreamsStub extends StreamsComponent {
  type CMap = collection.Map[Int, Subscriber[Any]]
  override protected lazy val streams: CMap = new collection.Map[Int, Subscriber[Any]] {
    override def get(k: Int): Option[Subscriber[Any]] = None
    override def iterator: Iterator[(Int, Subscriber[Any])] = Iterator.empty
    override def -(key: Int): CMap = streams // ignored 
    override def +[B1 >: Subscriber[Any]](kv: (Int, B1)): collection.Map[Int, B1] = streams // ignored
  }
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
trait Services extends StreamsComponent { self: IbConnectionComponent with OutgoingMessages with IncomingMessages with ConfigSettings =>

  import ServerLogLevel.{ ServerLogLevel }
  /**
    * A [[org.reactivestreams.Publisher]] for a stream of all system messages for a connection.
    *
    * The following messages will be published:
    * - all connection messages ({connect, disconnect} X {OK, Error})
    * - [[io.deftrade.UpdateNewsBullitin]] api messages
    * - [[io.deftrade.CurrentTime]] api messages
    * - [[io.deftrade.Error]] api messages for which the id == -1, or for which an id cannot be
    * found in the id map (which records the ids of all currently outstanding requests). Note:
    * [[Error]] messages whose ids map to an outstanding request are routed to the publisher for the
    * associated responses; those streams will be terminated with an error.
    *
    * The stream will complete normally only when an [[IbDisconnectOk]] message is published (this
    * will be the last message). This "graceful disconnect" is initiated when the stream is
    * `cancel`ed.
    *
    * The stream is completed with an error when an [[IbDisconnectError]]
    * is encountered. // TODO: how to wrap this.
    *
    * More than one connection cannot be created: subsequent Publishers will
    * immediately complete with an error.
    */
  def connection(host: String = settings.ibc.host,
                 port: Int = settings.ibc.port,
                 clientId: Int = settings.ibc.clientId,
                 serverLogLevel: ServerLogLevel = ServerLogLevel.ERROR): Publisher[SystemMessage] = {
    IbPublisher(IbConnect(host, port, clientId)) {
      conn ! SetServerLogLevel(serverLogLevel)
      conn ! ReqIds(1)  // per IB API manual
      conn ! ReqCurrentTime()
      conn ! ReqNewsBulletins(allMsgs = true)
    }
  }

  def scannerParameters(implicit mat: akka.stream.Materializer,
                        ec: scala.concurrent.ExecutionContext): Future[xml.Elem] =
    Future { <bright/> }

  // news bulletins will go to the connection stream. Can be filtered out / replicated from there.
  // def news: Publisher[UpdateNewsBulletin]

  // TODO: idea: a message bus for exchanges; subscribe to an exchange and get available / not available
  // also: for data farms

  /*
   * ReferenceData 
   */

  // Will often be used with .toFuture, but want to allow for streaming directly into DB
  /*
   * wrz(symbol, secType, expiry, strike, right, multiplier, exchange, currency, localSymbol)

      if (serverVersion >= MinServerVer.TradingClass) wrz(tradingClass)

      wrz(includeExpired, secIdType, secId)
   */
  def contractDetails(contract: Contract): Publisher[ContractDetails]

  // TODO: verify this really has RPC semantics.
  // cancel if timeout?

  /**
    * @param contract
    * @param reportType
    * @param mat
    * @param ec
    * @return
    */
  def fundamentals(
    contract: Contract,
    reportType: FundamentalType.Value)(implicit mat: akka.stream.Materializer,
                                       ec: scala.concurrent.ExecutionContext): Future[xml.Elem] = {
    import Services.PublisherToFuture

    IbPublisher[String](
      ReqFundamentalData(ReqId.next, contract, reportType))().toFuture map { ss =>
        xml.XML.load(ss.head)
      }
  }

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

    IbPublisher(ReqMktData(ReqId.next, contract, genericTickList, snapshot))()
  }

  import WhatToShow.WhatToShow
  def bars(contract: Contract, whatToShow: WhatToShow): Publisher[RealTimeBar] = {
    IbPublisher(ReqRealTimeBars(ReqId.next, contract, 5, whatToShow, true))()
  }

  def optionPrice(contract: Contract, volatility: Double): Publisher[TickOptionComputation] = ???

  def impliedVolatility(contract: Contract, optionPrice: Double): Publisher[TickOptionComputation] = ???

  def depth(contract: Contract, rows: Int): Publisher[MarketDepthMessage] = ???

  def scan(params: ScannerParameters): Publisher[ScannerData]

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

    IbPublisher(
      ReqHistoricalData(
        ReqId.next,
        contract,
        end.toString, // FIXME LMAO
        duration.toString, // FIXME
        barSize,
        whatToShow,
        if (regularHoursOnly) 1 else 0,
        DateFormatType.SecondsSinceEpoch))()
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

  /**
    * Request that all open orders be canceled.
    *
    * @return A [[scala.concurrent.Future]] which indicates successful transmission
    * if the cancel request if completed successfully,
    *  or with a failure in the (unlikely) event that the cancel request could not be transmitted.
    *  Note that a successful completion of the `Future` does 'not' mean that all open
    *  orders were in fact canceled.
    */
  def cancelAll(): Future[Unit] = ???

  /*
   * Internals. TODO: review scoping
   */
  import scala.language.existentials

  type CMap = concurrent.Map[Int, Subscriber[Any]]
  override protected lazy val streams: CMap = concurrent.TrieMap.empty[Int, Subscriber[Any]]

  type Msg = HasRawId with Cancellable 

  case class IbPublisher[T](msg: Msg)(coda: => Unit = {}) extends Publisher[T] {

    import java.util.concurrent.atomic.AtomicBoolean

    val subscribed = new AtomicBoolean(false) // no anticipation of race but why take chances
    override def subscribe(subscriber: Subscriber[_ >: T]): Unit = {
      if (subscriber == null) throw new NullPointerException("null Subscriber")
      val first = !subscribed.getAndSet(true)
      if (first) subscriber onSubscribe IbSubscription(subscriber.asInstanceOf[Subscriber[Any]], msg, () => coda)
    }
  }

  case class IbSubscription(subscriber: Subscriber[Any], msg: Msg, coda: () => Unit) extends Subscription {

    private var r = (n: Long) => {
      require(n == Long.MaxValue) // FIXME: log this in production, no assertion  
      streams + (msg.rawId -> subscriber)
      conn ! msg
      coda()
    }

    override def request(n: Long): Unit = { r(n); r = _ => () }

    private var c = () => {
      streams - msg.rawId
      conn ! msg.cancelMessage
    }
    override def cancel(): Unit = { c(); c = () => () }
  }

  //  object TicksToBar {
  //    import java.time._
  //
  //    val zdt: ZonedDateTime = ???
  //    val dur: Duration = ???
  //  }

}

object Services {

  import org.reactivestreams.Publisher
  import akka.stream.scaladsl.{ Source, Flow, Sink }

  implicit class PublisherToFuture[M](publisher: Publisher[M])(implicit mat: akka.stream.Materializer) {
    def toFuture: Future[List[M]] = {
      Source.fromPublisher(publisher).fold(List.empty[M]) { (u, t) => t :: u } map (_.reverse) runWith Sink.head
    }

    // def toFutureSeq: Future[Seq[M]] = Source(publisher).grouped(Int.MaxValue) runWith Sink.head
  }
}