package com.gradle.internal

import com.gradle.develocity.agent.sbt.api.configuration.BuildScan
import com.gradle.internal.CiUtils.*
import com.gradle.internal.Utils.*
import sbt.URL

/**
 * Adds a standard set of useful tags, links and custom values to all build scans published.
 */
class CustomBuildScanEnhancements(serverConfig: ServerConfigTemp, scalaVersions: String) {

  private val buildScan = new BuildScanTemp()

  private val SYSTEM_PROP_IDEA_VENDOR_NAME = "idea.vendor.name"
  private val SYSTEM_PROP_IDEA_VERSION = "idea.version"
  private val SYSTEM_PROP_IDEA_MANAGED = "idea.managed"
  private val SYSTEM_PROP_ECLIPSE_BUILD_ID = "eclipse.buildId"
  private val SYSTEM_PROP_IDEA_SYNC_ACTIVE = "idea.sync.active"

  def withAdditionalData(originBuildScan: BuildScan): BuildScan = {
    captureOs()
    captureIde()
    captureCiOrLocal()
    captureCiMetadata()
    captureGitMetadata()

    buildScan.addValue("Scala versions", scalaVersions.mkString(","))

    originBuildScan
      .withTags(buildScan.tags())
      .withValues(buildScan.values())
      .withLinks(buildScan.links())
  }

  private def captureOs(): Unit = {
    sysProperty("os.name").foreach(buildScan.tag)
  }

  private def captureIde(): Unit = {
    if (!isCi) {
      if (sysProperty(SYSTEM_PROP_IDEA_VENDOR_NAME).isDefined) {
        val ideaVendorNameValue = sysProperty(SYSTEM_PROP_IDEA_VENDOR_NAME).get
        if (ideaVendorNameValue == "JetBrains")
          tagIde("IntelliJ IDEA", getOrEmpty(sysProperty(SYSTEM_PROP_IDEA_VERSION)))
      } else if (sysProperty(SYSTEM_PROP_IDEA_VERSION).isDefined) {
        // this case should be handled by the ideaVendorName condition but keeping it for compatibility reason (ideaVendorName started with 2020.1)
        tagIde("IntelliJ IDEA", sysProperty(SYSTEM_PROP_IDEA_VERSION).get)
      } else if (sysProperty(SYSTEM_PROP_IDEA_MANAGED).isDefined) {
        tagIde("IntelliJ IDEA", "")
      } else if (sysProperty(SYSTEM_PROP_ECLIPSE_BUILD_ID).isDefined) {
        tagIde("Eclipse", sysProperty(SYSTEM_PROP_ECLIPSE_BUILD_ID).get)
      } else buildScan.tag("Cmd Line")

      ifDefined(sysProperty(SYSTEM_PROP_IDEA_SYNC_ACTIVE))((_: String) => buildScan.tag("IDE sync"))
    }
  }

  private def captureCiOrLocal(): Unit = {
    buildScan.tag(if (isCi) "CI" else "LOCAL")
  }

