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
import scala.language.{postfixOps, higherKinds}
import java.time.OffsetDateTime
import com.github.tminglei.slickpg.{ Range => PgRange, `(_,_)` }

/**
 * Not intended to be actually thrown, but to communicate error messages within Try or DBIO.
 */
final case class ValidationEx(msg: String) extends scala.util.control.NoStackTrace

/*
* Type safe primary key classes for Int and Long
*/
final case class Id[T, V <: AnyVal](val value: V) extends AnyVal with slick.lifted.MappedTo[V]
object Id {
  implicit def ordering[A, V <: AnyVal: Ordering] = Ordering.by[Id[A, V], V]((id: Id[A, V]) => id.value)
}
trait IdCompanion[T, V <: AnyVal] {
  def apply(value: V) = Id.apply[T, V](value)
  def unapply(id: Id[T, V]) = Id.unapply[T, V](id)
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

trait RepositoryLike {
  type TT
  type EE <: Table[TT]
  type Get[A] = EE => Rep[A]
  final type QueryType = Query[EE, TT, Seq]
  def rows: TableQuery[EE]
}
  /**
   *  Base trait for all repositories. The abstract type members follow the naming convention used
   *  by Slick.
   */
  abstract class Repository[T, E <: Table[T]](tq: => TableQuery[E]) extends RepositoryLike {

    type TT = T
    type EE = E

    final override val rows: TableQuery[E] = tq

    def findByQuery[A: ColumnType](getA: Get[A], a: A): QueryType = rows filter (getA(_) === a)
    def findBy[A: ColumnType](get: Get[A], a: A): StreamingReadAction[T] = findByQuery(get, a).result
    def insert(t: T): DBIO[T] = rows returning rows += t
    def deleteBy[A: ColumnType](get: Get[A], a: A): DBIO[Int] = findByQuery(get, a).delete
    def size: DBIO[Int] = rows.size.result
    def stream(fetchSize: Int = 100)(implicit exc: ExecutionContext): StreamingReadDBIO[T] = {
      rows.result.withStatementParameters(fetchSize = fetchSize) // widens result type
    }
  }

  trait EntityPkLike {
    type PK
    type EPK // as expressed by the entity
    def _pk: EPK
  }
  trait TablePkLike[T <: EntityPkLike] {
    type RPK
    def _pk: RPK
  }
  trait RepositoryPkLike[T <: EntityPkLike, E <: Table[T] with TablePkLike[T]] extends Repository[T, E] {
    type PK = T#PK // convenience
    type EPK = T#EPK
    type RPK = E#RPK
    type GetPK = E => RPK

    type RIAC = profile.ReturningInsertActionComposer[T, EPK]
    def returningPkQuery: RIAC

    protected val getPk: GetPK = e => e._pk

    def findQuery(pk: PK)(implicit ev: ColumnType[PK]): Query[E, T, Seq]
    def find(pk: PK)(implicit ev: ColumnType[PK]): DBIO[T] = findQuery(pk).result.head
    def maybeFind(pk: PK)(implicit ev: ColumnType[PK]): DBIO[Option[T]] =
      findQuery(pk).result.headOption

    def findQuery(t: T)(implicit ev: ColumnType[PK]): Query[E, T, Seq]
    def find(t: T)(implicit ev: ColumnType[PK]): DBIO[T] = findQuery(t).result.head
    def maybeFind(t: T)(implicit ev: ColumnType[PK]): DBIO[Option[T]] =
      findQuery(t).result.headOption


    def update(t: T)(implicit exc: ExecutionContext, ev: ColumnType[PK]): DBIO[T] =
      findQuery(t).update(t) flatMap { _ match {
        case 1 => DBIO.successful(t)
        case n => DBIO.failed(ValidationEx(s"update of $t affected $n rows"))
      }
    }

    def upsert(t: T)(implicit exc: ExecutionContext): DBIO[T] =
      rows.insertOrUpdate(t) flatMap { _ match {
        case 1 => DBIO.successful(t)
        case n => DBIO.failed(ValidationEx(s"upsert of $t affected $n rows"))
      }
    }
  }

  trait EntityPk extends EntityPkLike {
    type EPK = PK
  }

  trait TablePk[T <: EntityPk] extends TablePkLike[T] {
    type RPK = Rep[T#PK]
  }

  trait RepositoryPk[T <: EntityPk, E <: Table[T] with TablePk[T]] extends RepositoryPkLike[T, E] {

    def findQuery(pk: PK)(implicit ev: ColumnType[PK]): Query[E, T, Seq] = findByQuery(getPk, pk)

