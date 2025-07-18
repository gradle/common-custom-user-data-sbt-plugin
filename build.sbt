import Dependencies.*

ThisBuild / scalaVersion := "2.12.15"
ThisBuild / organization := "com.gradle"
ThisBuild / organizationName := "Gradle Inc."

ThisBuild / dynverSonatypeSnapshots := true

sbtPlugin := true

ThisBuild / develocityConfiguration ~= { prev =>
  prev
    .withServer(
      prev.server.withUrl(Some(url("https://ge.solutions-team.gradle.com")))
    )
    .withBuildScan(
      prev.buildScan
        .withObfuscation(prev.buildScan.obfuscation.withIpAddresses(_.map(_ => "0.0.0.0")))
        .withBackgroundUpload(!sys.env.contains("CI"))
        .withPublishing(Publishing.onlyIf(_.authenticated))
    )
}

lazy val sbtCommonCustomUserDataPlugin = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    scalacOptions ++= Seq(
      "-Xfatal-warnings",
      "-Xlint:-unused",
      "-Ywarn-unused:imports,privates,locals,implicits"
    ),
    name := "Develocity Common Custom User Data sbt Plugin",
    normalizedName := "sbt-develocity-common-custom-user-data",
    libraryDependencies ++= Seq(
      scalaTest % Test
    ),
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.6.0" // set minimum sbt version - best to keep it in sync with the GE plugin
      }
    },
    addSbtPlugin(develocityPlugin),
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-Dplugin.version=" + version.value,
      "-Dscan=false" // Don't publish build scans from scripted builds
    )
  )

// Publishing setup
ThisBuild / description := "A sbt plugin to capture common custom user data used for sbt Build Scans in Develocity"
ThisBuild / licenses := List("Apache-2.0" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/gradle/common-custom-user-data-sbt-plugin"))
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
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}
credentials ++= {
  for {
    username <- sys.env.get("OSSRH_REPO_USER")
    password <- sys.env.get("OSSRH_REPO_PASSWORD")
  } yield Credentials("Sonatype Nexus Repository Manager", "central.sonatype.com", username, password)
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

addCommandAlias(
  "publishSbtSnapshot",
  "; set publishTo := Some(\"SbtSnapshot\" at \"https://repo.grdev.net/artifactory/enterprise-libs-sbt-snapshots-local\") ; publish"
)
addCommandAlias(
  "publishSbtRc",
  "; set publishTo := Some(\"SbtReleaseCandidate\" at \"https://repo.grdev.net/artifactory/enterprise-libs-sbt-release-candidates-local\") ; publish"
)
