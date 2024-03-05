package com.gradle.internal

import com.gradle.develocity.agent.sbt.api.configuration.BuildScan
import com.gradle.develocity.agent.sbt.api.configuration.Server
import com.gradle.internal.CiUtils._
import com.gradle.internal.Utils._
import java.util.Properties

/**
 * Adds a standard set of useful tags, links and custom values to all build scans published.
 */
class BuildScanTransformer(serverConfig: Server, scalaVersions: Seq[String]) extends Transformer[BuildScan] {

  private val SYSTEM_PROP_IDEA_VENDOR_NAME = "idea.vendor.name"
  private val SYSTEM_PROP_IDEA_VERSION = "idea.version"
  private val SYSTEM_PROP_IDEA_MANAGED = "idea.managed"
  private val SYSTEM_PROP_ECLIPSE_BUILD_ID = "eclipse.buildId"
  private val SYSTEM_PROP_IDEA_SYNC_ACTIVE = "idea.sync.active"

  override def transform(originBuildScan: BuildScan): BuildScan = {
    val ops = Seq(
      captureOs,
      captureIde,
      captureCiOrLocal,
      captureCiMetadata,
      captureGitMetadata,
      (_: BuildScan).value("Scala versions", scalaVersions.mkString(","))
    )
    Function.chain(ops)(originBuildScan)
  }

  private val captureOs = {
    ifDefined(sysProperty("os.name"))(_.tag(_))
  }

  private val captureIde: BuildScan => BuildScan =
    if (!isCi) identity
    else {
      val (ide, version) = currentIde()
      val ops = Seq(
        ifDefined(sysProperty(SYSTEM_PROP_IDEA_SYNC_ACTIVE))((bs, _) => bs.tag("IDE sync")),
        (bs: BuildScan) => bs.tag(ide),
        ifDefined(version)((bs, v) => bs.tag(s"$ide version $v"))
      )
      Function.chain(ops)
    }

  private val captureCiOrLocal: BuildScan => BuildScan =
    _.tag(if (isCi) "CI" else "LOCAL")

  private def currentIde(): (String, Option[String]) =
    sysProperty(SYSTEM_PROP_IDEA_VENDOR_NAME)
      .filter(_ == "JetBrains")
      .map(_ => ("IntelliJ IDEA", sysProperty(SYSTEM_PROP_IDEA_VERSION)))
      .orElse(sysProperty(SYSTEM_PROP_IDEA_VERSION).map(v => ("IntelliJ IDEA", Some(v))))
      .orElse(sysProperty(SYSTEM_PROP_IDEA_MANAGED).map(_ => ("IntelliJ IDEA", None)))
      .orElse(sysProperty(SYSTEM_PROP_ECLIPSE_BUILD_ID).map(v => ("Eclipse", Some(v))))
      .getOrElse(("Cmd Line", None))

  private val captureJenkinsOrHudson: BuildScan => BuildScan = {
    if (!isJenkins && !isHudson) identity
    else {
      val jobName = envVariable("JOB_NAME")
      val buildNumber = envVariable("BUILD_NUMBER")
      val ops = Seq(
        ifDefined(envVariable("BUILD_URL")) { case (bs, url) =>
          val label = if (isJenkins) "Jenkins build" else "Hudson build"
          bs.link(label, sbt.url(url))
        },
        ifDefined(buildNumber)(_.value("CI build number", _)),
        ifDefined(envVariable("NODE_NAME"))(withCustomValueAndSearchLink(_, "CI node", _)),
        ifDefined(jobName)(withCustomValueAndSearchLink(_, "CI job", _)),
        ifDefined(envVariable("STAGE_NAME"))(withCustomValueAndSearchLink(_, "CI stage", _)),
        ifDefined(buildNumber.zip(jobName).headOption) { case (bs, (build, job)) =>
          val params = Map("CI job" -> job, "CI build number" -> build)
          withSearchLink(bs, "CI pipeline", params)
        }
      )
      Function.chain(ops)
    }
  }

