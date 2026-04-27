package com.gradle

import com.gradle.develocity.agent.sbt.DevelocityPlugin
import com.gradle.develocity.agent.sbt.DevelocityPlugin.autoImport.{DevelocityConfiguration, develocityConfiguration}
import com.gradle.internal.{CustomBuildScanEnhancements, Overrides}
import sbt.{AutoPlugin, Keys, Logger, Plugins, Setting, settingKey}

object SbtCommonCustomUserDataPlugin extends AutoPlugin {

  override def requires: Plugins = DevelocityPlugin

  // This plugin is automatically enabled for projects which have DevelocityPlugin.
  override def trigger = allRequirements

  // This key is defined in sbt-develocity 1.4.5 and later. sbt compares keys by name only,
  // so the re-definition will actually point to the same key as in sbt-develocity.
  // If the project uses an older version of sbt-develocity, then this key will be bound to
  // no value, allowing us to detect the older version.
  private val internalDevelocityConfigurationTransformers =
    settingKey[Seq[DevelocityConfiguration => DevelocityConfiguration]](
      "Internal transformers to apply to the Develocity configuration."
    )

  override lazy val buildSettings: Seq[Setting[_]] = Seq(
    // If the build knows about internalDevelocityConfigurationTransformers, then it means that
    // we are using sbt-develocity 1.4.5 or later, so we can add our transformer to the list.
    // Otherwise, we are using an older version, and we will apply our transformation directly.
    internalDevelocityConfigurationTransformers := {
      val logger = Keys.sLog.value
      internalDevelocityConfigurationTransformers.?.value match {
        case Some(transformers) => applyCCUD(logger) +: transformers
        case None               => Nil
      }
    },
    // At this point, if the CcudTransformers is not already present, we know we use an older
    // version of sbt-develocity, so we apply our transformation directly.
    develocityConfiguration := {
      val logger = Keys.sLog.value
      internalDevelocityConfigurationTransformers.?.value match {
        case Some(transformers) if transformers.exists(_.isInstanceOf[CcudTransform]) =>
          develocityConfiguration.value
        case _ =>
          applyCCUD(logger)(develocityConfiguration.value)
      }
    }
  )

  private def applyCCUD(
      logger: Logger
  ): CcudTransform = {
    val op = (configuration: DevelocityConfiguration) => {
      implicit val env: Env = Env.SystemEnvironment
      val transformers = Seq(
        Overrides.lift(_.server, _.withServer(_)),
        CustomBuildScanEnhancements.transformer(logger)
      )

      transformers
        .reduce(_ andThen _)
        .transform(configuration)
    }

    new CcudTransform(op)
  }

  /** A wrapper class that allows us to identify our transformer in a list. */
  private class CcudTransform(op: DevelocityConfiguration => DevelocityConfiguration)
      extends (DevelocityConfiguration => DevelocityConfiguration) {
    override def apply(v: DevelocityConfiguration): DevelocityConfiguration = op(v)
  }
}
