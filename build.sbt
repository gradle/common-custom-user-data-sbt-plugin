import Dependencies.*

ThisBuild / scalaVersion := "2.12.15"
ThisBuild / version := "1.0-SNAPSHOT"
ThisBuild / organization := "com.gradle"
ThisBuild / organizationName := "gradle"

sbtPlugin := true
publishMavenStyle := true
resolvers += Resolver.mavenLocal

Global / develocityConfiguration :=
  DevelocityConfiguration(
    server = Server(
//      url = Some(url("https://ge.solutions-team.gradle.com"))
        url = Some(url("https://ge-helm-cluster-unstable.grdev.net"))
    ),
    buildScan = BuildScan(
      tags = Set(),
      obfuscation = Obfuscation(
        ipAddresses = _.map(_ => "0.0.0.0")
      ),
      backgroundUpload = !sys.env.contains("CI"),
      publishing = Publishing.onlyIf(_ => true),
    )
  )


lazy val sbtCommonCustomUserDataPlugin = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Xlint:-unused",
      "-Ywarn-unused:imports,privates,locals,implicits"
    ),
    name := "sbt-common-custom-user-data",
    libraryDependencies ++= Seq(
      scalaTest % Test,
    ),
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.6.0" // set minimum sbt version - best to keep it in sync with the GE plugin
      }
    },
    addSbtPlugin(develocityPlugin)
  )

// Uncomment the following for publishing to Sonatype.
// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for more detail.

// ThisBuild / description := "Some descripiton about your project."
// ThisBuild / licenses    := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
// ThisBuild / homepage    := Some(url("https://github.com/example/project"))
// ThisBuild / scmInfo := Some(
//   ScmInfo(
//     url("https://github.com/your-account/your-project"),
//     "scm:git@github.com:your-account/your-project.git"
//   )
// )
// ThisBuild / developers := List(
//   Developer(
//     id    = "Your identifier",
//     name  = "Your Name",
//     email = "your@email",
//     url   = url("http://your.url")
//   )
// )
// ThisBuild / pomIncludeRepository := { _ => false }
// ThisBuild / publishTo := {
//   val nexus = "https://oss.sonatype.org/"
//   if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
//   else Some("releases" at nexus + "service/local/staging/deploy/maven2")
// }
// ThisBuild / publishMavenStyle := true
