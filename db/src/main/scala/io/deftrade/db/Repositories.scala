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
// import scala.language.{postfixOps}
import java.time.OffsetDateTime
import com.github.tminglei.slickpg.{ Range => PgRange, `(_,_)` }

/**
 * Not intended to be actually thrown, but to communicate error messages within Try or DBIO.
 */
final case class ValidationEx(msg: String) extends scala.util.control.NoStackTrace {
  override def getMessage(): String = s"$productPrefix: $msg"
}

/*
* Type safe primary key classes for Int and Long.
* FIXME: making this a value class exposes a compiler bug?!?!
*/
// final case class Id[T, V <: AnyVal](value: V) extends AnyVal with slick.lifted.MappedTo[V]
final case class Id[T, V <: AnyVal](val value: V) // extends AnyVal
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
 * - if there is a RepoEid as above, and table has a column 'ts' of type timestamptz,
 *   then RepoPit trait is stacked to provide PointInTime methods.
 *
 * === TODO: ===
 * - if there is a pk 'id' as above, and also an eid col identified by an index,
 *   then also stack a RepoEid trait which identifies the latest (highest id)
 *   (also make a view?)
 * - optimistic locking
 * - pre-insert and post-insert hooks - issue - how to get these picked up by code gen?
 * - junction repo stuff
 * - save multiple entities
 * - final val tableName = query.baseTableRow.tableName useful for exceptions
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

  implicit def idIntColumnType[T] = MappedColumnType.base[Id[T, Int], Int](
    { _.value }, { Id[T, Int](_) }
  )

  implicit def idLongColumnType[T] = MappedColumnType.base[Id[T, Long], Long](
    { _.value }, { Id[T, Long](_) }
  )

  trait RepositoryLike {
    type TT
    type EE <: Table[TT]
    type Get[A] = EE => Rep[A]
    final type QueryType = Query[EE, TT, Seq]
    def rows: TableQuery[EE]

    protected def validateSingleRowAffected(n: Int, errMsg: String)(implicit ec: ExecutionContext): DBIO[Unit] =
      n match {
        case 1 => DBIO.successful(())
        case _ => DBIO.failed(ValidationEx(s"$errMsg affected $n rows"))
      }
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
    def insert(t: T)(implicit exc: ExecutionContext): DBIO[T] = rows returning rows += t
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

  trait RepositoryPkLike[T <: EntityPkLike, E <: Table[T] with TablePkLike[T]] extends Repository[T, E] { rpkl =>
    type PK = T#PK // convenience
    type EPK = T#EPK
    type RPK = E#RPK
    type GetPK = E => RPK
    protected val getPk: GetPK = e => e._pk

    protected type EqPk = (RPK, RPK) => Rep[Boolean]
    protected def eqPk: EqPk

    // val orderIdToOrderPk[Order, OrdersItems](ef): Order#RPK = ef._pk
    // 'p' := primary, 'f' := foreign
    final protected def xpkQuery[
        TF <: EntityPkLike,
        EF <: Table[TF] with TablePkLike[TF],
        RF <: RepositoryPkLike[TF, EF]](rf: RF)(implicit xpk: EF => RPK): T => rf.QueryType =
      t => {
        findQuery(t) flatMap {
          ep => rf expatPk { ef => eqPk(ep._pk, xpk(ef)) }
        }
      }

    private def expatPk(pred: E => Rep[Boolean]): QueryType = rows filter { ef => pred(ef) }

    private def expatPkJ[
        TJ <: EntityPkLike,
        EJ <: Table[TJ] with TablePkLike[TJ],
        RJ <: RepositoryPkLike[TJ, EJ]](rj: RJ)(q: QueryType)(implicit ypk: E => EJ#RPK): rj.QueryType =
      q flatMap { ef => rj.rows filter { ej => rj.eqPk(ef |> ypk, ej._pk) }
    }

    def findQuery(pk: PK): Query[E, T, Seq]
    def find(pk: PK): DBIO[T] = findQuery(pk).result.head
    def maybeFind(pk: PK): DBIO[Option[T]] =
      findQuery(pk).result.headOption

    def findQuery(t: T): Query[E, T, Seq]
    def find(t: T): DBIO[T] = findQuery(t).result.head
    def maybeFind(t: T): DBIO[Option[T]] = findQuery(t).result.headOption

    def update(t: T)(implicit exc: ExecutionContext): DBIO[T] =
      for {
        n <- findQuery(t).update(t)
        _ <- validateSingleRowAffected(n, errMsg = s"updating $t")
      } yield t

    def upsert(t: T)(implicit exc: ExecutionContext): DBIO[T] =
      for {
        n <- rows.insertOrUpdate(t)
        _ <- validateSingleRowAffected(n, errMsg = s"upserting $t")
      } yield t

    def delete(t: T)(implicit exc: ExecutionContext): DBIO[Boolean] = {
      DBIO.failed(new UnsupportedOperationException("not yet implemented"))
    }
  }

  trait EntityPk extends EntityPkLike {
    type EPK = PK
  }

  trait TablePk[T <: EntityPk] extends TablePkLike[T] {
    type RPK = Rep[T#PK]
  }

  abstract class RepositoryPk[T <: EntityPk, E <: Table[T] with TablePk[T]](tq: => TableQuery[E])(implicit ctpk: ColumnType[T#PK]) extends Repository[T, E](tq) { pkl: RepositoryPkLike[T, E] =>

    override protected def eqPk: EqPk = (rpk1, rpk2) => rpk1 === rpk2

    override def findQuery(pk: PK): Query[E, T, Seq] = findByQuery(getPk, pk)

    override def findQuery(t: T): Query[E, T, Seq] = findByQuery(getPk, t._pk)

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

  abstract class RepositoryPk2[T <: EntityPk2, E <: Table[T] with TablePk2[T]](tq: => TableQuery[E])(implicit ctpk1: ColumnType[T#PK_1], ctpk2: ColumnType[T#PK_2]) extends Repository[T, E](tq) { pkl: RepositoryPkLike[T, E] =>

    type PK_1 = T#PK_1
    type PK_2 = T#PK_2

    override protected def eqPk: EqPk = (rpk1, rpk2) => rpk1._1 === rpk2._1 && rpk1._2 === rpk2._2

    def findQuery(pk_1: PK_1, pk_2: PK_2): QueryType = rows filter { e =>
      e._pk._1 === pk_1 && e._pk._2 === pk_2
    }

    override def findQuery(pk: PK): QueryType = findQuery(pk._1, pk._2)

    override def findQuery(t: T): QueryType = findQuery(t._pk._1, t._pk._2)

  }

  trait RepositoryJunction[T <: EntityPk2, E <: Table[T] with TablePk2[T]] extends RepositoryPkLike[T, E] { pk2: RepositoryPk2[T, E] =>
    // type T1 <: EntityPk
    // type E1 <: Table[T1] with TablePk[T1]
    // type R1 <: RepositoryLike
    // def R1: R1
    // type Q1 = Query[E1, T1, Seq]
    // def Q1: Q1
    // type T2 <: EntityPk
    // type E2 <: Table[T2] with TablePk[T2]
    // type Q2 = Query[E2, T2, Seq]
    // protected final def link_1(q1: Q2): Q1 = ???
    // protected final def link_2(q2: Q1): Q2 = ???
  }

  trait EntityId extends EntityPkLike {
    type V <: AnyVal
    type T <: EntityId
    // type PK = Id[T, V]
    type PK <: Id[T, V]
    type EPK = Option[PK]
    def id: EPK
    override def _pk: EPK = id
  }

  trait TableId[T <: EntityId] extends TablePkLike[T] { self: Table[T] =>
    type RPK = Rep[T#PK]
    def id: RPK
    override def _pk: RPK = id
  }

  abstract class RepositoryId[T <: EntityId, E <: Table[T] with TableId[T]](tq: => TableQuery[E])(implicit ctpk: ColumnType[T#PK]) extends Repository[T, E](tq) { pkl: RepositoryPkLike[T, E] =>

    override protected def eqPk: EqPk = (rpk1, rpk2) => rpk1 === rpk2

    type RIAC = profile.ReturningInsertActionComposer[T, PK]
    def returningPkQuery: RIAC = rows returning rows.map(_.id)

    // override def findQuery(pk: PK): Query[E, T, Seq] = rows filter (_.id === pk)
    override def findQuery(pk: PK): Query[E, T, Seq] = findByQuery(getPk, pk)

    override def findQuery(t: T): Query[E, T, Seq] = t.id match {
      case None => rows filter (_ => (false: Rep[Boolean]))
      case Some(pk) => findByQuery(getPk, pk)
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
  case class SpanLens[T <: EntityPit](init: SpanSetter[T], conclude: SpanSetter[T])

  trait TablePit[T <: EntityId with EntityPit] { self: Table[T] with TableId[T] =>
    def span: Rep[Span]
  }

  trait RepositoryPit[T <: EntityId with EntityPit, E <: Table[T] with TableId[T] with TablePit[T]] extends RepositoryPkLike[T, E] { this: RepositoryId[T, E] =>

    def spanLens: SpanLens[T]

    // TODO: compile this. Also - is there a way to make this a view?
    @inline def asOfNowQuery: QueryType = asOfQuery(now)
    def asOfNow: DBIO[Seq[T]] = asOfNowQuery.result

    def asOfQuery(ts: OffsetDateTime): QueryType = rows filter { _.span @>^ ts }

    def asOf(ts: OffsetDateTime): DBIO[Seq[T]] = asOfQuery(ts).result

    override def insert(t: T)(implicit exc: ExecutionContext): DBIO[T] = {
      val ts = now
      for {
        _ <- validateIsCurrent(t.span)
        tt <- super.insert(spanLens.init(ts, t))
      } yield tt
    }

    override def update(t: T)(implicit exc: ExecutionContext): DBIO[T] = {
      val ts = now
      val action = for {
        _ <- validateIsCurrent(t.span)
        _ <- super.update(spanLens.conclude(ts, t))
        tt <- rows returning rows += spanLens.init(ts, t)
      } yield tt

      action.transactionally
    }
    override def upsert(t: T)(implicit exc: ExecutionContext): DBIO[T] = {
      val ts = now
      val updateAction = t.id match {
        case Some(_) => super.update(spanLens.conclude(ts, t))
        case None => DBIO.successful(t)
      }
      val action = for {
        _ <- validateIsCurrent(t.span)
        _ <- updateAction
        tt <- rows returning rows += spanLens.init(ts, t)
      } yield tt

      action.transactionally
    }

    private def now = OffsetDateTime.now

    private def validateIsCurrent(span: Span): DBIO[Unit] = span match {
      case PgRange(_, None, _) => DBIO.successful(())
      case _ => DBIO.failed(ValidationEx(s"""already expired: $span"""))
    }
  }
  abstract class PassiveAggressiveRecord[T, E <: Table[T], R <: Repository[T, E]](val repo: R)(implicit ec: ExecutionContext) {

    def entity: T

    final def insert: DBIO[T] = repo insert entity
  }

  abstract class PassiveAggressiveRecordPk[T <: EntityPkLike, E <: Table[T] with TablePkLike[T], R <: Repository[T, E] with RepositoryPkLike[T, E]](override val repo: R)(implicit ec: ExecutionContext)  extends PassiveAggressiveRecord[T, E, R](repo)(ec){

    final def save: DBIO[T] = repo upsert entity
    final def delete: DBIO[Boolean] = repo delete entity
  }

  // attach methods to access other records via 1->1 links (a foreign key) or 1->n links (junction table - where we take the name for the link from).
  // this will need some per-repo refinements
  // possible to use this implicit class to carry hook method for pre-insert validation?

  trait RepositorySchema { self: RepositoryLike =>
    final def schema: SchemaDescription = rows.schema
    final def create(): DBIO[Unit] = rows.schema.create
    final def drop(): DBIO[Unit] = rows.schema.drop
  }

}

// object api {
//   trait Foo[A] { def foo(i: Int): A }
//   implicit val fooi: Foo[Int] = new Foo[Int] { override def foo(i: Int): Int = i }
//   implicit def footi[T](implicit i2t: Int => T): Foo[T] = new Foo[T] {
//     override def foo(i: Int): T = i2t(i)
//   }
// }
//
// trait Bases {
//   import api._
//
//   trait Entity {
//     type T
//     def t: T
//   }
//
//   abstract class Base[E <: Entity](i: Int)(implicit ft: Foo[E#T]) {
//     def et: E#T  = ft.foo(i)
//   }
// }
//
// case class IntVal(val value: Int) extends AnyVal
//
// trait Clients extends Bases {
//   import api._
//   implicit val i2iv: Int => IntVal = i => IntVal(i)
//
//   case class Ecc(t: IntVal) extends Entity { type T = IntVal }
//
//   trait Client { self: Base[Ecc] =>
//     val ecc = Ecc(et)
//     def print() = println(s"client: $ecc")
//   }
//   def print() = (new Base[Ecc](33) with Client {} ).print()
// }
// object Clients extends Clients
