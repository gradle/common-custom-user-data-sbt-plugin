package com.gradle

import com.gradle.develocity.agent.sbt.DevelocityPlugin
import com.gradle.develocity.agent.sbt.DevelocityPlugin.autoImport._
import com.gradle.internal.{CustomBuildScanEnhancements, ServerConfigTemp, Overrides}
import sbt.Keys._
import sbt._

object SbtCommonCustomUserDataPlugin extends AutoPlugin {

  override def requires: Plugins = DevelocityPlugin

  // This plugin is automatically enabled for projects which have DevelocityPlugin.
  override def trigger = allRequirements

  override lazy val buildSettings: Seq[Setting[_]] = Seq(
    DevelocityPlugin.autoImport.develocityConfiguration := {
      val allScalaVersions = crossScalaVersions.all(ScopeFilter(inAnyProject)).value.flatten.distinct.sorted
      applyCCUD(
        DevelocityPlugin.autoImport.develocityConfiguration.value,
        allScalaVersions
      )
    }
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
