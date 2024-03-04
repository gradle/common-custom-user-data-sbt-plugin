package com.gradle

import com.gradle.develocity.agent.sbt.DevelocityPlugin
import com.gradle.develocity.agent.sbt.DevelocityPlugin.autoImport._
import com.gradle.internal.{CustomBuildScanEnhancements, Overrides}
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

    val newServer = Overrides.applyTo(server)
    val newBuildScan =
      new CustomBuildScanEnhancements(newServer, scalaVersions.mkString(",")).withAdditionalData(scan)

    currentConfiguration
      .withServer(newServer)
      .withBuildScan(newBuildScan)
  }
}
