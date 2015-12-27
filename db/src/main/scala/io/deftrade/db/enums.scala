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
 * is searched: this includes the enclosing object, the Enumeration.
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

object WeekDays extends Enumeration with SlickPgImplicits {
  type WeekDay = Value
  val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
}

object Rainbows extends Enumeration with SlickPgImplicits {
  type Rainbow = Value
  val red, orange, yellow, green, blue, purple = Value
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


object IfThisCompilesItWorks {

  import slick.jdbc.JdbcType
  import DefTradePgDriver.EnumColumnExtensionMethods
  import DefTradePgDriver.api._

  implicitly[JdbcType[WeekDays.WeekDay]]
  implicitly[JdbcType[List[WeekDays.WeekDay]]]
  implicitly[Rep[WeekDays.Value] => EnumColumnExtensionMethods[WeekDays.Value, WeekDays.Value]]
  implicitly[Rep[Option[WeekDays.Value]] => EnumColumnExtensionMethods[WeekDays.Value, Option[WeekDays.Value]]]

  implicitly[JdbcType[Rainbows.Rainbow]]
  implicitly[JdbcType[List[Rainbows.Rainbow]]]
  implicitly[Rep[Rainbows.Value] => EnumColumnExtensionMethods[Rainbows.Value, Rainbows.Value]]
  implicitly[Rep[Option[Rainbows.Value]] => EnumColumnExtensionMethods[Rainbows.Value, Option[Rainbows.Value]]]

}