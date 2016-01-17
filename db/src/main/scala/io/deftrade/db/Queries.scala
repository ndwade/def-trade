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

import DefTradePgDriver.api._
import scala.language.postfixOps
import java.time._
import Tables._

// TODO: annotate all PIT tables with this trait via the SourceCodeGenerator
trait PointInTime {
  def statusTs: Rep[LocalDateTime]
  def endTs: Rep[Option[LocalDateTime]]
}

trait Pit { self =>

  type PitTable[T] = Table[T] with PointInTime

  def asOfNow[T](tq: TableQuery[PitTable[T]]): Query[PitTable[T], T, Seq] = tq filter { _.endTs isEmpty }
  
  def asOf[T](tq: TableQuery[PitTable[T]], ts: LocalDateTime): Query[PitTable[T], T, Seq] = 
    tq filter { row => row.statusTs <= ts && (row.endTs.isEmpty || row.endTs > ts) }
  
  object Implicits {
    implicit class PitTableQueryInfix[T](val tq: TableQuery[PitTable[T]]) /* extends AnyVal */ {
      def asOfNow = self.asOfNow(tq)
      def asOf(ts: LocalDateTime) = self.asOf(tq, ts)
    }
  }

  def updated[T](tq: TableQuery[PitTable[T]],
                 ts: LocalDateTime = LocalDateTime.now)(f: T => T): DBIO[Option[Int]] = {

    import scala.concurrent.ExecutionContext.Implicits.global

    val checkAction = (tq filterNot { _.endTs isEmpty } result) flatMap {
      case Seq() => DBIO.successful(())
      case rows  => DBIO.failed(new IllegalStateException(s"""already expired:\n${rows mkString "\n"}"""))
    }
    val updateAction = tq map { _.endTs } update Some(ts)
    val modifyAction = tq.result map { rows => rows map { f(_) } }
    def insertAction(rows: Seq[T]) = tq ++= rows

    val action = for {
      _ <- checkAction
      cs <- modifyAction
      nrows <- insertAction(cs)
    } yield nrows

    action.transactionally
  }

}