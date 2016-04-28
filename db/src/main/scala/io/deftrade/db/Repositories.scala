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

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

final case class Id[T, V <: AnyVal](val value: V) extends AnyVal with slick.lifted.MappedTo[V]
object Id {
  implicit def ordering[A, V <: AnyVal: Ordering] = Ordering.by[Id[A, V], V]((id: Id[A, V]) => id.value)
}

/**
 * Mixin for the `Tables` trait created by the [[SourceCodeGenerator]], adding various repository
 * features including ActiveRecord.
 *
 * The [[Repository]] instance for each table is constructed by the `SourceCodeGenerator`,
 * including type member and value member definitions.
 *
 * === Usage within [[SourceCodeGenerator]] ===
 * - Repository is created for every table, even those without a primary key
 * - RepoId trait stacked when table has primary key 'id' of type int4 or int8
 *   - also generate value classes for the id
 * - if there is a pk 'id' as above, and also an eid col identified by an index,
 *   then also stack a RepoEid trait which identifies the latest (highest id)
 *   (also make a view?)
 * - if there is a RepoEid as above, and table has a column 'ts' of type timestamptz,
 *   then RepoPit trait is stacked to provide PointInTime methods.
 *
 * === TODO: ===
 * - optimistic locking
 * - `save(r: T)`: insert or update (upsert?)
 * - revisit using implicit for ColumnType instead of implicit param everywhere
 * - pre-insert and post-insert hooks - issue - how to get these picked up by code gen?
 * - junction repo stuff
 * - save multiple entities
 * - final val tableName = query.baseTableRow.tableName useful for exceptions
 * - implement Query versions of `findBy` (not just Action)
 * - consider restricting `findBy` to btree indexes only - will need some hints or annotations...
 + - PasAggRecId, PasAggRecIdPit, etc
 */
trait Repositories {

  val profile: DefTradePgDriver // must retain this driver
  import profile.SchemaDescription
  import profile.api._

  trait EntityIdLike { self =>
    type V <: AnyVal
    type T
    type ID = Id[T, V]
    def id: Option[ID]
  }

  trait EntityId[T_, V_ <: AnyVal] extends EntityIdLike { type T = T_; type V = V_ }

  trait TableId[T <: EntityIdLike] { self: Table[T] =>
    type V = T#V
    type ID = Id[T, V]
    def id: Rep[ID]
  }

  /**
   *  Base trait for all repositories. The abstract type members follow the naming convention used
   *  by Slick.
   */
  trait Repository {
    /** type T represents the Element type */
    type T
    /** type E represents the Table type */
    type E <: Table[T]
    def rows: TableQuery[E]
    final def schema: SchemaDescription = rows.schema
    final def apply: DBIO[Seq[T]] = rows.result
    final def insert(t: T): DBIO[T] = rows returning rows += t
    final def delete(): DBIO[Int] = rows.delete
    final def size(): DBIO[Int] = rows.size.result
    final def create(): DBIO[Unit] = rows.schema.create
    final def drop(): DBIO[Unit] = rows.schema.drop
    final def findBy[A: ColumnType](getA: E => Rep[A], a: A): DBIO[Seq[T]] = (rows filter { getA(_) === a }).result
    final def stream(fetchSize: Int = 100)(implicit exc: ExecutionContext): StreamingDBIO[Seq[T], T] = {
      rows.result.transactionally.withStatementParameters(fetchSize = fetchSize)
    }
  }

  trait RepositoryId extends Repository {
    type T <: EntityIdLike
    type E <: Table[T] with TableId[T]
    type ID = Id[T, T#V]
    //    implicit def idColumnType: slick.ast.BaseTypedType[Id[T]] // type ascription needed for ProvenShape inference?!
    implicit def idColumnType: ColumnType[ID] // type ascription needed for ProvenShape inference?!
    def findByIdQuery(id: ID): Query[E, T, Seq] = rows filter { _.id === id }
    def findById(id: ID): DBIO[T] = findByIdQuery(id).result.head
    def maybeFindById(id: ID): DBIO[Option[T]] = findByIdQuery(id).result.headOption

    final def insertReturnId(t: T): DBIO[ID] = rows returning (rows map (_.id)) += t

    /**
     * Returns the most recent version of an element indexed by a column of type `A`.
     * This will typically be the natural (domain) key for the entity (as opposed to the
     * surrogate key `id`).
     * Relies on the fact that the `id` index is always increasing.
     */
    final def findCurrentBy[A](getA: E => Rep[A], a: A)(
      implicit evA: ColumnType[A]): DBIO[Option[T]] = {

      val aRows = for {
        row <- rows filter { getA(_) === a }
      } yield row

      val maxId = aRows map (_.id) max

      val rs = for {
        r <- aRows if r.id === maxId
      } yield r
      rs.result.headOption
    }
  }

  //  import java.sql.{ Timestamp => OffsetDateTime }
  import java.time.OffsetDateTime
  private def now() = OffsetDateTime.from(java.time.Instant.now())
  import com.github.tminglei.slickpg.{ Range => PgRange, `[_,_)` }
  type Span = PgRange[OffsetDateTime]
  object Span {
    lazy val empty: Span = PgRange.apply(None, None, `[_,_)`) 
  }
  trait EntityPit {
    def span: Span
  }
  type SpanSetter[T <: EntityPit] = (OffsetDateTime, T) => T
  case class SpanScope[T <: EntityPit](init: SpanSetter[T], conclude: SpanSetter[T])

  trait TablePit[T <: EntityPit] { self: Table[T] =>
    def span: Rep[Span]
  }

  trait RepositoryPit extends RepositoryId {

    type T <: EntityIdLike with EntityPit
    type E <: Table[T] with TableId[T] with TablePit[T]

    type QueryType = Query[E, T, Seq]

    def spanScope: SpanScope[T]

    // FIXME: compile this. Also - is there a way to make this a view?
    @inline def asOfNowQuery: QueryType = asOfQuery(now())
    def asOfNow: DBIO[Seq[T]] = asOfNowQuery.result

    def asOfQuery(ts: OffsetDateTime): QueryType = rows filter { _.span @>^ ts }

    def asOf(ts: OffsetDateTime): DBIO[Seq[T]] = asOfQuery(ts).result

    def updated(id: ID, ts: OffsetDateTime = now())(f: T => T): DBIO[ID] = {

      import scala.concurrent.ExecutionContext.Implicits.global

      def update(t: T) = t.span match {
        case PgRange(Some(_), None, _) => findByIdQuery(id) update (spanScope.conclude(ts, t))
        case range => DBIO.failed(new IllegalStateException(s"""already expired: $range"""))
      }

      val insertAction = for {
        t <- findById(id)
        n <- update(t) if n == 1
        nid <- rows returning rows.map(_.id) += f(spanScope.init(ts, t))
      } yield nid

      insertAction.transactionally
    }
  }
  abstract class PassiveAggressiveRecord[R <: Repository](val repo: R) {
    def entity: repo.T
    final def insert(): DBIO[repo.T] = repo insert entity

  }

}
