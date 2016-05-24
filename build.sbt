import Dependencies._
import Resolvers._
import Defs._

// import scalariform.formatter.preferences._
// import com.typesafe.sbt.SbtScalariform
// import com.typesafe.sbt.SbtScalariform.ScalariformKeys
//
// import de.heikoseeberger.sbtheader
// import sbtheader.AutomateHeaderPlugin
// import sbtheader.license.Apache2_0
//
//
// SbtScalariform.scalariformSettings



crossPaths in Global := false

// lazy val gitHeadCommitSha = taskKey[String](
//   "Determines the current git commit SHA"
// )
//
// gitHeadCommitSha := Process("git rev-parse HEAD").lines.head

// http://stackoverflow.com/questions/20083564/can-multi-projects-from-git-be-used-as-sbt-dependencies
// lazy val staminaCore = ProjectRef(uri("git://github.com/scalapenos/stamina.git#master"), "stamina-core")



val genSlickCode = taskKey[Seq[File]]("Generate Slick types and repos from Postgres schema.")

lazy val buildSettings = Seq(
    organization := "io.deftrade",
    version := "0.1-SNAPSHOT",
    scalaVersion := Version.Scala,
    scalacOptions in Compile := Seq("-deprecation", "-feature", "-Xlint"),

    // TODO: check - parallelExecution in Test := false // will this work?
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
    resolvers ++= Seq(typesafeReleases, pathikrit)//,
    // headers := Map(
    //   "scala" -> Apache2_0("2014-2016", "Panavista Technologies LLC"),
    //   "conf"  -> Apache2_0("2014-2016", "Panavista Technologies LLC", "#")
    // )
  )


// lazy val buildLicenseSettings = {
//   import com.banno.license.Plugin.LicenseKeys._
//   import com.banno.license.Licenses._
//   licenseSettings ++ Seq(
//     license := apache2("Copyright 2014-2016 Panavista Technologies, LLC"),
//     removeExistingHeaderBlock := true,
//     licenseTests := true
//   )
// }

lazy val deftrade = (project in file (".")).
  aggregate(macros, ibClient, demo).
  // enablePlugins(AutomateHeaderPlugin).
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
  settings(buildSettings: _*).
  // settings(ScalariformKeys.preferences := ScalariformKeys.preferences.value
  //   .setPreference(AlignSingleLineCaseStatements, true)
  //   .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 40)
  //   .setPreference(DoubleIndentClassDeclaration, true)
  //   .setPreference(PreserveDanglingCloseParenthesis, true)
  //   .setPreference(SpacesWithinPatternBinders, true)).
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
      val pkg = "io.deftrade.db.test"
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



// #Scalariform formatter preferences
// #Fri Apr 01 21:09:37 BST 2011
// alignParameters=true
// compactStringConcatenation=false
// indentPackageBlocks=true
// formatXml=true
// preserveSpaceBeforeArguments=false
// doubleIndentClassDeclaration=false
// doubleIndentMethodDeclaration=false
// rewriteArrowSymbols=false
// alignSingleLineCaseStatements=true
// alignSingleLineCaseStatements.maxArrowIndent=40
// spaceBeforeColon=false
// spaceInsideBrackets=false
// spaceInsideParentheses=false
// preserveDanglingCloseParenthesis=false
// indentSpaces=2
// indentLocalDefs=false
// spacesWithinPatternBinders=true
// spacesAroundMultiImports=true

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
