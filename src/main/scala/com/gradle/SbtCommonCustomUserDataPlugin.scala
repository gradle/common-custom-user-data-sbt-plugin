package com.gradle

import com.gradle.develocity.agent.sbt.DevelocityPlugin
import com.gradle.develocity.agent.sbt.DevelocityPlugin.autoImport.{DevelocityConfiguration, develocityConfiguration}
import com.gradle.internal.{CustomBuildScanEnhancements, Overrides}
import sbt.{AttributeKey, AutoPlugin, BuiltinCommands, Keys, Logger, Plugins, Project, Setting, ThisBuild, sbtSlashSyntaxRichReference}

object SbtCommonCustomUserDataPlugin extends AutoPlugin {

  override def requires: Plugins = DevelocityPlugin

  // This plugin is automatically enabled for projects which have DevelocityPlugin.
  override def trigger = allRequirements

  private val CcudApplied = AttributeKey[Unit]("ccud-applied", "Marker attribute to indicate that common custom user data has been applied")
  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    Keys.onLoad := {
      val original = Keys.onLoad.value
      original andThen { state =>
        state.get(CcudApplied) match {
          case Some(_) =>
            state // already applied
          case None =>
            val extracted = Project.extract(state)
            val develocityConfigurationKey = ThisBuild / develocityConfiguration
            val configuration = extracted.get(develocityConfigurationKey)
            val newConfiguration = applyCCUD(state.log, configuration)
            val newSettings = Project.setAll(extracted, Seq(ThisBuild / develocityConfigurationKey := newConfiguration))
            BuiltinCommands.reapply(newSettings, extracted.structure, state.put(CcudApplied, ()))
        }
      }
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
