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
//package io.deftrade
//
//object dummy {
//
//  import NonDefaultNamedValues._
//
//  sealed trait Dummy
//  
//  case class Foo(i: Int = 33, d: Option[Double] = None, s: String = "") extends Dummy {
//	  override def toString: String = nonDefaultNamedValues
//  }
//}
//
//import org.scalatest.{ WordSpec, Matchers }
//
//class NdnvsSpec extends WordSpec with Matchers {
//
//  import NonDefaultNamedValues.{ start, sep, end }
//  import dummy._
//
//  "An NDNVS-enabled case class" when {
//    "constructed with default parameter values" should {
//      "show an empty list of name-value pairs" in {
//        val f = Foo()
//        f.toString should be(s"Foo$start$end")
//      }
//    }
//    "constructed with a single non default parameter value" should {
//      "show a single named value" in {
//        val f = Foo(d = Some(1.618))
//        f.toString should be(s"Foo${start}d=Some(1.618)$end")
//      }
//    }
//    "constructed with two non default parameter values" should {
//      "show two named values delimited by a separator" in {
//        val f = Foo(i = 42, s = "lolz")
//        f.toString should be(s"Foo${start}i=42${sep}s=lolz$end")
//      }
//    }
//  }
//}