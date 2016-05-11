/*
 * Copyright 2014-2016 Panavista Technologies LLC
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

package io.deftrade.db

import org.scalactic.{ Equivalence => Eq }
import org.scalactic.TypeCheckedTripleEquals._

trait TupleEquvalenceImplicits {
  implicit def tuple2Equivalence[A, B](implicit ea: Eq[A], eb: Eq[B]): Eq[(A, B)] =
    new Eq[(A, B)] {
      override def areEquivalent(a: (A, B), b: (A, B)) =
        a._1 === b._1 && a._2 === b._2
    }

  implicit def tuple3Equivalence[A, B, C](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C]): Eq[(A, B, C)] =
    new Eq[(A, B, C)] {
      override def areEquivalent(a: (A, B, C), b: (A, B, C)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3
    }

  implicit def tuple4Equivalence[A, B, C, D](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D]): Eq[(A, B, C, D)] =
    new Eq[(A, B, C, D)] {
      override def areEquivalent(a: (A, B, C, D), b: (A, B, C, D)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4
    }

  implicit def tuple5Equivalence[A, B, C, D, E](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E]): Eq[(A, B, C, D, E)] =
    new Eq[(A, B, C, D, E)] {
      override def areEquivalent(a: (A, B, C, D, E), b: (A, B, C, D, E)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5
    }

  implicit def tuple6Equivalence[A, B, C, D, E, F](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F]): Eq[(A, B, C, D, E, F)] =
    new Eq[(A, B, C, D, E, F)] {
      override def areEquivalent(a: (A, B, C, D, E, F), b: (A, B, C, D, E, F)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6
    }

  implicit def tuple7Equivalence[A, B, C, D, E, F, G](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G]): Eq[(A, B, C, D, E, F, G)] =
    new Eq[(A, B, C, D, E, F, G)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G), b: (A, B, C, D, E, F, G)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7
    }

  implicit def tuple8Equivalence[A, B, C, D, E, F, G, H](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H]): Eq[(A, B, C, D, E, F, G, H)] =
    new Eq[(A, B, C, D, E, F, G, H)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H), b: (A, B, C, D, E, F, G, H)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8
    }

  implicit def tuple9Equivalence[A, B, C, D, E, F, G, H, I](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H], ei: Eq[I]): Eq[(A, B, C, D, E, F, G, H, I)] =
    new Eq[(A, B, C, D, E, F, G, H, I)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H, I), b: (A, B, C, D, E, F, G, H, I)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8 && a._9 === b._9
    }

  implicit def tuple10Equivalence[A, B, C, D, E, F, G, H, I, J](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H], ei: Eq[I], ej: Eq[J]): Eq[(A, B, C, D, E, F, G, H, I, J)] =
    new Eq[(A, B, C, D, E, F, G, H, I, J)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H, I, J), b: (A, B, C, D, E, F, G, H, I, J)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8 && a._9 === b._9 && a._10 === b._10
    }

  implicit def tuple11Equivalence[A, B, C, D, E, F, G, H, I, J, K](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H], ei: Eq[I], ej: Eq[J], ek: Eq[K]): Eq[(A, B, C, D, E, F, G, H, I, J, K)] =
    new Eq[(A, B, C, D, E, F, G, H, I, J, K)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H, I, J, K), b: (A, B, C, D, E, F, G, H, I, J, K)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8 && a._9 === b._9 && a._10 === b._10 && a._11 === b._11
    }

  implicit def tuple12Equivalence[A, B, C, D, E, F, G, H, I, J, K, L](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H], ei: Eq[I], ej: Eq[J], ek: Eq[K], el: Eq[L]): Eq[(A, B, C, D, E, F, G, H, I, J, K, L)] =
    new Eq[(A, B, C, D, E, F, G, H, I, J, K, L)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H, I, J, K, L), b: (A, B, C, D, E, F, G, H, I, J, K, L)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8 && a._9 === b._9 && a._10 === b._10 && a._11 === b._11 && a._12 === b._12
    }

  implicit def tuple13Equivalence[A, B, C, D, E, F, G, H, I, J, K, L, M](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H], ei: Eq[I], ej: Eq[J], ek: Eq[K], el: Eq[L], em: Eq[M]): Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M)] =
    new Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H, I, J, K, L, M), b: (A, B, C, D, E, F, G, H, I, J, K, L, M)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8 && a._9 === b._9 && a._10 === b._10 && a._11 === b._11 && a._12 === b._12 && a._13 === b._13
    }

  implicit def tuple14Equivalence[A, B, C, D, E, F, G, H, I, J, K, L, M, N](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H], ei: Eq[I], ej: Eq[J], ek: Eq[K], el: Eq[L], em: Eq[M], en: Eq[N]): Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N)] =
    new Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H, I, J, K, L, M, N), b: (A, B, C, D, E, F, G, H, I, J, K, L, M, N)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8 && a._9 === b._9 && a._10 === b._10 && a._11 === b._11 && a._12 === b._12 && a._13 === b._13 && a._14 === b._14
    }

  implicit def tuple15Equivalence[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H], ei: Eq[I], ej: Eq[J], ek: Eq[K], el: Eq[L], em: Eq[M], en: Eq[N], eo: Eq[O]): Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O)] =
    new Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O), b: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8 && a._9 === b._9 && a._10 === b._10 && a._11 === b._11 && a._12 === b._12 && a._13 === b._13 && a._14 === b._14 && a._15 === b._15
    }

  implicit def tuple16Equivalence[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H], ei: Eq[I], ej: Eq[J], ek: Eq[K], el: Eq[L], em: Eq[M], en: Eq[N], eo: Eq[O], ep: Eq[P]): Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P)] =
    new Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P), b: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8 && a._9 === b._9 && a._10 === b._10 && a._11 === b._11 && a._12 === b._12 && a._13 === b._13 && a._14 === b._14 && a._15 === b._15 && a._16 === b._16
    }

  implicit def tuple17Equivalence[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H], ei: Eq[I], ej: Eq[J], ek: Eq[K], el: Eq[L], em: Eq[M], en: Eq[N], eo: Eq[O], ep: Eq[P], eq: Eq[Q]): Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q)] =
    new Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q), b: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8 && a._9 === b._9 && a._10 === b._10 && a._11 === b._11 && a._12 === b._12 && a._13 === b._13 && a._14 === b._14 && a._15 === b._15 && a._16 === b._16 && a._17 === b._17
    }

  implicit def tuple18Equivalence[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H], ei: Eq[I], ej: Eq[J], ek: Eq[K], el: Eq[L], em: Eq[M], en: Eq[N], eo: Eq[O], ep: Eq[P], eq: Eq[Q], er: Eq[R]): Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R)] =
    new Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R), b: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8 && a._9 === b._9 && a._10 === b._10 && a._11 === b._11 && a._12 === b._12 && a._13 === b._13 && a._14 === b._14 && a._15 === b._15 && a._16 === b._16 && a._17 === b._17 && a._18 === b._18
    }

  implicit def tuple19Equivalence[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H], ei: Eq[I], ej: Eq[J], ek: Eq[K], el: Eq[L], em: Eq[M], en: Eq[N], eo: Eq[O], ep: Eq[P], eq: Eq[Q], er: Eq[R], es: Eq[S]): Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S)] =
    new Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S), b: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8 && a._9 === b._9 && a._10 === b._10 && a._11 === b._11 && a._12 === b._12 && a._13 === b._13 && a._14 === b._14 && a._15 === b._15 && a._16 === b._16 && a._17 === b._17 && a._18 === b._18 && a._19 === b._19
    }

  implicit def tuple20Equivalence[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H], ei: Eq[I], ej: Eq[J], ek: Eq[K], el: Eq[L], em: Eq[M], en: Eq[N], eo: Eq[O], ep: Eq[P], eq: Eq[Q], er: Eq[R], es: Eq[S], et: Eq[T]): Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T)] =
    new Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T), b: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8 && a._9 === b._9 && a._10 === b._10 && a._11 === b._11 && a._12 === b._12 && a._13 === b._13 && a._14 === b._14 && a._15 === b._15 && a._16 === b._16 && a._17 === b._17 && a._18 === b._18 && a._19 === b._19 && a._20 === b._20
    }

  implicit def tuple21Equivalence[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H], ei: Eq[I], ej: Eq[J], ek: Eq[K], el: Eq[L], em: Eq[M], en: Eq[N], eo: Eq[O], ep: Eq[P], eq: Eq[Q], er: Eq[R], es: Eq[S], et: Eq[T], eu: Eq[U]): Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U)] =
    new Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U), b: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8 && a._9 === b._9 && a._10 === b._10 && a._11 === b._11 && a._12 === b._12 && a._13 === b._13 && a._14 === b._14 && a._15 === b._15 && a._16 === b._16 && a._17 === b._17 && a._18 === b._18 && a._19 === b._19 && a._20 === b._20 && a._21 === b._21
    }

  implicit def tuple22Equivalence[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V](implicit ea: Eq[A], eb: Eq[B], ec: Eq[C], ed: Eq[D], ee: Eq[E], ef: Eq[F], eg: Eq[G], eh: Eq[H], ei: Eq[I], ej: Eq[J], ek: Eq[K], el: Eq[L], em: Eq[M], en: Eq[N], eo: Eq[O], ep: Eq[P], eq: Eq[Q], er: Eq[R], es: Eq[S], et: Eq[T], eu: Eq[U], ev: Eq[V]): Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V)] =
    new Eq[(A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V)] {
      override def areEquivalent(a: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V), b: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T, U, V)) =
        a._1 === b._1 && a._2 === b._2 && a._3 === b._3 && a._4 === b._4 && a._5 === b._5 && a._6 === b._6 && a._7 === b._7 && a._8 === b._8 && a._9 === b._9 && a._10 === b._10 && a._11 === b._11 && a._12 === b._12 && a._13 === b._13 && a._14 === b._14 && a._15 === b._15 && a._16 === b._16 && a._17 === b._17 && a._18 === b._18 && a._19 === b._19 && a._20 === b._20 && a._21 === b._21 && a._22 === b._22
    }
}

object TupleEquvalenceImplicits extends TupleEquvalenceImplicits

object generate extends (() => Seq[String]) {
  def apply() = {
    val chars = 'a' to 'z' take 22
    def typeChars(n: Int) = chars take n map (_.toUpper) mkString (", ")
    def iparam(c: Char) = s"e${c}: Eq[${c.toUpper}]"
    def eqs(n: Int) = 1 to n map { i => s"a._$i === b._$i" } mkString (" && ")
    def apply(n: Int) = // n from 2 to 22
      s"""|implicit def tuple${n}Equivalence[${typeChars(n)}](implicit ${chars take n map (iparam(_)) mkString (", ")}): Eq[(${typeChars(n)})] =
          |new Eq[(${typeChars(n)})] {
          |  override def areEquivalent(a: (${typeChars(n)}), b: (${typeChars(n)})) =
          |    ${eqs(n)}
          |}
          |""".stripMargin

    2 to 22 map { n => apply(n) }
  }
}
