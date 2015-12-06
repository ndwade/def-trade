import sbt._
import Keys._
import Tests._

object Version {
  val Scala = "2.11.7"
  val Akka = "2.4.1"
  val AkkaStream = "2.0-M2"
  val ScalaTest = "2.2.4"
  val Scalactic = "2.2.4"
  val Slick = "3.1.0"
  //val ScalaCheck = "1.11.3"
}

object Resolvers {
  val typesafeReleases = "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
  //val typesafeSnapshots = "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
  //resolvers += Resolver.mavenLocal
  val pathikrit = Resolver.bintrayRepo("pathikrit", "maven")
}

object Dependencies {

  val reflection = "org.scala-lang" % "scala-reflect" % Version.Scala
  val xml = "org.scala-lang.modules" %% "scala-xml" % "1.0.2"
  val parserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"

  val akka = "com.typesafe.akka" %% "akka-actor" % Version.Akka
  //val agent  = "com.typesafe.akka" %% "akka-agent" % Version.Akka
  val testkit = "com.typesafe.akka" %% "akka-testkit" % Version.Akka

  val akkaStream = "com.typesafe.akka" %% "akka-stream-experimental" % Version.AkkaStream
  val akkaHttp = "com.typesafe.akka" %% "akka-http-core-experimental" % Version.AkkaStream
  val akkaHttpDsl = "com.typesafe.akka" %% "akka-http-experimental" % Version.AkkaStream

  val slick = "com.typesafe.slick" %% "slick" % Version.Slick
  val slickPg = "com.github.tminglei" %% "slick-pg" % "0.10.0"

  val scalatest = "org.scalatest" %% "scalatest" % Version.ScalaTest
  val scalactic = "org.scalactic" %% "scalactic" % Version.Scalactic
  //val scalacheck = "org.scalacheck" %% "scalacheck" % Version.ScalaCheck

  val betterFiles = "com.github.pathikrit" %% "better-files" % "2.11.0"
  val timeforscala = "com.markatta" %% "timeforscala" % "1.2"

  val slf4j = "com.typesafe.akka" %% "akka-slf4j" % Version.Akka
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"

}

object Defs {

  // one test per Group
  def singleTests(tests: Seq[TestDefinition]) =
    tests map { test =>
      new Group(
        name = test.name,
        tests = Seq(test),
        runPolicy = SubProcess(javaOptions = Seq.empty[String]))
    }
}
