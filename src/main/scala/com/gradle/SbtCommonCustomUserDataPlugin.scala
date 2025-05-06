package com.gradle

import com.gradle.develocity.agent.sbt.DevelocityPlugin
import com.gradle.develocity.agent.sbt.DevelocityPlugin.autoImport.{DevelocityConfiguration, develocityConfiguration}
import com.gradle.internal.{CustomBuildScanEnhancements, Overrides}
import sbt.{AutoPlugin, Keys, Logger, Plugins, Setting}

object SbtCommonCustomUserDataPlugin extends AutoPlugin {

  override def requires: Plugins = DevelocityPlugin

  // This plugin is automatically enabled for projects which have DevelocityPlugin.
  override def trigger = allRequirements

  override lazy val buildSettings: Seq[Setting[_]] = Seq(
    develocityConfiguration := {
      applyCCUD(
        Keys.sLog.value,
        develocityConfiguration.value
      )
    }
  )

  private def applyCCUD(
      logger: Logger,
      currentConfiguration: DevelocityConfiguration
  ): DevelocityConfiguration = {
    implicit val env: Env = Env.SystemEnvironment
    val transformers = Seq(
      Overrides.lift(_.server, _.withServer(_)),
      CustomBuildScanEnhancements.transformer(logger)
    )

    transformers
      .reduce(_ andThen _)
      .transform(currentConfiguration)
  }
}