  private val captureTeamCity: BuildScan => BuildScan = {
    if (!isTeamCity) identity
    else {
      def teamCityBuildUrl(properties: Properties) = for {
        buildId <- getProperty(properties, "teamcity.build.id")
        encodedBuildId <- urlEncode(buildId)
        configFile <- getProperty(properties, "teamcity.configuration.properties.file")
        configProperties = readPropertiesFile(configFile)
        serverUrl <- getProperty(configProperties, "teamcity.serverUrl")
        buildUrl = s"${appendIfMissing(serverUrl, '/')}viewLog.html?buildId=$encodedBuildId"
      } yield sbt.url(buildUrl)

      ifDefined(envVariable("TEAMCITY_BUILD_PROPERTIES_FILE")) { (bs, teamCityBuildPropertiesFile) =>
        val buildProperties = readPropertiesFile(teamCityBuildPropertiesFile)
        val ops = Seq(
          ifDefined(teamCityBuildUrl(buildProperties))(_.link("TeamCity build", _)),
          ifDefined(getProperty(buildProperties, "build.number"))(_.value("CI build number", _)),
          ifDefined(getProperty(buildProperties, "teamcity.buildType.id"))(
            withCustomValueAndSearchLink(_, "CI build config", _)
          ),
          ifDefined(getProperty(buildProperties, "agent.name"))(withCustomValueAndSearchLink(_, "CI agent", _))
        )
        Function.chain(ops)(bs)
      }
    }
  }

  private val captureCircleCi: BuildScan => BuildScan = {
    if (!isCircleCI) identity
    else {
      val ops = Seq(
        ifDefined(envVariable("CIRCLE_BUILD_URL").map(sbt.url))(_.link("CircleCI build", _)),
        ifDefined(envVariable("CIRCLE_BUILD_NUM"))(_.value("CircleCI build number", _)),
        ifDefined(envVariable("CIRCLE_JOB"))(withCustomValueAndSearchLink(_, "CI job", _)),
        ifDefined(envVariable("CIRCLE_WORKFLOW_ID"))(withCustomValueAndSearchLink(_, "CI workflow", _))
      )
      Function.chain(ops)
    }
  }

  private val captureBamboo: BuildScan => BuildScan = {
    if (!isBamboo) identity
    else {
      val ops = Seq(
        ifDefined(envVariable("bamboo_resultsUrl").map(sbt.url))(_.link("Bamboo build", _)),
        ifDefined(envVariable("bamboo_buildNumber"))(_.value("CI build number", _)),
        ifDefined(envVariable("bamboo_planName"))(withCustomValueAndSearchLink(_, "CI plan", _)),
        ifDefined(envVariable("bamboo_buildPlanName"))(withCustomValueAndSearchLink(_, "CI build plan", _)),
        ifDefined(envVariable("bamboo_agentId"))(withCustomValueAndSearchLink(_, "CI agent", _))
      )
      Function.chain(ops)
    }
  }

  private val captureGitHubActions: BuildScan => BuildScan = {
    if (!isGitHubActions) identity
    else {
      val buildUrl = for {
        url <- envVariable("GITHUB_SERVER_URL")
        repository <- envVariable("GITHUB_REPOSITORY")
        runId <- envVariable("GITHUB_RUN_ID")
      } yield sbt.url(s"$url/$repository/actions/runs/$runId")

      val ops = Seq(
        ifDefined(buildUrl)(_.link("GitHub Actions build", _)),
        ifDefined(envVariable("GITHUB_WORKFLOW"))(withCustomValueAndSearchLink(_, "CI workflow", _)),
        ifDefined(envVariable("GITHUB_RUN_ID"))(withCustomValueAndSearchLink(_, "CI run", _))
      )
      Function.chain(ops)
    }
  }

  private val captureGitLab: BuildScan => BuildScan = {
    if (!isGitLab) identity
    else {
      val ops = Seq(
        ifDefined(envVariable("CI_JOB_URL").map(sbt.url))(_.link("GitLab build", _)),
        ifDefined(envVariable("CI_PIPELINE_URL").map(sbt.url))(_.link("GitLab pipeline", _)),
        ifDefined(envVariable("CI_JOB_NAME"))(withCustomValueAndSearchLink(_, "CI job", _)),
        ifDefined(envVariable("CI_JOB_STAGE"))(withCustomValueAndSearchLink(_, "CI stage", _))
      )
      Function.chain(ops)
    }
  }

  private val captureTravis: BuildScan => BuildScan = {
    if (!isTravis) identity
    else {
      val ops = Seq(
        ifDefined(envVariable("TRAVIS_BUILD_WEB_URL").map(sbt.url))(_.link("Travis build", _)),
        ifDefined(envVariable("TRAVIS_BUILD_NUMBER"))(_.value("CI build number", _)),
        ifDefined(envVariable("TRAVIS_JOB_NAME"))(withCustomValueAndSearchLink(_, "CI job", _)),
        ifDefined(envVariable("TRAVIS_EVENT_TYPE"))(_.tag(_))
      )
      Function.chain(ops)
    }
  }

