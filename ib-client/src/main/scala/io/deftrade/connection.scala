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

import java.io.{ InputStream, DataInputStream, OutputStream, IOException, EOFException }
import java.net.Socket
import scala.util.{ Try, Success, Failure }
import scala.util.control.{ NonFatal, ControlThrowable }
import scala.concurrent.{ Future, ExecutionContext }
import akka.actor._
import akka.event.{ ActorEventBus, SubchannelClassification }
import akka.util.Subclassification

class Subscriptions(system: ActorSystem) extends ActorEventBus with SubchannelClassification {

  type Event = AnyRef
  type Classifier = Class[_]

  import ActorDSL._
  private case class Subscribed(subscriber: ActorRef)

  /*
   * arguably dodgy because the reaper actor closes over the "this" pointer of the Subscriptions
   * instance... pretty sure this is OK in this instance but generally not best practice. 
   */
  private val reaper = actor(system, "subscription-reaper") {
    new Act {
      become {
        case Subscribed(subscriber) => context.watch(subscriber)
        case Terminated(subscriber) => unsubscribe(subscriber)
      }
    }
  }

  protected implicit val subclassification = new Subclassification[Class[_]] {
    def isEqual(x: Class[_], y: Class[_]) = x == y
    def isSubclass(x: Class[_], y: Class[_]) = y isAssignableFrom x
  }

  protected def classify(event: Event): Class[_] = event.getClass

  protected def publish(event: Event, subscriber: ActorRef) = subscriber ! event

  override def subscribe(subscriber: Subscriber, to: Classifier): Boolean = {
    val ret = super.subscribe(subscriber, to)
    // subscriber may die before watch() - OK because Terminated will still be received by reaper
    if (ret) reaper ! Subscribed(subscriber)
    ret
  }

}

trait SubscriptionsComponent {
  def subs: Subscriptions
}

/**
 * Implements the IbConnection actor, which manages the connection to TWS.
 */
