package com.gradle

import sbt.Keys._
import sbt._

object CommonCustomUserDataScalaPlugin extends AutoPlugin {

    object autoImport {
        val helloGreeting = settingKey[String]("greeting")
        val hello = taskKey[Unit]("say hello")
    }

    import autoImport._

    override def requires: Plugins = com.gradle.enterprise.sbt.GradleEnterprisePlugin

    // This plugin is automatically enabled for projects which are GradleEnterprisePlugin.
    override def trigger = allRequirements

    override lazy val globalSettings: Seq[Setting[_]] = Seq(
        helloGreeting := "hi",
    )

    override lazy val projectSettings: Seq[Setting[_]] = Seq(
        hello := {
            val s = streams.value
            val g = helloGreeting.value
            s.log.info(g)
        }
    )
}
