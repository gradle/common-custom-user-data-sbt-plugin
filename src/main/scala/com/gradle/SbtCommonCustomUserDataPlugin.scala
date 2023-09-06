package com.gradle

import com.gradle.enterprise.sbt.GradleEnterprisePlugin
import com.gradle.enterprise.sbt.GradleEnterprisePlugin.autoImport.GradleEnterpriseConfiguration
import com.gradle.internal.{CustomBuildScanConfig, CustomBuildScanEnhancements, CustomServerConfig, Overrides}
import sbt.Keys._
import sbt._

object SbtCommonCustomUserDataPlugin extends AutoPlugin {

  override def requires: Plugins = com.gradle.enterprise.sbt.GradleEnterprisePlugin

  // This plugin is automatically enabled for projects which have GradleEnterprisePlugin.
  override def trigger = allRequirements

  override lazy val buildSettings: Seq[Setting[_]] = Seq(
    GradleEnterprisePlugin.autoImport.gradleEnterpriseConfiguration := applyCCUD(
      GradleEnterprisePlugin.autoImport.gradleEnterpriseConfiguration.value,
      scalaVersion.value)
  )

  private def applyCCUD(currentConfiguration: GradleEnterpriseConfiguration, scalaVersion: String): GradleEnterpriseConfiguration = {
    val scan = currentConfiguration.buildScan
    val server = currentConfiguration.server

    CustomServerConfig.fromServer(server)
    Overrides.apply()
    CustomBuildScanConfig.addValue("Scala version", scalaVersion)
    CustomBuildScanEnhancements.apply()

    currentConfiguration.copy(
      buildScan = scan.copy(
        tags = scan.tags ++ CustomBuildScanConfig.tags,
        links = scan.links ++ CustomBuildScanConfig.links,
        values = scan.values ++ CustomBuildScanConfig.values
      ),
      server = server.copy(
        url = CustomServerConfig.url.orElse(server.url),
        allowUntrusted = CustomServerConfig.allowUntrusted.getOrElse(server.allowUntrusted)
      )
    )
  }
}
