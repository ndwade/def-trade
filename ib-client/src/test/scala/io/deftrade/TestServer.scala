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

import java.net.{ Socket, ServerSocket }
import java.io.DataInputStream
import java.util.Random

import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.concurrent.{ promise, Promise, Future, Await, duration }
import duration._

import akka.util.{ ByteString }
import akka.actor.ActorSystem
import akka.event.Logging

/**
 * A test fixture which represents the TWS server side. Note that this is purely a protocol testing
 * oriented fixture; no functional testing is done - the only goal is to ensure that the message
 * fields defined by the com.ib.client API are conformed to by the ib-scala messages.
 *
 * For `OutgoingMessages`, the random messages sent by the ib-scala and com.ib.client libraries
 * are compared.
 *
 * For `IncomingMessages`, the following observation is exploited: if the random fields are
 * restricted to be integers, they will be parsed correctly no matter if the field type is
 * String, Double, Boolean or Int. Combine this with the lenient error handling for the
 * `Enumeration` based ib-scala message types, and it's possibly to generate random valued
 * fields and have them parsed correctly (enough) by the clients.
 *
 * One nuance is that the random values must themselves not correspond to valid API message
 * numbers: (FIXME: why?)
 */
class TestServer(system: ActorSystem) {
  import TestServer._

  val log = Logging(system, this.getClass)
  val ss = new ServerSocket(TEST_PORT)
  //  val executor = new misc.SingleThreadPool()

  // need to be careful on each release to adjust these.

  def startTestServer(params: Params): Unit = {
    import params._
    val okToGo = Promise[String]
    class Server extends Runnable {
      override def run: Unit = {
        import OutgoingMessages.wrz
        //        import IncomingMessages.rdz
        // commented out - rdz() is cut/pasted to avoid interfering with nFields counter
        import ImplicitConversions.{ i2s, s2i }
        val threadName = Thread.currentThread().getName()
        okToGo.success(threadName)
        log.debug("Test server waiting for connection on port {} on thread {}",
          ss.getLocalPort, threadName)
        val socket = ss.accept()
        log.debug("connected on for server version {}", serverVersion)
        implicit val is = new DataInputStream(socket.getInputStream())
        implicit val os = socket.getOutputStream()
        val clientVersion: Int = rdz
        log.debug("clientVersion is {}", clientVersion)
        wrz(serverVersion)
        wrz("TWS date/time field")
        val clientId: Int = rdz
        log.debug(params.toString)
        if (incoming) {
          val random = new Random(seed)
          def nextRandomField = {
            val index = random.nextInt(nics)
            illegalCodes(index)
          }
          /*
           * send the api code and version code, and then a random string until shut down.
           */
          try {
            wrz(apiCode)
            wrz(apiVers)
            fields match {
              case Some(fs) => fs foreach (wrz(_))
              case None => misc.repeat(nFields) { wrz(nextRandomField) }
            }
            params.connected foreach { Await.ready(_, 1000 millis) }
            wrz("-1")
            os.close()
          } catch {
            case NonFatal(ex) => log.info("caught exception: {}", ex)
          }
        } else {
          /*
           * read bytes and store in a map of clientId -> ByteString
           */
          log.debug("TestServer: reading bytes")
          val bsb = ByteString.newBuilder
          while ({
            val b = is.read()
            if (b != -1) { bsb += b.asInstanceOf[Byte]; true }
            else { false }
          }) {}
          val bs = bsb.result
          log.debug("raw fields: {}EOF", bs map { b =>
            val c = b.asInstanceOf[Char]; if (c == 0) '|' else c
          } mkString)
          recMap(clientId) success bs
        }
        socket.close()
      }
    }
    new Thread(new Server()).start
    //    executor.execute(new Server())
    val threadName = Await.result(okToGo.future, 1000 millis)
    log.info("startServer: Thread {} started", threadName)
  }

  def close() = ss.close()
}
object TestServer {

  case class Params(serverVersion: Int,
    incoming: Boolean,
    apiCode: Int,
    apiVers: Int,
    nFields: Int = 8192,
    fields: Option[List[String]] = None,
    connected: Option[Future[Unit]] = None,
    seed: Long = SEED) {
    require(nFields == 0 || fields == None)
    def hasList: Boolean = fields.isDefined
    override def toString = NonDefaultNamedValues.nonDefaultNamedValues
  }
  object Params {
    def list(serverVersion: Int, apiCode: Int, apiVers: Int, fields: String*): Params =
      Params(serverVersion, true, apiCode, apiVers, nFields = 0, fields = Some(List(fields: _*)))
  }

  val TEST_PORT = 8888
  val illegalCodes = Vector.fill(10)("0") ++ Vector.fill(10)("") ++
    ((22 until 45) map (_.toString)) ++
    ((60 until 99) map (_.toString))
  val nics = illegalCodes.size
  val SEED = 0x4DBABEBEABAD8F00L

  def rdz(implicit input: DataInputStream): String = {
    val sb = StringBuilder.newBuilder
    //      sb.clear
    while ({
      val c = input.readByte()
      if (c != 0) { sb += c.asInstanceOf[Char]; true } else false
    }) {}
    sb.result
  }

  /*
   * map from clientId to Bytes received
   */
  import collection.mutable

  val recMap = mutable.Map.empty[Int, Promise[ByteString]]

  def future(cid: Int): Future[ByteString] = {
    val p = Promise[ByteString]
    recMap.put(cid, p)
    p.future
  }

  def toFieldList(bs: ByteString): List[String] = {
    @annotation.tailrec
    def step(bs: ByteString, acc: List[String]): List[String] = {
      if (bs == ByteString.empty) acc else {
        val (first, rest) = bs span { _ != 0 }
        val s = new String(first.toArray, "US-ASCII")
        step(rest drop 1, s :: acc)
      }
    }
    step(bs, List.empty[String]).reverse
  }
}