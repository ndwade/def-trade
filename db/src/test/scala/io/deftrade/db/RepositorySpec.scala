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

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.{ Duration, DurationInt }
import scala.language.postfixOps

import org.scalactic._
import org.scalatest.{ BeforeAndAfterEach, FlatSpec, Matchers }

import slick.backend.DatabasePublisher
import com.github.tminglei.slickpg.{ Range => PgRange }

import test.Tables.{ Span, User, UserPasAggRec, Users }

object EquivalenceImplicits extends TupleEquvalenceImplicits with TypeCheckedTripleEquals {

  implicit val offsetDateTimeEquivalence = new Equivalence[OffsetDateTime] {
    override def areEquivalent(a: OffsetDateTime, b: OffsetDateTime): Boolean = a isEqual b
  }
  implicit def optionEquivalence[A](implicit ev: Equivalence[A]) = new Equivalence[Option[A]] {
    override def areEquivalent(a: Option[A], b: Option[A]) = (a, b) match {
      case (Some(l), Some(r)) => l === r
      case (None, None)       => true
      case _                  => false
    }
  }
  implicit def pgRangeEquivalence[A: Equivalence] = new Equivalence[PgRange[A]] {
    override def areEquivalent(a: PgRange[A], b: PgRange[A]) =
      PgRange.unapply(a) === PgRange.unapply(b)
  }
  implicit val userEquivalence = new Equivalence[User] {
    override def areEquivalent(a: User, b: User) =
      User.unapply(a) === User.unapply(b)
  }
}

object Misc {
  def now = OffsetDateTime.now()
  val yearStart = OffsetDateTime.parse("2016-01-01T00:00:00+00:00")
  val ytd: Span = PgRange(yearStart, yearStart).copy(end = None) // WAT
  def fromNow: Span = PgRange(now, now).copy(end = None)
}

trait PgSpec extends FlatSpec with Matchers with TypeCheckedTripleEquals with BeforeAndAfterEach {
  import test.Tables.profile
  import profile.api._

  lazy val db = Database.forConfig("postgres")

  implicit val globalTimeout = 2 seconds

  def exec[T](action: DBIO[T])(implicit ex: ExecutionContext, timeout: Duration): T = {
    Await.result(db.run(action), timeout)
  }
  def execTransactionally[T](action: DBIO[T])(implicit ex: ExecutionContext, timeout: Duration): T = {
    Await.result(db.run(action.transactionally), timeout)
  }

  /**
   * Execute the `action` transactionally and return the value, but roll back the transaction.
   * - if `action` succeeds, return the result and rollback
   * - if `action` fails, throw the exception
   */
  def execAndRollback[T](action: DBIO[T])(implicit ex: ExecutionContext, timeout: Duration): T = {

    case class ResultEscapePod(t: T) extends Throwable

    val actionWhichWillFailAndRollBack = for {
      t <- action
      _ <- DBIO failed ResultEscapePod(t)
    } yield t

    val future = db run actionWhichWillFailAndRollBack.transactionally recoverWith {
      case ResultEscapePod(t) => Future successful t
      case e                  => Future failed e
    }
    Await.result(future, timeout)
  }

  def stream[T](action: StreamingDBIO[T, T]): DatabasePublisher[T] = ???

}

class RepoSpec extends PgSpec {
  import Misc._
  import ExecutionContext.Implicits.global
  import test.Tables.profile
  import profile.api._
  import EquivalenceImplicits._

  val user0 = User(
    userName = "Binky",
    signup = yearStart,
    btcAddr = "1GaAWoGCMTnmXH4o8ciNTedshgsdwX2Get"
  )
  val user1exp = user0.copy(id = Some(Id[User, Long](1L)))

  behavior of "the base repo"

  it should "insert a record" in {
    val user1 = exec {
      user0.insert() // Users.insert(user0)
    }
    user1.id should ===(Some(Id[User, Long](1L)))
    user1 should ===(user1exp)
  }

  it should "find an existing record by id" in {
    val user1 = exec { Users.find(Id[User, Long](1L)) }
    user1.id should ===(Some(Id[User, Long](1L)))
    user1 should ===(user1exp)
  }

  it should "delete all records" in {
    val nsize = exec { Users.size }
    val ngone = exec { Users.delete() }
    ngone should ===(nsize)
  }
  "my stupid implicit tuple idea" should "work" in {
    val odt0 = now
    val odt1 = odt0.withOffsetSameInstant(java.time.ZoneOffset.UTC)
    val s = "foo"
    (s, odt0) should ===((s, odt1))
  }
}
