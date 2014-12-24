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

import java.util.Random

import scala.concurrent.{ Future, promise, Promise, Await, duration }
import duration._
import scala.annotation.meta.companionObject
import scala.language.{ postfixOps, reflectiveCalls }

import akka.actor.{ ActorSystem, Actor, ActorLogging, Props }
import akka.util.ByteString
import akka.testkit.{ TestKit, ImplicitSender }
import akka.event.Logging

import org.scalatest.{ WordSpecLike, BeforeAndAfterAll, Matchers }
import org.scalatest.concurrent.AsyncAssertions
import org.scalatest.time.{ Span, Millis }

import TestServer.SEED

import com.ib.client.{ EClientSocket, EWrapperMsgGenerator, AnyWrapper, EWrapper }

abstract class ConnectionSpecBase(_system: ActorSystem) extends TestKit(_system) {
  /*
   * ib-conn scaffold
   */
  val serverVersion = 69 // current version - should be param?

  object IB extends IbConnectionComponent(system) with IbDomainTypesComponent
  import IB._

  final val server = new TestServer(system)

  final val log = Logging(system, "ConnectionSpec")

  final val msgLogger = system.actorOf(Props(new Actor with ActorLogging {
    def receive = {
      case msg: IncomingMessage => log.debug("IB published: {}", msg)
    }
  }))
  IB.subs.subscribe(msgLogger, classOf[IncomingMessage])

}

