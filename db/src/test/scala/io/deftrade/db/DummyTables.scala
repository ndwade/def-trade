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
///*
// * Copyright 2014-2016 Panavista Technologies, LLC
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package io.deftrade.db
//
//object ExchangeOs extends Enumeration with SlickPgImplicits {
//  type ExchangeOs = Value
//  val Dummy = Value
//}
//
///** A dummy `Tables` module - [[SourceCodeGenerator]] should generate all of this */
//
//trait DummyTables { self: Repositories =>
//  //  val profile: DefTradePgDriver // must retain this driver
//  import profile.api._
//
//  import slick.model.ForeignKeyAction
//  // NOTE: GetResult mappers for plain SQL are only generated for tables where Slick knows how to map the types of all columns.
//  import slick.jdbc.{ GetResult => GR }
//
//  /**
//    * Entity class storing rows of table IsoExchanges
//    *  @param mic Database column mic SqlType(bpchar), Length(4,false)
//    *  @param omic Database column omic SqlType(bpchar), Length(4,false)
//    *  @param oOrS Database column o_or_s SqlType(exchange_os_e)
//    *  @param acronym Database column acronym SqlType(varchar), Length(126,true), Default(None)
//    *  @param name Database column name SqlType(varchar), Length(126,true)
//    *  @param city Database column city SqlType(varchar), Length(126,true)
//    *  @param url Database column url SqlType(varchar), Length(126,true), Default(None)
//    *  @param statusTs Database column status_ts SqlType(timestamp)
//    *  @param endTs Database column end_ts SqlType(timestamp), Default(None)
//    *  @param id Database column id SqlType(serial), AutoInc, PrimaryKey
//    */
//  case class IsoExchange(mic: String, omic: String, oOrS: io.deftrade.db.ExchangeOs.ExchangeOs, acronym: Option[String] = None, name: String, city: String, url: Option[String] = None, statusTs: java.time.OffsetDateTime, endTs: Option[java.time.OffsetDateTime] = None, id: Option[io.deftrade.db.IsoexchangeId] = None) extends EntityId[Int, IsoexchangeId] with EntityPit
//  /** GetResult implicit for fetching IsoExchange objects using plain SQL queries */
//  implicit def GetResultIsoExchange(implicit e0: GR[String], e1: GR[io.deftrade.db.ExchangeOs.ExchangeOs], e2: GR[Option[String]], e3: GR[java.time.OffsetDateTime], e4: GR[Option[java.time.OffsetDateTime]], e5: GR[Option[io.deftrade.db.IsoexchangeId]]): GR[IsoExchange] = GR{
//    prs =>
//      import prs._
//      val r = (<<?[io.deftrade.db.IsoexchangeId], <<[String], <<[String], <<[io.deftrade.db.ExchangeOs.ExchangeOs], <<?[String], <<[String], <<[String], <<?[String], <<[java.time.OffsetDateTime], <<?[java.time.OffsetDateTime])
//      import r._
//      IsoExchange.tupled((_2, _3, _4, _5, _6, _7, _8, _9, _10, _1)) // putting AutoInc last
//  }
//  /** Table description of table iso_exchanges. Objects of this class serve as prototypes for rows in queries. */
//  class IsoExchanges(_tableTag: Tag) extends Table[IsoExchange](_tableTag, "iso_exchanges") with TableId[IsoExchange] with TablePit[IsoExchange] {
//    def * = (mic, omic, oOrS, acronym, name, city, url, statusTs, endTs, Rep.Some(id)) <> (IsoExchange.tupled, IsoExchange.unapply)
//    /** Maps whole row to an option. Useful for outer joins. */
//    def ? = (Rep.Some(mic), Rep.Some(omic), Rep.Some(oOrS), acronym, Rep.Some(name), Rep.Some(city), url, Rep.Some(statusTs), endTs, Rep.Some(id)).shaped.<>({ r => import r._; _1.map(_ => IsoExchange.tupled((_1.get, _2.get, _3.get, _4, _5.get, _6.get, _7, _8.get, _9, _10))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))
//
//    /** Database column id SqlType(serial), AutoInc, PrimaryKey */
//    val id: Rep[io.deftrade.db.IsoexchangeId] = column[io.deftrade.db.IsoexchangeId]("id", O.AutoInc, O.PrimaryKey)
//    /** Database column mic SqlType(bpchar), Length(4,false) */
//    val mic: Rep[String] = column[String]("mic", O.Length(4, varying = false))
//    /** Database column omic SqlType(bpchar), Length(4,false) */
//    val omic: Rep[String] = column[String]("omic", O.Length(4, varying = false))
//    /** Database column o_or_s SqlType(exchange_os_e) */
//    val oOrS: Rep[io.deftrade.db.ExchangeOs.ExchangeOs] = column[io.deftrade.db.ExchangeOs.ExchangeOs]("o_or_s")
//    /** Database column acronym SqlType(varchar), Length(126,true), Default(None) */
//    val acronym: Rep[Option[String]] = column[Option[String]]("acronym", O.Length(126, varying = true), O.Default(None))
//    /** Database column name SqlType(varchar), Length(126,true) */
//    val name: Rep[String] = column[String]("name", O.Length(126, varying = true))
//    /** Database column city SqlType(varchar), Length(126,true) */
//    val city: Rep[String] = column[String]("city", O.Length(126, varying = true))
//    /** Database column url SqlType(varchar), Length(126,true), Default(None) */
//    val url: Rep[Option[String]] = column[Option[String]]("url", O.Length(126, varying = true), O.Default(None))
//    /** Database column status_ts SqlType(timestamp) */
//    val statusTs: Rep[java.time.OffsetDateTime] = column[java.time.OffsetDateTime]("status_ts")
//    /** Database column end_ts SqlType(timestamp), Default(None) */
//    val endTs: Rep[Option[java.time.OffsetDateTime]] = column[Option[java.time.OffsetDateTime]]("end_ts", O.Default(None))
//
//    /** Index over (mic) (database name iso_exchanges_mic_index) */
//    val index1 = index("iso_exchanges_mic_index", mic)
//  }
//  /** Collection-like TableQuery object for table IsoExchanges */
//  //  lazy val IsoExchanges = new Repository with RepositoryId with RepositoryPit {
//  class IsoExchangesRepository extends Repository with RepositoryId with RepositoryPit {
//    import profile.api._
//    type T = IsoExchange
//    type E = IsoExchanges
//    //    override lazy val rows: TableQuery[IsoExchanges] = new TableQuery(tag => new IsoExchanges(tag))
//    override lazy val rows = TableQuery[E]
//    /** generated due to index1 on mic */
//    def findByMic(mic: String): DBIO[Seq[T]] = findBy(_.mic, mic) //(stringColumnType)
//  }
//  lazy val IsoExchanges = new IsoExchangesRepository
//  implicit class IsoExchangePAR(val entity: IsoExchange) extends PassiveAggressiveRecord[IsoExchanges.type](IsoExchanges)
//}
//
//object DummyTables extends {
//  val profile = DefTradePgDriver
//} with DummyTables with Repositories
//
//case class IsoexchangeId(val value: Int) extends AnyVal with slick.lifted.MappedTo[Int]
//object IsoexchangeId extends IdCompanion[IsoexchangeId]