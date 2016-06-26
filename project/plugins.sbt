
resolvers += Resolver.typesafeRepo("releases")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

resolvers += Resolver.url("bintray-sbt-plugin-releases")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.5.1")

resolvers += "Flyway" at "https://flywaydb.org/repo"
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.0.2")

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "com.typesafe.slick" %% "slick-codegen" %"3.1.1",
  "com.github.tminglei" %% "slick-pg" % "0.14.0"
)
