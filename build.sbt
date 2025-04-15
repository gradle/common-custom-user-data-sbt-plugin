import Dependencies.*

ThisBuild / scalaVersion := "2.12.15"
ThisBuild / organization := "com.gradle"
ThisBuild / organizationName := "Gradle Inc."

ThisBuild / dynverSonatypeSnapshots := true

lazy val sharedSettings = Seq(
  scalacOptions ++= Seq(
    "-Xfatal-warnings",
    "-Xlint:-unused",
    "-Ywarn-unused:imports,privates,locals,implicits"
  ),
  pluginCrossBuild / sbtVersion := {
    scalaBinaryVersion.value match {
      case "2.12" => "1.6.0" // set minimum sbt version - best to keep it in sync with the DV plugin
    }
  },
  addSbtPlugin(develocityPlugin),
  scriptedLaunchOpts ++= Seq(
    "-Xmx1024M",
    "-Dplugin.version=" + version.value,
    "-Dscan=false", // Don't publish build scans from scripted builds
  )
)

ThisBuild / develocityConfiguration :=
  DevelocityConfiguration(
    server = Server(
      url = Some(url("https://ge.solutions-team.gradle.com"))
    ),
    buildScan = BuildScan(
      obfuscation = Obfuscation(
        ipAddresses = _.map(_ => "0.0.0.0")
      ),
      backgroundUpload = !sys.env.contains("CI"),
      publishing = Publishing.onlyIf { _.authenticated },
    )
  )

lazy val `sbt-develocity-common-custom-user-data` = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    sharedSettings,
    name := "Develocity Common Custom User Data sbt Plugin",
    normalizedName := "sbt-develocity-common-custom-user-data",
    libraryDependencies ++= Seq(
      scalaTest % Test,
    ),
  )

lazy val `sbt-example-company-plugin` = project.in(file("sbt-example-company-plugin"))
  .dependsOn(`sbt-develocity-common-custom-user-data`)
  .enablePlugins(SbtPlugin)
  .settings(
    sharedSettings,
  )

// Publishing setup
ThisBuild / description := "A sbt plugin to capture common custom user data used for sbt Build Scans in Develocity"
ThisBuild / licenses    := List("Apache-2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage    := Some(url("https://github.com/gradle/common-custom-user-data-sbt-plugin"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/gradle/common-custom-user-data-sbt-plugin"),
    "scm:git@github.com:gradle/common-custom-user-data-sbt-plugin.git"
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
credentials ++= {
  for {
    username <- sys.env.get("OSSRH_REPO_USER")
    password <- sys.env.get("OSSRH_REPO_PASSWORD")
  } yield Credentials("Sonatype Nexus Repository Manager", "s01.oss.sonatype.org", username, password)
}
credentials ++= {
  for {
    username <- sys.env.get("ARTIFACTORY_REPO_USER")
    password <- sys.env.get("ARTIFACTORY_REPO_PASSWORD")
  } yield Credentials("Artifactory Realm", "repo.grdev.net", username, password)
}
credentials ++= {
  for {
    keyId <- sys.env.get("SIGN_KEY_ID")
  } yield Credentials("GnuPG Key ID", "gpg", keyId, "ignored")
}
publishM2Configuration := publishM2Configuration.value.withOverwrite(true) // allows overwriting local .m2 publishings

addCommandAlias("publishSbtSnapshot", "; set publishTo := Some(\"SbtSnapshot\" at \"https://repo.grdev.net/artifactory/enterprise-libs-sbt-snapshots-local\") ; publish")
addCommandAlias("publishSbtRc", "; set publishTo := Some(\"SbtReleaseCandidate\" at \"https://repo.grdev.net/artifactory/enterprise-libs-sbt-release-candidates-local\") ; publish")
