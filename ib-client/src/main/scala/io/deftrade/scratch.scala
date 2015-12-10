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

 /*
package io.deftrade.ib

trait StringPickly { _: Enumeration =>
  implicit val e2w = upickle.default.Writer[Value] { case v => Js.Str(v.toString) }
  implicit val e2r = upickle.default.Reader[Value] { case Js.Str(s) => withName(s) }
}

object Field extends Enumeration with StringPickly {
  val FOO, BAR, CLEM, ZORP = Value
}
case class Tick(field: Field.Value, size: Int)

// class OrdinalStringConversions[E <: Enumeration](E: e) {
//   type V = e.Value
//   def e2s: String =
// }

object TypeClassHacks {
  trait EV[V <: Enumeration#Value] {
    type VV = V
    def e: Enumeration
    def show(v: VV): String
    def from(s: String): VV
  }
  implicit def v2s[V <: Enumeration#Value](v: V)(implicit ev: EV[V]): String = { ev.show(v) }

  implicit def s2v[V <: Enumeration#Value](s: String)(implicit ev: EV[V]): V = { ev.from(s) }

  object Foos extends Enumeration { type Foo = Value; val FOO, BAR, CLEM = Value }

  object Stooges extends Enumeration { type Stooge = Value; val MOE, LARRY, CURLY, SHEMP = Value }

  object Greeks extends Enumeration{ type Greek = Value; val DELTA, GAMMA, VEGA = Value }

  implicit object FoosEV extends EV[Foos.Value] {
    override def e = Foos // DANGER: potential for mismatch
    override def show(v: VV): String = v.toString
    override def from(s: String): VV = e.withName(s)
  }

  implicit object StoogesEV extends EV[Stooges.Value] {
    override def e = Stooges
    override def show(v: VV): String = v.id.toString
    override def from(s: String):VV = e(s.toInt)
  }

}
*/
