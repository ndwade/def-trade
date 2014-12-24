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

import collection.mutable
import java.util.NoSuchElementException
import scala.language.{ postfixOps, implicitConversions }

/*
 * Note on error handling:
 * - parsing errors on primitives are considered to be fatal because they represent a major 
 * malfunction of the protocol: e.g. if we expect an Int and cannot parse one, something is
 * very wrong and we ought to disconnect.
 * - parsing errors on enums are presumed to be errors of omission; "higher layers" (downstream
 * actors) can choose the appropriate action.
 */
private[deftrade] object ImplicitConversions {

  import collection.immutable.{ StringOps => SO }
  /*
   * implicit conversions for simple datatypes
   */
  implicit def s2i(s: String): Int = if (s.isEmpty) 0 else (s: SO) toInt
  implicit def i2s(i: Int): String = i.toString

  implicit def s2l(s: String): Long = if (s.isEmpty) 0L else (s: SO) toLong
  implicit def l2s(l: Long): String = l.toString

  implicit def s2d(s: String): Double = if (s.isEmpty) 0.0 else (s: SO) toDouble
  implicit def d2s(d: Double): String = d.toString

  implicit def s2b(s: String): Boolean = if (s.isEmpty) false else (s: Int) != 0
  implicit def b2s(b: Boolean): String = if (b) "1" else "0"

  implicit def s2oi(s: String): Option[Int] = if (s.isEmpty) None else Some(s)
  implicit def oi2s(oi: Option[Int]): String = (oi map i2s) getOrElse ""

  implicit def s2od(s: String): Option[Double] = if (s.isEmpty) None else Some(s)
  implicit def od2s(od: Option[Double]): String = (od map d2s) getOrElse ""

}

import ImplicitConversions._

/**
 * Value classes for ConId, OrderId, ReqId
 * Motivation:
 * prevent bad assignments and disallow meaningless operators (id's should be opaque and immutable)
 * limit construction and ops on ids (TODO: public for now until services better defined)
 * TODO: scala 2.10 reflection bug: can't invoke methods with Value Class params
 * https://issues.scala-lang.org/browse/SI-6411
 * when this is fixed, migrate back to Value Class
 * UPDATE: still broken in 2.11 when reflecting on value classes which are part of case classes
 */
sealed trait GenId extends Any {
  def id: Int
}

trait GenIdCompanion[I <: GenId] {
  def apply(id: Int): I
  import math.Ordering
  implicit lazy val ordering: Ordering[I] = Ordering.by[I, Int](_.id)
  implicit def ops(i: I) = ordering.mkOrderingOps(i)
  private[deftrade] implicit def id2s(i: I) = i.id.toString
  private[deftrade] implicit def s2id(s: String): I = apply(s.toString)
}

/**
 *
 */
case class OrderId(val id: Int) extends /* AnyVal with */ GenId
object OrderId extends GenIdCompanion[OrderId]
/**
 *
 */
case class ReqId(val id: Int) extends /* AnyVal with */ GenId
object ReqId extends GenIdCompanion[ReqId]
/**
 *
 */
case class ConId(val id: Int) extends /* AnyVal with */ GenId
object ConId extends GenIdCompanion[ConId]