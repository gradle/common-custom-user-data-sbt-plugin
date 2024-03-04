package com.gradle

import com.gradle.develocity.agent.sbt.DevelocityPlugin
import com.gradle.develocity.agent.sbt.DevelocityPlugin.autoImport.DevelocityConfiguration
import com.gradle.internal.{CustomBuildScanEnhancements, ServerConfigTemp, Overrides}
import sbt.Keys._
import sbt._

object SbtCommonCustomUserDataPlugin extends AutoPlugin {

  override def requires: Plugins = com.gradle.develocity.agent.sbt.DevelocityPlugin

  // This plugin is automatically enabled for projects which have DevelocityPlugin.
  override def trigger = allRequirements

  override lazy val buildSettings: Seq[Setting[_]] = Seq(
    DevelocityPlugin.autoImport.develocityConfiguration := applyCCUD(
      DevelocityPlugin.autoImport.develocityConfiguration.value,
      crossScalaVersions.all(ScopeFilter(inAnyProject)).value.flatten.distinct.sorted
    )
  )

  private def applyCCUD(
      currentConfiguration: DevelocityConfiguration,
      scalaVersions: Seq[String]
  ): DevelocityConfiguration = {
    val scan = currentConfiguration.buildScan
    val server = currentConfiguration.server

    val serverConfigTemp = ServerConfigTemp.fromServer(server)
    Overrides.applyTo(serverConfigTemp)

    val newBuildScan =
      new CustomBuildScanEnhancements(serverConfigTemp, scalaVersions.mkString(",")).withAdditionalData(scan)

    currentConfiguration
      .withServer(
        server
          .withUrl(serverConfigTemp.url())
          .withAllowUntrusted(serverConfigTemp.allowUntrusted().getOrElse(server.allowUntrusted))
      )
      .withBuildScan(newBuildScan)
  }
}