  private val captureBitrise: BuildScan => BuildScan = {
    if (!isBitrise) identity
    else {
      val ops = Seq(
        ifDefined(envVariable("BITRISE_BUILD_URL").map(sbt.url))(_.link("Bitrise build", _)),
        ifDefined(envVariable("BITRISE_BUILD_NUMBER"))(_.value("CI build number", _))
      )
      Function.chain(ops)
    }
  }

  private val captureGoCd: BuildScan => BuildScan = {
    if (!isGoCD) identity
    else {
      val pipelineName = envVariable("GO_PIPELINE_NAME")
      val stageName = envVariable("GO_STAGE_NAME")
      val jobName = envVariable("GO_JOB_NAME")
      val goServerUrl = envVariable("GO_SERVER_URL").map(sbt.url)
      val goCdBuild = for {
        pipelineName <- pipelineName
        pipelineNumber <- envVariable("GO_PIPELINE_COUNTER")
        stageName <- stageName
        stageNumber <- envVariable("GO_STAGE_COUNTER")
        jobName <- jobName
        goServerUrl <- goServerUrl
      } yield {
        sbt.url(s"$goServerUrl/tab/build/detail/$pipelineName/$pipelineNumber/$stageName/$stageNumber/$jobName")
      }
      val buildLink = goCdBuild.map(("GoCD build", _)).orElse(goServerUrl.map(("GoCD", _)))

      val ops = Seq(
        ifDefined(buildLink) { case (bs, (label, url)) => bs.link(label, url) },
        ifDefined(pipelineName)(withCustomValueAndSearchLink(_, "CI pipeline", _)),
        ifDefined(jobName)(withCustomValueAndSearchLink(_, "CI job", _)),
        ifDefined(stageName)(withCustomValueAndSearchLink(_, "CI stage", _))
      )
      Function.chain(ops)
    }
  }

  private val captureAzurePipelines: BuildScan => BuildScan = {
    if (!isAzurePipelines) identity
    else {
      val azureServerUrl = envVariable("SYSTEM_TEAMFOUNDATIONCOLLECTIONURI").map(sbt.url)
      val buildId = envVariable("BUILD_BUILDID")
      val buildUrl = for {
        azureServerUrl <- azureServerUrl
        azureProject <- envVariable("SYSTEM_TEAMPROJECT")
        buildId <- buildId
      } yield sbt.url(s"$azureServerUrl$azureProject/_build/results?buildId=$buildId")
      val buildLink = buildUrl.map(("Azure Pipelines build", _)).orElse(azureServerUrl.map(("Azure Pipelines", _)))

      val ops = Seq(
        ifDefined(buildLink) { case (bs, (label, url)) => bs.link(label, url) },
        ifDefined(buildId)(_.value("CI build number", _))
      )
      Function.chain(ops)
    }
  }

  private val captureBuildkite: BuildScan => BuildScan = {
    if (!isBuildkite) identity
    else {
      val prSource = for {
        repository <- envVariable("BUILDKITE_PULL_REQUEST_REPO")
        webRepoUri <- toWebRepoUri(repository)
        prNumber <- envVariable("BUILDKITE_PULL_REQUEST")
      } yield sbt.url(s"$webRepoUri/pull/$prNumber")

      val ops = Seq(
        ifDefined(envVariable("BUILDKITE_BUILD_URL").map(sbt.url))(_.link("Buildkite build", _)),
        ifDefined(envVariable("BUILDKITE_COMMAND"))(withCustomValueAndSearchLink(_, "CI command", _)),
        ifDefined(envVariable("BUILDKITE_BUILD_ID"))(_.value("CI build number", _)),
        ifDefined(prSource)(_.link("PR source", _))
      )
      Function.chain(ops)
    }
  }

  private val captureCiMetadata: BuildScan => BuildScan = {
    val ops = Seq(
      captureJenkinsOrHudson,
      captureTeamCity,
      captureCircleCi,
      captureBamboo,
      captureGitHubActions,
      captureGitLab,
      captureTravis,
      captureBitrise,
      captureGoCd,
      captureAzurePipelines,
      captureBuildkite
    )
    Function.chain(ops)
  }

