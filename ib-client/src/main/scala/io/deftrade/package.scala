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
package io {

  /**
   * global notes
   * 
   * 
   * cake structure: ib module needs to accept the actor system as a parameter -
   * therefore config, pubsub, and all actors within ib - and all transitive dependencies - 
   * must be baked in the same cake. This includes IncomingMessages and OutgoingMessages, but not
   * DTOs.
   *  
   * moar new ideas 11/17/15
   * 
   * usage seems to fall into some distinct request/response patterns.
   * 
   * Pure Streams (explicit cancel, no explicit End responce)
   *  - ticks (incl options comp)
   *  - bars
   *  - market depth
   *  * frozen indicator - akin to synthetic timeout inactivity warnings (tap tap this thing on?)
   *  - Fundamental data?! Yes, appears to be. Verify.
   *  - Historical data - special handling; end must be calculated.
   *  - news bullitins
   *  
   *  Future Aggregate - self canceling, explicit End response
   *  - ticks (no genericTickList)
   *  - openOrders (at startup only! TODO - are End responses seen? in regular order flow?)
   *  - reqExecutions (at startup only! TODO - check this)
   *  - contractDetails and BondContractDetails
   *  
   *  Stream of Aggregates - explicit Cancel, Explicit End Response
   *  (not sure about this - thinking these are pure streams)
   *  - account value
   *  - Account summary
   *  - positions
   *  - scanner data
   *  
   *  Pure RPC
   *  - scanner parameters
   *  
   *  REST-like (seems to be explicit GET and PUT analogs here)
   *   - FA managed account stuff
   *   
   *   ORDER STUFF -> wtf
   *   DISPLAY GROUPS -> wtfc
   *  
   *
   * Clean layering: ib client should be *testable* against java api without service layer
   * Services layer should be usable without persistence schema.
   *
   * Services:
   *
   * OrderManagementService
   * MarketDataService
   * HistoricalDataService
   * ReferenceDataService
   * AccountService
   *
   * No messages get sent directly to IB connection - go through service layer.
   *
   * Error routing: happens at Service layer. Services subscribe to all error messages
   * and keep Map[Id, ActorRef] to further route errors.
   * Stream type parameter needs to comprehend errors!
   *
   * IDs:
   * - policy is set in the services layer
   * - OMS manages the OrderId.
   * - TickId = ReqId -> one space.
   * - ReqId has an offset (Int.Max / 2 + 1 == 0x40000000)
   * - ReqId.Offset is overrideable within the cake, for testing
   *
   * OMS:
   *
   * General functionality:
   * - manage the orderId sequence
   * = consolidate duplicate OrderStatus messages.
   * - condense redundant information between OpenOrder, OrderStatus and Executions and provide a
   * single "OrderResponse" with the consolidated data.
   * - receive Portfolio Updates and reconcile
   * - watchdog timer to request executions / order status if no acks (1 sec or so)
   *
   * - assume the "download open orders on connection" checkbox in TWS is set,
   * but do not require it.
   *
   * issue: if clients don't deal with OrderId's directly, how do they issues retries idemopotently?
   * who is responsible for retrying orders which get no response from IB servers (due to whatever).
   * Would be best if OMS had a layer to do this.
   *
   * issue: routing for OrderStatus messages (etc).
   * - route the consolidated response to the actor which made the request (or its delegate).
   * - allow requester to provide a function which consolidates the responses.
   * - allow other actors to subscribe to order fills generally (thru Subscriptions)
   * - allow other actors to subscribe to consolidated order fills (thru OMS)
   *
   * Risk budgeting: Account should have a simple risk managment strategy - simpler than the
   * Portfolio margin rules of IB - but could be more aggressive than a simple margin account.
   * Risk should be checked before order sent.
   *
   * Keep my persistent copy of the current order number.
   * At start up use max(my number, NextValidId).
   *
   * "Inactive" status: https://groups.yahoo.com/neo/groups/TWSAPI/conversations/topics/22357
   * Multiple causes - knowing when to reuse OrderId is tricky - policy will be to burn new OrderId
   *
   *
   */

  package object deftrade {
    
    // TODO: framework-wide utilities must be factored into a separate util proj
    // once the services subprojexts are in place.

    import scala.language.implicitConversions

    def indent[A: Indentable](a: A): String = misc.indent(a.toString)

    implicit def im2indent[A <: IncomingMessages#IncomingMessage](a: A) = new Indentable[A](a)
    implicit def om2indent[A <: OutgoingMessages#OutgoingMessage](a: A) = new Indentable[A](a)
    implicit def dtoindent[A <: DTO](a: A) = new Indentable[A](a)

  }
}

package io.deftrade {

  import akka.actor.{ Extension, ExtensionId, ExtensionIdProvider, ExtendedActorSystem }
  import scala.concurrent.duration.Duration
  import com.typesafe.config.Config
  import java.util.concurrent.TimeUnit

  class Indentable[A](a: A) {
    def indent: String = misc.indent(a.toString)
  }

  private[deftrade] class SettingsImpl(config: Config) extends Extension {
    object ibc {      
      val host: String = config.getString("deftrade.ibc.host")
      val port: Int = config.getInt("deftrade.ibc.port")
      val clientId: Int = config.getInt("deftrade.ibc.client-id")
      val msgsPerSec: Int = config.getInt("deftrade.ibc.throttle.max-msgs-per-sec")
      val intervalsPerSec: Int = config.getInt("deftrade.ibc.throttle.intervals-per-sec")
    }
    object oms
    object ams
    object mds
    object rds
    object hds
  }

  /**
   * Access all deftrade specific config settings through this Settings object.
   */
  object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {

    override def lookup = Settings

    override def createExtension(system: ExtendedActorSystem) =
      new SettingsImpl(system.settings.config)
  }

  private[deftrade] object misc {

    object indent extends Indent(nindent = 2)

    def repeat[U](n: Int)(block: => U): Unit = {
      var i = 0
      while (i < n) {
        i += 1
        block
      }
    }

    import scala.util.control.NonFatal
    
    class SingleThreadPool extends java.util.concurrent.Executor {
      import java.util.concurrent.RejectedExecutionException
      private[this] val lock = new {}
      private var task: Option[Runnable] = None
      def isRunning: Boolean = lock.synchronized {
        task != None
      }
      override def execute(r: Runnable): Unit = lock.synchronized {
        if (task != None) throw new RejectedExecutionException("already running")
        else {
          task = Some(r)
          lock.notifyAll()
        }
      }
      private var thread = newThread() // need to hold the reference somewhere I guess...
      private def newThread(): Thread = {
        val ret = new Thread(new Runnable {
          override def run(): Unit = while (true) {
            lock.synchronized {
              while (task == None) lock.wait()
            }
            try {
              task.get.run()
            } catch {
              case NonFatal(t) => {
                thread = newThread()
                throw t
              }
            } finally {
              lock.synchronized {
                task = None
              }
            }
          }
        })
        ret.start()
        ret
      }
    }

    import scala.util.{ Try, Failure }

    implicit class HookFailure[T](t: Try[T]) {
      def hookFailure(block: => Unit): Try[T] = t recoverWith {
        case NonFatal(x) => block; Failure(x)
      }
      def hookFailure(block: Throwable => Unit): Try[T] = t recoverWith {
        case NonFatal(x) => block(x); Failure(x)
      }
    }

    private val ignore: PartialFunction[Any, Unit] = { case _ => () }

    type Closeable = { def close(): Unit }

    def silentClose(resource: Closeable): Unit = {
      import scala.language.reflectiveCalls
      try { resource.close } catch { ignore }
    }

    def using[U](resource: Closeable)(block: => U) = {
      try { block } finally { silentClose(resource) }
    }

    /*
     * rules for case classes to make indenting work: (i.e. "known fragilities")
     * - use ndnvs for toString
     * - prefix should match [A-Z][A-Za-z0-9]+
     * - constructor param names should be valid Java identifiers
     * - NDNVS.sep must be string with first char non-ws delimiter, followed by optional ws
     * - ndnvs sep must _not_ be ", " because parser will choke on pure value strings containing ','
     */
    import scala.util.parsing.combinator.JavaTokenParsers

    private[misc] class Indent(nindent: Int) extends (String => String) with JavaTokenParsers {

      import NonDefaultNamedValues.{ start, sep, end }
      import scala.util.Properties.{ lineSeparator => lsep }

      val sindent = " " * nindent

      /*
       * AST representing the parsed string
       */
      sealed trait Tree {

        /*
         * TODO: Code is hacktastic at best. Rationalize.
         */
        final def show(i: Int = 0): String = this match {
          case Case(pfx, nvs) => s"$pfx$start${(nvs map { _.show(i + 1) }) mkString sep.toString}$end"
          case Container(pfx, vs) => vs match {
            case Nil => s"${pfx}()"
            case head :: Nil => head.show(i)
            case _ => s"$pfx(${vs map { v => s"$lsep${sindent * (i + 1)}${v.show(i + 2)}" } mkString ", "})"
          }
          case NamedValue(n, v) => s"$lsep${sindent * i}$n=${v.show(i)}"
          case PureValue(s) => s
        }
      }

      sealed trait Value extends Tree
      case class Case(pfx: String, nvs: List[NamedValue]) extends Value
      case class Container(pfx: String, vs: List[Value]) extends Value
      case class NamedValue(n: String, v: Value) extends Tree
      case class PureValue(s: String) extends Value

      // override val skipWhitespace = false

      def kase: Parser[Case] = prefix ~ start ~ namedValues ~ end ^^ {
        case pfx ~ start ~ nvs ~ end => Case(pfx, nvs)
      }
      def container: Parser[Container] = prefix ~ "(" ~ values ~ ")" ^^ {
        case pfx ~ "(" ~ vs ~ ")" => Container(pfx, vs) // note: start/sep/end not used
      }
      def namedValues: Parser[List[NamedValue]] = repsep(namedValue, sep)
      def namedValue: Parser[NamedValue] = ident ~ "=" ~ value ^^ {
        case n ~ "=" ~ v => NamedValue(n, v)
      }
      def values: Parser[List[Value]] = repsep(value, ',')
      def value: Parser[Value] = kase | container | pureValue

      def prefix: Parser[String] = """[A-Z][A-Za-z0-9]+""".r
      def pureValue: Parser[Value] = {
        val sepchar = sep.charAt(0)
        val endchar = if (end == ']') """\]""" else end.toString
        s"""[^${sepchar}${endchar}]*""".r ^^ { v => PureValue(v) }
      }

      def apply(s: String): String = parseAll(kase, s) match {
        case Success(pretty, _) => pretty.show()
        case NoSuccess(msg, rest) => msg
      }
    }

  }

}