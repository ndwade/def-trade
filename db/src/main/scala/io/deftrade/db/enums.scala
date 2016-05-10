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

import com.github.tminglei.slickpg._

/**
 * Mixin trait for implicit conversion of Enumeration values to
 * Slick column types (with extension methods).
 *
 * The mixin strategy relies on Scala rules for the implicit search path: because the enum values
 * are type parameters of the mapping type constructors, the implicit scope of those type parameters
 * is searched: this includes the enclosing object, which is the Enumeration.
 */
trait SlickPgImplicits { self: Enumeration =>

  def createSql = PgEnumSupportUtils.buildCreateSql(self.toString, self, quoteName = false)
  def dropSql = PgEnumSupportUtils.buildDropSql(self.toString, quoteName = false)

  import DefTradePgDriver.{
    createEnumJdbcType,
    createEnumListJdbcType,
    createEnumColumnExtensionMethodsBuilder,
    createEnumOptionColumnExtensionMethodsBuilder
  }
  import SlickPgImplicits._

  implicit val typeMapper = createEnumJdbcType(self.toString.toUnderscore, self, quoteName = false)
  implicit val listTypeMapper = createEnumListJdbcType(self.toString.toUnderscore, self, quoteName = false)
  implicit val columnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder(self)
  implicit val optionColumnExtensionMethodsBuilder = createEnumOptionColumnExtensionMethodsBuilder(self)
}

object SlickPgImplicits {
  implicit class ToUnderscore(val cc: String) extends AnyVal {
    def toUnderscore = cc.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase
  }
}

object EnumPickler {

  import upickle.default.{ Reader, Writer }
  import upickle.Js

  private def enum2Reader(e: Enumeration) = Reader[e.Value] { case Js.Str(s) => e.withName(s) }
  private def enum2Writer(e: Enumeration) = Writer[e.Value] { case v => Js.Str(v.toString) }

  def apply(e: Enumeration) = (enum2Reader(e), enum2Writer(e))
}

//object EnumImplicits {
//  implicit val (rdSecType, wrSecType) = EnumPickler(SecType)
//  implicit val (rdRfST, wrRfST) = EnumPickler(RfStatementType)
//}

object EnumConverter {
  case class StringMap[E1 <: Enumeration, E2 <: Enumeration](val e1: E1, val e2: E2) {
    val f: E1#Value => e2.Value = v => e2 withName v.toString
  }
  implicit class EC[E1 <: Enumeration](val v: E1#Value) {
    /**
     * Returns an `Option[e2.Value]`. Need to return an `Option` because it's not possible to
     * guarantee that the scala Enumeration and the postgres enum are updated in sync. Each
     * enumerated type is well motivated (don't want to use simple strings in the database - schema
     * would lose info and integrity) but there needs to be a single point which exposes the
     * potential for mismatch. This is it. In critical case can map to a default value, log,
     * and go on (e.g. live trading where the exact recording of every detail may be secondary
     * to not crashing right then and there).
     */
    def to[E2 <: Enumeration](implicit ev: StringMap[E1, E2]): Option[ev.e2.Value] =
      scala.util.Try(ev.f(v)).toOption
  }
  //  implicit val secTypeStringIdentity = new StringMap(io.deftrade.SecType, SecType) {
  //    override val f: io.deftrade.SecType.type#Value => e2.Value = Map(
  //      e1.FUT -> e2.dummy,
  //      e1.BOND -> e2.dummy
  //    )
  //  }
}

object IfThisCompilesItWorks {

  import slick.jdbc.JdbcType
  import DefTradePgDriver.EnumColumnExtensionMethods
  import DefTradePgDriver.api._
  import EnumConverter._
  import scala.language.implicitConversions

}
