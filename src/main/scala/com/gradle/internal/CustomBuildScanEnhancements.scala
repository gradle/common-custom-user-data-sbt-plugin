package com.gradle.internal

import com.gradle.develocity.agent.sbt.api.configuration.BuildScan
import com.gradle.develocity.agent.sbt.api.configuration.Server
import com.gradle.internal.Utils.Env
import java.util.Properties
import java.net.URL
import sbt.Logger

/**
 * Adds a standard set of useful tags, links and custom values to all build scans published.
 */
class CustomBuildScanEnhancements(serverConfig: Server, scalaVersions: Seq[String], logger: Logger)(implicit
    env: Env
) extends Transformer[BuildScan] {

  private val SYSTEM_PROP_IDEA_VERSION = Env.Key[String]("idea.version")

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
    ifDefined(env.sysProperty[String]("os.name"))(_.tag(_))
  }

  private val captureIde: BuildScan => BuildScan =
    if (!CiUtils.isCi) identity
    else {
      val (ide, version) =
        env
          .sysProperty[String]("idea.vendor.name")
          .filter(_ == "JetBrains")
          .map(_ => ("IntelliJ IDEA", env.sysProperty(SYSTEM_PROP_IDEA_VERSION)))
          // this case should be handled by the ideaVendorName condition but keeping it for compatibility reason
          // (ideaVendorName started with 2020.1)
          .orElse(env.sysProperty(SYSTEM_PROP_IDEA_VERSION).map(v => ("IntelliJ IDEA", Some(v))))
          .orElse(env.sysProperty[String]("idea.managed").map(_ => ("IntelliJ IDEA", None)))
          .orElse(env.sysProperty[String]("eclipse.buildId").map(v => ("Eclipse", Some(v))))
          .getOrElse(("Cmd Line", None))

      val ops = Seq(
        ifDefined(env.sysProperty[Unit]("idea.sync.active"))((bs, _) => bs.tag("IDE sync")),
        (bs: BuildScan) => bs.tag(ide),
        ifDefined(version)((bs, v) => bs.tag(s"$ide version $v"))
      )
      Function.chain(ops)
    }

  private val captureCiOrLocal: BuildScan => BuildScan =
    _.tag(if (CiUtils.isCi) "CI" else "LOCAL")

  private val captureJenkinsOrHudson: BuildScan => BuildScan = {
    if (!CiUtils.isJenkins && !CiUtils.isHudson) identity
    else {
      val jobName = env.envVariable[String]("JOB_NAME")
      val buildNumber = env.envVariable[String]("BUILD_NUMBER")
      val ops = Seq(
        (bs: BuildScan) => bs.value("CI provider", if (CiUtils.isJenkins) "Jenkins" else "Hudson"),
        ifDefined(env.envVariable[URL]("BUILD_URL")) { case (bs, url) =>
          val label = if (CiUtils.isJenkins) "Jenkins build" else "Hudson build"
          bs.link(label, url)
        },
        ifDefined(buildNumber)(_.value("CI build number", _)),
        ifDefined(env.envVariable[String]("NODE_NAME"))(withCustomValueAndSearchLink(_, "CI node", _)),
        ifDefined(jobName)(withCustomValueAndSearchLink(_, "CI job", _)),
        ifDefined(env.envVariable[String]("STAGE_NAME"))(withCustomValueAndSearchLink(_, "CI stage", _)),
        ifDefined(buildNumber.zip(jobName).headOption) { case (bs, (build, job)) =>
          val params = Map("CI job" -> job, "CI build number" -> build)
          withSearchLink(bs, "CI pipeline", params)
        }
      )
      Function.chain(ops)
    }
  }

  private val captureTeamCity: BuildScan => BuildScan = {
    if (!CiUtils.isTeamCity) identity
    else {
      def teamCityBuildUrl(properties: Properties) = for {
        buildId <- Utils.getProperty(properties, "teamcity.build.id")
        encodedBuildId <- Utils.urlEncode(buildId)
        configFile <- Utils.getProperty(properties, "teamcity.configuration.properties.file")
        configProperties = Utils.readPropertiesFile(configFile)
        serverUrl <- Utils.getProperty(configProperties, "teamcity.serverUrl")
        buildUrl = s"${Utils.appendIfMissing(serverUrl, '/')}viewLog.html?buildId=$encodedBuildId"
      } yield sbt.url(buildUrl)

      ifDefined(env.envVariable[String]("TEAMCITY_BUILD_PROPERTIES_FILE")) { (bs, teamCityBuildPropertiesFile) =>
        val buildProperties = Utils.readPropertiesFile(teamCityBuildPropertiesFile)
        val ops = Seq(
          (bs: BuildScan) => bs.value("CI provider", "TeamCity"),
          ifDefined(teamCityBuildUrl(buildProperties))(_.link("TeamCity build", _)),
          ifDefined(Utils.getProperty(buildProperties, "build.number"))(_.value("CI build number", _)),
          ifDefined(Utils.getProperty(buildProperties, "teamcity.buildType.id"))(
            withCustomValueAndSearchLink(_, "CI build config", _)
          ),
          ifDefined(Utils.getProperty(buildProperties, "agent.name"))(withCustomValueAndSearchLink(_, "CI agent", _))
        )
        Function.chain(ops)(bs)
      }
    }
  }

  private val captureCircleCi: BuildScan => BuildScan = {
    if (!CiUtils.isCircleCI) identity
    else {
      val ops = Seq(
        (bs: BuildScan) => bs.value("CI provider", "CircleCI"),
        ifDefined(env.envVariable[URL]("CIRCLE_BUILD_URL"))(_.link("CircleCI build", _)),
        ifDefined(env.envVariable[String]("CIRCLE_BUILD_NUM"))(_.value("CircleCI build number", _)),
        ifDefined(env.envVariable[String]("CIRCLE_JOB"))(withCustomValueAndSearchLink(_, "CI job", _)),
        ifDefined(env.envVariable[String]("CIRCLE_WORKFLOW_ID"))(withCustomValueAndSearchLink(_, "CI workflow", _))
      )
      Function.chain(ops)
    }
  }

  private val captureBamboo: BuildScan => BuildScan = {
    if (!CiUtils.isBamboo) identity
    else {
      val ops = Seq(
        (bs: BuildScan) => bs.value("CI provider", "Bamboo"),
        ifDefined(env.envVariable[URL]("bamboo_resultsUrl"))(_.link("Bamboo build", _)),
        ifDefined(env.envVariable[String]("bamboo_buildNumber"))(_.value("CI build number", _)),
        ifDefined(env.envVariable[String]("bamboo_planName"))(withCustomValueAndSearchLink(_, "CI plan", _)),
        ifDefined(env.envVariable[String]("bamboo_buildPlanName"))(withCustomValueAndSearchLink(_, "CI build plan", _)),
        ifDefined(env.envVariable[String]("bamboo_agentId"))(withCustomValueAndSearchLink(_, "CI agent", _))
      )
      Function.chain(ops)
    }
  }

  private val captureGitHubActions: BuildScan => BuildScan = {
    if (!CiUtils.isGitHubActions) identity
    else {
      val buildUrl = for {
        url <- env.envVariable[URL]("GITHUB_SERVER_URL")
        repository <- env.envVariable[String]("GITHUB_REPOSITORY")
        runId <- env.envVariable[String]("GITHUB_RUN_ID")
      } yield sbt.url(s"$url/$repository/actions/runs/$runId")

      val ops = Seq(
        (bs: BuildScan) => bs.value("CI provider", "GitHub Actions"),
        ifDefined(buildUrl)(_.link("GitHub Actions build", _)),
        ifDefined(env.envVariable[String]("GITHUB_WORKFLOW"))(withCustomValueAndSearchLink(_, "CI workflow", _)),
        ifDefined(env.envVariable[String]("GITHUB_RUN_ID"))(withCustomValueAndSearchLink(_, "CI run", _))
      )
      Function.chain(ops)
    }
  }

  private val captureGitLab: BuildScan => BuildScan = {
    if (!CiUtils.isGitLab) identity
    else {
      val ops = Seq(
        (bs: BuildScan) => bs.value("CI provider", "GitLab"),
        ifDefined(env.envVariable[URL]("CI_JOB_URL"))(_.link("GitLab build", _)),
        ifDefined(env.envVariable[URL]("CI_PIPELINE_URL"))(_.link("GitLab pipeline", _)),
        ifDefined(env.envVariable[String]("CI_JOB_NAME"))(withCustomValueAndSearchLink(_, "CI job", _)),
        ifDefined(env.envVariable[String]("CI_JOB_STAGE"))(withCustomValueAndSearchLink(_, "CI stage", _))
      )
      Function.chain(ops)
    }
  }

  private val captureTravis: BuildScan => BuildScan = {
    if (!CiUtils.isTravis) identity
    else {
      val ops = Seq(
        (bs: BuildScan) => bs.value("CI provider", "Travis"),
        ifDefined(env.envVariable[URL]("TRAVIS_BUILD_WEB_URL"))(_.link("Travis build", _)),
        ifDefined(env.envVariable[String]("TRAVIS_BUILD_NUMBER"))(_.value("CI build number", _)),
        ifDefined(env.envVariable[String]("TRAVIS_JOB_NAME"))(withCustomValueAndSearchLink(_, "CI job", _)),
        ifDefined(env.envVariable[String]("TRAVIS_EVENT_TYPE"))(_.tag(_))
      )
      Function.chain(ops)
    }
  }

  private val captureBitrise: BuildScan => BuildScan = {
    if (!CiUtils.isBitrise) identity
    else {
      val ops = Seq(
        (bs: BuildScan) => bs.value("CI provider", "Bitrise"),
        ifDefined(env.envVariable[URL]("BITRISE_BUILD_URL"))(_.link("Bitrise build", _)),
        ifDefined(env.envVariable[String]("BITRISE_BUILD_NUMBER"))(_.value("CI build number", _))
      )
      Function.chain(ops)
    }
  }

  private val captureGoCd: BuildScan => BuildScan = {
    if (!CiUtils.isGoCD) identity
    else {
      val pipelineName = env.envVariable[String]("GO_PIPELINE_NAME")
      val stageName = env.envVariable[String]("GO_STAGE_NAME")
      val jobName = env.envVariable[String]("GO_JOB_NAME")
      val goServerUrl = env.envVariable[URL]("GO_SERVER_URL")
      val goCdBuild = for {
        pipelineName <- pipelineName
        pipelineNumber <- env.envVariable[String]("GO_PIPELINE_COUNTER")
        stageName <- stageName
        stageNumber <- env.envVariable[String]("GO_STAGE_COUNTER")
        jobName <- jobName
        goServerUrl <- goServerUrl
      } yield {
        sbt.url(s"$goServerUrl/tab/build/detail/$pipelineName/$pipelineNumber/$stageName/$stageNumber/$jobName")
      }
      val buildLink = goCdBuild.map(("GoCD build", _)).orElse(goServerUrl.map(("GoCD", _)))

      val ops = Seq(
        (bs: BuildScan) => bs.value("CI provider", "GoCD"),
        ifDefined(buildLink) { case (bs, (label, url)) => bs.link(label, url) },
        ifDefined(pipelineName)(withCustomValueAndSearchLink(_, "CI pipeline", _)),
        ifDefined(jobName)(withCustomValueAndSearchLink(_, "CI job", _)),
        ifDefined(stageName)(withCustomValueAndSearchLink(_, "CI stage", _))
      )
      Function.chain(ops)
    }
  }

  private val captureAzurePipelines: BuildScan => BuildScan = {
    if (!CiUtils.isAzurePipelines) identity
    else {
      val azureServerUrl = env.envVariable[URL]("SYSTEM_TEAMFOUNDATIONCOLLECTIONURI")
      val buildId = env.envVariable[String]("BUILD_BUILDID")
      val buildUrl = for {
        azureServerUrl <- azureServerUrl
        azureProject <- env.envVariable[String]("SYSTEM_TEAMPROJECT")
        buildId <- buildId
      } yield sbt.url(s"$azureServerUrl$azureProject/_build/results?buildId=$buildId")
      val buildLink = buildUrl.map(("Azure Pipelines build", _)).orElse(azureServerUrl.map(("Azure Pipelines", _)))

      val ops = Seq(
        (bs: BuildScan) => bs.value("CI provider", "Azure Pipelines"),
        ifDefined(buildLink) { case (bs, (label, url)) => bs.link(label, url) },
        ifDefined(buildId)(_.value("CI build number", _))
      )
      Function.chain(ops)
    }
  }

  private val captureBuildkite: BuildScan => BuildScan = {
    if (!CiUtils.isBuildkite) identity
    else {
      val prSource = for {
        repository <- env.envVariable[String]("BUILDKITE_PULL_REQUEST_REPO")
        webRepoUri <- Utils.toWebRepoUri(repository)
        prNumber <- env.envVariable[String]("BUILDKITE_PULL_REQUEST")
      } yield sbt.url(s"$webRepoUri/pull/$prNumber")

      val ops = Seq(
        (bs: BuildScan) => bs.value("CI provider", "Buildkite"),
        ifDefined(env.envVariable[URL]("BUILDKITE_BUILD_URL"))(_.link("Buildkite build", _)),
        ifDefined(env.envVariable[String]("BUILDKITE_COMMAND"))(withCustomValueAndSearchLink(_, "CI command", _)),
        ifDefined(env.envVariable[String]("BUILDKITE_BUILD_ID"))(_.value("CI build number", _)),
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
      val gitRepo = Utils.execAndGetStdOut("git", "config", "--get", "remote.origin.url")
      val gitCommitId = Utils.execAndGetStdOut("git", "rev-parse", "--verify", "HEAD")
      val gitCommitShortId = Utils.execAndGetStdOut("git", "rev-parse", "--short=8", "--verify", "HEAD")
      val gitBranchName = getGitBranchName()
      val gitStatus = Utils.execAndGetStdOut("git", "status", "--porcelain")
      val githubRepositoryLink = for {
        githubUrl <- env.envVariable[URL]("GITHUB_SERVER_URL")
        repository <- env.envVariable[String]("GITHUB_REPOSITORY")
        commit <- gitCommitId
      } yield ("GitHub source", sbt.url(s"$githubUrl/$repository/tree/$commit"))
      lazy val webRepo = for {
        origin <- gitRepo
        commit <- gitCommitId
        webRepoUri <- Utils.toWebRepoUri(origin)
        host = webRepoUri.getHost
        if host.contains("github") || host.contains("gitlab")
      } yield {
        if (host.contains("github")) ("GitHub source", sbt.url(s"$webRepoUri/tree/$commit"))
        else ("GitLab source", sbt.url(s"$webRepoUri/-/commit/$commit"))
      }

      val ops = Seq(
        ifDefined(gitRepo)((bs, repo) => bs.value("Git repository", Utils.redactUserInfo(repo))),
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
    val installed = Utils.execAndCheckSuccess("git", "--version")
    if (!installed) logger.info("Git executable missing")
    installed
  }

  private def getGitBranchName(): Option[String] = {
    val branch =
      if (CiUtils.isJenkins || CiUtils.isHudson) env.envVariable[String]("BRANCH_NAME")
      else if (CiUtils.isGitLab) env.envVariable[String]("CI_COMMIT_REF_NAME")
      else if (CiUtils.isAzurePipelines) env.envVariable[String]("BUILD_SOURCEBRANCH")
      else if (CiUtils.isBuildkite) env.envVariable[String]("BUILDKITE_BRANCH")
      else None
    branch.orElse(gitBranchFromGit())
  }

  private def gitBranchFromGit(): Option[String] = Utils.execAndGetStdOut("git", "rev-parse", "--abbrev-ref", "HEAD")

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
        val params = Utils.urlEncode(name).map(n => "&search.names=" + n).getOrElse("") + Utils
          .urlEncode(value)
          .map(v => "&search.values=" + v)
          .getOrElse("")
        val searchParams = params.replaceFirst("&", "")
        val buildScanSelection = Utils.urlEncode("{SCAN_ID}").map(s => "#selection.buildScanB=" + s).getOrElse("")
        val url = Utils.appendIfMissing(server.toString, '/') + "scans?" + searchParams + buildScanSelection
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
