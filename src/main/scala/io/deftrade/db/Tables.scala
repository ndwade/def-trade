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
// AUTO-GENERATED Slick data model

/** Stand-alone Slick data model for immediate use */
object Tables extends {
  val profile = DefTradePgDriver
} with Tables

/** Slick data model trait for extension (cake pattern). (Make sure to initialize this late.) */
trait Tables {
  val profile: DefTradePgDriver // must retain this driver
  import profile.api._

  import slick.model.ForeignKeyAction
  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
  import slick.jdbc.{ GetResult => GR }

  /** DDL for all tables. Call .create to execute. */
  lazy val schema: profile.SchemaDescription = Array(Contracts.schema, Corporations.schema, Currencies.schema, DsBars.schema, DsInputs.schema, DsPecks.schema, DsRoot.schema, Exchanges.schema, MdFeeds.schema, MdFeedsExchanges.schema, MdSeries.schema, MdVendors.schema, RfCoaCode.schema, RfLineItems.schema, RfStatements.schema).reduceLeft(_ ++ _)
  @deprecated("Use .schema instead of .ddl", "3.0")
  def ddl = schema

  /**
   * Entity class storing rows of table Contracts
   *  @param conId Database column con_id SqlType(int4), PrimaryKey
   *  @param symbol Database column symbol SqlType(varchar), Length(16,true)
   *  @param secType Database column sec_type SqlType(sec_type_e)
   *  @param localSymbol Database column local_symbol SqlType(varchar), Length(126,true), Default(None)
   *  @param exchangeId Database column exchange_id SqlType(int4)
   *  @param currency Database column currency SqlType(bpchar), Length(3,false)
   *  @param meta Database column meta SqlType(jsonb), Length(2147483647,false), Default(None)
   *  @param corpId Database column corp_id SqlType(int4), Default(None)
   */
  case class Contract(conId: Int, symbol: String, secType: io.deftrade.db.SecType.SecType, localSymbol: Option[String] = None, exchangeId: io.deftrade.db.ExchangeId, currency: String, meta: Option[String] = None, corpId: Option[io.deftrade.db.CorporationId] = None)
  /** GetResult implicit for fetching Contract objects using plain SQL queries */
  implicit def GetResultContract(implicit e0: GR[Int], e1: GR[String], e2: GR[io.deftrade.db.SecType.SecType], e3: GR[Option[String]], e4: GR[io.deftrade.db.ExchangeId], e5: GR[Option[io.deftrade.db.CorporationId]]): GR[Contract] = GR {
    prs =>
      import prs._
      val r = (<<[Int], <<[String], <<[io.deftrade.db.SecType.SecType], <<?[String], <<[io.deftrade.db.ExchangeId], <<[String], <<?[String], <<?[io.deftrade.db.CorporationId])
      import r._
      Contract.tupled((_1, _2, _3, _4, _5, _6, _7, _8)) // putting AutoInc last
  }
  /** Table description of table contracts. Objects of this class serve as prototypes for rows in queries. */
  class Contracts(_tableTag: Tag) extends Table[Contract](_tableTag, "contracts") {
    def * = (conId, symbol, secType, localSymbol, exchangeId, currency, meta, corpId) <> (Contract.tupled, Contract.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(conId), Rep.Some(symbol), Rep.Some(secType), localSymbol, Rep.Some(exchangeId), Rep.Some(currency), meta, corpId).shaped.<>({ r => import r._; _1.map(_ => Contract.tupled((_1.get, _2.get, _3.get, _4, _5.get, _6.get, _7, _8))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column con_id SqlType(int4), PrimaryKey */
    val conId: Rep[Int] = column[Int]("con_id", O.PrimaryKey)
    /** Database column symbol SqlType(varchar), Length(16,true) */
    val symbol: Rep[String] = column[String]("symbol", O.Length(16, varying = true))
    /** Database column sec_type SqlType(sec_type_e) */
    val secType: Rep[io.deftrade.db.SecType.SecType] = column[io.deftrade.db.SecType.SecType]("sec_type")
    /** Database column local_symbol SqlType(varchar), Length(126,true), Default(None) */
    val localSymbol: Rep[Option[String]] = column[Option[String]]("local_symbol", O.Length(126, varying = true), O.Default(None))
    /** Database column exchange_id SqlType(int4) */
    val exchangeId: Rep[io.deftrade.db.ExchangeId] = column[io.deftrade.db.ExchangeId]("exchange_id")
    /** Database column currency SqlType(bpchar), Length(3,false) */
    val currency: Rep[String] = column[String]("currency", O.Length(3, varying = false))
    /** Database column meta SqlType(jsonb), Length(2147483647,false), Default(None) */
    val meta: Rep[Option[String]] = column[Option[String]]("meta", O.Length(2147483647, varying = false), O.Default(None))
    /** Database column corp_id SqlType(int4), Default(None) */
    val corpId: Rep[Option[io.deftrade.db.CorporationId]] = column[Option[io.deftrade.db.CorporationId]]("corp_id", O.Default(None))

    /** Foreign key referencing Corporations (database name contracts_corp_id_fkey) */
    lazy val corporationsFk = foreignKey("contracts_corp_id_fkey", corpId, Corporations)(r => Rep.Some(r.id), onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
    /** Foreign key referencing Currencies (database name contracts_currency_fkey) */
    lazy val currenciesFk = foreignKey("contracts_currency_fkey", currency, Currencies)(r => r.symbol, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
    /** Foreign key referencing Exchanges (database name contracts_exchange_id_fkey) */
    lazy val exchangesFk = foreignKey("contracts_exchange_id_fkey", exchangeId, Exchanges)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    /** Uniqueness Index over (symbol,exchangeId) (database name contracts_symbol_exchange_id_key) */
    val index1 = index("contracts_symbol_exchange_id_key", (symbol, exchangeId), unique = true)
  }
  /** Collection-like TableQuery object for table Contracts */
  lazy val Contracts = new TableQuery(tag => new Contracts(tag))

  /**
   * Entity class storing rows of table Corporations
   *  @param name Database column name SqlType(varchar), Length(126,true)
   *  @param meta Database column meta SqlType(jsonb), Length(2147483647,false), Default(None)
   *  @param statusTs Database column status_ts SqlType(timestamp)
   *  @param endTs Database column end_ts SqlType(timestamp), Default(None)
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   */
  case class Corporation(name: String, meta: Option[String] = None, statusTs: java.time.LocalDateTime, endTs: Option[java.time.LocalDateTime] = None, id: Option[io.deftrade.db.CorporationId] = None)
  /** GetResult implicit for fetching Corporation objects using plain SQL queries */
  implicit def GetResultCorporation(implicit e0: GR[String], e1: GR[Option[String]], e2: GR[java.time.LocalDateTime], e3: GR[Option[java.time.LocalDateTime]], e4: GR[Option[io.deftrade.db.CorporationId]]): GR[Corporation] = GR {
    prs =>
      import prs._
      val r = (<<?[io.deftrade.db.CorporationId], <<[String], <<?[String], <<[java.time.LocalDateTime], <<?[java.time.LocalDateTime])
      import r._
      Corporation.tupled((_2, _3, _4, _5, _1)) // putting AutoInc last
  }
  /** Table description of table corporations. Objects of this class serve as prototypes for rows in queries. */
  class Corporations(_tableTag: Tag) extends Table[Corporation](_tableTag, "corporations") {
    def * = (name, meta, statusTs, endTs, Rep.Some(id)) <> (Corporation.tupled, Corporation.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(name), meta, Rep.Some(statusTs), endTs, Rep.Some(id)).shaped.<>({ r => import r._; _1.map(_ => Corporation.tupled((_1.get, _2, _3.get, _4, _5))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column name SqlType(varchar), Length(126,true) */
    val name: Rep[String] = column[String]("name", O.Length(126, varying = true))
    /** Database column meta SqlType(jsonb), Length(2147483647,false), Default(None) */
    val meta: Rep[Option[String]] = column[Option[String]]("meta", O.Length(2147483647, varying = false), O.Default(None))
    /** Database column status_ts SqlType(timestamp) */
    val statusTs: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("status_ts")
    /** Database column end_ts SqlType(timestamp), Default(None) */
    val endTs: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("end_ts", O.Default(None))
    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[io.deftrade.db.CorporationId] = column[io.deftrade.db.CorporationId]("id", O.AutoInc, O.PrimaryKey)
  }
  /** Collection-like TableQuery object for table Corporations */
  lazy val Corporations = new TableQuery(tag => new Corporations(tag))

  /**
   * Entity class storing rows of table Currencies
   *  @param symbol Database column symbol SqlType(bpchar), PrimaryKey, Length(3,false)
   *  @param countryCode Database column country_code SqlType(bpchar), Length(2,false), Default(None)
   *  @param description Database column description SqlType(varchar), Length(126,true), Default(None)
   */
  case class Currency(symbol: String, countryCode: Option[String] = None, description: Option[String] = None)
  /** GetResult implicit for fetching Currency objects using plain SQL queries */
  implicit def GetResultCurrency(implicit e0: GR[String], e1: GR[Option[String]]): GR[Currency] = GR {
    prs =>
      import prs._
      val r = (<<[String], <<?[String], <<?[String])
      import r._
      Currency.tupled((_1, _2, _3)) // putting AutoInc last
  }
  /** Table description of table currencies. Objects of this class serve as prototypes for rows in queries. */
  class Currencies(_tableTag: Tag) extends Table[Currency](_tableTag, "currencies") {
    def * = (symbol, countryCode, description) <> (Currency.tupled, Currency.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(symbol), countryCode, description).shaped.<>({ r => import r._; _1.map(_ => Currency.tupled((_1.get, _2, _3))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column symbol SqlType(bpchar), PrimaryKey, Length(3,false) */
    val symbol: Rep[String] = column[String]("symbol", O.PrimaryKey, O.Length(3, varying = false))
    /** Database column country_code SqlType(bpchar), Length(2,false), Default(None) */
    val countryCode: Rep[Option[String]] = column[Option[String]]("country_code", O.Length(2, varying = false), O.Default(None))
    /** Database column description SqlType(varchar), Length(126,true), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Length(126, varying = true), O.Default(None))
  }
  /** Collection-like TableQuery object for table Currencies */
  lazy val Currencies = new TableQuery(tag => new Currencies(tag))

  /**
   * Entity class storing rows of table DsBars
   *  @param dsId Database column ds_id SqlType(int4)
   *  @param start Database column start SqlType(timestamp)
   *  @param open Database column open SqlType(float8)
   *  @param high Database column high SqlType(float8)
   *  @param low Database column low SqlType(float8)
   *  @param close Database column close SqlType(float8)
   *  @param volume Database column volume SqlType(int4)
   *  @param count Database column count SqlType(int4), Default(None)
   *  @param wap Database column wap SqlType(float8)
   *  @param hasGaps Database column has_gaps SqlType(bool)
   */
  case class DsBar(dsId: io.deftrade.db.DsrootRowId, start: java.time.LocalDateTime, open: Double, high: Double, low: Double, close: Double, volume: Int, count: Option[Int] = None, wap: Double, hasGaps: Boolean)
  /** GetResult implicit for fetching DsBar objects using plain SQL queries */
  implicit def GetResultDsBar(implicit e0: GR[io.deftrade.db.DsrootRowId], e1: GR[java.time.LocalDateTime], e2: GR[Double], e3: GR[Int], e4: GR[Option[Int]], e5: GR[Boolean]): GR[DsBar] = GR {
    prs =>
      import prs._
      val r = (<<[io.deftrade.db.DsrootRowId], <<[java.time.LocalDateTime], <<[Double], <<[Double], <<[Double], <<[Double], <<[Int], <<?[Int], <<[Double], <<[Boolean])
      import r._
      DsBar.tupled((_1, _2, _3, _4, _5, _6, _7, _8, _9, _10)) // putting AutoInc last
  }
  /** Table description of table ds_bars. Objects of this class serve as prototypes for rows in queries. */
  class DsBars(_tableTag: Tag) extends Table[DsBar](_tableTag, "ds_bars") {
    def * = (dsId, start, open, high, low, close, volume, count, wap, hasGaps) <> (DsBar.tupled, DsBar.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(dsId), Rep.Some(start), Rep.Some(open), Rep.Some(high), Rep.Some(low), Rep.Some(close), Rep.Some(volume), count, Rep.Some(wap), Rep.Some(hasGaps)).shaped.<>({ r => import r._; _1.map(_ => DsBar.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get, _7.get, _8, _9.get, _10.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column ds_id SqlType(int4) */
    val dsId: Rep[io.deftrade.db.DsrootRowId] = column[io.deftrade.db.DsrootRowId]("ds_id")
    /** Database column start SqlType(timestamp) */
    val start: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("start")
    /** Database column open SqlType(float8) */
    val open: Rep[Double] = column[Double]("open")
    /** Database column high SqlType(float8) */
    val high: Rep[Double] = column[Double]("high")
    /** Database column low SqlType(float8) */
    val low: Rep[Double] = column[Double]("low")
    /** Database column close SqlType(float8) */
    val close: Rep[Double] = column[Double]("close")
    /** Database column volume SqlType(int4) */
    val volume: Rep[Int] = column[Int]("volume")
    /** Database column count SqlType(int4), Default(None) */
    val count: Rep[Option[Int]] = column[Option[Int]]("count", O.Default(None))
    /** Database column wap SqlType(float8) */
    val wap: Rep[Double] = column[Double]("wap")
    /** Database column has_gaps SqlType(bool) */
    val hasGaps: Rep[Boolean] = column[Boolean]("has_gaps")

    /** Foreign key referencing DsRoot (database name ds_bars_ds_id_fkey) */
    lazy val dsRootFk = foreignKey("ds_bars_ds_id_fkey", dsId, DsRoot)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table DsBars */
  lazy val DsBars = new TableQuery(tag => new DsBars(tag))

  /**
   * Entity class storing rows of table DsInputs
   *  @param dsId Database column ds_id SqlType(int4)
   *  @param inputId Database column input_id SqlType(int4)
   */
  case class DsInput(dsId: io.deftrade.db.DsrootRowId, inputId: io.deftrade.db.DsrootRowId)
  /** GetResult implicit for fetching DsInput objects using plain SQL queries */
  implicit def GetResultDsInput(implicit e0: GR[io.deftrade.db.DsrootRowId]): GR[DsInput] = GR {
    prs =>
      import prs._
      val r = (<<[io.deftrade.db.DsrootRowId], <<[io.deftrade.db.DsrootRowId])
      import r._
      DsInput.tupled((_1, _2)) // putting AutoInc last
  }
  /** Table description of table ds_inputs. Objects of this class serve as prototypes for rows in queries. */
  class DsInputs(_tableTag: Tag) extends Table[DsInput](_tableTag, "ds_inputs") {
    def * = (dsId, inputId) <> (DsInput.tupled, DsInput.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(dsId), Rep.Some(inputId)).shaped.<>({ r => import r._; _1.map(_ => DsInput.tupled((_1.get, _2.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column ds_id SqlType(int4) */
    val dsId: Rep[io.deftrade.db.DsrootRowId] = column[io.deftrade.db.DsrootRowId]("ds_id")
    /** Database column input_id SqlType(int4) */
    val inputId: Rep[io.deftrade.db.DsrootRowId] = column[io.deftrade.db.DsrootRowId]("input_id")

    /** Primary key of DsInputs (database name ds_inputs_pkey) */
    val pk = primaryKey("ds_inputs_pkey", (dsId, inputId))

    /** Foreign key referencing DsRoot (database name ds_inputs_ds_id_fkey) */
    lazy val dsRootFk1 = foreignKey("ds_inputs_ds_id_fkey", dsId, DsRoot)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.Cascade)
    /** Foreign key referencing DsRoot (database name ds_inputs_input_id_fkey) */
    lazy val dsRootFk2 = foreignKey("ds_inputs_input_id_fkey", inputId, DsRoot)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.Cascade)
  }
  /** Collection-like TableQuery object for table DsInputs */
  lazy val DsInputs = new TableQuery(tag => new DsInputs(tag))

  /**
   * Entity class storing rows of table DsPecks
   *  @param dsId Database column ds_id SqlType(int4)
   *  @param start Database column start SqlType(timestamp)
   *  @param peck Database column peck SqlType(jsonb), Length(2147483647,false)
   */
  case class DsPeck(dsId: io.deftrade.db.DsrootRowId, start: java.time.LocalDateTime, peck: String)
  /** GetResult implicit for fetching DsPeck objects using plain SQL queries */
  implicit def GetResultDsPeck(implicit e0: GR[io.deftrade.db.DsrootRowId], e1: GR[java.time.LocalDateTime], e2: GR[String]): GR[DsPeck] = GR {
    prs =>
      import prs._
      val r = (<<[io.deftrade.db.DsrootRowId], <<[java.time.LocalDateTime], <<[String])
      import r._
      DsPeck.tupled((_1, _2, _3)) // putting AutoInc last
  }
  /** Table description of table ds_pecks. Objects of this class serve as prototypes for rows in queries. */
  class DsPecks(_tableTag: Tag) extends Table[DsPeck](_tableTag, "ds_pecks") {
    def * = (dsId, start, peck) <> (DsPeck.tupled, DsPeck.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(dsId), Rep.Some(start), Rep.Some(peck)).shaped.<>({ r => import r._; _1.map(_ => DsPeck.tupled((_1.get, _2.get, _3.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column ds_id SqlType(int4) */
    val dsId: Rep[io.deftrade.db.DsrootRowId] = column[io.deftrade.db.DsrootRowId]("ds_id")
    /** Database column start SqlType(timestamp) */
    val start: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("start")
    /** Database column peck SqlType(jsonb), Length(2147483647,false) */
    val peck: Rep[String] = column[String]("peck", O.Length(2147483647, varying = false))

    /** Foreign key referencing DsRoot (database name ds_pecks_ds_id_fkey) */
    lazy val dsRootFk = foreignKey("ds_pecks_ds_id_fkey", dsId, DsRoot)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.Restrict)

    /** Index over (start) (database name ds_pecks_ts_index) */
    val index1 = index("ds_pecks_ts_index", start)
  }
  /** Collection-like TableQuery object for table DsPecks */
  lazy val DsPecks = new TableQuery(tag => new DsPecks(tag))

  /**
   * Entity class storing rows of table DsRoot
   *  @param ts Database column ts SqlType(timestamp), Default(None)
   *  @param meta Database column meta SqlType(jsonb), Length(2147483647,false)
   *  @param encoding Database column encoding SqlType(jsonb), Length(2147483647,false)
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   */
  case class DsRootRow(ts: Option[java.time.LocalDateTime] = None, meta: String, encoding: String, id: Option[io.deftrade.db.DsrootRowId] = None)
  /** GetResult implicit for fetching DsRootRow objects using plain SQL queries */
  implicit def GetResultDsRootRow(implicit e0: GR[Option[java.time.LocalDateTime]], e1: GR[String], e2: GR[Option[io.deftrade.db.DsrootRowId]]): GR[DsRootRow] = GR {
    prs =>
      import prs._
      val r = (<<?[io.deftrade.db.DsrootRowId], <<?[java.time.LocalDateTime], <<[String], <<[String])
      import r._
      DsRootRow.tupled((_2, _3, _4, _1)) // putting AutoInc last
  }
  /** Table description of table ds_root. Objects of this class serve as prototypes for rows in queries. */
  class DsRoot(_tableTag: Tag) extends Table[DsRootRow](_tableTag, "ds_root") {
    def * = (ts, meta, encoding, Rep.Some(id)) <> (DsRootRow.tupled, DsRootRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (ts, Rep.Some(meta), Rep.Some(encoding), Rep.Some(id)).shaped.<>({ r => import r._; _2.map(_ => DsRootRow.tupled((_1, _2.get, _3.get, _4))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column ts SqlType(timestamp), Default(None) */
    val ts: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("ts", O.Default(None))
    /** Database column meta SqlType(jsonb), Length(2147483647,false) */
    val meta: Rep[String] = column[String]("meta", O.Length(2147483647, varying = false))
    /** Database column encoding SqlType(jsonb), Length(2147483647,false) */
    val encoding: Rep[String] = column[String]("encoding", O.Length(2147483647, varying = false))
    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[io.deftrade.db.DsrootRowId] = column[io.deftrade.db.DsrootRowId]("id", O.AutoInc, O.PrimaryKey)
  }
  /** Collection-like TableQuery object for table DsRoot */
  lazy val DsRoot = new TableQuery(tag => new DsRoot(tag))

  /**
   * Entity class storing rows of table Exchanges
   *  @param name Database column name SqlType(varchar), Length(126,true)
   *  @param statusTs Database column status_ts SqlType(timestamp)
   *  @param endTs Database column end_ts SqlType(timestamp), Default(None)
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   */
  case class Exchange(name: String, statusTs: java.time.LocalDateTime, endTs: Option[java.time.LocalDateTime] = None, id: Option[io.deftrade.db.ExchangeId] = None)
  /** GetResult implicit for fetching Exchange objects using plain SQL queries */
  implicit def GetResultExchange(implicit e0: GR[String], e1: GR[java.time.LocalDateTime], e2: GR[Option[java.time.LocalDateTime]], e3: GR[Option[io.deftrade.db.ExchangeId]]): GR[Exchange] = GR {
    prs =>
      import prs._
      val r = (<<?[io.deftrade.db.ExchangeId], <<[String], <<[java.time.LocalDateTime], <<?[java.time.LocalDateTime])
      import r._
      Exchange.tupled((_2, _3, _4, _1)) // putting AutoInc last
  }
  /** Table description of table exchanges. Objects of this class serve as prototypes for rows in queries. */
  class Exchanges(_tableTag: Tag) extends Table[Exchange](_tableTag, "exchanges") {
    def * = (name, statusTs, endTs, Rep.Some(id)) <> (Exchange.tupled, Exchange.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(name), Rep.Some(statusTs), endTs, Rep.Some(id)).shaped.<>({ r => import r._; _1.map(_ => Exchange.tupled((_1.get, _2.get, _3, _4))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column name SqlType(varchar), Length(126,true) */
    val name: Rep[String] = column[String]("name", O.Length(126, varying = true))
    /** Database column status_ts SqlType(timestamp) */
    val statusTs: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("status_ts")
    /** Database column end_ts SqlType(timestamp), Default(None) */
    val endTs: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("end_ts", O.Default(None))
    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[io.deftrade.db.ExchangeId] = column[io.deftrade.db.ExchangeId]("id", O.AutoInc, O.PrimaryKey)
  }
  /** Collection-like TableQuery object for table Exchanges */
  lazy val Exchanges = new TableQuery(tag => new Exchanges(tag))

  /**
   * Entity class storing rows of table MdFeeds
   *  @param vendorId Database column vendor_id SqlType(int4)
   *  @param name Database column name SqlType(varchar), Length(126,true)
   *  @param meta Database column meta SqlType(jsonb), Length(2147483647,false), Default(None)
   *  @param statusTs Database column status_ts SqlType(timestamp)
   *  @param endTs Database column end_ts SqlType(timestamp), Default(None)
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   */
  case class MdFeed(vendorId: io.deftrade.db.MdvendorId, name: String, meta: Option[String] = None, statusTs: java.time.LocalDateTime, endTs: Option[java.time.LocalDateTime] = None, id: Option[io.deftrade.db.MdfeedId] = None)
  /** GetResult implicit for fetching MdFeed objects using plain SQL queries */
  implicit def GetResultMdFeed(implicit e0: GR[io.deftrade.db.MdvendorId], e1: GR[String], e2: GR[Option[String]], e3: GR[java.time.LocalDateTime], e4: GR[Option[java.time.LocalDateTime]], e5: GR[Option[io.deftrade.db.MdfeedId]]): GR[MdFeed] = GR {
    prs =>
      import prs._
      val r = (<<?[io.deftrade.db.MdfeedId], <<[io.deftrade.db.MdvendorId], <<[String], <<?[String], <<[java.time.LocalDateTime], <<?[java.time.LocalDateTime])
      import r._
      MdFeed.tupled((_2, _3, _4, _5, _6, _1)) // putting AutoInc last
  }
  /** Table description of table md_feeds. Objects of this class serve as prototypes for rows in queries. */
  class MdFeeds(_tableTag: Tag) extends Table[MdFeed](_tableTag, "md_feeds") {
    def * = (vendorId, name, meta, statusTs, endTs, Rep.Some(id)) <> (MdFeed.tupled, MdFeed.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(vendorId), Rep.Some(name), meta, Rep.Some(statusTs), endTs, Rep.Some(id)).shaped.<>({ r => import r._; _1.map(_ => MdFeed.tupled((_1.get, _2.get, _3, _4.get, _5, _6))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column vendor_id SqlType(int4) */
    val vendorId: Rep[io.deftrade.db.MdvendorId] = column[io.deftrade.db.MdvendorId]("vendor_id")
    /** Database column name SqlType(varchar), Length(126,true) */
    val name: Rep[String] = column[String]("name", O.Length(126, varying = true))
    /** Database column meta SqlType(jsonb), Length(2147483647,false), Default(None) */
    val meta: Rep[Option[String]] = column[Option[String]]("meta", O.Length(2147483647, varying = false), O.Default(None))
    /** Database column status_ts SqlType(timestamp) */
    val statusTs: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("status_ts")
    /** Database column end_ts SqlType(timestamp), Default(None) */
    val endTs: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("end_ts", O.Default(None))
    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[io.deftrade.db.MdfeedId] = column[io.deftrade.db.MdfeedId]("id", O.AutoInc, O.PrimaryKey)

    /** Foreign key referencing MdVendors (database name md_feeds_vendor_id_fkey) */
    lazy val mdVendorsFk = foreignKey("md_feeds_vendor_id_fkey", vendorId, MdVendors)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)

    /** Uniqueness Index over (name) (database name md_feeds_name_key) */
    val index1 = index("md_feeds_name_key", name, unique = true)
  }
  /** Collection-like TableQuery object for table MdFeeds */
  lazy val MdFeeds = new TableQuery(tag => new MdFeeds(tag))

  /**
   * Entity class storing rows of table MdFeedsExchanges
   *  @param feedId Database column feed_id SqlType(int4)
   *  @param exchangeId Database column exchange_id SqlType(int4)
   *  @param secTypes Database column sec_types SqlType(_sec_type_e), Length(2147483647,false)
   *  @param statusTs Database column status_ts SqlType(timestamp)
   *  @param endTs Database column end_ts SqlType(timestamp), Default(None)
   */
  case class MdFeedsExchange(feedId: io.deftrade.db.MdfeedId, exchangeId: io.deftrade.db.ExchangeId, secTypes: List[io.deftrade.db.SecType.SecType], statusTs: java.time.LocalDateTime, endTs: Option[java.time.LocalDateTime] = None)
  /** GetResult implicit for fetching MdFeedsExchange objects using plain SQL queries */
  implicit def GetResultMdFeedsExchange(implicit e0: GR[io.deftrade.db.MdfeedId], e1: GR[io.deftrade.db.ExchangeId], e2: GR[List[io.deftrade.db.SecType.SecType]], e3: GR[java.time.LocalDateTime], e4: GR[Option[java.time.LocalDateTime]]): GR[MdFeedsExchange] = GR {
    prs =>
      import prs._
      val r = (<<[io.deftrade.db.MdfeedId], <<[io.deftrade.db.ExchangeId], <<[List[io.deftrade.db.SecType.SecType]], <<[java.time.LocalDateTime], <<?[java.time.LocalDateTime])
      import r._
      MdFeedsExchange.tupled((_1, _2, _3, _4, _5)) // putting AutoInc last
  }
  /** Table description of table md_feeds_exchanges. Objects of this class serve as prototypes for rows in queries. */
  class MdFeedsExchanges(_tableTag: Tag) extends Table[MdFeedsExchange](_tableTag, "md_feeds_exchanges") {
    def * = (feedId, exchangeId, secTypes, statusTs, endTs) <> (MdFeedsExchange.tupled, MdFeedsExchange.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(feedId), Rep.Some(exchangeId), Rep.Some(secTypes), Rep.Some(statusTs), endTs).shaped.<>({ r => import r._; _1.map(_ => MdFeedsExchange.tupled((_1.get, _2.get, _3.get, _4.get, _5))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column feed_id SqlType(int4) */
    val feedId: Rep[io.deftrade.db.MdfeedId] = column[io.deftrade.db.MdfeedId]("feed_id")
    /** Database column exchange_id SqlType(int4) */
    val exchangeId: Rep[io.deftrade.db.ExchangeId] = column[io.deftrade.db.ExchangeId]("exchange_id")
    /** Database column sec_types SqlType(_sec_type_e), Length(2147483647,false) */
    val secTypes: Rep[List[io.deftrade.db.SecType.SecType]] = column[List[io.deftrade.db.SecType.SecType]]("sec_types", O.Length(2147483647, varying = false))
    /** Database column status_ts SqlType(timestamp) */
    val statusTs: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("status_ts")
    /** Database column end_ts SqlType(timestamp), Default(None) */
    val endTs: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("end_ts", O.Default(None))

    /** Foreign key referencing Exchanges (database name md_feeds_exchanges_exchange_id_fkey) */
    lazy val exchangesFk = foreignKey("md_feeds_exchanges_exchange_id_fkey", exchangeId, Exchanges)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.Restrict)
    /** Foreign key referencing MdFeeds (database name md_feeds_exchanges_feed_id_fkey) */
    lazy val mdFeedsFk = foreignKey("md_feeds_exchanges_feed_id_fkey", feedId, MdFeeds)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.Restrict)
  }
  /** Collection-like TableQuery object for table MdFeedsExchanges */
  lazy val MdFeedsExchanges = new TableQuery(tag => new MdFeedsExchanges(tag))

  /**
   * Entity class storing rows of table MdSeries
   *  @param dsId Database column ds_id SqlType(int4)
   *  @param conId Database column con_id SqlType(int4)
   *  @param exchangeId Database column exchange_id SqlType(int4)
   *  @param currency Database column currency SqlType(bpchar), Length(3,false)
   *  @param feedId Database column feed_id SqlType(int4), Default(None)
   *  @param period Database column period SqlType(interval), Length(49,false)
   *  @param startTs Database column start_ts SqlType(timestamp)
   *  @param endTs Database column end_ts SqlType(timestamp), Default(None)
   */
  case class MdSeriesRow(dsId: io.deftrade.db.DsrootRowId, conId: Int, exchangeId: io.deftrade.db.ExchangeId, currency: String, feedId: Option[io.deftrade.db.MdfeedId] = None, period: String, startTs: java.time.LocalDateTime, endTs: Option[java.time.LocalDateTime] = None)
  /** GetResult implicit for fetching MdSeriesRow objects using plain SQL queries */
  implicit def GetResultMdSeriesRow(implicit e0: GR[io.deftrade.db.DsrootRowId], e1: GR[Int], e2: GR[io.deftrade.db.ExchangeId], e3: GR[String], e4: GR[Option[io.deftrade.db.MdfeedId]], e5: GR[java.time.LocalDateTime], e6: GR[Option[java.time.LocalDateTime]]): GR[MdSeriesRow] = GR {
    prs =>
      import prs._
      val r = (<<[io.deftrade.db.DsrootRowId], <<[Int], <<[io.deftrade.db.ExchangeId], <<[String], <<?[io.deftrade.db.MdfeedId], <<[String], <<[java.time.LocalDateTime], <<?[java.time.LocalDateTime])
      import r._
      MdSeriesRow.tupled((_1, _2, _3, _4, _5, _6, _7, _8)) // putting AutoInc last
  }
  /** Table description of table md_series. Objects of this class serve as prototypes for rows in queries. */
  class MdSeries(_tableTag: Tag) extends Table[MdSeriesRow](_tableTag, "md_series") {
    def * = (dsId, conId, exchangeId, currency, feedId, period, startTs, endTs) <> (MdSeriesRow.tupled, MdSeriesRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(dsId), Rep.Some(conId), Rep.Some(exchangeId), Rep.Some(currency), feedId, Rep.Some(period), Rep.Some(startTs), endTs).shaped.<>({ r => import r._; _1.map(_ => MdSeriesRow.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6.get, _7.get, _8))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column ds_id SqlType(int4) */
    val dsId: Rep[io.deftrade.db.DsrootRowId] = column[io.deftrade.db.DsrootRowId]("ds_id")
    /** Database column con_id SqlType(int4) */
    val conId: Rep[Int] = column[Int]("con_id")
    /** Database column exchange_id SqlType(int4) */
    val exchangeId: Rep[io.deftrade.db.ExchangeId] = column[io.deftrade.db.ExchangeId]("exchange_id")
    /** Database column currency SqlType(bpchar), Length(3,false) */
    val currency: Rep[String] = column[String]("currency", O.Length(3, varying = false))
    /** Database column feed_id SqlType(int4), Default(None) */
    val feedId: Rep[Option[io.deftrade.db.MdfeedId]] = column[Option[io.deftrade.db.MdfeedId]]("feed_id", O.Default(None))
    /** Database column period SqlType(interval), Length(49,false) */
    val period: Rep[String] = column[String]("period", O.Length(49, varying = false))
    /** Database column start_ts SqlType(timestamp) */
    val startTs: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("start_ts")
    /** Database column end_ts SqlType(timestamp), Default(None) */
    val endTs: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("end_ts", O.Default(None))

    /** Foreign key referencing Contracts (database name md_series_con_id_fkey) */
    lazy val contractsFk = foreignKey("md_series_con_id_fkey", conId, Contracts)(r => r.conId, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
    /** Foreign key referencing Currencies (database name md_series_currency_fkey) */
    lazy val currenciesFk = foreignKey("md_series_currency_fkey", currency, Currencies)(r => r.symbol, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
    /** Foreign key referencing DsRoot (database name md_series_ds_id_fkey) */
    lazy val dsRootFk = foreignKey("md_series_ds_id_fkey", dsId, DsRoot)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
    /** Foreign key referencing Exchanges (database name md_series_exchange_id_fkey) */
    lazy val exchangesFk = foreignKey("md_series_exchange_id_fkey", exchangeId, Exchanges)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
    /** Foreign key referencing MdFeeds (database name md_series_feed_id_fkey) */
    lazy val mdFeedsFk = foreignKey("md_series_feed_id_fkey", feedId, MdFeeds)(r => Rep.Some(r.id), onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.Restrict)

    /** Index over (conId,exchangeId,currency) (database name md_series_con_id_index) */
    val index1 = index("md_series_con_id_index", (conId, exchangeId, currency))
  }
  /** Collection-like TableQuery object for table MdSeries */
  lazy val MdSeries = new TableQuery(tag => new MdSeries(tag))

  /**
   * Entity class storing rows of table MdVendors
   *  @param name Database column name SqlType(varchar), Length(126,true)
   *  @param description Database column description SqlType(text), Default(None)
   *  @param statusTs Database column status_ts SqlType(timestamp)
   *  @param endTs Database column end_ts SqlType(timestamp), Default(None)
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   */
  case class MdVendor(name: String, description: Option[String] = None, statusTs: java.time.LocalDateTime, endTs: Option[java.time.LocalDateTime] = None, id: Option[io.deftrade.db.MdvendorId] = None)
  /** GetResult implicit for fetching MdVendor objects using plain SQL queries */
  implicit def GetResultMdVendor(implicit e0: GR[String], e1: GR[Option[String]], e2: GR[java.time.LocalDateTime], e3: GR[Option[java.time.LocalDateTime]], e4: GR[Option[io.deftrade.db.MdvendorId]]): GR[MdVendor] = GR {
    prs =>
      import prs._
      val r = (<<?[io.deftrade.db.MdvendorId], <<[String], <<?[String], <<[java.time.LocalDateTime], <<?[java.time.LocalDateTime])
      import r._
      MdVendor.tupled((_2, _3, _4, _5, _1)) // putting AutoInc last
  }
  /** Table description of table md_vendors. Objects of this class serve as prototypes for rows in queries. */
  class MdVendors(_tableTag: Tag) extends Table[MdVendor](_tableTag, "md_vendors") {
    def * = (name, description, statusTs, endTs, Rep.Some(id)) <> (MdVendor.tupled, MdVendor.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(name), description, Rep.Some(statusTs), endTs, Rep.Some(id)).shaped.<>({ r => import r._; _1.map(_ => MdVendor.tupled((_1.get, _2, _3.get, _4, _5))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column name SqlType(varchar), Length(126,true) */
    val name: Rep[String] = column[String]("name", O.Length(126, varying = true))
    /** Database column description SqlType(text), Default(None) */
    val description: Rep[Option[String]] = column[Option[String]]("description", O.Default(None))
    /** Database column status_ts SqlType(timestamp) */
    val statusTs: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("status_ts")
    /** Database column end_ts SqlType(timestamp), Default(None) */
    val endTs: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("end_ts", O.Default(None))
    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[io.deftrade.db.MdvendorId] = column[io.deftrade.db.MdvendorId]("id", O.AutoInc, O.PrimaryKey)

    /** Uniqueness Index over (name) (database name md_vendors_name_key) */
    val index1 = index("md_vendors_name_key", name, unique = true)
  }
  /** Collection-like TableQuery object for table MdVendors */
  lazy val MdVendors = new TableQuery(tag => new MdVendors(tag))

  /**
   * Entity class storing rows of table RfCoaCode
   *  @param code Database column code SqlType(bpchar), Length(6,false)
   *  @param rfStatementType Database column rf_statement_type SqlType(rf_statement_type_e)
   *  @param description Database column description SqlType(text)
   *  @param statusTs Database column status_ts SqlType(timestamp)
   *  @param endTs Database column end_ts SqlType(timestamp), Default(None)
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   */
  case class RfCoaCodeRow(code: String, rfStatementType: io.deftrade.db.RfStatementType.RfStatementType, description: String, statusTs: java.time.LocalDateTime, endTs: Option[java.time.LocalDateTime] = None, id: Option[io.deftrade.db.RfcoacodeRowId] = None)
  /** GetResult implicit for fetching RfCoaCodeRow objects using plain SQL queries */
  implicit def GetResultRfCoaCodeRow(implicit e0: GR[String], e1: GR[io.deftrade.db.RfStatementType.RfStatementType], e2: GR[java.time.LocalDateTime], e3: GR[Option[java.time.LocalDateTime]], e4: GR[Option[io.deftrade.db.RfcoacodeRowId]]): GR[RfCoaCodeRow] = GR {
    prs =>
      import prs._
      val r = (<<?[io.deftrade.db.RfcoacodeRowId], <<[String], <<[io.deftrade.db.RfStatementType.RfStatementType], <<[String], <<[java.time.LocalDateTime], <<?[java.time.LocalDateTime])
      import r._
      RfCoaCodeRow.tupled((_2, _3, _4, _5, _6, _1)) // putting AutoInc last
  }
  /** Table description of table rf_coa_code. Objects of this class serve as prototypes for rows in queries. */
  class RfCoaCode(_tableTag: Tag) extends Table[RfCoaCodeRow](_tableTag, "rf_coa_code") {
    def * = (code, rfStatementType, description, statusTs, endTs, Rep.Some(id)) <> (RfCoaCodeRow.tupled, RfCoaCodeRow.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(code), Rep.Some(rfStatementType), Rep.Some(description), Rep.Some(statusTs), endTs, Rep.Some(id)).shaped.<>({ r => import r._; _1.map(_ => RfCoaCodeRow.tupled((_1.get, _2.get, _3.get, _4.get, _5, _6))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column code SqlType(bpchar), Length(6,false) */
    val code: Rep[String] = column[String]("code", O.Length(6, varying = false))
    /** Database column rf_statement_type SqlType(rf_statement_type_e) */
    val rfStatementType: Rep[io.deftrade.db.RfStatementType.RfStatementType] = column[io.deftrade.db.RfStatementType.RfStatementType]("rf_statement_type")
    /** Database column description SqlType(text) */
    val description: Rep[String] = column[String]("description")
    /** Database column status_ts SqlType(timestamp) */
    val statusTs: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("status_ts")
    /** Database column end_ts SqlType(timestamp), Default(None) */
    val endTs: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("end_ts", O.Default(None))
    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[io.deftrade.db.RfcoacodeRowId] = column[io.deftrade.db.RfcoacodeRowId]("id", O.AutoInc, O.PrimaryKey)
  }
  /** Collection-like TableQuery object for table RfCoaCode */
  lazy val RfCoaCode = new TableQuery(tag => new RfCoaCode(tag))

  /**
   * Entity class storing rows of table RfLineItems
   *  @param coaId Database column coa_id SqlType(int4)
   *  @param statementId Database column statement_id SqlType(int4)
   *  @param value Database column value SqlType(numeric)
   */
  case class RfLineItem(coaId: io.deftrade.db.RfcoacodeRowId, statementId: io.deftrade.db.RfstatementId, value: scala.math.BigDecimal)
  /** GetResult implicit for fetching RfLineItem objects using plain SQL queries */
  implicit def GetResultRfLineItem(implicit e0: GR[io.deftrade.db.RfcoacodeRowId], e1: GR[io.deftrade.db.RfstatementId], e2: GR[scala.math.BigDecimal]): GR[RfLineItem] = GR {
    prs =>
      import prs._
      val r = (<<[io.deftrade.db.RfcoacodeRowId], <<[io.deftrade.db.RfstatementId], <<[scala.math.BigDecimal])
      import r._
      RfLineItem.tupled((_1, _2, _3)) // putting AutoInc last
  }
  /** Table description of table rf_line_items. Objects of this class serve as prototypes for rows in queries. */
  class RfLineItems(_tableTag: Tag) extends Table[RfLineItem](_tableTag, "rf_line_items") {
    def * = (coaId, statementId, value) <> (RfLineItem.tupled, RfLineItem.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(coaId), Rep.Some(statementId), Rep.Some(value)).shaped.<>({ r => import r._; _1.map(_ => RfLineItem.tupled((_1.get, _2.get, _3.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column coa_id SqlType(int4) */
    val coaId: Rep[io.deftrade.db.RfcoacodeRowId] = column[io.deftrade.db.RfcoacodeRowId]("coa_id")
    /** Database column statement_id SqlType(int4) */
    val statementId: Rep[io.deftrade.db.RfstatementId] = column[io.deftrade.db.RfstatementId]("statement_id")
    /** Database column value SqlType(numeric) */
    val value: Rep[scala.math.BigDecimal] = column[scala.math.BigDecimal]("value")

    /** Foreign key referencing RfCoaCode (database name rf_line_items_coa_id_fkey) */
    lazy val rfCoaCodeFk = foreignKey("rf_line_items_coa_id_fkey", coaId, RfCoaCode)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
    /** Foreign key referencing RfStatements (database name rf_line_items_statement_id_fkey) */
    lazy val rfStatementsFk = foreignKey("rf_line_items_statement_id_fkey", statementId, RfStatements)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table RfLineItems */
  lazy val RfLineItems = new TableQuery(tag => new RfLineItems(tag))

  /**
   * Entity class storing rows of table RfStatements
   *  @param corpId Database column corp_id SqlType(int4)
   *  @param rfStatementType Database column rf_statement_type SqlType(rf_statement_type_e)
   *  @param periodLength Database column period_length SqlType(int2)
   *  @param periodType Database column period_type SqlType(varchar), Length(126,true)
   *  @param statementDate Database column statement_date SqlType(date)
   *  @param meta Database column meta SqlType(jsonb), Length(2147483647,false), Default(None)
   *  @param statusTs Database column status_ts SqlType(timestamp)
   *  @param endTs Database column end_ts SqlType(timestamp), Default(None)
   *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
   */
  case class RfStatement(corpId: io.deftrade.db.CorporationId, rfStatementType: io.deftrade.db.RfStatementType.RfStatementType, periodLength: Short, periodType: String, statementDate: java.time.LocalDate, meta: Option[String] = None, statusTs: java.time.LocalDateTime, endTs: Option[java.time.LocalDateTime] = None, id: Option[io.deftrade.db.RfstatementId] = None)
  /** GetResult implicit for fetching RfStatement objects using plain SQL queries */
  implicit def GetResultRfStatement(implicit e0: GR[io.deftrade.db.CorporationId], e1: GR[io.deftrade.db.RfStatementType.RfStatementType], e2: GR[Short], e3: GR[String], e4: GR[java.time.LocalDate], e5: GR[Option[String]], e6: GR[java.time.LocalDateTime], e7: GR[Option[java.time.LocalDateTime]], e8: GR[Option[io.deftrade.db.RfstatementId]]): GR[RfStatement] = GR {
    prs =>
      import prs._
      val r = (<<?[io.deftrade.db.RfstatementId], <<[io.deftrade.db.CorporationId], <<[io.deftrade.db.RfStatementType.RfStatementType], <<[Short], <<[String], <<[java.time.LocalDate], <<?[String], <<[java.time.LocalDateTime], <<?[java.time.LocalDateTime])
      import r._
      RfStatement.tupled((_2, _3, _4, _5, _6, _7, _8, _9, _1)) // putting AutoInc last
  }
  /** Table description of table rf_statements. Objects of this class serve as prototypes for rows in queries. */
  class RfStatements(_tableTag: Tag) extends Table[RfStatement](_tableTag, "rf_statements") {
    def * = (corpId, rfStatementType, periodLength, periodType, statementDate, meta, statusTs, endTs, Rep.Some(id)) <> (RfStatement.tupled, RfStatement.unapply)
    /** Maps whole row to an option. Useful for outer joins. */
    def ? = (Rep.Some(corpId), Rep.Some(rfStatementType), Rep.Some(periodLength), Rep.Some(periodType), Rep.Some(statementDate), meta, Rep.Some(statusTs), endTs, Rep.Some(id)).shaped.<>({ r => import r._; _1.map(_ => RfStatement.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6, _7.get, _8, _9))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))

    /** Database column corp_id SqlType(int4) */
    val corpId: Rep[io.deftrade.db.CorporationId] = column[io.deftrade.db.CorporationId]("corp_id")
    /** Database column rf_statement_type SqlType(rf_statement_type_e) */
    val rfStatementType: Rep[io.deftrade.db.RfStatementType.RfStatementType] = column[io.deftrade.db.RfStatementType.RfStatementType]("rf_statement_type")
    /** Database column period_length SqlType(int2) */
    val periodLength: Rep[Short] = column[Short]("period_length")
    /** Database column period_type SqlType(varchar), Length(126,true) */
    val periodType: Rep[String] = column[String]("period_type", O.Length(126, varying = true))
    /** Database column statement_date SqlType(date) */
    val statementDate: Rep[java.time.LocalDate] = column[java.time.LocalDate]("statement_date")
    /** Database column meta SqlType(jsonb), Length(2147483647,false), Default(None) */
    val meta: Rep[Option[String]] = column[Option[String]]("meta", O.Length(2147483647, varying = false), O.Default(None))
    /** Database column status_ts SqlType(timestamp) */
    val statusTs: Rep[java.time.LocalDateTime] = column[java.time.LocalDateTime]("status_ts")
    /** Database column end_ts SqlType(timestamp), Default(None) */
    val endTs: Rep[Option[java.time.LocalDateTime]] = column[Option[java.time.LocalDateTime]]("end_ts", O.Default(None))
    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
    val id: Rep[io.deftrade.db.RfstatementId] = column[io.deftrade.db.RfstatementId]("id", O.AutoInc, O.PrimaryKey)

    /** Foreign key referencing Corporations (database name rf_statements_corp_id_fkey) */
    lazy val corporationsFk = foreignKey("rf_statements_corp_id_fkey", corpId, Corporations)(r => r.id, onUpdate = ForeignKeyAction.NoAction, onDelete = ForeignKeyAction.NoAction)
  }
  /** Collection-like TableQuery object for table RfStatements */
  lazy val RfStatements = new TableQuery(tag => new RfStatements(tag))
}
// auto generated type-safe primary key value classes

case class CorporationId(val value: Int) extends AnyVal with slick.lifted.MappedTo[Int]
object CorporationId extends IdCompanion[CorporationId]

case class DsrootRowId(val value: Int) extends AnyVal with slick.lifted.MappedTo[Int]
object DsrootRowId extends IdCompanion[DsrootRowId]

case class ExchangeId(val value: Int) extends AnyVal with slick.lifted.MappedTo[Int]
object ExchangeId extends IdCompanion[ExchangeId]

case class MdfeedId(val value: Int) extends AnyVal with slick.lifted.MappedTo[Int]
object MdfeedId extends IdCompanion[MdfeedId]

case class MdvendorId(val value: Int) extends AnyVal with slick.lifted.MappedTo[Int]
object MdvendorId extends IdCompanion[MdvendorId]

case class RfcoacodeRowId(val value: Int) extends AnyVal with slick.lifted.MappedTo[Int]
object RfcoacodeRowId extends IdCompanion[RfcoacodeRowId]

case class RfstatementId(val value: Int) extends AnyVal with slick.lifted.MappedTo[Int]
object RfstatementId extends IdCompanion[RfstatementId]
