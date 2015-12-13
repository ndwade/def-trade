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
package demo

import akka.actor._
import io.deftrade._
import io.deftrade.Action._

object CustomCakeIb extends IbConnectionComponent(ActorSystem.create("system")) 

object StandAloneRandomCrap {

  import CustomCakeIb._

  import Right._, Action._, SecType._, SecIdType._

  val cost: Double = 99.95
  val dcost: BigDecimal = cost

  val contract = Contract(symbol = "AAPL")

  // ... TO BE CONTINUED...

  val x = collection.mutable.ArrayBuffer(4096)
  val a = x.toArray
}