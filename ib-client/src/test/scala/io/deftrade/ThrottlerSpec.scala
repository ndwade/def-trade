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

import scala.concurrent.duration._
import akka.actor.{ ActorSystem, Actor, ActorRef, ActorLogging, Props }
import akka.testkit._
import org.scalatest.{ WordSpecLike, Matchers, BeforeAndAfterAll }

object ThrottlerSpec {

  val msgsPerSecond = 50

  class RateLimitedEchoActor extends Actor {

    var timestamps = scala.collection.immutable.Queue.empty[Duration]

    def receive = {
      case (msg) =>
        val now = TestKit.now
        val oneSecondAgo = now - 1.second
        timestamps = timestamps enqueue now
        while (timestamps.front < oneSecondAgo) {
          val (_, ts) = timestamps.dequeue
          timestamps = ts
        }
        if (timestamps.size <= msgsPerSecond) sender ! msg
        else sender ! s"error: rate limit exceeded, ${timestamps.size} msgs seen in last ${now - timestamps.front}"
    }
  }
}

class ThrottlerSpec extends TestKit(ActorSystem("ThrottlerSpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  import throttle._
  import ThrottlerSpec._

  def sendMsgs(n: Int, throttler: ActorRef): Unit = (1 to n) foreach {
    throttler.tell(_, testActor) // resolves ambig actorRef implicits.
  }

  def verify(n: Int): Unit = (1 to n) foreach {
    expectMsg(_)
  }

  override def afterAll() = TestKit.shutdownActorSystem(system, verifySystemShutdown = true)

  "A Throttler" must {

    "not delay a burst of msgs" in {

      val service = system.actorOf(Props[RateLimitedEchoActor])
      implicit val throttler = system.actorOf(ApiMsgThrottler.props(service, msgsPerSecond))
      
      val aFewGoodMsgs = msgsPerSecond 
      sendMsgs(aFewGoodMsgs, throttler)
      within(.1.second) { verify(aFewGoodMsgs) }

    }

    "not exceed the service rate limit" in {

      val service = system.actorOf(Props[RateLimitedEchoActor])
      val throttler = system.actorOf(ApiMsgThrottler.props(service, msgsPerSecond))

      val n = msgsPerSecond 
      
      Thread.sleep(0.9.seconds.toMillis)
      sendMsgs(n, throttler)
      within(0.1.seconds) { verify(n) }
      
      sendMsgs(n, throttler)
      within(1.05.seconds) { verify(n) }
    }
  }

}