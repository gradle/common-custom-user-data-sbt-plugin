package com.gradle

import com.gradle.enterprise.sbt.GradleEnterprisePlugin
import com.gradle.enterprise.sbt.GradleEnterprisePlugin.autoImport.GradleEnterpriseConfiguration
import com.gradle.internal.{CustomBuildScanConfig, CustomBuildScanEnhancements, CustomServerConfig, Overrides}
import sbt.Keys._
import sbt._

object CommonCustomUserDataScalaPlugin extends AutoPlugin {

  object autoImport {
    val helloGreeting = settingKey[String]("greeting")
    val hello = taskKey[Unit]("say hello")
  }

  //    val autoImport = GradleEnterprisePlugin.autoImport

  import autoImport._

  override def requires: Plugins = com.gradle.enterprise.sbt.GradleEnterprisePlugin

  // This plugin is automatically enabled for projects which are GradleEnterprisePlugin.
  override def trigger = allRequirements

  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    helloGreeting := "hi from CCUD plugin"
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    hello := {
      val s = streams.value
      val g = helloGreeting.value
      s.log.info(g)
    },
    GradleEnterprisePlugin.autoImport.gradleEnterpriseConfiguration := applyCCUD(
      GradleEnterprisePlugin.autoImport.gradleEnterpriseConfiguration.value,
      scalaVersion.value)
  )

  private def applyCCUD(currentConfiguration: GradleEnterpriseConfiguration, scalaVersion: String): GradleEnterpriseConfiguration = {
    val scan = currentConfiguration.buildScan
    val server = currentConfiguration.server

    CustomBuildScanConfig.addValue("Scala version", scalaVersion)
    CustomBuildScanEnhancements.apply()
    Overrides.apply()

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