  private def captureCiMetadata(): Unit = {
    if (isJenkins || isHudson) {
      val buildUrl: Option[String] = envVariable("BUILD_URL")
      val buildNumber: Option[String] = envVariable("BUILD_NUMBER")
      val nodeName: Option[String] = envVariable("NODE_NAME")
      val jobName: Option[String] = envVariable("JOB_NAME")
      val stageName: Option[String] = envVariable("STAGE_NAME")
      if (buildUrl.isDefined) {
        val label = if (isJenkins) "Jenkins build" else "Hudson build"
        buildScan.link(label, buildUrl.get)
      }
      ifDefined(buildNumber)((value: String) => buildScan.addValue("CI build number", value))
      ifDefined(nodeName)((value: String) => addCustomValueAndSearchLink("CI node", value))
      ifDefined(jobName)((value: String) => addCustomValueAndSearchLink("CI job", value))
      ifDefined(stageName)((value: String) => addCustomValueAndSearchLink("CI stage", value))
      ifDefined(jobName)((job: String) =>
        ifDefined(buildNumber)((build: String) => {
          val params = Map(
            "CI job" -> job,
            "CI build number" -> build
          )
          addSearchLink("CI pipeline", params)
        })
      )
    }

    if (isTeamCity) {
      ifDefined(envVariable("TEAMCITY_BUILD_PROPERTIES_FILE")) { teamcityBuildPropertiesFile =>
        val buildProperties = readPropertiesFile(teamcityBuildPropertiesFile)
        ifDefined(getProperty(buildProperties, "teamcity.build.id")) { teamCityBuildId =>
          ifDefined(getProperty(buildProperties, "teamcity.configuration.properties.file")) { teamcityConfigFile =>
            val configProperties = readPropertiesFile(teamcityConfigFile)
            ifDefined(getProperty(configProperties, "teamcity.serverUrl")) { teamCityServerUrl =>
              val buildUrl: String =
                appendIfMissing(teamCityServerUrl, '/') + "viewLog.html?buildId=" + urlEncode(teamCityBuildId).get
              buildScan.link("TeamCity build", buildUrl)
            }
          }
        }
        ifDefined(getProperty(buildProperties, "build.number")) { teamCityBuildNumber =>
          buildScan.addValue("CI build number", teamCityBuildNumber)
        }
        ifDefined(getProperty(buildProperties, "teamcity.buildType.id")) { teamCityBuildTypeId =>
          addCustomValueAndSearchLink("CI build config", teamCityBuildTypeId)
        }
        ifDefined(getProperty(buildProperties, "agent.name")) { teamCityAgentName =>
          addCustomValueAndSearchLink("CI agent", teamCityAgentName)
        }
      }
    }

    if (isCircleCI) {
      ifDefined(envVariable("CIRCLE_BUILD_URL"))((url: String) => buildScan.link("CircleCI build", url))
      ifDefined(envVariable("CIRCLE_BUILD_NUM"))((value: String) => buildScan.addValue("CI build number", value))
      ifDefined(envVariable("CIRCLE_JOB"))((value: String) => addCustomValueAndSearchLink("CI job", value))
      ifDefined(envVariable("CIRCLE_WORKFLOW_ID"))((value: String) => addCustomValueAndSearchLink("CI workflow", value))
    }

    if (isBamboo) {
      ifDefined(envVariable("bamboo_resultsUrl"))((url: String) => buildScan.link("Bamboo build", url))
      ifDefined(envVariable("bamboo_buildNumber"))((value: String) => buildScan.addValue("CI build number", value))
      ifDefined(envVariable("bamboo_planName"))((value: String) => addCustomValueAndSearchLink("CI plan", value))
      ifDefined(envVariable("bamboo_buildPlanName"))((value: String) =>
        addCustomValueAndSearchLink("CI build plan", value)
      )
      ifDefined(envVariable("bamboo_agentId"))((value: String) => addCustomValueAndSearchLink("CI agent", value))
    }

    if (isGitHubActions) {
      val gitHubUrl: Option[String] = envVariable("GITHUB_SERVER_URL")
      val gitRepository: Option[String] = envVariable("GITHUB_REPOSITORY")
      val gitHubRunId: Option[String] = envVariable("GITHUB_RUN_ID")
      if (gitHubUrl.isDefined && gitRepository.isDefined && gitHubRunId.isDefined) {
        buildScan.link(
          "GitHub Actions build",
          gitHubUrl.get + "/" + gitRepository.get + "/actions/runs/" + gitHubRunId.get
        )
      }
      ifDefined(envVariable("GITHUB_WORKFLOW"))((value: String) => addCustomValueAndSearchLink("CI workflow", value))
      ifDefined(envVariable("GITHUB_RUN_ID"))((value: String) => addCustomValueAndSearchLink("CI run", value))
    }

    if (isGitLab) {
      ifDefined(envVariable("CI_JOB_URL"))((url: String) => buildScan.link("GitLab build", url))
      ifDefined(envVariable("CI_PIPELINE_URL"))((url: String) => buildScan.link("GitLab pipeline", url))
      ifDefined(envVariable("CI_JOB_NAME"))((value: String) => addCustomValueAndSearchLink("CI job", value))
      ifDefined(envVariable("CI_JOB_STAGE"))((value: String) => addCustomValueAndSearchLink("CI stage", value))
    }

    if (isTravis) {
      ifDefined(envVariable("TRAVIS_BUILD_WEB_URL"))((url: String) => buildScan.link("Travis build", url))
      ifDefined(envVariable("TRAVIS_BUILD_NUMBER"))((value: String) => buildScan.addValue("CI build number", value))
      ifDefined(envVariable("TRAVIS_JOB_NAME"))((value: String) => addCustomValueAndSearchLink("CI job", value))
      ifDefined(envVariable("TRAVIS_EVENT_TYPE"))(buildScan.tag)
    }

    if (isBitrise) {
      ifDefined(envVariable("BITRISE_BUILD_URL"))((url: String) => buildScan.link("Bitrise build", url))
      ifDefined(envVariable("BITRISE_BUILD_NUMBER"))((value: String) => buildScan.addValue("CI build number", value))
    }

    if (isGoCD) {
      val pipelineName: Option[String] = envVariable("GO_PIPELINE_NAME")
      val stageName: Option[String] = envVariable("GO_STAGE_NAME")
      val jobName: Option[String] = envVariable("GO_JOB_NAME")
      val goServerUrl: Option[String] = envVariable("GO_SERVER_URL")

      val goCdBuild = for {
        pipelineName <- pipelineName
        pipelineNumber <- envVariable("GO_PIPELINE_COUNTER")
        stageName <- stageName
        stageNumber <- envVariable("GO_STAGE_COUNTER")
        jobName <- jobName
        goServerUrl <- goServerUrl
      } yield {
        s"$goServerUrl/tab/build/detail/$pipelineName/$pipelineNumber/$stageName/$stageNumber/$jobName"
      }

      goCdBuild match {
        case Some(buildUrl) => buildScan.link("GoCD build", buildUrl)
        case None           => envVariable("GO_SERVER_URL").foreach(url => buildScan.link("GoCD", url))
      }

      ifDefined(pipelineName)((value: String) => addCustomValueAndSearchLink("CI pipeline", value))
      ifDefined(jobName)((value: String) => addCustomValueAndSearchLink("CI job", value))
      ifDefined(stageName)((value: String) => addCustomValueAndSearchLink("CI stage", value))
    }

    if (isAzurePipelines) {
      val azureServerUrl: Option[String] = envVariable("SYSTEM_TEAMFOUNDATIONCOLLECTIONURI")
      val buildId: Option[String] = envVariable("BUILD_BUILDID")

      val buildUrl = for {
        azureServerUrl <- azureServerUrl
        azureProject <- envVariable("SYSTEM_TEAMPROJECT")
        buildId <- buildId
      } yield {
        s"$azureServerUrl$azureProject/_build/results?buildId=$buildId"
      }

      buildUrl match {
        case Some(buildUrl) => buildScan.link("Azure Pipelines build", buildUrl)
        case None           => ifDefined(azureServerUrl)((value: String) => buildScan.link("Azure Pipelines", value))
      }
      ifDefined(buildId)((value: String) => buildScan.addValue("CI build number", value))
    }

    if (isBuildkite) {
      ifDefined(envVariable("BUILDKITE_BUILD_URL"))((url: String) => buildScan.link("Buildkite build", url))
      ifDefined(envVariable("BUILDKITE_COMMAND"))((command: String) =>
        addCustomValueAndSearchLink("CI command", command)
      )
      ifDefined(envVariable("BUILDKITE_BUILD_ID"))((id: String) => buildScan.addValue("CI build ID", id))

      for {
        buildkitePrRepo <- envVariable("BUILDKITE_PULL_REQUEST_REPO").flatMap(toWebRepoUri)
        buildkitePrNumber <- envVariable("BUILDKITE_PULL_REQUEST")
      } yield {
        // Create a GitHub link with the pr number and full repo url
        buildScan.link("PR source", s"$buildkitePrRepo/pull/$buildkitePrNumber")
      }
    }
  }