abstract class IbConnectionComponent(val system: ActorSystem)
  extends SubscriptionsComponent with DTOs with IncomingMessages with OutgoingMessages {

  _: DomainTypesComponent =>

  //  /**
  //   * Base type for EventBus, to be refined by client extensions
  //   */
  //  type EventBusType <: ActorEventBus { type Event = AnyRef /* IncommingMessage*/ }
  //
  //  /**
  //   * Factory method for event bus, to be implemented by client extensions
  //   */
  //  def eventBus: EventBusType

  /**
   * An EventBus instance which holds all the subscriptions for message deliver.
   */
  //  final val subs: ActorEventBus = new Subscriptions
  val subs = new Subscriptions(system)
  /**
   * The Actor (and FSM) which manages the connection state, reads and writes messages to the
   * socket, and publishes TWS API messages, connection status messages and error messages.
   */
  final val conn: ActorRef = system.actorOf(Props(new IbConnection), name = "ib-conn")

  /**
   * Minimum TWS API client version which clients of this library may be written against.
   */
  val clientVersion: Int = 62

  /**
   * Minimum TWS API server version which this library supports. Note, this must never be less
   * than 20.
   */
  val minServerVersion = 66

  /**
   * An Actor based FSM which manages the connection to the TWS API socket.
   *
   * This actor will connect and disconnect based on command messages, and will send
   * OutgoingMessages to the API socket when connected. On connection, a dedicated thread is
   * created which reads messages from the socket and publishes them.
   *
   * Clients who interact with the Connection are assumed to be subscribed to ConnectionMessages
   * and ErrorMessages. There should be no need to subscribe to the FSM state transitions,
   * but that can be dome as well.
   *
   * FIXME: what happens when shutdown hits an FSM? Want to close the socket cleanly.
   */
  class IbConnection extends Actor with LoggingFSM[IbcImpl.State, IbcImpl.Data] {

    import IbcImpl._
    import ErrorMessage._
    import IncomingMessages.rdz
    import OutgoingMessages.{ wrz, OutgoingMessageValidationException }

    val executor = new misc.SingleThreadPool()

    /*
     * 
     */
    startWith(Disconnected, Empty)

    /*
     * wait for a connect message
     */
    when(Disconnected) {

      /*
       * connect
       */
      case Event(ibconn @ IbConnect(host, port, clientId), _) =>

        import ImplicitConversions.{ s2i, i2s }
        import ExecutionContext.Implicits.global
        import misc.silentClose

        {
          for {
            socket <- Future { new Socket(host, port) }
            data <- Future { // separate Future so socket can be closed
              val os = socket.getOutputStream
              wrz(clientVersion)(os)
              val dis = new DataInputStream(socket.getInputStream)
              IncomingMessages.resetNrdz()
              val serverVersion: Int = rdz(dis)
              val reader = new Reader()(dis, serverVersion) // made explicit for clarity
              val twsTime: String = rdz(dis)
              if (serverVersion < minServerVersion) {
                silentClose(socket)
                subs publish IbConnectError(ReasonUpdateTWS, sender, ibconn)
                Empty
              } else { // TODO: Log the connection
                wrz(clientId)(os)
                Connection(os, reader, IbConnectOk(sender, ibconn, serverVersion, twsTime))
              }
            } recoverWith { // second Future failed; need to close the socket
              case ex => silentClose(socket); Future.failed(ex)
            }
          } yield data

        } onComplete {
          case Success(data) =>
            conn ! ConnectData(data)
          case Failure(ex) =>
            conn ! ConnectData(Empty)
            subs publish IbConnectError(ReasonEx, sender, ibconn, Some(ex))
        }

        goto(ConnectPending) using Empty
    }

    /*
     * wait for Futures to send the connection data
     * starting the reader here makes error recovery easier; 
     * we wait to start it after we know the rest has completed OK.
     */
    when(ConnectPending) {

      /*
       * connection attempt succeeded 
       */
      case Event(ConnectData(data @ Connection(_, reader, _)), _) =>
        //        new Thread(reader).start()
        executor.execute(reader)
        goto(Connected) using data

      /*
       * connection attempt failed - Futures published errors
       */
      case Event(ConnectData(Empty), _) =>
        goto(Disconnected) using Empty

    }

    onTransition {
      case ConnectPending -> Connected =>
        nextStateData match {
          case Connection(_, _, ok) => subs publish ok
          case wtf => {
            log.error(s"Connecting with bad Data: $wtf")
            throw new AssertionError("internal error: see logfile") // something's badly confused
          }
        }
    }

    /*
     * write all outgoing messages to the socket
     * wait for a disconnect message
     */
    when(Connected) {

      /*
       * Justification for the @unchecked annotation.
       * the unchecked warning is also suppressed if we use type projection for OutgoingMessage
       * this is ok because 
       * - it says _exactly_ what the pattern matcher does - it can't check the outer ref
       * - is suppresses the following warning:
       * 	- "The outer reference in this type test cannot be checked at run time"
       *  	- this is as of scala 2.10.1 - follow up, it's unstable.
       * - problem is that then case classes expecting Ib.ConnectionComponent.this.OutgoingMessage
       * don't type check.
       * So just live with the @unchecked... it's close enough until the scalac issue stabilizes.
       *   
       */
      //      case Event(msg: Messages#OutgoingMessage, Connection(os, _, _)) =>
      case Event(msg: OutgoingMessage @unchecked, Connection(os, _, IbConnectOk(_, _, sv, _))) =>
        import OutgoingError._
        try {
          msg.write(os, sv)
          os.flush() // likely superfluous, hopefully harmless
          stay
        } catch {
          case omvx: OutgoingMessageValidationException =>
            subs publish OutgoingError(ReasonEx, sender, msg, Some(omvx))
            stay
          case NonFatal(ex) =>
            subs publish OutgoingError(ReasonEx, sender, msg, Some(ex))
            goto(Disconnected) using Disconnection(
              IbDisconnectError(ReasonEx, Some(sender), Some(msg), Some(ex)))
        }

      case Event(IbDisconnect(why), Connection(os, reader, _)) =>
        // n.b. - the Thread.interrupt handshake in the com.ib.client.EClientSocket code is 
        // completely pointless - blocking reads from a socket InputStream do not get interrupted.
        // EClientSocket almost always closes by whacking the socket, causing the EReader to throw. 
        misc.silentClose(os) // kills the Reader - throws at next invocation of is.read
        goto(DisconnectPending) using Disconnection(IbDisconnectOk(why, sender))

      case Event(ReaderStopped(ex), Connection(os, _, _)) =>
        misc.silentClose(os)
        goto(Disconnected) using Disconnection(IbDisconnectError(ReasonEx, ex = Some(ex)))
    }

    /*
     * wait for the reader to let us know it's done.
     */
    when(DisconnectPending) {

      /*
       * A SocketException thrown _precisely here_ is the only Reader shutdown which is (almost)
       * certainly a result of a client request.
       */
      case Event(ReaderStopped(ex: java.net.SocketException), Disconnection(ibd: IbDisconnectOk)) =>
        goto(Disconnected) using Disconnection(ibd)

      /*
       * All other cases are errors.
       */
      case Event(ReaderStopped(ex), _) =>
        goto(Disconnected) using Disconnection(IbDisconnectError(ReasonEx, ex = Some(ex)))

    }

    /*
     * also flesh out the other errors
     */
    onTransition {
      case _ -> Disconnected =>
        nextStateData match {
          case Disconnection(ibDisconnect) => subs publish ibDisconnect
          case Empty => () // it's ok, Future failed and published error
          case wtf => log.error("disconnection with no message: {}", wtf)
        }
    }

    whenUnhandled {
      /*
       * Except when connected, all attempts to send outgoing API messages result in error
       */
      case Event(msg: OutgoingMessage with Product, _) =>
        import OutgoingError._
        subs publish OutgoingError(mkReason(msg, NotConnected), sender, msg)
        stay

      case Event(ibconn: IbConnect, _) =>
        import IbConnectError._
        val reason = if (stateName == DisconnectPending) BadSequence else Redundant
        subs publish IbConnectError(reason, sender, ibconn)
        stay

      case Event(IbDisconnect(why), _) =>
        import IbDisconnectError._
        val reason = if (stateName == ConnectPending) BadSequence else Redundant
        subs publish IbDisconnectError(s"$reason: $why", Some(sender))
        stay
    }

    initialize()
  }

  /*
   * hide dangerous wires and gears from the small children
   */
  private[deftrade] object IbcImpl {

    /*
     * FSM state and data
     */
    sealed trait State
    case object Disconnected extends State
    case object ConnectPending extends State
    case object Connected extends State
    case object DisconnectPending extends State

    sealed trait Data
    case object Empty extends Data
    case class Connection(os: OutputStream, reader: Reader, ok: IbConnectOk) extends Data
    case class Disconnection(connMsg: IbConnectionMessage) extends Data 

    /*
     * messages private to the FSM / Future / Reader complex
     */
    sealed trait Handshake
    case class ConnectData(data: Data) extends Handshake
    case class ReaderStopped(ex: Throwable) extends Handshake

    /*
     * Control exceptions for Reader: reports the reason why it stopped. 
     */
    class TerminatedId extends ControlThrowable
    class UnknownId(id: String) extends ControlThrowable {
      override def getMessage(): String = id
    }
    /*
     * reader
     */
    class Reader(implicit dis: DataInputStream, serverVersion: Int) extends Runnable {

      import ImplicitConversions._
      import IncomingMessages.rdz

      def run() {
        try while (true) {
          (rdz: Int @annotation.switch) match {
            case 1 => TickPrice.read
            case 2 => TickSize.read
            case 3 => OrderStatus.read
            case 4 => Error.read
            case 5 => OpenOrder.read
            case 6 => UpdateAccountValue.read
            case 7 => UpdatePortfolio.read
            case 8 => UpdateAccountTime.read
            case 9 => NextValidId.read
            case 10 => ContractDetailsCont.read
            case 11 => ExecDetails.read
            case 12 => UpdateMktDepth.read
            case 13 => UpdateMktDepthL2.read
            case 14 => UpdateNewsBulletin.read
            case 15 => ManageAccounts.read
            // case 16 => ReceiveFA.read
            case 17 => HistoricalData.read
            case 18 => BondContractDetails.read
            case 19 => ScannerParameters.read
            case 20 => ScannerData.read
            case 21 => TickOptionComputation.read
            case 45 => TickGeneric.read
            case 46 => TickString.read
            case 47 => TickEFP.read
            case 49 => CurrentTime.read
            case 50 => RealTimeBar.read
            case 51 => FundamentalData.read
            case 52 => ContractDetailsEnd.read
            case 53 => OpenOrderEnd.read
            case 54 => AccountDownloadEnd.read
            case 55 => ExecDetailsEnd.read
            case 56 => DeltaNeutralValidation.read
            case 57 => TickSnapshotEnd.read
            case 58 => MarketDataType.read
            case 59 => CommissionReportMsg.read
//            case 61 => Position.read
//            case 62 => PositionEnd.read
//            case 63 => AccountSummary.read
//            case 64 => AccountSummaryEnd.read
            
            case -1 => throw new TerminatedId()
            case unk => throw new UnknownId(unk)
          }
        } catch {
          case ct @ (_: TerminatedId | _: UnknownId) => conn ! ReaderStopped(ct)
          case NonFatal(nfx) => conn ! ReaderStopped(nfx)
        }
      }
    }
  }
}

