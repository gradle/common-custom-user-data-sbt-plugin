package com.gradle.internal

import com.gradle.internal.CiUtils._
import com.gradle.internal.Utils._
import sbt.URL

import java.net.URI
import scala.collection.mutable

/**
 * Adds a standard set of useful tags, links and custom values to all build scans published.
 */
class CustomBuildScanEnhancements(buildScan: CustomBuildScanConfig, serverConfig: CustomServerConfig) {

  private val SYSTEM_PROP_IDEA_VENDOR_NAME = "idea.vendor.name"
  private val SYSTEM_PROP_IDEA_VERSION = "idea.version"
  private val SYSTEM_PROP_IDEA_MANAGED = "idea.managed"
  private val SYSTEM_PROP_ECLIPSE_BUILD_ID = "eclipse.buildId"
  private val SYSTEM_PROP_IDEA_SYNC_ACTIVE = "idea.sync.active"

  def apply(): Unit = {
    captureOs()
    captureIde()
    captureCiOrLocal()
    captureCiMetadata()
    captureGitMetadata()
  }

  private def captureOs(): Unit = {
    sysProperty("os.name").foreach(buildScan.tag)
  }

  private def captureIde(): Unit = {
    if (!isCi) {
      if (sysProperty(SYSTEM_PROP_IDEA_VENDOR_NAME).isDefined) {
        val ideaVendorNameValue = sysProperty(SYSTEM_PROP_IDEA_VENDOR_NAME).get
        if (ideaVendorNameValue == "JetBrains") tagIde("IntelliJ IDEA", getOrEmpty(sysProperty(SYSTEM_PROP_IDEA_VERSION)))
      }
      else if (sysProperty(SYSTEM_PROP_IDEA_VERSION).isDefined) {
        // this case should be handled by the ideaVendorName condition but keeping it for compatibility reason (ideaVendorName started with 2020.1)
        tagIde("IntelliJ IDEA", sysProperty(SYSTEM_PROP_IDEA_VERSION).get)
      }
      else if (sysProperty(SYSTEM_PROP_IDEA_MANAGED).isDefined) {
        tagIde("IntelliJ IDEA", "")
      }
      else if (sysProperty(SYSTEM_PROP_ECLIPSE_BUILD_ID).isDefined) {
          tagIde("Eclipse", sysProperty(SYSTEM_PROP_ECLIPSE_BUILD_ID).get)
      }
      else buildScan.tag("Cmd Line")

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
              buildScan.link(if (isJenkins) {
                  "Jenkins build"
              }
              else {
                  "Hudson build"
              }, buildUrl.get)
          }
          ifDefined(buildNumber)((value: String) => buildScan.addValue("CI build number", value))
          ifDefined(nodeName)((value: String) => addCustomValueAndSearchLink("CI node", value))
          ifDefined(jobName)((value: String) => addCustomValueAndSearchLink("CI job", value))
          ifDefined(stageName)((value: String) => addCustomValueAndSearchLink("CI stage", value))
          ifDefined(jobName)((job: String) => ifDefined(buildNumber)((build: String) => {
              val params = Map(
                "CI job" -> job,
                "CI build number" -> build
              )
              addSearchLink("CI pipeline", params)
          }))
      }

