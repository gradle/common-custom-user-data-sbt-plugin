package com.gradle.internal

import com.gradle.internal.Utils.{envVariable, sysProperty}

object CiUtils {

  private[gradle] lazy val isCi = isGenericCI || isJenkins || isHudson || isTeamCity || isCircleCI || isBamboo || isGitHubActions || isGitLab || isTravis || isBitrise || isGoCD || isAzurePipelines || isBuildkite

  private[gradle] lazy val isGenericCI = envVariable("CI").isDefined || sysProperty("CI").isDefined

  private[gradle] lazy val isJenkins = envVariable("JENKINS_URL").isDefined

  private[gradle] lazy val isHudson = envVariable("HUDSON_URL").isDefined

  private[gradle] lazy val isTeamCity = envVariable("TEAMCITY_VERSION").isDefined

  private[gradle] lazy val isCircleCI = envVariable("CIRCLE_BUILD_URL").isDefined

  private[gradle] lazy val isBamboo = envVariable("bamboo_resultsUrl").isDefined

  private[gradle] lazy val isGitHubActions = envVariable("GITHUB_ACTIONS").isDefined

  private[gradle] lazy val isGitLab = envVariable("GITLAB_CI").isDefined

  private[gradle] lazy val isTravis = envVariable("TRAVIS_JOB_ID").isDefined

  private[gradle] lazy val isBitrise = envVariable("BITRISE_BUILD_URL").isDefined

  private[gradle] lazy val isGoCD = envVariable("GO_SERVER_URL").isDefined

  private[gradle] lazy val isAzurePipelines = envVariable("TF_BUILD").isDefined

  private[gradle] lazy val isBuildkite = envVariable("BUILDKITE").isDefined

}
