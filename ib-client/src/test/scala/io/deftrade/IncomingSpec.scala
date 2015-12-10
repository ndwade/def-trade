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

class IncomingSpec extends IncomingSpecBase("IncomingSpec") {

  import com.ib.client.{
    Contract => IbContract,
    ContractDetails => IbContractDetails,
    Order => IbOrder,
    OrderState => IbOrderState,
    Execution => IbExecution,
    UnderComp => IbUnderComp
  }

  import IB._

  // TODO: Error msg

  override val nreps = 20

  "IbConnection receiving an IncomingMessage from the TestServer " should {

    "deserialize Error homologous with EReader" in {
      homologize[Error](versions = 1 to 2) {
        e =>
          w =>
            new EWrapperCallbacks(w) {
              override def error(id: Int, errorCode: Int, errorMsg: String): Unit = {
                asyncAssertionBlock(w) {
                  assert(e.eid =#= id)
                  assert(e.errorCode =#= errorCode)
                  assert(e.errorMsg =#= errorMsg)
                }
              }
              override def error(errorMsg: String): Unit = {
                asyncAssertionBlock(w) {
                  assert(e.eid =#= -1)
                  assert(e.errorCode =#= -1)
                  assert(e.errorMsg =#= errorMsg)
                }
              }
            }
      }
    }

    "deserialize NextValidId homologous with EReader" in {
      homologize[NextValidId](versions = 1 to 1) {
        nvi =>
          w =>
            new EWrapperCallbacks(w) {
              override def nextValidId(orderId: Int): Unit = {
                asyncAssertionBlock(w) {
                  assert(nvi.orderId =#= orderId)
                }
              }
            }
      }
    }

    "deserialize ContractDetailsCont homologous with EReader" in {
      homologize[ContractDetailsCont](versions = 1 to 8) {
        cdc =>
          w =>
            new EWrapperCallbacks(w) {
              override def contractDetails(reqId: Int, cd: IbContractDetails): Unit = {
                asyncAssertionBlock(w) {
                  assert(cdc.reqId =#= reqId)
                  assert(cdc.contractDetails =#= cd)
                }
              }
            }
      }
    }
    "deserialize ContractDetailsEnd homologous with EReader" in {
      homologize[ContractDetailsEnd](versions = 1 to 1) {
        cde =>
          w =>
            new EWrapperCallbacks(w) {
              override def contractDetailsEnd(reqId: Int): Unit = {
                asyncAssertionBlock(w) {
                  assert(cde.reqId =#= reqId)
                }
              }
            }
      }
    }
    "deserialize BondContractDetails homologous with EReader" in {
      homologize[BondContractDetails](versions = 1 to 8) {
        bcd =>
          w =>
            new EWrapperCallbacks(w) {
              override def bondContractDetails(reqId: Int, cd: IbContractDetails): Unit = {
                asyncAssertionBlock(w) {
                  assert(bcd.reqId =#= reqId)
                  assert(bcd.contract =#= cd)
                }
              }
            }
      }
    }
    "deserialize OpenOrder homologous with EReader" in {
      homologize[OpenOrder](versions = 1 to 33) {
        oo =>
          w =>
            new EWrapperCallbacks(w) {
              override def openOrder(
                orderId: Int, contract: IbContract, order: IbOrder, orderState: IbOrderState): Unit = {
                asyncAssertionBlock(w) {
                  assert(oo.orderId =#= orderId)
                  assert(oo.contract =#= contract)
                  assert(oo.order =#= order)
                  assert(oo.orderState =#= orderState)
                }
              }
            }
      }
    }
    "deserialize OpenOrderEnd homologous with EReader" in {
      homologize[OpenOrderEnd](versions = 1 to 1) {
        ooe =>
          w =>
            new EWrapperCallbacks(w) {
              override def openOrderEnd(): Unit = {
                asyncAssertionBlock(w) {
                }
              }
            }
      }
    }
    "deserialize ExecDetails homologous with EReader" in {
      homologize[ExecDetails](versions = 1 to 10) {
        ed =>
          w =>
            new EWrapperCallbacks(w) {
              override def execDetails(reqId: Int, contract: IbContract, exec: IbExecution): Unit = {
                asyncAssertionBlock(w) {
                  assert(ed.reqId =#= reqId)
                  assert(ed.contract =#= contract)
                  assert(ed.exec =#= exec)
                }
              }
            }
      }
    }

    "deserialize OrderStatus homologous with EReader" in {
      homologize[OrderStatus](versions = 1 to 8) {
        os =>
          w =>
            new EWrapperCallbacks(w) {
              override def orderStatus(orderId: Int, status: String, filled: Int, remaining: Int,
                avgFillPrice: Double, permId: Int,
                parentId: Int, lastFillPrice: Double,
                clientId: Int, whyHeld: String): Unit = {
                asyncAssertionBlock(w) {
                  assert(os.orderId =#= orderId)
                  assert(os.status =#= status)
                  assert(os.filled =#= filled)
                  assert(os.remaining =#= remaining)
                  assert(os.avgFillPrice =#= avgFillPrice)
                  assert(os.permId =#= permId)
                  assert(os.parentId =#= parentId)
                  assert(os.lastFillPrice =#= lastFillPrice)
                  assert(os.clientId =#= clientId)
                  assert(os.whyHeld =#= whyHeld)
                }
              }
            }
      }
    }
    "deserialize UpdateMkDepth homologous with EReader" in {
      homologize[UpdateMktDepth](versions = 1 to 3) {
        umd =>
          w =>
            new EWrapperCallbacks(w) {
              override def updateMktDepth(tickerId: Int, position: Int, operation: Int,
                side: Int, price: Double, size: Int): Unit = {
                asyncAssertionBlock(w) {
                  assert(umd.tickerId =#= tickerId)
                  assert(umd.position =#= position)
                  assert(umd.operation =#= operation)
                  assert(umd.side =#= side)
                  assert(umd.price =#= price)
                  assert(umd.size =#= size)
                }
              }
            }
      }
    }
    "deserialize UpdateMkDepthL2 homologous with EReader" in {
      homologize[UpdateMktDepthL2](versions = 1 to 3) {
        umd2 =>
          w =>
            new EWrapperCallbacks(w) {
              override def updateMktDepthL2(tickerId: Int, position: Int,
                marketMaker: String,
                operation: Int, side: Int, price: Double, size: Int): Unit = {
                asyncAssertionBlock(w) {
                  assert(umd2.tickerId =#= tickerId)
                  assert(umd2.position =#= position)
                  assert(umd2.marketMaker =#= marketMaker)
                  assert(umd2.operation =#= operation)
                  assert(umd2.side =#= side)
                  assert(umd2.price =#= price)
                  assert(umd2.size =#= size)
                }
              }
            }
      }
    }
    "deserialize TickSize homologous with EReader" in {
      homologize[TickSize](versions = 1 to 3) {
        ts =>
          w =>
            new EWrapperCallbacks(w) {
              override def tickSize(tickerId: Int, field: Int, size: Int): Unit = {
                asyncAssertionBlock(w) {
                  assert(ts.tickerId =#= tickerId)
                  assert(ts.field =#= field)
                  assert(ts.size =#= size)
                }
              }
            }
      }
    }

    "deserialize TickEFP homologous with EReader" in {
      homologize[TickEFP](versions = 1 to 1) {
        ts =>
          w =>
            new EWrapperCallbacks(w) {
              override def tickEFP(tickerId: Int, tickType: Int, basisPoints: Double,
                formattedBasisPoints: String, impliedFuture: Double, holdDays: Int,
                futureExpiry: String,
                dividendImpact: Double, dividendsToExpiry: Double): Unit = {
                asyncAssertionBlock(w) {
                  assert(ts.tickerId =#= tickerId)
                  assert(ts.tickType =#= tickType)
                  assert(ts.basisPoints =#= basisPoints)
                  assert(ts.formattedBasisPoints =#= formattedBasisPoints)
                  assert(ts.impliedFuture =#= impliedFuture)
                  assert(ts.holdDays =#= holdDays)
                  assert(ts.futureExpiry =#= futureExpiry)
                  assert(ts.dividendImpact =#= dividendImpact)
                  assert(ts.dividendsToExpiry =#= dividendsToExpiry)
                }
              }
            }
      }
    }

    "deserialize UpdateNewsBulliten homologous with EReader" in {
      homologize[UpdateNewsBulletin](versions = 1 to 1) {
        unb =>
          w =>
            new EWrapperCallbacks(w) {
              override def updateNewsBulletin(msgId: Int, msgType: Int, message: String,
                origExchange: String): Unit = {
                asyncAssertionBlock(w) {
                  assert(unb.msgId =#= msgId)
                  assert(unb.msgType =#= msgType)
                  assert(unb.message =#= message)
                  assert(unb.origExchange =#= origExchange)
                }
              }
            }
      }
    }
    "deserialize FundamentalData homologous with EReader" in {
      homologize[FundamentalData](versions = 1 to 1) {
        fd =>
          w =>
            new EWrapperCallbacks(w) {
              override def fundamentalData(reqId: Int, data: String): Unit = {
                asyncAssertionBlock(w) {
                  assert(fd.reqId =#= reqId)
                  assert(fd.data =#= data)
                }
              }
            }
      }
    }
    "deserialize DeltaNeutralValidation homologous with EReader" in {
      homologize[DeltaNeutralValidation](versions = 1 to 1) {
        dnv =>
          w =>
            new EWrapperCallbacks(w) {
              override def deltaNeutralValidation(reqId: Int, underComp: IbUnderComp): Unit = {
                asyncAssertionBlock(w) {
                  assert(dnv.reqId =#= reqId)
                  assert(dnv.underComp =#= underComp)
                }
              }
            }
      }
    }
    "deserialize MarketDataType homologous with EReader" in {
      homologize[MarketDataType](versions = 1 to 1) {
        mdt =>
          w =>
            new EWrapperCallbacks(w) {
              override def marketDataType(reqId: Int, marketDataType: Int): Unit = {
                asyncAssertionBlock(w) {
                  assert(mdt.reqId =#= reqId)
                  assert(mdt.marketDataType =#= marketDataType)
                }
              }
            }
      }
    }
    "deserialize TickPrice (and TickSize) homologous with EReader" in {
      homologize2[TickPrice, TickSize](versions = 1 to 2) {
        (tp, ots) =>
          w =>
            new EWrapperCallbacks(w) {
              override def tickPrice(tickerId: Int, field: Int, price: Double, canAutoExecute: Int) {
                asyncAssertionBlock(w) {
                  assert(tp.tickerId =#= tickerId)
                  assert(tp.field =#= field)
                  assert(tp.price =#= price)
                  assert(tp.canAutoExecute =#= (canAutoExecute != 0))
                }
              }
              override def tickSize(tickerId: Int, field: Int, size: Int): Unit = {
                asyncAssertionBlock(w) {
                  ots match {
                    case Some(ts) =>
                      log.info("Got {}", ts)
                      assert(ts.tickerId =#= tickerId)
                      assert(ts.field =#= field)
                      assert(ts.size =#= size)
                    case None => fail("EWrapper.tickSize() called when no TickSize msg received")
                  }
                }
              }
            }
      }
    }
    "deserialize TickOptionComputation homologous with EReader given explicit test vector" in {
      import ImplicitConversions.{ d2s, i2s }
      import TickType._
      import scala.language.implicitConversions
      implicit def tt2s(tt: TickType) = tt.id.toString
      val runs: List[(Int, List[String])] = List(
        (6, List(23, MODEL_OPTION, 33.3, .5, 3.14, 1.68, .9, .7, .3, 99.95)),
        (6, List(23, ASK_OPTION, 33.3, .5, 3.14, 1.68, .9, .7, .3, 99.95)),
        (5, List(19, MODEL_OPTION, 33.3, .5, 3.14, 1.68)),
        (5, List(19, ASK_OPTION, 33.3, .5)),
        (6, List(32, MODEL_OPTION, -1.0, -2.0, -1.0, -1.0, -2.0, -2.0, -2.0, -1.0)))
      runs foreach {
        case (version, fields) =>
          homologize[TickOptionComputation](version, fields) {
            toc =>
              w =>
                new EWrapperCallbacks(w) {
                  override def tickOptionComputation(
                    tickerId: Int, field: Int,
                    impliedVol: Double, delta: Double, optPrice: Double, pvDividend: Double,
                    gamma: Double, vega: Double, theta: Double, undPrice: Double) {

                    log.info("toc={}", toc)

                    asyncAssertionBlock(w) {
                      assert(toc.tickerId =#= tickerId)
                      assert(toc.field =#= field)
                      assert(toc.impliedVol =#= impliedVol)
                      assert(toc.delta =#= delta)
                      assert(toc.optPrice =#= optPrice)
                      assert(toc.pvDividend =#= pvDividend)
                      assert(toc.gamma =#= gamma)
                      assert(toc.vega =#= vega)
                      assert(toc.theta =#= theta)
                      assert(toc.undPrice =#= undPrice)
                    }
                  }
                }
          }
      }
    }
    "deserialize HistoricalData homologous with EReader given explicit test vector" in {
      import ImplicitConversions._
      import Currency._
      val runs: List[(Int, Seq[String], Seq[Seq[String]])] = List(
        (1,
          List(ReqId(111), /*"19990401", "19990402",*/ 5),
          Vector.fill(5)(List("20010720", 3.14, 3.16, 3.11, 3.14, 777, 3.13, "TrUe"))),
        (2,
          List(ReqId(222), "19990401", "19990402", 5),
          Vector.fill(5)(List("20010720", 3.14, 3.16, 3.11, 3.14, 777, 3.13, "TrUe"))),
        (3,
          List(ReqId(333), "19990401", "19990402", 5),
          Vector.fill(5)(List("20010720", 3.14, 3.16, 3.11, 3.14, 777, 3.13, "TrUe", 67))))
      runs foreach {
        case (version, prolog, repss) =>
          var ended = false
          homologize[HistoricalData, HistoricalDataEnd](version, prolog, repss) {
            (hds, hde) =>
              w =>
                new EWrapperCallbacks(w) {
                  var i = 0 // confined to com.ib.client.EReader callback thread
                  override def historicalData(reqId: Int, date: String, open: Double, high: Double,
                    low: Double, close: Double, volume: Int, count: Int,
                    WAP: Double, hasGaps: Boolean) {

                    if (!(date startsWith "finished")) {
                      val hd = hds(i)
                      log.debug("hd={}", hd)
                      asyncAssertionBlock(w) {
                        assert(hd.reqId =#= reqId)
                        assert(hd.date =#= date)
                        assert(hd.open =#= open)
                        assert(hd.high =#= high)
                        assert(hd.low =#= low)
                        assert(hd.close =#= close)
                        assert(hd.volume =#= volume)
                        assert(hd.count =#= count)
                        assert(hd.WAP =#= WAP)
                        assert(hd.hasGaps =#= hasGaps)
                        i += 1
                      }
                    } else {
                      asyncAssertionBlock(w) {
                        val dates = if (version >= 2) date split "-" drop 1 else Array("", "")
                        assert(hde.reqId =#= reqId)
                        assert(hde.startDate =#= dates(0))
                        assert(hde.endDate =#= dates(1))
                        assert(i === hds.size)
                        ended = true // happens-before invocation of waiter.dismiss()
                      }
                    }
                  }
                }
          }
          assert(ended) // ordered by Waiter.await synchronization.
      }
    }

    "deserialize UpdateAccountValue homologous with EReader" in {
      homologize[UpdateAccountValue](versions = 1 to 3) {
        uav =>
          w =>
            new EWrapperCallbacks(w) {
              override def updateAccountValue(key: String, value: String, currency: String,
                accountName: String): Unit = {
                asyncAssertionBlock(w) {
                  assert(uav.key =#= key)
                  assert(uav.value =#= value)
                  assert(uav.currency =#= currency)
                  assert(uav.accountName =#= accountName)
                }
              }
            }
      }
    }

    "deserialize UpdatePortfolio homologous with EReader" in {
      homologize[UpdatePortfolio](versions = 1 to 8) {
        up =>
          w =>
            new EWrapperCallbacks(w) {
              override def updatePortfolio(contract: IbContract, position: Int, marketPrice: Double,
                marketValue: Double, averageCost: Double, unrealizedPNL: Double,
                realizedPNL: Double, accountName: String): Unit = {
                asyncAssertionBlock(w) {
                  assert(up.contract =#= contract)
                  assert(up.position =#= position)
                  assert(up.marketPrice =#= marketPrice)
                  assert(up.marketValue =#= marketValue)
                  assert(up.averageCost =#= averageCost)
                  assert(up.unrealizedPNL =#= unrealizedPNL)
                  assert(up.realizedPNL =#= realizedPNL)
                  assert(up.accountName =#= accountName)
                }
              }
            }
      }
    }

    "deserialize ScannerParameters homologous with EReader" in {
      homologize[ScannerParameters](versions = 1 to 1) {
        sp =>
          w =>
            new EWrapperCallbacks(w) {
              override def scannerParameters(xml: String): Unit = {
                asyncAssertionBlock(w) {
                  assert(sp.xml =#= xml)
                }
              }
            }
      }
    }
    "deserialize UpdateAccountTime homologous with EReader" in {
      homologize[UpdateAccountTime](versions = 1 to 1) {
        uat =>
          w =>
            new EWrapperCallbacks(w) {
              override def updateAccountTime(timeStamp: String): Unit = {
                asyncAssertionBlock(w) {
                  assert(uat.timeStamp =#= timeStamp)
                }
              }
            }
      }
    }
    "deserialize AccountDownloadEnd homologous with EReader" in {
      homologize[AccountDownloadEnd](versions = 1 to 1) {
        ade =>
          w =>
            new EWrapperCallbacks(w) {
              override def accountDownloadEnd(accountName: String): Unit = {
                asyncAssertionBlock(w) {
                  assert(ade.accountName =#= accountName)
                }
              }
            }
      }
    }
    "deserialize ScannerData homologous with EReader given explicit test vector" in {
      import ImplicitConversions._
      //      import MoneyType.implicitConversions._
      import Currency._
      val runs: List[(Int, Seq[String], Seq[Seq[String]])] = List(
        (1,
          List(ReqId(333), 5),
          Vector.fill(5)(List(93, /*("12345": ConId),*/ "NVDA", SecType.STK, "19990909",
            33.32, Right.Put, "NASDAQ", "LIRA", "NVDA.local", "marketName", "tradingClass",
            "distance", "benchmark", "projection" /*, "legs"*/ ))),
        (2,
          List(ReqId(333), 5),
          Vector.fill(5)(List(93, /*("12345": ConId),*/ "NVDA", SecType.STK, "19990909",
            33.32, Right.Put, "NASDAQ", "LIRA", "NVDA.local", "marketName", "tradingClass",
            "distance", "benchmark", "projection", "legs"))),
        (3,
          List(ReqId(333), 5),
          Vector.fill(5)(List(93, ("12345": ConId), "NVDA", SecType.STK, "19990909",
            33.32, Right.Put, "NASDAQ", "LIRA", "NVDA.local", "marketName", "tradingClass",
            "distance", "benchmark", "projection", "legs"))))
      runs foreach {
        case (version, prolog, repss) =>
          var ended = false
          homologize[ScannerData, ScannerDataEnd](version, prolog, repss) {
            (sds, sde) =>
              w =>
                new EWrapperCallbacks(w) {
                  var i = 0 // confined to com.ib.client.EReader callback thread
                  override def scannerData(reqId: Int, rank: Int, contractDetails: IbContractDetails,
                    distance: String, benchmark: String, projection: String, legsStr: String) {

                    val sd = sds(i)
                    log.debug("sd={}", sd)
                    asyncAssertionBlock(w) {
                      assert(sd.reqId =#= reqId)
                      assert(sd.rank =#= rank)
                      assert(sd.contractDetails =#= contractDetails)
                      assert(sd.distance =#= distance)
                      assert(sd.benchmark =#= benchmark)
                      assert(sd.projection =#= projection)
                      assert(sd.legsStr =#= legsStr)
                      i += 1
                    }
                  }
                  override def scannerDataEnd(reqId: Int) {
                    asyncAssertionBlock(w) {
                      assert(sde.reqId =#= reqId)
                      assert(i === sds.size)
                      ended = true // happens-before invocation of waiter.dismiss()
                    }
                  }
                }
          }
          assert(ended) // ordered by Waiter.await synchronization.
      }
    }
  }
}