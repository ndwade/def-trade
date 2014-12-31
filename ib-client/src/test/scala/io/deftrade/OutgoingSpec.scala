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

class OutgoingSpec extends OutgoingSpecBase("OutgoingSpec") {

  import com.ib.client.{
    Contract => IbContract,
    ContractDetails => IbContractDetails,
    Order => IbOrder,
    OrderState => IbOrderState,
    Execution => IbExecution,
    UnderComp => IbUnderComp
  }

  import IB._

  import misc.repeat
  
  val n = 10

  "IbConnection sending a ReqContractDetails message" should {

    "send same byte stream as EClientSocket for PlaceOrder" in repeat(n) {
      val (orderId, ibOrderId) = genDtoAndIbHomolog[OrderId]
      val (contract, ibContract) = genDtoAndIbHomolog[Contract]
      val (order, ibOrder) = genDtoAndIbHomolog[Order]
      assertOmWritesEqual(
        PlaceOrder(id = orderId, contract = contract, order = order),
        ibSocket.placeOrder(ibOrderId, ibContract, ibOrder))
    }

    "send same byte stream as EClientSocket for ReqContractDetails" in repeat(n) {
      val (reqId, ibReqId) = genDtoAndIbHomolog[ReqId]
      val (contract, ibContract) = genDtoAndIbHomolog[Contract]
      assertOmWritesEqual(
        ReqContractDetails(reqId = reqId, contract = contract),
        ibSocket.reqContractDetails(ibReqId, ibContract))
    }
    "send same byte stream as EClientSocket for ReqIds" in repeat(n) {
      val numIds = 123 // this is basically ignored in the API as far as I can see.
      assertOmWritesEqual(
        ReqIds(numIds),
        ibSocket.reqIds(numIds))
    }
    "send same byte stream as EClientSocket for ReqMktData" in repeat(n) {
      import GenericTickType._
      misc.repeat(n) {
        val (tickerId, ibTickerId) = genDtoAndIbHomolog[ReqId]
        val genericTickList = generate[List[GenericTickType]]
        val ibGenericTickList = (genericTickList map (v => (v: String))) mkString ","
        val (contract, ibContract) = genDtoAndIbHomolog[Contract]
        val snapshot = generate[Boolean]
        val ibSnapshot = snapshot
        assertOmWritesEqual(
          ReqMktData(tickerId, contract, genericTickList, snapshot),
          ibSocket.reqMktData(ibTickerId, ibContract, ibGenericTickList, ibSnapshot))
        ibContract.m_secType = "BAG"
        val contract2 = contract.copy(secType = SecType.BAG)
        // log.info(contract2.indent)
        assertOmWritesEqual(
          ReqMktData(tickerId, contract2, genericTickList, snapshot),
          ibSocket.reqMktData(ibTickerId, ibContract, ibGenericTickList, ibSnapshot))
      }
    }
    "send the same byte stream as EClientSocket for CancelMktData" in repeat(n) {
      val (tickerId, ibTickerId) = genDtoAndIbHomolog[ReqId]
      assertOmWritesEqual(
        CancelMktData(tickerId),
        ibSocket.cancelMktData(ibTickerId))
    }
    "send the same byte stream as EClientSocket for CancelOrder" in repeat(n) {
      val (orderId, ibOrderId) = genDtoAndIbHomolog[OrderId]
      assertOmWritesEqual(
        CancelOrder(orderId),
        ibSocket.cancelOrder(ibOrderId))
    }
    "send the same byte stream as EClientSocket for ReqOpenOrders" in repeat(n) {
      assertOmWritesEqual(
        ReqOpenOrders(),
        ibSocket.reqOpenOrders())
    }
    "send same byte stream as EClientSocket for ReqAccountUpdates" in repeat(n) {
      val subscribe = true
      val acctCode = "ACME"
      assertOmWritesEqual(
        ReqAccountUpdates(subscribe, acctCode),
        ibSocket.reqAccountUpdates(subscribe, acctCode))
    }
    "send same byte stream as EClientSocket for ReqExecutions" in repeat(n) {
      val (reqId, ibReqId) = genDtoAndIbHomolog[ReqId]
      val (executionFilter, ibExecutionFilter) = genDtoAndIbHomolog[ExecutionFilter]
      assertOmWritesEqual(
        ReqExecutions(reqId, executionFilter),
        ibSocket.reqExecutions(ibReqId, ibExecutionFilter))
    }
    "send same byte stream as EClientSocket for ReqMktDepth" in repeat(n) {
      val (tickerId, ibTickerId) = genDtoAndIbHomolog[ReqId]
      val (contract, ibContract) = genDtoAndIbHomolog[Contract]
      val numRows = generate[Int]
      assertOmWritesEqual(
        ReqMktDepth(tickerId, contract, numRows),
        ibSocket.reqMktDepth(ibTickerId, ibContract, numRows))
    }
    "send same byte stream as EClientSocket for CancelMktDepth" in repeat(n) {
      val (tickerId, ibTickerId) = genDtoAndIbHomolog[ReqId]
      val numRows = generate[Int]
      assertOmWritesEqual(
        CancelMktDepth(tickerId),
        ibSocket.cancelMktDepth(ibTickerId))
    }
    "send same byte stream as EClientSocket for ReqNewsBulletins" in repeat(n) {
      val allMsgs = generate[Boolean]
      assertOmWritesEqual(
        ReqNewsBulletins(allMsgs),
        ibSocket.reqNewsBulletins(allMsgs))
    }
    "send same byte stream as EClientSocket for CancelNewsBulletins" in repeat(n) {
      assertOmWritesEqual(
        CancelNewsBulletins(),
        ibSocket.cancelNewsBulletins())
    }
    "send same byte stream as EClientSocket for SetServerLogLevel" in repeat(n) {
      val logLevel = generate[ServerLogLevel.ServerLogLevel]
      val ibLogLevel = logLevel.id
      assertOmWritesEqual(
        SetServerLogLevel(logLevel),
        ibSocket.setServerLogLevel(ibLogLevel))
    }
    "send same byte stream as EClientSocket for ReqAutoOpenOrders" in repeat(n) {
      assertOmWritesEqual(
        ReqAllOpenOrders(),
        ibSocket.reqAllOpenOrders())
    }
    "send same byte stream as EClientSocket for ReqAllOpenOrders" in repeat(n) {
      val bAutoBind = generate[Boolean]
      assertOmWritesEqual(
        ReqAutoOpenOrders(bAutoBind),
        ibSocket.reqAutoOpenOrders(bAutoBind))
    }
    "send same byte stream as EClientSocket for ReqHistoricalData" in repeat(n) {
      val (reqId, ibReqId) = genDtoAndIbHomolog[ReqId]
      val (contract, ibContract) = genDtoAndIbHomolog[Contract]
      val endDateTime = generate[String]
      val durationStr = generate[String]
      val barSizeSetting = generate[String]
      val whatToShow = generate[String]
      val useRTH = generate[Int]
      val formatDate = generate[DateFormatType.DateFormatType]
      val ibFormatDate = formatDate.id
      assertOmWritesEqual(
        ReqHistoricalData(reqId, contract,
          endDateTime, durationStr, barSizeSetting, whatToShow, useRTH, formatDate),
        ibSocket.reqHistoricalData(ibReqId, ibContract,
          endDateTime, durationStr, barSizeSetting, whatToShow, useRTH, ibFormatDate))
    }

    "send same byte stream as EClientSocket for CancelHistoricalData" in repeat(n) {
      val (reqId, ibReqId) = genDtoAndIbHomolog[ReqId]
      assertOmWritesEqual(
        CancelHistoricalData(reqId),
        ibSocket.cancelHistoricalData(ibReqId))
    }

    "send same byte stream as EClientSocket for ExerciseOptions" in repeat(n) {
      val (reqId, ibReqId) = genDtoAndIbHomolog[ReqId]
      val (contract, ibContract) = genDtoAndIbHomolog[Contract]
      val exerciseAction = generate[ExerciseType.ExerciseType]
      val ibExerciseAction = exerciseAction.id
      val exerciseQuantity = generate[Int]
      val account = generate[String]
      val overrideDefaults = generate[Boolean]
      assertOmWritesEqual(
        ExerciseOptions(reqId, contract, exerciseAction,
          exerciseQuantity, account, overrideDefaults),
        ibSocket.exerciseOptions(ibReqId, ibContract, ibExerciseAction,
          exerciseQuantity, account, if (overrideDefaults) 1 else 0))
      val contractBag = contract.copy(secType = SecType.BAG)
      val ibContractBag = ibCopyOf(contractBag).asInstanceOf[IbContract]
      assertOmWritesEqual(
        ExerciseOptions(reqId, contractBag, exerciseAction,
          exerciseQuantity, account, overrideDefaults),
        ibSocket.exerciseOptions(ibReqId, ibContractBag, ibExerciseAction,
          exerciseQuantity, account, if (overrideDefaults) 1 else 0))
    }

    "send same byte stream as EClientSocket for ReqScannerSubscription" in repeat(n) {
      val (reqId, ibReqId) = genDtoAndIbHomolog[ReqId]
      val (subscription, ibSubscription) = genDtoAndIbHomolog[ScannerSubscription]
      assertOmWritesEqual(
        ReqScannerSubscription(reqId, subscription),
        ibSocket.reqScannerSubscription(ibReqId, ibSubscription))
    }

    "send same byte stream as EClientSocket for CancelScannerSubscription" in repeat(n) {
      val (reqId, ibReqId) = genDtoAndIbHomolog[ReqId]
      assertOmWritesEqual(
        CancelScannerSubscription(reqId),
        ibSocket.cancelScannerSubscription(ibReqId))
    }

    "send same byte stream as EClientSocket for ReqScannerParameters" in repeat(n) {
      assertOmWritesEqual(
        ReqScannerParameters(),
        ibSocket.reqScannerParameters())
    }

    "send same byte stream as EClientSocket for ReqCurrentTime" in repeat(n) {
      assertOmWritesEqual(
        ReqCurrentTime(),
        ibSocket.reqCurrentTime())
    }
    "send same byte stream as EClientSocket for ReqRealTimeBars" in repeat(n) {
      val (tickerId, ibTickerId) = genDtoAndIbHomolog[ReqId]
      val (contract, ibContract) = genDtoAndIbHomolog[Contract]
      val barSize = generate[Int]
      val whatToShow = generate[String]
      val useRTH = generate[Boolean]
      assertOmWritesEqual(
        ReqRealTimeBars(tickerId, contract,
          barSize, whatToShow, useRTH),
        ibSocket.reqRealTimeBars(ibTickerId, ibContract,
          barSize, whatToShow, useRTH))
    }
    "send same byte stream as EClientSocket for CancelRealTimeBars" in repeat(n) {
      val (tickerId, ibTickerId) = genDtoAndIbHomolog[ReqId]
      assertOmWritesEqual(
        CancelRealTimeBars(tickerId),
        ibSocket.cancelRealTimeBars(ibTickerId))
    }
    "send same byte stream as EClientSocket for ReqFundamentalData" in repeat(n) {
      val (reqId, ibReqId) = genDtoAndIbHomolog[ReqId]
      val (contract, ibContract) = genDtoAndIbHomolog[Contract]
      val reportType = generate[FundamentalType.FundamentalType]
      assertOmWritesEqual(
        ReqFundamentalData(reqId, contract,
          reportType),
        ibSocket.reqFundamentalData(ibReqId, ibContract,
          reportType.toString))
    }
    "send same byte stream as EClientSocket for CancelFundamentalData" in repeat(n) {
      val (reqId, ibReqId) = genDtoAndIbHomolog[ReqId]
      assertOmWritesEqual(
        CancelFundamentalData(reqId),
        ibSocket.cancelFundamentalData(ibReqId))
    }
    "send same byte stream as EClientSocket for CalculateImpliedVolatility" in repeat(n) {
      val (reqId, ibReqId) = genDtoAndIbHomolog[ReqId]
      val (contract, ibContract) = genDtoAndIbHomolog[Contract]
      val optionPrice = generate[Double]
      val underPrice = generate[Double]
      assertOmWritesEqual(
        CalculateImpliedVolatility(reqId, contract,
          optionPrice, underPrice),
        ibSocket.calculateImpliedVolatility(ibReqId, ibContract,
          optionPrice, underPrice))
    }
    "send same byte stream as EClientSocket for CancelCalculateImpliedVolatility" in repeat(n) {
      val (reqId, ibReqId) = genDtoAndIbHomolog[ReqId]
      assertOmWritesEqual(
        CancelCalculateImpliedVolatility(reqId),
        ibSocket.cancelCalculateImpliedVolatility(ibReqId))
    }
    "send same byte stream as EClientSocket for CalculateOptionPrice" in repeat(n) {
      val (reqId, ibReqId) = genDtoAndIbHomolog[ReqId]
      val (contract, ibContract) = genDtoAndIbHomolog[Contract]
      val volatility = generate[Double]
      val underPrice = generate[Double]
      assertOmWritesEqual(
        CalculateOptionPrice(reqId, contract,
          volatility, underPrice),
        ibSocket.calculateOptionPrice(ibReqId, ibContract,
          volatility, underPrice))
    }
    "send same byte stream as EClientSocket for CancelCalculateOptionPrice" in repeat(n) {
      val (reqId, ibReqId) = genDtoAndIbHomolog[ReqId]
      assertOmWritesEqual(
        CancelCalculateOptionPrice(reqId),
        ibSocket.cancelCalculateOptionPrice(ibReqId))
    }

    "send same byte stream as EClientSocket for ReqGlobalCancel" in repeat(n) {
      assertOmWritesEqual(
        ReqGlobalCancel(),
        ibSocket.reqGlobalCancel())
    }
    "send same byte stream as EClientSocket for ReqMarketDataType" in repeat(n) {
      val marketDataType = generate[MktDataType.MktDataType]
      assertOmWritesEqual(
        ReqMarketDataType(marketDataType),
        ibSocket.reqMarketDataType(marketDataType.id))
    }

  }  
}