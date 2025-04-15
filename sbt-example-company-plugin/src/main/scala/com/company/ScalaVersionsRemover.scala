package com.company

import com.gradle.Transformer
import com.gradle.Env
import com.gradle.develocity.agent.sbt.api.configuration.BuildScan

object ScalaVersionsRemover extends Transformer[BuildScan] {
  override def transform(in: BuildScan)(implicit env: Env): BuildScan =
    in.withValues(in.values - "Scala versions")
}
