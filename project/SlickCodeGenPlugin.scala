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

package io.deftrade.sbt

import sbt._
import Keys._

//import scala.util.{ Success, Failure }
// import scala.language.postfixOps
// import scala.collection.JavaConversions.asScalaBuffer
// import scala.concurrent.{ ExecutionContext, Await, duration }
// import duration._
// import ExecutionContext.Implicits.global

object SlickCodeGenPlugin extends AutoPlugin {

  override def requires = org.flywaydb.sbt.FlywayPlugin
  // override def trigger = allRequirements

  object autoImport {

    lazy val scgPackage = settingKey[String](
      "package for generated tables and repos."
    )
    lazy val scgOutFile = settingKey[File](
      "Output file from SourceCodeGenerator."
    )
    lazy val scgRunUncached = taskKey[Seq[File]](
      "run the SourceCodeGenerator unconditionally"
    )
    lazy val scgRun = taskKey[Seq[File]](
      "run the SourceCodeGenerator if db evolution will trigger based on input set"
    )
    def scgBaseSettings(conf: Configuration): Seq[Setting[_]] = {
      import io.deftrade.db.SourceCodeGenerator
      import org.flywaydb.sbt.FlywayPlugin.autoImport._

      Seq(

        scgPackage := "db",

        scgOutFile := {
          ((scgPackage.value split '.').foldLeft((sourceManaged in conf).value) { _ / _ }) / "Tables.scala"
        },

        scgRunUncached := {
          flywayMigrate.value  // Unit - pure effect
          SourceCodeGenerator(
            driver = flywayDriver.value,
            url = flywayUrl.value,
            user = flywayUser.value,
            password = flywayPassword.value,
            folder = (sourceManaged in conf).value,
            pkg = scgPackage.value
          )
          Seq(scgOutFile.value)
        },
        
        scgRun := {
          val inSet = Set.empty[File] ++ (flywayLocations.value map { fl =>
            (fl.stripPrefix("filesystem:") split '/').foldLeft(baseDirectory.value) { _ / _ }
          })
          val tag = cacheDirectory.value / "gen-slick-code"
          val cachedScg: Set[File] => Set[File] = FileFunction.cached(
            tag, FilesInfo.lastModified, FilesInfo.exists
          ) {
            _ =>
              flywayMigrate.value  // Unit - pure effect
              scgRunUncached.value
              Set(scgOutFile.value)
          }
          cachedScg(inSet).toSeq
        },

        sourceGenerators in conf += scgRun.taskValue
      )
    }
  }

  import autoImport._

  override def projectSettings: Seq[Setting[_]] = scgBaseSettings(Compile)

}