    def findQuery(t: T)(implicit ev: ColumnType[PK]): Query[E, T, Seq] = findByQuery(getPk, t._pk)

    def returningPkQuery(implicit ev: ColumnType[PK]): RIAC = rows returning rows.map(_._pk)

  }

  trait EntityPk2 extends EntityPkLike {
    type PK_1
    type PK_2
    type PK = (PK_1, PK_2)
    type EPK = PK
  }

  trait TablePk2[T <: EntityPk2] extends TablePkLike[T] {
    type RPK = (Rep[T#PK_1], Rep[T#PK_2])
  }

  trait RepositoryPk2[T <: EntityPk2, E <: Table[T] with TablePk2[T]] extends RepositoryPkLike[T, E]{

    type PK_1 = T#PK_1
    type PK_2 = T#PK_2

    def findQuery(pk_1: PK_1, pk_2: PK_2)(implicit ev1: ColumnType[PK_1], ev2: ColumnType[PK_2]): QueryType =
      rows filter { e => e._pk._1 === pk_1 && e._pk._2 === pk_2 }

    override def findQuery(pk: PK)(implicit ev1: ColumnType[PK_1], ev2: ColumnType[PK_2]): QueryType =
      findQuery(pk._1, pk._2)

    override def findQuery(t: T)(implicit ev1: ColumnType[PK_1], ev2: ColumnType[PK_2]): QueryType =      findQuery(t._pk._1, t._pk._2)

    // def returningPkQuery(implicit ev1: ColumnType[PK_1], ev2: ColumnType[PK_2]): RIAC =
    //   rows returning rows.map(t => (t._pk._1, t._pk._2))

  }

  trait EntityId extends EntityPkLike {
    type V <: AnyVal
    type T <: EntityId
    type PK = Id[T, V]
    type EPK = Option[PK]
    def id: EPK
  }

  trait TableId[T <: EntityId] extends TablePkLike[T] { self: Table[T] =>
    type RPK = Rep[T#PK]
    def id: RPK
  }

  trait RepositoryId[T <: EntityId, E <: Table[T] with TableId[T]] extends RepositoryPkLike[T, E] {
    // override type PK = T#PK

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
  trait EntityPit {
    def span: Span
  }
  type SpanSetter[T <: EntityPit] = (OffsetDateTime, T) => T
  case class SpanScope[T <: EntityPit](init: SpanSetter[T], conclude: SpanSetter[T])

  trait TablePit[T <: EntityPkLike with EntityPit] { self: Table[T] with TablePkLike[T] =>
    def span: Rep[Span]
  }

  trait RepositoryPit[T <: EntityPkLike with EntityPit, E <: Table[T] with TablePkLike[T] with TablePit[T]] extends RepositoryPkLike[T, E]{

    def spanScope: SpanScope[T]

    // TODO: compile this. Also - is there a way to make this a view?
    @inline def asOfNowQuery: QueryType = asOfQuery(now)
    def asOfNow: DBIO[Seq[T]] = asOfNowQuery.result

    def asOfQuery(ts: OffsetDateTime): QueryType = rows filter { _.span @>^ ts }

    def asOf(ts: OffsetDateTime): DBIO[Seq[T]] = asOfQuery(ts).result

    def updated(id: PK, ts: OffsetDateTime = now)(f: T => T)(implicit ev: ColumnType[PK]): DBIO[EPK] = {

      import scala.concurrent.ExecutionContext.Implicits.global

      def update(t: T) = t.span match {
        case PgRange(Some(_), None, _) => findQuery(id) update (spanScope.conclude(ts, t))
        case range                     => DBIO.failed(new IllegalStateException(s"""already expired: $range"""))
      }

      val insertAction = for {
        t <- find(id)
        n <- update(t) if n == 1
        nid <- returningPkQuery += f(spanScope.init(ts, t))
      } yield nid

      insertAction.transactionally
    }
    private def now = OffsetDateTime.now
  }
  abstract class PassiveAggressiveRecord[T, E <: Table[T], R <: Repository[T, E]](val repo: R) {
    def entity: T

    // final def insert(): DBIO[repo.TT] = repo insert entity
    // final def delete(): DBIO[Boolean] = ???
  }

  trait RepositorySchema { self: RepositoryLike =>
    final def schema: SchemaDescription = rows.schema
    final def create(): DBIO[Unit] = rows.schema.create
    final def drop(): DBIO[Unit] = rows.schema.drop
  }

}
