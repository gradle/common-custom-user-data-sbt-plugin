package com.gradle.internal

import com.gradle.internal.Utils.{booleanSysPropertyOrEnvVariable, sysPropertyOrEnvVariable}
import com.gradle.develocity.agent.sbt.api.configuration.Server

object ServerTransformer extends Transformer[Server] {

  // System properties to override Develocity configuration
  private val GRADLE_ENTERPRISE_URL = "gradle.enterprise.url"
  private val GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER = "gradle.enterprise.allowUntrustedServer"

  override def transform(serverConfig: Server): Server = {
    val ops = Seq(
      ifDefined(getAllowUntrusted())(_.withAllowUntrusted(_)),
      ifDefined(getUrl())(_.withUrl(_))
    )

    Function.chain(ops)(serverConfig)
  }

  private def getAllowUntrusted(): Option[Boolean] = booleanSysPropertyOrEnvVariable(
    GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER
  )

  private def getUrl() = sysPropertyOrEnvVariable(GRADLE_ENTERPRISE_URL).map(u => Some(sbt.url(u)))
}