      if (isTeamCity) {
          val teamcityBuildPropertiesFile = envVariable("TEAMCITY_BUILD_PROPERTIES_FILE")
          if (teamcityBuildPropertiesFile.isDefined) {
              val buildProperties = readPropertiesFile(teamcityBuildPropertiesFile.get)
              val teamCityBuildId = Option.apply(buildProperties.getProperty("teamcity.build.id"))
              if (isNotEmpty(teamCityBuildId)) {
                  val teamcityConfigFile = Option.apply(buildProperties.getProperty("teamcity.configuration.properties.file"))
                  if (isNotEmpty(teamcityConfigFile)) {
                      val configProperties = readPropertiesFile(teamcityConfigFile.get)
                      val teamCityServerUrl = Option.apply(configProperties.getProperty("teamcity.serverUrl"))
                      if (isNotEmpty(teamCityServerUrl)) {
                          val buildUrl: String = appendIfMissing(teamCityServerUrl.get, '/') + "viewLog.html?buildId=" + urlEncode(teamCityBuildId.get).get
                          buildScan.link("TeamCity build", buildUrl)
                      }
                  }
              }
              val teamCityBuildNumber = Option.apply(buildProperties.getProperty("build.number"))
              if (isNotEmpty(teamCityBuildNumber)) {
                  buildScan.addValue("CI build number", teamCityBuildNumber.get)
              }
              val teamCityBuildTypeId = Option.apply(buildProperties.getProperty("teamcity.buildType.id"))
              if (isNotEmpty(teamCityBuildTypeId)) {
                  addCustomValueAndSearchLink("CI build config", teamCityBuildTypeId.get)
              }
              val teamCityAgentName = Option.apply(buildProperties.getProperty("agent.name"))
              if (isNotEmpty(teamCityAgentName)) {
                  addCustomValueAndSearchLink("CI agent", teamCityAgentName.get)
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
          ifDefined(envVariable("bamboo_buildPlanName"))((value: String) => addCustomValueAndSearchLink("CI build plan", value))
          ifDefined(envVariable("bamboo_agentId"))((value: String) => addCustomValueAndSearchLink("CI agent", value))
      }

      if (isGitHubActions) {
          val gitHubUrl: Option[String] = envVariable("GITHUB_SERVER_URL")
          val gitRepository: Option[String] = envVariable("GITHUB_REPOSITORY")
          val gitHubRunId: Option[String] = envVariable("GITHUB_RUN_ID")
          if (gitHubUrl.isDefined && gitRepository.isDefined && gitHubRunId.isDefined) {
              buildScan.link("GitHub Actions build", gitHubUrl.get + "/" + gitRepository.get + "/actions/runs/" + gitHubRunId.get)
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
          val pipelineNumber: Option[String] = envVariable("GO_PIPELINE_COUNTER")
          val stageName: Option[String] = envVariable("GO_STAGE_NAME")
          val stageNumber: Option[String] = envVariable("GO_STAGE_COUNTER")
          val jobName: Option[String] = envVariable("GO_JOB_NAME")
          val goServerUrl: Option[String] = envVariable("GO_SERVER_URL")
          if (List(pipelineName, pipelineNumber, stageName, stageNumber, jobName, goServerUrl).forall( item => item.isDefined )) {
              //noinspection OptionGetWithoutIsPresent
              val buildUrl: String = String.format("%s/tab/build/detail/%s/%s/%s/%s/%s", goServerUrl.get, pipelineName.get, pipelineNumber.get, stageName.get, stageNumber.get, jobName.get)
              buildScan.link("GoCD build", buildUrl)
          }
          else {
              if (goServerUrl.isDefined) {
                  buildScan.link("GoCD", goServerUrl.get)
              }
          }
          ifDefined(pipelineName)((value: String) => addCustomValueAndSearchLink("CI pipeline", value))
          ifDefined(jobName)((value: String) => addCustomValueAndSearchLink("CI job", value))
          ifDefined(stageName)((value: String) => addCustomValueAndSearchLink("CI stage", value))
      }

      if (isAzurePipelines) {
          val azureServerUrl: Option[String] = envVariable("SYSTEM_TEAMFOUNDATIONCOLLECTIONURI")
          val azureProject: Option[String] = envVariable("SYSTEM_TEAMPROJECT")
          val buildId: Option[String] = envVariable("BUILD_BUILDID")
          if (List(azureServerUrl, azureProject, buildId).forall( item => item.isDefined )) {
              //noinspection OptionGetWithoutIsPresent
              val buildUrl: String = String.format("%s%s/_build/results?buildId=%s", azureServerUrl.get, azureProject.get, buildId.get)
              buildScan.link("Azure Pipelines build", buildUrl)
          }
          else {
              if (azureServerUrl.isDefined) {
                  buildScan.link("Azure Pipelines", azureServerUrl.get)
              }
          }
          ifDefined(buildId)((value: String) => buildScan.addValue("CI build number", value))
      }

      if (isBuildkite) {
          ifDefined(envVariable("BUILDKITE_BUILD_URL"))((url: String) => buildScan.link("Buildkite build", url))
          ifDefined(envVariable("BUILDKITE_COMMAND"))((command: String) => addCustomValueAndSearchLink("CI command", command))
          ifDefined(envVariable("BUILDKITE_BUILD_ID"))((id: String) => buildScan.addValue("CI build ID", id))
          val buildkitePrRepo: Option[String] = envVariable("BUILDKITE_PULL_REQUEST_REPO")
          val buildkitePrNumber: Option[String] = envVariable("BUILDKITE_PULL_REQUEST")
          if (buildkitePrRepo.isDefined && buildkitePrNumber.isDefined) {
              // Create a GitHub link with the pr number and full repo url
              val prNumber: String = buildkitePrNumber.get
              ifDefined(toWebRepoUri(buildkitePrRepo.get))((url: URI) => buildScan.link("PR source", url + "/pull/" + prNumber))
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
    }
    else {
      if (isNotEmpty(gitRepo) && isNotEmpty(gitCommitId)) {
        val webRepoUri = toWebRepoUri(gitRepo.get)
        webRepoUri.foreach(uri => {
          if (uri.getHost.contains("github")) {
            buildScan.link("GitHub source", uri + "/tree/" + gitCommitId.get)
          }
          else {
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
    }
    else if (isGitLab) {
      val branch = envVariable("CI_COMMIT_REF_NAME")
      if (branch.isDefined) return branch
    }
    else if (isAzurePipelines) {
      val branch = envVariable("BUILD_SOURCEBRANCH")
      if (branch.isDefined) return branch
    }
    else if (isBuildkite) {
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
    val server = getServer()
    if (server.isDefined) {
      val params = urlEncode(name).map(n => "&search.names=" + n).getOrElse("") + urlEncode(value).map(v => "&search.values=" + v).getOrElse("")
      val searchParams = params.replaceFirst("&", "")
      val buildScanSelection = urlEncode("{SCAN_ID}").map(s => "#selection.buildScanB=" + s).getOrElse("")
      val url = appendIfMissing(server.get.toString, '/') + "scans?" + searchParams + buildScanSelection
      buildScan.link(linkLabel + " build scans", url)
    }
  }

  private def getServer(): Option[URL] = serverConfig.url

  private def ifDefined[A](optional: Option[A])(function: A => Unit): Unit = {
    optional.foreach(function)
  }

}
