
resolvers += Resolver.typesafeRepo("releases")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

resolvers += Resolver.url("bintray-sbt-plugin-releases")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.5.1")

resolvers += "Flyway" at "https://flywaydb.org/repo"
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.0.1")
