package com.company

import com.gradle.Transformer
import com.gradle.develocity.agent.sbt.api.configuration.DevelocityConfiguration
import com.gradle.Env

object ExampleCompanySetup extends Transformer[DevelocityConfiguration] {
  private val ciKey = Env.Key[Boolean]("ci")

  override def transform(in: DevelocityConfiguration)(implicit env: Env): DevelocityConfiguration = {
    val isCi = env.envVariable(ciKey).isDefined

    in
      .withTestRetry(
        in.testRetry
          // Configure retries in CI only
          .withMaxRetries(if (isCi) 3 else 0)
          // Don't retry if there are more than 20 failures
          .withMaxFailures(20)
      )
      .withBuildScan(
        in.buildScan
          .withTag("company-custom-tag")
          .withLink("Company Homepage", sbt.url("https://company.com"))
      )
  }
}