  private def captureGitMetadata(): Unit = {
    if (!isGitInstalled) {
      return
    }

    val gitRepo = execAndGetStdOut("git", "config", "--get", "remote.origin.url")
    val gitCommitId = execAndGetStdOut("git", "rev-parse", "--verify", "HEAD")
    val gitCommitShortId = execAndGetStdOut("git", "rev-parse", "--short=8", "--verify", "HEAD")
    val gitBranchName = getGitBranchName()
    val gitStatus = execAndGetStdOut("git", "status", "--porcelain")

    if (isNotEmpty(gitRepo)) {
      buildScan.addValue("Git repository", redactUserInfo(gitRepo.get))
    }
    if (isNotEmpty(gitCommitId)) {
      buildScan.addValue("Git commit id", gitCommitId.get)
    }
    if (isNotEmpty(gitCommitShortId)) {
      // Ensure server URL is configured by deferring call at execution time
      addCustomValueAndSearchLink("Git commit id", "Git commit id short", gitCommitShortId.get)
    }
    if (isNotEmpty(gitBranchName)) {
      buildScan.tag(gitBranchName.get)
      buildScan.addValue("Git branch", gitBranchName.get)
    }
    if (isNotEmpty(gitStatus)) {
      buildScan.tag("Dirty")
      buildScan.addValue("Git status", gitStatus.get)
    }

    val gitHubUrl = envVariable("GITHUB_SERVER_URL")
    val gitRepository = envVariable("GITHUB_REPOSITORY")
    if (gitHubUrl.isDefined && gitRepository.isDefined && isNotEmpty(gitCommitId)) {
      buildScan.link("GitHub source", gitHubUrl.get + "/" + gitRepository.get + "/tree/" + gitCommitId.get)
    } else {
      if (isNotEmpty(gitRepo) && isNotEmpty(gitCommitId)) {
        val webRepoUri = toWebRepoUri(gitRepo.get)
        webRepoUri.foreach(uri => {
          if (uri.getHost.contains("github")) {
            buildScan.link("GitHub source", uri + "/tree/" + gitCommitId.get)
          } else {
            if (uri.getHost.contains("gitlab")) {
              buildScan.link("GitLab source", uri + "/-/commit/" + gitCommitId.get)
            }
          }
        })
      }
    }
  }

