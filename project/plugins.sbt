resolvers += Resolver.mavenLocal
lazy val geVersion = "0.9"
addSbtPlugin("com.gradle" % "sbt-gradle-enterprise" % geVersion)
addSbtPlugin("com.gradle" % "common-custom-user-data-scala-plugin" % "0.1.0-SNAPSHOT")
