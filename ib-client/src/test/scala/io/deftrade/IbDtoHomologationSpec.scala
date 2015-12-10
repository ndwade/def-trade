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

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.testkit.TestKit
import akka.testkit.ImplicitSender

import org.scalatest.WordSpec
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

class IbDtoHomologationSpec extends WordSpec with Matchers with IbApiUtils {

  object TestIbModule

  import TestIbModule._
  import com.ib.client.{
    Contract => IbContract,
    ContractDetails => IbContractDetails,
    Order => IbOrder
  }

  import scala.reflect.runtime.universe.{ typeOf, Type }
  implicit val outer = scala.reflect.runtime.currentMirror.reflect(TestIbModule)

  def assertDtoGenIbCopyIsHomologous(tpe: Type): Unit = {
    val dtoGenerated = generate(tpe)
    val ibDtoGenerated = ibCopyOf(dtoGenerated)
    assert(dtoGenerated =#= ibDtoGenerated)
  }

  "The DTO homologation code" when {
    "run against Contract, UnderComp, and ComboLeg" should {
      "genererate and verify a homologous ib copy of the instances" in {

        val contractEmpty = Contract()
        val ibContract = ibCopyOf(contractEmpty)
        assert(contractEmpty =#= ibContract)
        assert(negate(contractEmpty.copy(includeExpired = true) =#= ibContract))
        //        println(contractEmpty.copy(includeExpired = true) =#= ibContract)

        val contractNonEmpty = Contract(
          conId = new ConId(555),
          symbol = "AAPL",
          secType = SecType.STK,
          expiry = "whenever",
          strike = 33.33,
          right = Right.Put,
          multiplier = "1234",
          exchange = "new YAWK")

        val ibContractNonEmpty = ibCopyOf(contractNonEmpty)
        assert(contractNonEmpty =#= ibContractNonEmpty)
        //  println(contractEmpty =#= ibContractNonEmpty)
        assert(negate(contractEmpty =#= ibContractNonEmpty)) // problem!

        val contractCombo = Contract.combo("IBRK")(
          ComboLeg(conId = new ConId(12345), ratio = 2, action = Action.BUY, exchange = "ICE"),
          ComboLeg(conId = new ConId(54321), ratio = 3, action = Action.SELL, exchange = "FIRE"))

        val ibContractCombo = ibCopyOf(contractCombo)
        assert(contractCombo =#= ibContractCombo)

        import scala.reflect.runtime.universe.{ typeOf }
        implicit val outer = scala.reflect.runtime.currentMirror.reflect(TestIbModule)

        //        (1 to 100) foreach { _ =>
        val contractGenerated = generate(typeOf[Contract])
        val ibContractGenerated = ibCopyOf(contractGenerated)
        assert(contractGenerated =#= ibContractGenerated)
        //        println(contractGenerated =#= ibContractGenerated)
        //        }

      }
    }
    "run against Order" should {
      "genererate and verify a homologous ib copy of the instances" in {

        val orderEmpty = Order()
        val ibOrderEmpty = ibCopyOf(orderEmpty)
        // println(orderEmpty =#= ibOrderEmpty)
        assert(orderEmpty =#= ibOrderEmpty)

        val orderSimple = Order(
          orderId = new OrderId(42),
          clientId = 555,
          permId = 0xcafe,
          action = Action.BUY,
          totalQuantity = 33,
          orderType = OrderType.LMT,
          lmtPrice = Some(4.99),
          auxPrice = Some(1.95))

        val ibOrderSimple = ibCopyOf(orderSimple)
        assert(orderSimple =#= ibOrderSimple)
        // println(orderSimple =#= ibOrderEmpty)
        assert(negate(orderSimple =#= ibOrderEmpty))

        val orderExt = orderSimple.copy(
          tif = TimeInForce.GTC,
          ocaGroup = "hug",
          ocaType = OcaType.CancelWithBlocking,
          orderRef = "disorder",
          transmit = false,
          parentId = new OrderId(333),
          blockOrder = true,
          sweepToFill = true,
          displaySize = 100,
          triggerMethod = TriggerMethod.DoubleBidAsk,
          outsideRth = true,
          hidden = true,
          goodAfterTime = "20070505 08:00:00 GMT",
          goodTillDate = "20070505 19:00:00 GMT",
          overridePercentageConstraints = true,
          rule80A = Rule80A.IndivSmallNonArb,
          allOrNone = true,
          minQty = Some(9000),
          percentOffset = Some(5),
          trailStopPrice = Some(7.99),
          trailingPercent = Some(12.13),
          notHeld = true)

        val ibOrderExt = ibCopyOf(orderExt)
        assert(orderExt =#= ibOrderExt)

        val orderCombo = orderExt.copy(
          basisPoints = Some(0.01),
          basisPointsType = Some(5),
          smartComboRoutingParams = List("foo" -> "bar", "clem" -> "zorp", "biz" -> "baz"),
          orderComboLegs = List(
            OrderComboLeg(Some(66.77)), OrderComboLeg(Some(888.88))))

        val ibOrderCombo = ibCopyOf(orderCombo)
        assert(orderCombo =#= ibOrderCombo)
        // println(orderCombo =#= ibOrderExt)

      }
    }
    "run with randomly generated instances against all DTO classes" should {
      "genererate and verify a homologous ib copy of the instances" in {

        assert(Contract() =#= ibCopyOf(Contract()))
        assertDtoGenIbCopyIsHomologous(typeOf[Contract])
        assertDtoGenIbCopyIsHomologous(typeOf[Contract])
        assertDtoGenIbCopyIsHomologous(typeOf[Contract])

        assert(Order() =#= ibCopyOf(Order()))
        assertDtoGenIbCopyIsHomologous(typeOf[Order])
        assertDtoGenIbCopyIsHomologous(typeOf[Order])
        assertDtoGenIbCopyIsHomologous(typeOf[Order])

        assert(ContractDetails() =#= ibCopyOf(ContractDetails()))
        assertDtoGenIbCopyIsHomologous(typeOf[ContractDetails])
        assertDtoGenIbCopyIsHomologous(typeOf[ContractDetails])
        assertDtoGenIbCopyIsHomologous(typeOf[ContractDetails])

        assert(Execution() =#= ibCopyOf(Execution()))
        assertDtoGenIbCopyIsHomologous(typeOf[Execution])
        assertDtoGenIbCopyIsHomologous(typeOf[Execution])
        assertDtoGenIbCopyIsHomologous(typeOf[Execution])

        assert(ExecutionFilter() =#= ibCopyOf(ExecutionFilter()))
        assertDtoGenIbCopyIsHomologous(typeOf[ExecutionFilter])
        assertDtoGenIbCopyIsHomologous(typeOf[ExecutionFilter])
        assertDtoGenIbCopyIsHomologous(typeOf[ExecutionFilter])

        assert(OrderState() =#= ibCopyOf(OrderState()))
        assertDtoGenIbCopyIsHomologous(typeOf[OrderState])
        assertDtoGenIbCopyIsHomologous(typeOf[OrderState])
        assertDtoGenIbCopyIsHomologous(typeOf[OrderState])

        assert(ScannerSubscription() =#= ibCopyOf(ScannerSubscription()))
        assertDtoGenIbCopyIsHomologous(typeOf[ScannerSubscription])
        assertDtoGenIbCopyIsHomologous(typeOf[ScannerSubscription])
        assertDtoGenIbCopyIsHomologous(typeOf[ScannerSubscription])

      }
    }

    "run with DTO instances generated with all numeric fields" should {
      "veryify scala-ib and java ib client DTOs as homologous" in {

        val contract = Contract(symbol = "99", secType = "98")
        val ibContract = new IbContract()
        ibContract.m_symbol = "99"
        ibContract.m_secType = "98"
        assert(contract =#= ibContract)

        val contractDetails = ContractDetails()
        val ibContractDetails = new IbContractDetails()
        assert(contractDetails =#= ibContractDetails)
      }
    }
  }
}