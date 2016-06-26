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

import java.time._
import java.time.temporal._
import java.time.ZoneOffset.UTC

import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration.{ Duration => ScDuration }
import scala.language.postfixOps

import org.scalactic._
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers, matchers }
import matchers.{ Matcher, MatchResult }

import slick.backend.DatabasePublisher
import com.github.tminglei.slickpg.{ Range => PgRange }

import io.deftrade.db.test.Tables._

object EquivalenceImplicits extends TupleEquvalenceImplicits with TypeCheckedTripleEquals {

  implicit val offsetDateTimeEquivalence = new Equivalence[OffsetDateTime] {
    override def areEquivalent(a: OffsetDateTime, b: OffsetDateTime): Boolean = a isEqual b
  }
  implicit def optionEquivalence[A](implicit ev: Equivalence[A]) = new Equivalence[Option[A]] {
    override def areEquivalent(a: Option[A], b: Option[A]) = (a, b) match {
      case (Some(l), Some(r)) => l === r
      case (None, None) => true
      case _ => false
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
  import java.time._
  def now = OffsetDateTime.now()
  val yearStart = OffsetDateTime.parse("2016-01-01T00:00:00+00:00")
  val ytd: Span = PgRange(yearStart, yearStart).copy(end = None) // WAT
  def fromNow: Span = PgRange(now, now).copy(end = None)
}

trait PgSpec extends FlatSpec with Matchers with TypeCheckedTripleEquals with BeforeAndAfterAll {
  import test.Tables.profile
  import profile.api._

  lazy val db = Database.forConfig("postgres")

  def matchResult[T <: Temporal: Ordering](interval: (T, T)) =
    (left: T) =>
      MatchResult(
        left within interval,
        s"$left not within $interval",
        s"$left is within $interval"
      )

  def beWithin[T <: Temporal: Ordering: Manifest](interval: (T, T)): Matcher[T] =
    Matcher { matchResult(interval) }

  def beDefinedAndWithin[T <: Temporal: Ordering: Manifest](interval: (T, T)): Matcher[Option[T]] =
    Matcher {
      _ match {
        case Some(odt) => { val fn = matchResult(interval); fn(odt) }
        case None => MatchResult(false, "undefined", "undefined")
      }
    }

  override protected def afterAll(): Unit = {
    db.close()
  }

  implicit val globalTimeout: ScDuration = ScDuration.Inf

  def exec[T](action: DBIO[T])(implicit ex: ExecutionContext, timeout: ScDuration): T = {
    Await.result(db.run(action), timeout)
  }
  def execTransactionally[T](action: DBIO[T])(implicit ex: ExecutionContext, timeout: ScDuration): T = {
    Await.result(db.run(action.transactionally), timeout)
  }

  /**
   * Execute the `action` transactionally and return the value, but roll back the transaction.
   * - if `action` succeeds, return the result and rollback
   * - if `action` fails, throw the exception
   */
  def execAndRollback[T](action: DBIO[T])(implicit ex: ExecutionContext, timeout: ScDuration): T = {

    case class ResultEscapePod(t: T) extends Throwable

    val actionWhichWillFailAndRollBack = for {
      t <- action
      _ <- DBIO failed ResultEscapePod(t)
    } yield t

    val future = db run actionWhichWillFailAndRollBack.transactionally recoverWith {
      case ResultEscapePod(t) => Future successful t
      case e => Future failed e
    }

    Await.result(future, timeout)
  }

  def stream[T](action: StreamingDBIO[T, T]): DatabasePublisher[T] = ???

}

class RepoIdSpec extends PgSpec {
  import com.github.tminglei.slickpg.JsonString
  import Misc._
  import ExecutionContext.Implicits.global
  import test.Tables.profile
  import profile.api._
  import EquivalenceImplicits._

  "implicit tuple equivalence" should "work" in {
    val odt0 = now
    val odt1 = odt0.withOffsetSameInstant(UTC)
    val s = "foo"
    (s, odt0) should ===((s, odt1))
  }

  val user0 = User(userName = "Binky", signup = yearStart)

  behavior of "the id repo"

  it should "insert a record" in {
    val user1exp = user0.copy(id = Some(UserId(1)))
    val user1 = exec { Users insert user0 }
    // val user1 = execAndRollback { Users insert user0 }
    // val user1again = execTransactionally { Users insert user0 }
    user1.id should ===(Some(UserId(1)))
    user1.copy(span = Span.empty) should ===(user1exp)
    user1.span.start should beDefinedAndWithin(now /- 1.second)
    user1.span.end shouldBe empty
    val maybeUser2 = exec { Users maybeFind UserId(2) }
    maybeUser2 shouldBe empty
  }

  it should "find an existing record by id" in {
    val user1b = execTransactionally { Users find UserId(1) }
    user1b.id should ===(Some(UserId(1)))
    user1b.span.start should beDefinedAndWithin(now /- 1.second)
  }

  def catify(user: User) = user.copy(meta = JsonString("""{
    |  "greeting": "oh hai",
    |  "nCats": 117
    |}""".stripMargin))

  it should "update existing records with expire / insert" in exec {
    for {
      user1 <- Users find UserId(1)
      user1new = user1 |> catify
      user2 <- Users update user1new
      user1old <- Users find UserId(1)
    } yield {
      user1.id should ===(Some(UserId(1)))
      user2.id should ===(Some(UserId(2)))
      val updateTs = user2.span.start
      updateTs shouldBe defined
      user1old.span.end should ===(updateTs)
    }
  }
  it should "upsert new records as if by insert" in exec {
    for {
      user3 <- Users upsert user0.copy(userName = "Sheeba")
    } yield {
      user3.id should ===(Some(UserId(3)))
      user3.span.start should beDefinedAndWithin(now /- 1.second)
    }
  }

  it should "delete all records" in {
    val action = for {
      size <- Users.size
      n <- Users.rows.delete
    } yield n should ===(size)
    exec(action)
  }
}
