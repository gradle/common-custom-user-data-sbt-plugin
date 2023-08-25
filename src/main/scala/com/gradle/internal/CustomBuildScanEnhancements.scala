package com.gradle.internal

import com.gradle.internal.CiUtils._
import com.gradle.internal.Utils._
import sbt.URL

/**
 * Adds a standard set of useful tags, links and custom values to all build scans published.
 */
object CustomBuildScanEnhancements {

  private val SYSTEM_PROP_IDEA_VENDOR_NAME = "idea.vendor.name"
  private val SYSTEM_PROP_IDEA_VERSION = "idea.version"
  private val SYSTEM_PROP_IDEA_MANAGED = "idea.managed"
  private val SYSTEM_PROP_ECLIPSE_BUILD_ID = "eclipse.buildId"
  private val SYSTEM_PROP_IDEA_SYNC_ACTIVE = "idea.sync.active"

  private val buildScan = CustomBuildScanConfig

  def apply(): Unit = {
    captureOs()
    captureIde()
    captureGitMetadata()
  }

  private def captureOs(): Unit = {
    sysProperty("os.name").foreach(buildScan.tag)
  }

  private def captureIde(): Unit = {
    if (!isCi()) {
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
      else if (sysProperty(SYSTEM_PROP_ECLIPSE_BUILD_ID).isDefined) tagIde("Eclipse", sysProperty(SYSTEM_PROP_ECLIPSE_BUILD_ID).get)
      else buildScan.tag("Cmd Line")

      if (sysProperty(SYSTEM_PROP_IDEA_SYNC_ACTIVE).isDefined) buildScan.tag("IDE sync")
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
    if (isJenkins() || isHudson()) {
      val branch = envVariable("BRANCH_NAME")
      if (branch.isDefined) return branch
    }
    else if (isGitLab()) {
      val branch = envVariable("CI_COMMIT_REF_NAME")
      if (branch.isDefined) return branch
    }
    else if (isAzurePipelines()) {
      val branch = envVariable("BUILD_SOURCEBRANCH")
      if (branch.isDefined) return branch
    }
    else if (isBuildkite()) {
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

  private def addSearchLink(linkLabel: String, vals: Map[String, String]): Unit = {
    // the parameters for a link querying multiple custom values look like:
    // search.names=name1,name2&search.values=value1,value2
    // this reduction groups all names and all values together in order to properly generate the query

    val keys = vals.keys.toList.sorted
    val searchNames = keys.mkString(",")
    val searchValues = keys.map(vals.get).mkString(",")

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

  private def getServer(): Option[URL] = CustomServerConfig.url

}
