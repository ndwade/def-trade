
//resolvers += Resolver.url(
//  "bintray-sbt-plugin-releases",
//   url("http://dl.bintray.com/banno/oss"))(
//       Resolver.ivyStylePatterns)

resolvers += Resolver.mavenLocal

addSbtPlugin("com.banno" % "sbt-license-plugin" % "0.1.1-SNAPSHOT")
