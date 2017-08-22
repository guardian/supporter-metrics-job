addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.4")

addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "0.9.8")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.7.0")

resolvers += Resolver.url(
  "idio",
  url("http://dl.bintray.com/idio/sbt-plugins")
)(Resolver.ivyStylePatterns)

addSbtPlugin("org.idio" % "sbt-assembly-log4j2" % "0.1.0")