private[deftrade] object throttle {

  import scala.collection.immutable.Queue

  sealed trait State
  case object Active extends State
  case object Idle extends State

  // holds max possible messages in each interval
  type Quotas = Queue[Int]

  final class ApiMsgThrottler private (dest: ActorRef, msgsPerSec: Int, intervalsPerSec: Int)
    extends Actor with Stash with LoggingFSM[State, Quotas] {

    // divide a second into equal intervals
    import scala.concurrent.duration._
    val interval = (1.0 / intervalsPerSec).second

    // when a message is forwarded, charge it against _all_ intervals in the next second
    private def chargeMsgAgainst(qs: Quotas) = { qs map (_ - 1) } ensuring { qs forall (_ >= 0) }

    // current interval is done; interval ending one second later gets full quota
    private def replenish(qs: Quotas) = {
      val (_, rest) = qs.dequeue
      rest enqueue msgsPerSec
    }

    case object Tick
    override def preStart() = {
      setTimer("intervalTimer", Tick, interval, repeat = true)
      initialize()
    }

    startWith(Active, Queue.fill(intervalsPerSec + 1) { msgsPerSec })

    when(Active) {
      case Event(Tick, quotas) => stay using replenish(quotas)
      case Event(msg, quotas) if quotas.front > 0 =>
        dest forward msg
        stay using chargeMsgAgainst(quotas)
      case Event(_, _) => // exhausted quota for this interval
        stash()
        goto(Idle)
    }

    when(Idle) {
      case Event(Tick, quotas) =>
        unstashAll()
        goto(Active) using replenish(quotas)
      case Event(_, _) =>
        stash()
        stay
    }

    onTermination {
      case _ => cancelTimer("intervalTimer")
    }

  }

  object ApiMsgThrottler {
    def props(dest: ActorRef, msgsPerSec: Int, intervalsPerSec: Int = 10): Props =
      Props(new ApiMsgThrottler(dest, msgsPerSec, intervalsPerSec))
  }

}