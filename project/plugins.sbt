resolvers += Resolver.mavenLocal
//lazy val dvVersion = "0.11-for-local-testing"
lazy val dvVersion = "1.0"
addSbtPlugin("com.gradle" % "sbt-develocity" % dvVersion)
//addSbtPlugin("com.gradle" % "sbt-common-custom-user-data" % "0.1.0-SNAPSHOT")
