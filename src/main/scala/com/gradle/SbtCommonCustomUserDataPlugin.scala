package com.gradle

import com.gradle.develocity.agent.sbt.DevelocityPlugin
import com.gradle.develocity.agent.sbt.DevelocityPlugin.autoImport.DevelocityConfiguration
import com.gradle.internal.{CustomBuildScanConfig, CustomBuildScanEnhancements, CustomServerConfig, Overrides}
import sbt.Keys._
import sbt._

object SbtCommonCustomUserDataPlugin extends AutoPlugin {

  override def requires: Plugins = com.gradle.develocity.agent.sbt.DevelocityPlugin

  // This plugin is automatically enabled for projects which have DevelocityPlugin.
  override def trigger = allRequirements

  override lazy val buildSettings: Seq[Setting[_]] = Seq(
    DevelocityPlugin.autoImport.develocityConfiguration := applyCCUD(
      DevelocityPlugin.autoImport.develocityConfiguration.value,
      crossScalaVersions.all(ScopeFilter(inAnyProject)).value.flatten.distinct.sorted)
  )

  private def applyCCUD(currentConfiguration: DevelocityConfiguration, scalaVersions: Seq[String]): DevelocityConfiguration = {
    val scan = currentConfiguration.buildScan
    val server = currentConfiguration.server

    val customServerConfig = CustomServerConfig.fromServer(server)
    val customBuildScanConfig = new CustomBuildScanConfig()
    customBuildScanConfig.addValue("Scala versions", scalaVersions.mkString(","))

    Overrides.apply(customServerConfig)
    new CustomBuildScanEnhancements(customBuildScanConfig, customServerConfig).apply()


    currentConfiguration.withServer(server
        .withUrl(customServerConfig.url().orElse(server.url))
        .withAllowUntrusted(customServerConfig.allowUntrusted().getOrElse(server.allowUntrusted))
      )
      .withBuildScan(scan
        .withTags(customBuildScanConfig.tags())
        .withValues(customBuildScanConfig.values())
        .withLinks(customBuildScanConfig.links())
      )
  }
}