  private val captureGitMetadata: BuildScan => BuildScan = {
    if (!isGitInstalled) identity
    else {
      val gitRepo = execAndGetStdOut("git", "config", "--get", "remote.origin.url")
      val gitCommitId = execAndGetStdOut("git", "rev-parse", "--verify", "HEAD")
      val gitCommitShortId = execAndGetStdOut("git", "rev-parse", "--short=8", "--verify", "HEAD")
      val gitBranchName = getGitBranchName()
      val gitStatus = execAndGetStdOut("git", "status", "--porcelain")
      val githubRepositoryLink = for {
        githubUrl <- envVariable("GITHUB_SERVER_URL")
        repository <- envVariable("GITHUB_REPOSITORY")
        commit <- gitCommitId
      } yield ("GitHub source", sbt.url(s"$githubUrl/$repository/tree/$commit"))
      lazy val webRepo = for {
        origin <- gitRepo
        commit <- gitCommitId
        webRepoUri <- toWebRepoUri(origin)
        host = webRepoUri.getHost
        if host.contains("github") || host.contains("gitlab")
      } yield {
        if (host.contains("github")) ("GitHub source", sbt.url(s"$webRepoUri/tree/$commit"))
        else ("GitLab source", sbt.url(s"$webRepoUri/-/commit/$commit"))
      }

      val ops = Seq(
        ifDefined(gitRepo)((bs, repo) => bs.value("Git repository", redactUserInfo(repo))),
        ifDefined(gitCommitId)(_.value("Git commit id", _)),
        ifDefined(gitCommitShortId)(withCustomValueAndSearchLink(_, "Git commit id", "Git commit id short", _)),
        ifDefined(gitBranchName) { (bs, branch) => bs.tag(branch).value("Git branch", branch) },
        ifDefined(gitStatus)(_.tag("Dirty").value("Git status", _)),
        ifDefined(githubRepositoryLink.orElse(webRepo)) { case (bs, (label, uri)) => bs.link(label, uri) }
      )
      Function.chain(ops)
    }
  }

  private lazy val isGitInstalled: Boolean = {
    val installed = execAndCheckSuccess("git", "--version")
    if (!installed) println("[info] Git executable missing")
    installed
  }

  private def getGitBranchName(): Option[String] = {
    val branch =
      if (isJenkins || isHudson) envVariable("BRANCH_NAME")
      else if (isGitLab) envVariable("CI_COMMIT_REF_NAME")
      else if (isAzurePipelines) envVariable("BUILD_SOURCEBRANCH")
      else if (isBuildkite) envVariable("BUILDKITE_BRANCH")
      else None
    branch.orElse(gitBranchFromGit())
  }

  private def gitBranchFromGit(): Option[String] = execAndGetStdOut("git", "rev-parse", "--abbrev-ref", "HEAD")

  private def withCustomValueAndSearchLink(buildScan: BuildScan, name: String, value: String): BuildScan =
    withSearchLink(buildScan.value(name, value), name, name, value)

  private def withCustomValueAndSearchLink(
      buildScan: BuildScan,
      linkLabel: String,
      name: String,
      value: String
  ): BuildScan =
    withSearchLink(buildScan.value(name, value), linkLabel, name, value)

  private def withSearchLink(buildScan: BuildScan, label: String, name: String, value: String): BuildScan = {
    serverConfig.url match {
      case None =>
        buildScan
      case Some(server) =>
        val params = urlEncode(name).map(n => "&search.names=" + n).getOrElse("") + urlEncode(value)
          .map(v => "&search.values=" + v)
          .getOrElse("")
        val searchParams = params.replaceFirst("&", "")
        val buildScanSelection = urlEncode("{SCAN_ID}").map(s => "#selection.buildScanB=" + s).getOrElse("")
        val url = appendIfMissing(server.toString, '/') + "scans?" + searchParams + buildScanSelection
        buildScan.link(s"$label build scans", sbt.url(url))
    }
  }

  private def withSearchLink(buildScan: BuildScan, label: String, values: Map[String, String]): BuildScan = {
    // the parameters for a link querying multiple custom values look like:
    // search.names=name1,name2&search.values=value1,value2
    // this reduction groups all names and all values together in order to properly generate the query

    val keys = values.keys.toList.sorted
    val searchNames = keys.mkString(",")
    val searchValues = keys.map(values.get).mkString(",")

    withSearchLink(buildScan, label, searchNames, searchValues)
  }

}