abstract class IncomingSpecBase(_system: ActorSystem) extends ConnectionSpecBase(_system)
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll
  with AsyncAssertions with IbApiUtils {

  def this(name: String) = this(ActorSystem(name))

  import IB._

  import scala.reflect.{ ClassTag, classTag }
  import scala.reflect.runtime.currentMirror
  import scala.reflect.runtime.universe.{ typeOf, Type, TypeTag, InstanceMirror }

  import org.scalatest.time.Span
  import org.scalatest.time.Millis

  import TestServer._

  def shorter(w: Waiter) = s"w${w.toString.substring(w.toString.lastIndexOf('@'))}"

  class EWrapperCallbacks(w: Waiter) extends EWrapperBase {
    // check for "-1 | 505 | Fatal Error: Unknown message id."
    override def error(id: Int, errorCode: Int, errorMsg: String): Unit = {
      w {
        val msg = s"com.ib.client wrapper error: ${errorCode}, ${errorMsg}"
        log.debug(msg)
        fail(msg)
      }
      super.error(id, errorCode, errorMsg)
    }
    override def connectionClosed() {
      w.dismiss()
      log.info("dismiss: connection closed for {}", shorter(w));
      super.connectionClosed();
    }
  }

  def nreps: Int

  //  implicit def classTagOf[T: TypeTag]: ClassTag[T] = ClassTag(currentMirror runtimeClass typeOf[T])

  def codeFor[A: TypeTag] = {
    import scala.reflect.runtime.currentMirror
    val im = currentMirror.reflect(IB)
    val mm = im.reflectModule(typeOf[A].typeSymbol.companion.asModule)
    mm.instance.asInstanceOf[{ def code: Int }].code
  }

  def homologize[A <: ApiMessage: TypeTag: ClassTag](versions: Seq[Int], n: Int = nreps)(
    wrapper: A => Waiter => EWrapperCallbacks): Unit = {

    import IB.IbcImpl.UnknownId
    IB.subs unsubscribe testActor
    IB.subs subscribe (testActor, classTag[A].runtimeClass)
    IB.subs subscribe (testActor, classOf[IbDisconnectError])

    @annotation.tailrec
    def repeat(n: Int, seed: Long): Unit = n match {
      case 0 => ()
      case i =>

        log.info("i={}; seed={}", i, seed)

        versions foreach { version =>

          log.debug("version: {}", version)

          val params = TestServer.Params(
            serverVersion = serverVersion, incoming = true,
            seed = seed, apiVers = version, apiCode = codeFor[A])
          server.startTestServer(params)
          IB.conn ! IbConnect(port = TEST_PORT, clientId = 0)

          val msg = expectMsgType[A](1000 millis)
          log.debug("Received API message: {}", msg)

          expectMsgType[IbDisconnectError](1000 millis) match {
            case IbDisconnectError("Exception", _, _, Some(x: UnknownId)) =>
              log.debug("Got _expected_ unknown API code: {}", x.getMessage)
            case wtf => fail(s"WTF? ${wtf}")
          }
          val n = IncomingMessages.nrdz -
            2 - // exclude the connection handshake
            2 - // exclude the api(code, version)
            1 // exclude the illegal api.code which terminated the IB stream
          log.debug("nFields: {}", n)

          doIbCheck(params.copy(nFields = n), 2, wrapper(msg))
        }
        repeat(i - 1, ((new Random(seed)).nextLong()))
    }
    repeat(nreps, SEED)
  }
  def homologize2[A <: ApiMessage: TypeTag: ClassTag, B <: ApiMessage: TypeTag: ClassTag](
    versions: Seq[Int], n: Int = nreps)(
      wrapper: (A, Option[B]) => Waiter => EWrapperCallbacks): Unit = {

    import IB.IbcImpl.UnknownId

    IB.subs unsubscribe testActor
    IB.subs subscribe (testActor, implicitly[ClassTag[A]].runtimeClass)
    IB.subs subscribe (testActor, implicitly[ClassTag[B]].runtimeClass)
    IB.subs subscribe (testActor, classOf[IbDisconnectError])

    @annotation.tailrec
    def repeat(n: Int, seed: Long): Unit = n match {
      case 0 => ()
      case i =>

        log.info("i={}; seed={}", i, seed)

        versions foreach { version =>

          log.debug("version: {}", version)

          val params = TestServer.Params(
            serverVersion = serverVersion, incoming = true,
            seed = seed, apiVers = version, apiCode = codeFor[A])
          server.startTestServer(params)

          IB.conn ! IbConnect(port = TEST_PORT, clientId = 0)

          val msgA = expectMsgType[A](1000 millis)
          log.debug("Received API message: {}", msgA)

          val clazzB = classTag[B].runtimeClass
          val clazzE: Class[_] = classTag[IbDisconnectError].runtimeClass
          val msgBE = expectMsgAnyClassOf(1000 millis, clazzB, clazzE)
          log.debug("Received API message {} of class {}", msgBE, msgBE.getClass)
          val msgB = msgBE match {
            case b: B =>
              expectMsgType[IbDisconnectError](1000 millis) match {
                case IbDisconnectError("Exception", _, _, Some(x: UnknownId)) =>
                  log.debug("Got _expected_ unknown API code: {}", x.getMessage)
                case wtf => fail(s"WTF? ${wtf}")
              }
              Some(b)
            case IbDisconnectError("Exception", _, _, Some(x: UnknownId)) =>
              log.debug("Got _expected_ unknown API code: {}", x.getMessage)
              None
            case wtf => fail(s"WTF? ${wtf}")
          }
          val n = IncomingMessages.nrdz -
            2 - // exclude the connection handshake
            2 - // exclude the api(code, version)
            1 // exclude the illegal api.code which terminated the IB stream
          log.debug("nFields: {}", n)

          doIbCheck(params.copy(nFields = n), 2 + 2 * msgB.size, wrapper(msgA, msgB))

        }
        repeat(i - 1, ((new Random(seed)).nextLong()))
    }
    repeat(nreps, SEED)
  }

  def homologize[A <: IncomingMessage: TypeTag: ClassTag](
    version: Int, fields: Seq[String])(wrapper: A => Waiter => EWrapperCallbacks): Unit = {

    IB.subs unsubscribe testActor

    IB.subs subscribe (testActor, implicitly[ClassTag[A]].runtimeClass)
    IB.subs subscribe (testActor, classOf[IbDisconnectError])

    val params = TestServer.Params.list(
      serverVersion = serverVersion, apiCode = codeFor[A], apiVers = version, fields = fields: _*)

    // first for scala-ib
    server.startTestServer(params)

    IB.conn ! IbConnect(port = TEST_PORT, clientId = 0)

    val msgA = expectMsgType[A](1000 millis)
    log.debug("Received API message: {}", msgA)

    import IbcImpl.TerminatedId
    expectMsgType[IbDisconnectError](1000 millis) match {
      case IbDisconnectError("Exception", _, _, Some(x: TerminatedId)) =>
        log.debug("Got _expected_ TerminatedId: {}", x.getMessage)
      case wtf => fail(s"WTF? ${wtf}")
    }

    doIbCheck(params, 2, wrapper(msgA))

  }
  def homologize[A <: ApiMessage: TypeTag: ClassTag, E <: ApiMessage: TypeTag: ClassTag](
    version: Int, prolog: Seq[String], repss: Seq[Seq[String]])(
      wrapper: (Seq[A], E) => Waiter => EWrapperCallbacks): Unit = {

    IB.subs unsubscribe testActor

    IB.subs subscribe (testActor, implicitly[ClassTag[A]].runtimeClass)
    IB.subs subscribe (testActor, implicitly[ClassTag[E]].runtimeClass)
    IB.subs subscribe (testActor, classOf[IbDisconnectError])

    val params = TestServer.Params.list(
      serverVersion = serverVersion, apiCode = codeFor[A], apiVers = version,
      fields = prolog ++ repss.flatten: _*)

    // first for scala-ib
    server.startTestServer(params)

    IB.conn ! IbConnect(port = TEST_PORT, clientId = 0)

    val msgs = repss map { fields =>
      val msg = expectMsgType[A](1000 millis)
      log.debug("Received API message: {} from fields: {}", msg, fields)
      msg
    }
    val msgEnd = expectMsgType[E](1000 millis)
    log.debug("Received API message (End): {}", msgEnd)

    import IbcImpl.TerminatedId
    expectMsgType[IbDisconnectError](1000 millis) match {
      case IbDisconnectError("Exception", _, _, Some(x: TerminatedId)) =>
        log.debug("Got _expected_ TerminatedId: {}", x.getMessage)
      case wtf => fail(s"WTF? ${wtf}")
    }

    doIbCheck(params, msgs.size + 2, wrapper(msgs, msgEnd))
  }

  private def doIbCheck(params: TestServer.Params, nDismissals: Int, f: Waiter => EWrapperCallbacks): Unit = {

    val connectionPromise = Promise[Unit]()
    server.startTestServer(params.copy(connected = Some(connectionPromise.future)))

    val w = new Waiter()
    log.debug("New Waiter {}", shorter(w))

    val ibSocket = new EClientSocket(f(w))
    ibSocket.eConnect(null, TEST_PORT, 0)
    connectionPromise.success(())

    log.debug("awaiting {} dismissals on {}", nDismissals, shorter(w))
    w.await(timeout(Span(1000, Millis)), dismissals(nDismissals))
    log.debug("received {} dismissals on {}", nDismissals, shorter(w))
  }

  def asyncAssertionBlock(w: Waiter)(assertionBlock: => Unit): Unit =
    try w { assertionBlock }
    finally {
      w.dismiss()
      log.debug("dismiss: assertion block complete for {}", shorter(w))
    }

  override def afterAll() {
    super.afterAll()
    server.close()
  }
}

