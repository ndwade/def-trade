
resolvers += Resolver.url("bintray-sbt-plugin-releases")
resolvers += "Flyway" at "https://flywaydb.org/repo"
// resolvers += Resolver.mavenLocal

// addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.5.1")
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.0.1")

resolvers += Resolver.typesafeRepo("releases")
