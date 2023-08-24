package com.gradle.internal

import com.gradle.internal.Utils.{envVariable, sysProperty}

object CiUtils {

  private[gradle] def isCi() = isGenericCI() || isJenkins() || isHudson() || isTeamCity() || isCircleCI() || isBamboo() || isGitHubActions() || isGitLab() || isTravis() || isBitrise() || isGoCD() || isAzurePipelines() || isBuildkite()

  private[gradle] def isGenericCI() = envVariable("CI").isDefined || sysProperty("CI").isDefined

  private[gradle] def isJenkins() = envVariable("JENKINS_URL").isDefined

  private[gradle] def isHudson() = envVariable("HUDSON_URL").isDefined

  private[gradle] def isTeamCity() = envVariable("TEAMCITY_VERSION").isDefined

  private[gradle] def isCircleCI() = envVariable("CIRCLE_BUILD_URL").isDefined

  private[gradle] def isBamboo() = envVariable("bamboo_resultsUrl").isDefined

  private[gradle] def isGitHubActions() = envVariable("GITHUB_ACTIONS").isDefined

  private[gradle] def isGitLab() = envVariable("GITLAB_CI").isDefined

  private[gradle] def isTravis() = envVariable("TRAVIS_JOB_ID").isDefined

  private[gradle] def isBitrise() = envVariable("BITRISE_BUILD_URL").isDefined

  private[gradle] def isGoCD() = envVariable("GO_SERVER_URL").isDefined

  private[gradle] def isAzurePipelines() = envVariable("TF_BUILD").isDefined

  private[gradle] def isBuildkite() = envVariable("BUILDKITE").isDefined

}
