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

import com.ib.client.{ EWrapperMsgGenerator => emg, AnyWrapperMsgGenerator => amg, _ }

class EWrapperBase extends EWrapper {

  // Members declared in com.ib.client.AnyWrapper
  def connectionClosed(): Unit = amg.connectionClosed
  def error(id: Int, errorCode: Int, errorMsg: String): Unit =
    println(amg.error(id, errorCode, errorMsg))
  def error(str: String): Unit = println(amg.error(str))
  def error(e: Exception): Unit = println(amg.error(e))

  // Members declared in com.ib.client.EWrapper
  def accountDownloadEnd(x$1: String): Unit = ???
  def bondContractDetails(x$1: Int, x$2: com.ib.client.ContractDetails): Unit = ???
  def commissionReport(x$1: com.ib.client.CommissionReport): Unit = ???
  def contractDetails(x$1: Int, x$2: com.ib.client.ContractDetails): Unit = ???
  def contractDetailsEnd(x$1: Int): Unit = ???
  def currentTime(x$1: Long): Unit = ???
  def deltaNeutralValidation(x$1: Int, x$2: com.ib.client.UnderComp): Unit = ???
  def execDetails(x$1: Int, x$2: com.ib.client.Contract, x$3: com.ib.client.Execution): Unit = ???
  def execDetailsEnd(x$1: Int): Unit = ???
  def fundamentalData(x$1: Int, x$2: String): Unit = ???
  def historicalData(x$1: Int, x$2: String, x$3: Double, x$4: Double, x$5: Double, x$6: Double, x$7: Int, x$8: Int, x$9: Double, x$10: Boolean): Unit = ???
  def managedAccounts(x$1: String): Unit = ???
  def marketDataType(x$1: Int, x$2: Int): Unit = ???
  def nextValidId(x$1: Int): Unit = ???
  def openOrder(x$1: Int, x$2: com.ib.client.Contract, x$3: com.ib.client.Order, x$4: com.ib.client.OrderState): Unit = ???
  def openOrderEnd(): Unit = ???
  def orderStatus(x$1: Int, x$2: String, x$3: Int, x$4: Int, x$5: Double, x$6: Int, x$7: Int, x$8: Double, x$9: Int, x$10: String): Unit = ???
  def realtimeBar(x$1: Int, x$2: Long, x$3: Double, x$4: Double, x$5: Double, x$6: Double, x$7: Long, x$8: Double, x$9: Int): Unit = ???
  def receiveFA(x$1: Int, x$2: String): Unit = ???
  def scannerData(x$1: Int, x$2: Int, x$3: com.ib.client.ContractDetails, x$4: String, x$5: String, x$6: String, x$7: String): Unit = ???
  def scannerDataEnd(x$1: Int): Unit = ???
  def scannerParameters(x$1: String): Unit = ???
  def tickEFP(x$1: Int, x$2: Int, x$3: Double, x$4: String, x$5: Double, x$6: Int, x$7: String, x$8: Double, x$9: Double): Unit = ???
  def tickGeneric(x$1: Int, x$2: Int, x$3: Double): Unit = ???
  def tickOptionComputation(x$1: Int, x$2: Int, x$3: Double, x$4: Double, x$5: Double, x$6: Double, x$7: Double, x$8: Double, x$9: Double, x$10: Double): Unit = ???
  def tickPrice(x$1: Int, x$2: Int, x$3: Double, x$4: Int): Unit = ???
  def tickSize(x$1: Int, x$2: Int, x$3: Int): Unit = ???
  def tickSnapshotEnd(x$1: Int): Unit = ???
  def tickString(x$1: Int, x$2: Int, x$3: String): Unit = ???
  def updateAccountTime(x$1: String): Unit = ???
  def updateAccountValue(x$1: String, x$2: String, x$3: String, x$4: String): Unit = ???
  def updateMktDepth(x$1: Int, x$2: Int, x$3: Int, x$4: Int, x$5: Double, x$6: Int): Unit = ???
  def updateMktDepthL2(x$1: Int, x$2: Int, x$3: String, x$4: Int, x$5: Int, x$6: Double, x$7: Int): Unit = ???
  def updateNewsBulletin(x$1: Int, x$2: Int, x$3: String, x$4: String): Unit = ???
  def updatePortfolio(x$1: com.ib.client.Contract, x$2: Int, x$3: Double, x$4: Double, x$5: Double, x$6: Double, x$7: Double, x$8: String): Unit = ???
  def position(x$1: String, x$2: com.ib.client.Contract, x$3: Int, x$4: Double): Unit = ???
  def positionEnd(): Unit = ???
  def accountSummary(x$1: Int, x$2: String, x$3: String, x$4: String, x$5: String): Unit = ???
  def accountSummaryEnd(x$1: Int): Unit = ???
}