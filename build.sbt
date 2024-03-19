import Dependencies.*

ThisBuild / scalaVersion := "2.12.15"
ThisBuild / version := "1.0-SNAPSHOT"
ThisBuild / organization := "com.gradle"
ThisBuild / organizationName := "gradle"

sbtPlugin := true
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

// Publishing setup
ThisBuild / description := "A sbt plugin to capture common custom user data used for sbt Build Scans in Develocity"
ThisBuild / licenses    := List("Apache-2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage    := Some(url("https://github.com/gradle/sbt-common-custom-user-data"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/gradle/sbt-common-custom-user-data"),
    "scm:git@github.com:gradle/sbt-common-custom-user-data.git"
  )
)
// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishMavenStyle := true
ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / publishTo := {
  val artifactory = "https://repo.grdev.net/artifactory/"
  if (isSnapshot.value) Some("sbtSnapshot" at artifactory + "enterprise-libs-sbt-snapshots-local")
  else Some("sbtReleaseCandidate" at artifactory + "enterprise-libs-sbt-release-candidates-local")
}

addCommandAlias("publishSbtPluginPublicationToMavenLocalRepository", "; set publishTo := Some(MavenCache(\"local-maven\", file(\"target/localRepo\"))) ; publish") ++
addCommandAlias("publishAll", "; set publishTo := Some(\"sbtSnapshot\" at \"https://repo.grdev.net/artifactory/enterprise-libs-sbt-snapshots-local\") ; publish") ++ // Publish to Snapshots
addCommandAlias("publishRc", "; set publishTo := Some(\"sbtReleaseCandidate\" at \"https://repo.grdev.net/artifactory/enterprise-libs-sbt-release-candidates-local\") ; publish") // Publish to RC
