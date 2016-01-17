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

  import PgEnumSupportUtils.{ buildCreateSql, buildDropSql }
  def createSql = buildCreateSql(self.toString, self, quoteName = false)
  def dropSql = buildDropSql(self.toString, quoteName = false)

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

object RfStatementType extends Enumeration with SlickPgImplicits {
  type RfStatementType = Value
  val INC, BAL, CAS = Value
  val desc = List(INC -> "Income", BAL -> "Balance Sheet", CAS -> "Cash FLow").toMap.withDefault(_ => "UNK")
}

object SecType extends Enumeration with SlickPgImplicits {
  type SecType = Value
  val dummy = Value
}

object ExchangeOs extends Enumeration with SlickPgImplicits {
  type ExchangeOs = Value
  val dummy = Value
}

object EnumPickler {

  import upickle.default.{ Reader, Writer }
  import upickle.Js

  private def enum2Reader(e: Enumeration) = Reader[e.Value] { case Js.Str(s) => e.withName(s) }
  private def enum2Writer(e: Enumeration) = Writer[e.Value] { case v => Js.Str(v.toString) }

  def apply(e: Enumeration) = (enum2Reader(e), enum2Writer(e))
}

object EnumImplicits {
  implicit val (rdSecType, wrSecType) = EnumPickler(SecType)
  implicit val (rdRfST, wrRfST) = EnumPickler(RfStatementType)

  implicit class PgEnum(val e: Enumeration) extends AnyVal {
    import PgEnumSupportUtils.{ buildCreateSql, buildDropSql }
    def createSql = buildCreateSql(e.toString, e, quoteName = false)
    def dropSql = buildDropSql(e.toString, quoteName = false)
  }
}

object IfThisCompilesItWorks {

  import slick.jdbc.JdbcType
  import DefTradePgDriver.EnumColumnExtensionMethods
  import DefTradePgDriver.api._

}