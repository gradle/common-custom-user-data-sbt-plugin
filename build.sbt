import Dependencies.*

ThisBuild / scalaVersion := "2.12.15"
ThisBuild / version := "1.0-SNAPSHOT"
ThisBuild / organization := "com.gradle"
ThisBuild / organizationName := "Gradle Inc."

sbtPlugin := true

Global / develocityConfiguration :=
  DevelocityConfiguration(
    server = Server(
      url = Some(url("https://ge.solutions-team.gradle.com"))
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
    url("https://github.com/ribafish/sbt-common-custom-user-data"),
    "scm:git@github.com:ribafish/sbt-common-custom-user-data.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "gradle",
    name = "The Gradle team",
    email = "info@gradle.com",
    url = url("https://gradle.com")
  )
)
// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishMavenStyle := true
ThisBuild / versionScheme := Some("semver-spec")
ThisBuild / publishTo := {
  if (isSnapshot.value) Some("ossrh" at "https://s01.oss.sonatype.org/content/repositories/snapshots")
  else Some("ossrh" at "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
}
credentials += Credentials("Artifactory", "https://repo.grdev.net/artifactory", sys.env.getOrElse("ARTIFACTORY_REPO_USER", ""), sys.env.getOrElse("ARTIFACTORY_REPO_PASSWORD", ""))
credentials += Credentials("ossrh", "https://s01.oss.sonatype.org", sys.env.getOrElse("OSSRH_REPO_USER", ""), sys.env.getOrElse("OSSRH_REPO_PASSWORD", ""))

addCommandAlias("publishSbtSnapshot", "; set publishTo := Some(\"SbtSnapshot\" at \"https://repo.grdev.net/artifactory/enterprise-libs-sbt-snapshots-local\") ; publish") ++
addCommandAlias("publishSbtRc", "; set publishTo := Some(\"SbtReleaseCandidate\" at \"https://repo.grdev.net/artifactory/enterprise-libs-sbt-release-candidates-local\") ; publish")