  private lazy val isGitInstalled: Boolean = {
    val installed = execAndCheckSuccess("git", "--version")
    if (!installed) println("[info] Git executable missing")
    installed
  }

  private def getGitBranchName(): Option[String] = {
    if (isJenkins || isHudson) {
      val branch = envVariable("BRANCH_NAME")
      if (branch.isDefined) return branch
    } else if (isGitLab) {
      val branch = envVariable("CI_COMMIT_REF_NAME")
      if (branch.isDefined) return branch
    } else if (isAzurePipelines) {
      val branch = envVariable("BUILD_SOURCEBRANCH")
      if (branch.isDefined) return branch
    } else if (isBuildkite) {
      val branch = envVariable("BUILDKITE_BRANCH")
      if (branch.isDefined) return branch
    }
    execAndGetStdOut("git", "rev-parse", "--abbrev-ref", "HEAD")
  }

  private def getOrEmpty(p: Option[String]) = p.getOrElse("")

  private def tagIde(ideLabel: String, version: String): Unit = {
    buildScan.tag(ideLabel)
    if (version.nonEmpty) buildScan.addValue(ideLabel + " version", version)
  }

  private def addCustomValueAndSearchLink(name: String, value: String): Unit = {
    buildScan.addValue(name, value)
    addSearchLink(name, name, value)
  }

  private def addCustomValueAndSearchLink(linkLabel: String, name: String, value: String): Unit = {
    buildScan.addValue(name, value)
    addSearchLink(linkLabel, name, value)
  }

  private def addSearchLink(linkLabel: String, values: Map[String, String]): Unit = {
    // the parameters for a link querying multiple custom values look like:
    // search.names=name1,name2&search.values=value1,value2
    // this reduction groups all names and all values together in order to properly generate the query

    val keys = values.keys.toList.sorted
    val searchNames = keys.mkString(",")
    val searchValues = keys.map(values.get).mkString(",")

    addSearchLink(linkLabel, searchNames, searchValues)
  }

  private def addSearchLink(linkLabel: String, name: String, value: String): Unit = {
    getServer().foreach { server =>
      val params = urlEncode(name).map(n => "&search.names=" + n).getOrElse("") + urlEncode(value)
        .map(v => "&search.values=" + v)
        .getOrElse("")
      val searchParams = params.replaceFirst("&", "")
      val buildScanSelection = urlEncode("{SCAN_ID}").map(s => "#selection.buildScanB=" + s).getOrElse("")
      val url = appendIfMissing(server.toString, '/') + "scans?" + searchParams + buildScanSelection
      buildScan.link(linkLabel + " build scans", url)
    }
  }

  private def getServer(): Option[URL] = serverConfig.url

  private def ifDefined[A](optional: Option[A])(function: A => Unit): Unit = {
    optional.foreach(function)
  }

}
