package com.company

import com.gradle.Env
import com.gradle.SbtCommonCustomUserDataPlugin
import com.gradle.develocity.agent.sbt.DevelocityPlugin.autoImport.develocityConfiguration

import sbt.{AutoPlugin, Plugins, Keys}
import sbt.Def
import sbt.PluginTrigger

object ExampleCompanyPlugin extends AutoPlugin {

  override def requires: Plugins = SbtCommonCustomUserDataPlugin

  override def trigger: PluginTrigger = allRequirements

  override def buildSettings: Seq[Def.Setting[_]] = Seq(
    develocityConfiguration := {
      val previous = develocityConfiguration.value

      // The simplest example of adding a custom value
      val withSbtVersion = previous.withBuildScan(
        previous.buildScan.withValue("sbt version", Keys.sbtVersion.value)
      )

      // A more complete example, using `Transformers`
      implicit val env: Env = Env.SystemEnvironment
      val transformers = Seq(
        ExampleCompanySetup,
        ScalaVersionsRemover.lift(_.buildScan, _.withBuildScan(_)),
      )

      transformers.reduce(_ andThen _).transform(withSbtVersion)

    }
  )

}
