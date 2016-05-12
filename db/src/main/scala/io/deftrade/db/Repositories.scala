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

import java.time.OffsetDateTime
import scala.concurrent.ExecutionContext
import scala.language.postfixOps
import java.time.OffsetDateTime
import com.github.tminglei.slickpg.{ Range => PgRange, `(_,_)` }

/*
* Type safe primary key classes for Int and Long
*/
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
 * + - PasAggRecId, PasAggRecIdPit, etc
 */
trait Repositories {

  val profile: DefTradePgDriver // must retain this driver
  import profile.SchemaDescription
  import profile.api._

  type StreamingReadDBIO[T] = DBIOAction[Seq[T], Streaming[T], Effect.Read]
  type StreamingReadAction[T] = profile.StreamingDriverAction[Seq[T], T, Effect.Read]

  def compiledComment[T](implicit ev: StreamingReadAction[T] <:< StreamingReadDBIO[T]) = ()

  trait EntityPkLike {
    type T
    type PK
  }

  trait EntityPk[T_, PK_] extends EntityPkLike {
    type T = T_
    type PK = PK_
  }

  trait TablePk[T <: EntityPkLike] { self: Table[T] =>
    type PK = T#PK
    def _pk: Rep[PK]
  }

  trait EntityIdLike extends EntityPkLike {
    type V <: AnyVal
    type T
    type PK = Id[T, V]
    def id: Option[PK]
  }

  trait EntityId[T_, V_ <: AnyVal] extends EntityIdLike { type T = T_; type V = V_ }

  trait TableId[T <: EntityIdLike] extends TablePk[T] { self: Table[T] =>
    type V = T#V
    def id: Rep[PK]
    final def _pk = id
  }

  trait RepositorySchema { self: Repository =>
    final def schema: SchemaDescription = rows.schema
    final def create(): DBIO[Unit] = rows.schema.create
    final def drop(): DBIO[Unit] = rows.schema.drop
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

    type Get[A] = E => Rep[A]
    def rows: TableQuery[E]

    final def apply: StreamingReadAction[T] = rows.result
    final def findByQuery[A: ColumnType](getA: E => Rep[A], a: A): Query[E, T, Seq] = rows filter { getA(_) === a }
    final def findBy[A: ColumnType](get: Get[A], a: A): StreamingReadAction[T] = findByQuery(get, a).result
    final def insert(t: T): DBIO[T] = rows returning rows += t
    final def delete(): DBIO[Int] = rows.delete
    final def deleteBy[A: ColumnType](get: Get[A], a: A): DBIO[Int] = findByQuery(get, a).delete
    final def size: DBIO[Int] = rows.size.result
    final def stream(fetchSize: Int = 100)(implicit exc: ExecutionContext): StreamingReadDBIO[T] = {
      rows.result.withStatementParameters(fetchSize = fetchSize) // widens result type
    }
  }

  trait RepositoryPk extends Repository {
    type T <: EntityPkLike
    type E <: Table[T] with TablePk[T]
    type PK = T#PK

    // implicit def pkColumnType: ColumnType[PK]

    protected val getPk: Get[PK] = e => e._pk

    def findQuery(pk: PK)(implicit ev: ColumnType[PK]): Query[E, T, Seq] = findByQuery(getPk, pk)
    def find(pk: PK)(implicit ev: ColumnType[PK]): DBIO[T] = findBy(getPk, pk).head
    def maybeFind(pk: PK)(implicit ev: ColumnType[PK]): DBIO[Option[T]] = findBy(_._pk, pk).headOption

    final def insertReturnId(t: T)(implicit ev: ColumnType[PK]): DBIO[PK] = rows returning (rows map (_._pk)) += t
  }

  trait RepositoryId extends RepositoryPk {
    type T <: EntityIdLike
    type E <: Table[T] with TableId[T]
    override type PK = T#PK

    override protected val getPk: Get[PK] = e => e.id

    /**
     * Returns the most recent version of an element indexed by a column of type `A`.
     * This will typically be the natural (domain) key for the entity (as opposed to the
     * surrogate key `id`).
     * Relies on the fact that the `id` index is always increasing.
     */
    final def findCurrentBy[A](getA: E => Rep[A], a: A)(
      implicit
      evA: ColumnType[A], evPK: ColumnType[PK]
    ): DBIO[Option[T]] = {

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

  type Span = PgRange[OffsetDateTime]
  object Span {
    lazy val empty: Span = PgRange.apply(None, None, `(_,_)`)
  }
  trait EntityPit extends EntityIdLike {
    def span: Span
  }
  type SpanSetter[T <: EntityPit] = (OffsetDateTime, T) => T
  case class SpanScope[T <: EntityPit](init: SpanSetter[T], conclude: SpanSetter[T])

  trait TablePit[T <: EntityPit] extends TableId[T] { self: Table[T] =>
    def span: Rep[Span]
  }

  trait RepositoryPit extends RepositoryId {

    type T <: EntityPit
    type E <: Table[T] with TablePit[T]

    type QueryType = Query[E, T, Seq]

    def spanScope: SpanScope[T]

    // TODO: compile this. Also - is there a way to make this a view?
    @inline def asOfNowQuery: QueryType = asOfQuery(now)
    def asOfNow: DBIO[Seq[T]] = asOfNowQuery.result

    def asOfQuery(ts: OffsetDateTime): QueryType = rows filter { _.span @>^ ts }

    def asOf(ts: OffsetDateTime): DBIO[Seq[T]] = asOfQuery(ts).result

    def updated(id: PK, ts: OffsetDateTime = now)(f: T => T)(implicit ev: ColumnType[PK]): DBIO[PK] = {

      import scala.concurrent.ExecutionContext.Implicits.global

      def update(t: T) = t.span match {
        case PgRange(Some(_), None, _) => findQuery(id) update (spanScope.conclude(ts, t))
        case range                     => DBIO.failed(new IllegalStateException(s"""already expired: $range"""))
      }

      val insertAction = for {
        t <- find(id)
        n <- update(t) if n == 1
        nid <- rows returning rows.map(_.id) += f(spanScope.init(ts, t))
      } yield nid

      insertAction.transactionally
    }
    private def now = OffsetDateTime.now
  }
  abstract class PassiveAggressiveRecord[R <: Repository](val repo: R) {
    def entity: repo.T
    final def insert(): DBIO[repo.T] = repo insert entity
    final def delete(): DBIO[Int] = repo delete ()
  }

}
