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
import akka.actor._
import akka.actor.ActorDSL._
import io.deftrade._
import java.io.{ File, FileOutputStream, PrintStream }

package demo {
  object Ib extends IbConnectionComponent(ActorSystem("demo")) with IbDomainTypesComponent
}

package object demo {

  import Ib._
  import Right._
  
  val config = Ib.system.settings.config
  val timestampFormat = new java.text.SimpleDateFormat("HH:mm:ss:SSS")

  private def psAct(ps: PrintStream, name: String): ActorRef =
    actor(system, name) {
      new Act {
        become {
          case msg: IncomingMessage =>
            val ts = timestampFormat.format(new java.util.Date())
            ps.println(s"$ts -> ${msg.indent}")
        }
        whenStopping { ps.close() }
      }
    }

  def consoleMsgs(clazzs: Class[_]*): Boolean = {
    val consoleActor = psAct(Console.out, "consoleMsgs")
    clazzs map { subs.subscribe(consoleActor, _) } reduce { _ & _ }
  }

  private val msgsDir = new File(
      System.getProperty("user.dir"), 
      config.getString("demo.msgs-dir"))
  
  if (!msgsDir.exists()) msgsDir.mkdir()
  
  def fileMsgs(clazzs: Class[_]*): Boolean = {
    val filename = s"""msgs-${clazzs map (_.getSimpleName) mkString "-"}"""
    val file = new File(msgsDir, filename)
    if (!file.exists()) file.createNewFile()
    val fos = new FileOutputStream(file, /* append = */ true)
    val ps = new PrintStream(fos, /* autoFlush = */ true)
    val fileActor = psAct(ps, filename)
    clazzs map { subs.subscribe(fileActor, _) } reduce { _ & _ }
    
    // FIXME: handles shouldn't leak; should close the files - maybe listen for disconnect?
  }

  def defaultMsgHandling(): Boolean = Seq(
    consoleMsgs(
      classOf[ErrorMessage],
      classOf[SystemMessage],
      classOf[ReferenceDataMessage],
      classOf[OrderManagementMessage]),
    fileMsgs(classOf[MarketDataMessage]),
    fileMsgs(classOf[AccountMessage]),
    fileMsgs(classOf[HistoricalDataMessage])) reduce (_ & _)

  def connect() = conn ! IbConnect(port = 7496, clientId = 0)
  def disconnect() = conn ! IbDisconnect("all done")

  def stk(symbol: String, exchange: String = "SMART", currency: CurrencyType = CurrencyType.USD) =
    Contract(secType = SecType.STK, symbol = symbol, exchange = exchange, currency = currency)

  def fut(symbol: String, expiry: String, exchange: String = "SMART", currency: CurrencyType = CurrencyType.USD) =
    Contract(secType = SecType.FUT, symbol = symbol, expiry = expiry, currency = "USD")

  def opt(symbol: String, expiry: String, right: Right, exchange: String = "SMART", currency: CurrencyType = CurrencyType.USD) =
    Contract(secType = SecType.OPT, symbol = symbol, right = right, expiry = expiry, currency = "USD")
}