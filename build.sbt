import Dependencies._
import Resolvers._
import Defs._

import de.heikoseeberger.sbtheader
import sbtheader.AutomateHeaderPlugin
import sbtheader.license.Apache2_0

import com.typesafe.sbt.SbtScalariform

import io.deftrade.sbt.SlickCodeGenPlugin
import SlickCodeGenPlugin.autoImport._

crossPaths in Global := false

lazy val buildSettings = Seq(
    organization := "io.deftrade",
    version := "0.1-SNAPSHOT",
    scalaVersion := Version.Scala,
    scalacOptions in Compile := Seq("-deprecation", "-feature", "-Xlint"),

    // TODO: check - parallelExecution in Test := false // will this work?
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
    resolvers ++= Seq(typesafeReleases),
    headers := Map(
      "scala" -> Apache2_0("2014-2016", "Panavista Technologies LLC"),
      "conf"  -> Apache2_0("2014-2016", "Panavista Technologies LLC", "#")
    )
  )
lazy val deftrade = (project in file (".")).
  aggregate(macros, ibClient, demo).
  settings(buildSettings: _*)

lazy val macros = (project in file ("macros")).
  settings(buildSettings: _*).
  settings(
    libraryDependencies ++=
      Seq(reflection) ++
      Seq(scalatest).map(_ % Test)
  )

lazy val ibClient = project.
  dependsOn(macros).
  settings(buildSettings: _*).
  settings(
    libraryDependencies ++=
      Seq(xml, parserCombinators, akka, akkaStream) ++
      Seq(scalatest, testkit).map(_ % Test),
    testGrouping in Test := singleTests((definedTests in Test).value)
  )

lazy val db = project.
  // enablePlugins(AutomateHeaderPlugin).
  disablePlugins(SbtScalariform, SlickCodeGenPlugin).
  settings(buildSettings: _*).
  settings(
    libraryDependencies ++=
      Seq(xml, slick, slickCodeGen, slickPg, slickPgDate, slf4jNop, postgres, upickle) ++
      Seq(scalatest, testkit).map(_ % Test)
  ).settings(
    // inConfig(Test)(scgBaseSettings ++ Seq(
    scgBaseSettings(Test) ++
    Seq(
      flywayLocations := List("filesystem:db/src/main/resources/db/migration"),
      flywayDriver := "org.postgresql.Driver",
      flywayUrl := "jdbc:postgresql://localhost:5432/test",
      flywayUser := "deftrade",
      flywayCleanOnValidationError := true,
      flywayTable := "schema_versions", // migrations metadata table name
      scgPackage := "io.deftrade.db.test"
    )
  )

lazy val demo = project.
  dependsOn(ibClient, db).
  settings(buildSettings: _*).
  settings(initialCommands in console :=
    """|import scala.concurrent.duration._
       |import akka.testkit.{ TestActors, TestKit, ImplicitSender }
       |import io.deftrade._
       |import demo._
       |import Ib._
       |object TK extends TestKit(system)
       |import TK._
       |""".stripMargin).
  settings(
    libraryDependencies ++=
      Seq(xml, akka, testkit, akkaStream, slf4j, logback) ++
      Seq(scalatest, akkaStreamTestkit).map(_ % Test)
  )


  //
  //
  // lazy val gitHeadCommitSha = taskKey[String](
  //   "Determines the current git commit SHA"
  // )
  //
  // gitHeadCommitSha := Process("git rev-parse HEAD").lines.head

  // http://stackoverflow.com/questions/20083564/can-multi-projects-from-git-be-used-as-sbt-dependencies
  // lazy val staminaCore = ProjectRef(uri("git://github.com/scalapenos/stamina.git#master"), "stamina-core")


/*
- if we're using Flyway SBT, it seems easiest to use Settings to hold db config info (url etc).
- SourceCodeGenerator should be Plugin-like and part of the build,
not part of the compile:compile task. BUT: in production, the application.conf HCON settings are the "ground truth"...
- SourceCodeGenerator should not depend on any part of the db/compile config.
In particular, it can't depend on DefTradePostgresDriver. use com.tminglei.PostgresDriverEx ?!
- so both flywayMigrate and getSlickCode depend on common settings for the migrations sql scripts
dir and the
- getSlickCode task should be defined with a Config param - i.e., in Compile or in Test
- genSlickCode task should depend on flywayMigrate
- should be able to FileFunction.cached genSlickCode against migrations sql script dir lastModified and Tables.scala existence.
- genSlickCode dependancy on migrations dir is just for cache invalidation.
- so flywayMigrate will be run automatically when genSlickCode must be run.
- the db/ project is the only project which will need to mess with SourceCodeGenerator.
- the demo/ project will use Flyway migrations programmatically. This is the template for typical application usage. Question - where does it get the db url config info - the HCON config files? Is there a plugin thingie which could be written to resolve HCON and publish flywayUrl setting etc...?!
(would need resourceUnmanaged on the classpath) Would be nice to be able to use the Flyway config case classes from the flyWay plugin... but this is production.,,
*/

//pg_dump --dbname=test --username=deftrade --no-password --schema-only --clean --file=genesis.sql
  /**
  * === SBT Task Breakdown ===
  *
  * === genesis.sql ===
  * - entire schema description from empty db
  * - must be dumped from postgres directly
  * - can't maintain by hand because of need to keep in sync with incremental scripts
  * - can't be written out from slick because the slick model doesn't comprehend the
  * different index types (e.g. gist) provided by postres
  *
  * ==== initializing a brand new database: ====
  * - run genesis.sql
  * - run the [[SourceCodeGenerator]] to generate Tables.scala
  * - run the rest of the compilation.
  *
  * ==== evolving a dev or production database: ====
  * - run Vn_evolution.sql script
  * - run the [[SourceCodeGenerator]] to generate Tables.scala
  * - run the rest of the compilation.
  *
  * ==== testing: ====
  * - build a Tables.scala from a test schema, in the test db
  * - exercise the repository methods
  *
  */
