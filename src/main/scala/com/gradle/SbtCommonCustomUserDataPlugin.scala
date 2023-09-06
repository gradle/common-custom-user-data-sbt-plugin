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

    val customServerConfig = new CustomServerConfig().fromServer(server)
    val customBuildScanConfig = new CustomBuildScanConfig()
    customBuildScanConfig.addValue("Scala version", scalaVersion)

    new Overrides(customServerConfig).apply()
    new CustomBuildScanEnhancements(customBuildScanConfig, customServerConfig).apply()

    currentConfiguration.copy(
      buildScan = scan.copy(
        tags = scan.tags ++ customBuildScanConfig.tags,
        links = scan.links ++ customBuildScanConfig.links,
        values = scan.values ++ customBuildScanConfig.values
      ),
      server = server.copy(
        url = customServerConfig.url.orElse(server.url),
        allowUntrusted = customServerConfig.allowUntrusted.getOrElse(server.allowUntrusted)
      )
    )
  }
}
