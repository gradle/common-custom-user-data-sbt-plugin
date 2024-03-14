package com.gradle.internal

import com.gradle.internal.Utils.Env

object CiUtils {

  private[gradle] def isCi(implicit env: Env): Boolean =
    isGenericCI || isJenkins || isHudson || isTeamCity || isCircleCI || isBamboo || isGitHubActions || isGitLab || isTravis || isBitrise || isGoCD || isAzurePipelines || isBuildkite

  private[gradle] def isGenericCI(implicit env: Env) = env.sysPropertyOrEnvVariable[Unit]("CI").isDefined

  private[gradle] def isJenkins(implicit env: Env) = env.sysPropertyOrEnvVariable[Unit]("JENKINS_URL").isDefined

  private[gradle] def isHudson(implicit env: Env) = env.sysPropertyOrEnvVariable[Unit]("HUDSON_URL").isDefined

  private[gradle] def isTeamCity(implicit env: Env) = env.sysPropertyOrEnvVariable[Unit]("TEAMCITY_VERSION").isDefined

  private[gradle] def isCircleCI(implicit env: Env) = env.sysPropertyOrEnvVariable[Unit]("CIRCLE_BUILD_URL").isDefined

  private[gradle] def isBamboo(implicit env: Env) = env.sysPropertyOrEnvVariable[Unit]("bamboo_resultsUrl").isDefined

  private[gradle] def isGitHubActions(implicit env: Env) =
    env.sysPropertyOrEnvVariable[Unit]("GITHUB_ACTIONS").isDefined

  private[gradle] def isGitLab(implicit env: Env) = env.sysPropertyOrEnvVariable[Unit]("GITLAB_CI").isDefined

  private[gradle] def isTravis(implicit env: Env) = env.sysPropertyOrEnvVariable[Unit]("TRAVIS_JOB_ID").isDefined

  private[gradle] def isBitrise(implicit env: Env) = env.sysPropertyOrEnvVariable[Unit]("BITRISE_BUILD_URL").isDefined

  private[gradle] def isGoCD(implicit env: Env) = env.sysPropertyOrEnvVariable[Unit]("GO_SERVER_URL").isDefined

  private[gradle] def isAzurePipelines(implicit env: Env) = env.sysPropertyOrEnvVariable[Unit]("TF_BUILD").isDefined

  private[gradle] def isBuildkite(implicit env: Env) = env.sysPropertyOrEnvVariable[Unit]("BUILDKITE").isDefined

}
