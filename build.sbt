import Dependencies._
import Resolvers._
import Defs._

lazy val buildSettings = buildLicenseSettings ++ Seq(
    organization := "io.deftrade",
    version := "0.1-SNAPSHOT",
    scalaVersion := Version.Scala,
    scalacOptions in Compile := Seq("-deprecation", "-feature", "-Xlint"),

    // TODO: check - parallelExecution in Test := false // will this work?
    concurrentRestrictions in Global += Tags.limit(Tags.Test, 1),
    resolvers ++= Seq(typesafeReleases)
  )

lazy val buildLicenseSettings = {
  import com.banno.license.Plugin.LicenseKeys._
  import com.banno.license.Licenses._
  licenseSettings ++ Seq(
    license := apache2("Copyright 2014 Panavista Technologies, LLC"),
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
      Seq(parserCombinators, akka) ++
      Seq(scalatest, testkit).map(_ % Test),
    testGrouping in Test := singleTests((definedTests in Test).value)
  )

lazy val demo = (project in file ("demo")).
  dependsOn(ibClient).
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
      Seq(xml, akka, testkit) ++
      Seq(scalatest).map(_ % Test)
  )
