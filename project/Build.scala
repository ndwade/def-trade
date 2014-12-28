import sbt._
import Keys._
import Tests._

object Version {
  val Scala = "2.11.4"
  val Akka = "2.3.8"
  val ScalaTest = "2.2.1"
  //val ScalaCheck = "1.11.3"
}

object Resolvers {
  val typesafeReleases = "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"
  //val typesafeSnapshots = "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
  //resolvers += Resolver.mavenLocal
}

object Dependencies {
  val xml = "org.scala-lang.modules" %% "scala-xml" % "1.0.2"
  val parserCombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.1"
  val reflection = "org.scala-lang" % "scala-reflect" % Version.Scala
  val akka = "com.typesafe.akka" %% "akka-actor" % Version.Akka
  //val agent  = "com.typesafe.akka" %% "akka-agent" % Version.Akka
  val testkit = "com.typesafe.akka" %% "akka-testkit" % Version.Akka
  val scalatest = "org.scalatest" %% "scalatest" % Version.ScalaTest
  //val scalacheck = "org.scalacheck" %% "scalacheck" % Version.ScalaCheck

  //val slf4j = "com.typesafe.akka" %% "akka-slf4j" % Version.Akka
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
