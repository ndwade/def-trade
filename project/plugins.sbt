
//resolvers += Resolver.url(
//  "bintray-sbt-plugin-releases",
//   url("http://dl.bintray.com/banno/oss"))(
//       Resolver.ivyStylePatterns)

// resolvers += Resolver.mavenLocal
//
// addSbtPlugin("com.banno" % "sbt-license-plugin" % "0.1.1-SNAPSHOT")
resolvers += Resolver.typesafeRepo("releases")

// addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")
//
// addSbtPlugin("de.heikoseeberger" % "sbt-header" % "1.5.1")
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "4.0.1")

resolvers += "Flyway" at "https://flywaydb.org/repo"
