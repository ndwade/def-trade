import sbt._
import Keys._
import Tests._

object Version {
  val Scala = "2.11.8"
  val Akka = "2.4.4"
  val ScalaTest = "2.2.6"
  val Scalactic = "2.2.6"
  val Slick = "3.1.1"
  val SlickPg = "0.14.0"
  val PgJdbc = "9.4-1201-jdbc41"
  val UPickle = "0.3.6"
}

object Resolvers {
  val typesafeReleases = "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
  //val typesafeSnapshots = "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
  //resolvers += Resolver.mavenLocal
  // val pathikrit = Resolver.bintrayRepo("pathikrit", "maven")
  // val sonatypeSnapshots =
  //   "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
}

object Dependencies {

  val reflection = "org.scala-lang" % "scala-reflect" % Version.Scala
  val xml = "org.scala-lang.modules" %% "scala-xml" % "1.0.4"
  val parserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"

  val akka = "com.typesafe.akka" %% "akka-actor" % Version.Akka
  //val agent  = "com.typesafe.akka" %% "akka-agent" % Version.Akka
  val testkit = "com.typesafe.akka" %% "akka-testkit" % Version.Akka

  val reactiveStreams = "org.reactivestreams" % "reactive-streams" % "1.0.0"
  val akkaStream = "com.typesafe.akka" %% "akka-stream" % Version.Akka
  val akkaStreamTestkit = "com.typesafe.akka" %% "akka-stream-testkit" % Version.Akka

  val slick = "com.typesafe.slick" %% "slick" % Version.Slick
  val slickCodeGen = "com.typesafe.slick" %% "slick-codegen" % Version.Slick
  val slickPg = "com.github.tminglei" %% "slick-pg" % Version.SlickPg
  val slickPgDate = "com.github.tminglei" %% "slick-pg_date2" % Version.SlickPg

  val postgres = "org.postgresql" % "postgresql" % Version.PgJdbc

  val upickle = "com.lihaoyi" %% "upickle" % Version.UPickle

  val slf4j = "com.typesafe.akka" %% "akka-slf4j" % Version.Akka
  val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"
  val slf4jNop = "org.slf4j" % "slf4j-nop" % "1.6.4"

  val scalatest = "org.scalatest" %% "scalatest" % Version.ScalaTest
  val scalactic = "org.scalactic" %% "scalactic" % Version.Scalactic

  // val betterFiles = "com.github.pathikrit" %% "better-files" % "2.11.0"
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
