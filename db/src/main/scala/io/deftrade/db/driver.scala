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

trait DefTradePgDriver extends ExPostgresDriver
    with PgArraySupport
    with PgDate2Support
    with PgRangeSupport
    with PgJsonSupport
    with PgEnumSupport {

  override def pgjson = "jsonb"

  // TODO: these don't work as advertized IMO...
  // bindPgDateTypesToScala[LocalDate, LocalTime, LocalDateTime, OffsetTime, OffsetDateTime, Interval]
  // bindPgTypeToScala("tstzrange", scala.reflect.classTag[Range[OffsetDateTime]])

  object _API extends API
      with ArrayImplicits
      with DateTimeImplicits
      with RangeImplicits
      with JsonImplicits {

    import java.time.{ format, OffsetDateTime }
    import PgRangeSupportUtils.mkRangeFn

    val pgOffsetDateTimeFormatter = format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssx")
    implicit val simpleOffsetDateTimeRangeTypeMapper =
      new GenericJdbcType[Range[OffsetDateTime]]("tstzrange", mkRangeFn { s =>
        OffsetDateTime.parse(s, pgOffsetDateTimeFormatter)
      })
  }

  override val api = _API

}

object DefTradePgDriver extends DefTradePgDriver
