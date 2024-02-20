import sbt._

object Dependencies {
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.15"
//  lazy val develocityPlugin = "com.gradle" % "sbt-develocity" % "0.11-for-local-testing"
  lazy val develocityPlugin = "com.gradle" % "sbt-develocity" % "1.0"
}
