import Dependencies._
import Resolvers._
import Defs._

crossPaths in Global := false

lazy val gitHeadCommitSha = taskKey[String](
  "Determines the current git commit SHA"
)

gitHeadCommitSha := Process("git rev-parse HEAD").lines.head

val genSlickCode = taskKey[Seq[File]]("Generate Slick types and repos from Postgres schema.")

lazy val buildSettings = buildLicenseSettings ++ Seq(
    organization := "io.deftrade",
    version := "0.1-SNAPSHOT",
    scalaVersion := Version.Scala,
    scalacOptions in Compile := Seq("-deprecation", "-feature", "-Xlint"),

    // TODO: check - parallelExecution in Test := false // will this work?
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
    resolvers ++= Seq(typesafeReleases, pathikrit)
  )

lazy val buildLicenseSettings = {
  import com.banno.license.Plugin.LicenseKeys._
  import com.banno.license.Licenses._
  licenseSettings ++ Seq(
    license := apache2("Copyright 2014-2016 Panavista Technologies, LLC"),
    removeExistingHeaderBlock := true,
    licenseTests := true
  )
}

lazy val deftrade = (project in file (".")).
  aggregate(macros, ibClient, demo).
  settings(buildSettings: _*).
  settings(buildLicenseSettings: _*)

lazy val macros = (project in file ("macros")).
  settings(buildSettings: _*).
  settings(
    libraryDependencies ++=
      Seq(reflection) ++
      Seq(scalatest).map(_ % Test)
  )

lazy val ibClient = (project in file ("ib-client")).
  dependsOn(macros).
  settings(buildSettings: _*).
  settings(
    libraryDependencies ++=
      Seq(xml, parserCombinators, akka, akkaStream) ++
      Seq(scalatest, testkit).map(_ % Test),
    testGrouping in Test := singleTests((definedTests in Test).value)
  )

lazy val db = project.
  settings(buildSettings: _*).
  settings(
    libraryDependencies ++=
      Seq(xml, slick, slickCodeGen, slickPg, slickPgDate, slf4jNop, postgres,
        timeforscala, upickle) ++
      Seq(scalatest, testkit).map(_ % Test)
  ).settings(
    (genSlickCode in Test) := {
      val r = (runner in Test).value
      val cp = (fullClasspath in Compile).value.files ++ (unmanagedResourceDirectories in Test).value
      val dir = (sourceManaged in Test).value
      val pkg = "io.deftrade.test"
      val pkgDir = (pkg split '.').foldLeft(dir) { _ / _ }
      val log = streams.value.log
      toError(r.run("io.deftrade.db.SourceCodeGenerator", cp, Array(dir.getPath, pkg), log))
      Seq(pkgDir / "Tables.scala")
    }
  ).settings(
    (sourceGenerators in Test) += (genSlickCode in Test).taskValue
  )

lazy val demo = (project in file ("demo")).
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
  * - run evolution_n.sql script
  * - run the [[SourceCodeGenerator]] to generate Tables.scala
  * - run the rest of the compilation.
  *
  * ==== testing: ====
  * - build a Tables.scala from a test schema, in the test db
  * - exercise the repository methods
  *
  */