abstract class OutgoingSpecBase(_system: ActorSystem) extends ConnectionSpecBase(_system)
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll
  with AsyncAssertions with IbApiUtils {

  def this(name: String) = this(ActorSystem(name))

  protected val REQ_ID = 33 // totally arbitrary

  import IB._

  import TestServer._

  val ibSocket = new EClientSocket(new EWrapperBase)

  import com.ib.client.{
    Contract => IbContract,
    ContractDetails => IbContractDetails,
    Order => IbOrder,
    OrderState => IbOrderState,
    Execution => IbExecution,
    ExecutionFilter => IbExecutionFilter,
    ScannerSubscription => IbScannerSubscription
  }
  import scala.reflect.runtime.universe.{ typeOf, Type, TypeTag, InstanceMirror }
  /*
   * reflection based instantiation of types dependent on IB module requires an instance
   * mirror specific to the IB module
   */
  implicit val outer: InstanceMirror = scala.reflect.runtime.currentMirror.reflect(IB)
  /*
   * type class to associate the com.ib.client homolog for a given DTO
   */
  def companionInstanceOf[D: TypeTag]: Any = {
    outer.reflectModule(typeOf[D].typeSymbol.companion.asModule).instance
  }
  def api[M: TypeTag] = companionInstanceOf[M].asInstanceOf[{ def api: (Int, Int) }].api

  /**
   * Typeclass which provides the mapping the com.ib.client homolog for the given ib-scala type
   */
  class Homolog[D: TypeTag] {
    val tpe = typeOf[D]
    type IbType
  }

  implicit val contractHomolog = new Homolog[Contract] {
    type IbType = IbContract
  }
  implicit val contractDetailsHomolog = new Homolog[ContractDetails] {
    type IbType = IbContractDetails
  }
  implicit val OrderHomolog = new Homolog[Order] {
    type IbType = IbOrder
  }
  implicit val ExecutionFilterHomolog = new Homolog[ExecutionFilter] {
    type IbType = IbExecutionFilter
  }
  implicit val ScannerSubscriptionHomolog = new Homolog[ScannerSubscription] {
    type IbType = IbScannerSubscription
  }

  implicit val ServerLogLevelHomolog = new Homolog[ServerLogLevel.ServerLogLevel] {
    type IbType = Int
  }

  implicit def genIdHomolog[D <: GenId: TypeTag] = new Homolog[D] {
    type IbType = Int
  }

  def genDtoAndIbHomolog[D](implicit td: Homolog[D]): (D, td.IbType) = {
    val dtoGenerated = generate(td.tpe)
    val ibDtoGenerated = ibCopyOf(dtoGenerated)
    assert(dtoGenerated =~= ibDtoGenerated)
    (dtoGenerated.asInstanceOf[D], ibDtoGenerated.asInstanceOf[td.IbType])
  }

  val cid = new java.util.concurrent.atomic.AtomicInteger(0)
  def nextCid = { cid.getAndIncrement() }

  def assertOmWritesEqual[OM <: OutgoingMessage: TypeTag](msg: => OM, ibCall: => Unit): Unit = {

    def assertTxFieldsEqual(fbs: Future[ByteString], ibfbs: Future[ByteString]): Unit = {
      import scala.concurrent.ExecutionContext.Implicits.global

      val w = new Waiter()

      val result = for {
        bs <- fbs
        ibbs <- ibfbs
      } yield {
        val xs = toFieldList(bs)
        val ys = toFieldList(ibbs)
        w {
          ((xs zip ys) zipWithIndex) foreach {
            case (xy, i) =>
              xy match { case (x, y) => assert(x === y, s"at index $i") }
          }
        }
        w { assert(xs.length === ys.length) }
      }
      result onSuccess { case _ => w.dismiss() }
      result onFailure { case _ => w { fail("promises weren't kept") }; w.dismiss() }
      w.await(timeout(Span(1000, Millis)), dismissals(1))
      Await.ready(result, 2 seconds) // serialize access to the Promise map
    }

    val cid = nextCid
    val ibCid = nextCid
    val fbs = TestServer.future(cid)
    val ibFbs = TestServer.future(ibCid)
    val (code, vers) = api[OM]
    val params = TestServer.Params(
      serverVersion = serverVersion, incoming = false, apiCode = code, apiVers = vers)

    server.startTestServer(params)
    IB.subs subscribe (testActor, classOf[IbConnectOk])
    IB.subs subscribe (testActor, classOf[IbDisconnectOk])
    IB.conn ! IbConnect(port = TEST_PORT, clientId = cid)
    expectMsgType[IbConnectOk](200 millis)
    IB.conn ! msg
    IB.conn ! IbDisconnect("all done")
    expectMsgType[IbDisconnectOk](200 millis)

    server.startTestServer(params)
    ibSocket.eConnect(null /* and by null we mean localhost */ , TEST_PORT, ibCid)
    ibCall
    ibSocket.eDisconnect()
    assertTxFieldsEqual(fbs, ibFbs)
  }

  override def afterAll() {
    super.afterAll()
    server.close()
  }
}