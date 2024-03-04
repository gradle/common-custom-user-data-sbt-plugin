package com.gradle.internal

import com.gradle.internal.Utils.{booleanSysPropertyOrEnvVariable, sysPropertyOrEnvVariable}
import com.gradle.develocity.agent.sbt.api.configuration.Server
import sbt.{url, URL}

object Overrides {

  // System properties to override Develocity configuration
  private val GRADLE_ENTERPRISE_URL = "gradle.enterprise.url"
  private val GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER = "gradle.enterprise.allowUntrustedServer"

  def applyTo(serverConfig: Server): Server = {
    val applyUntrusted = applyOverride(getAllowUntrusted())(_.withAllowUntrusted(_))
    val applyUrl = applyOverride(getUrl())(_.withUrl(_))
    (applyUntrusted andThen applyUrl)(serverConfig)
  }

  private def applyOverride[T](v: Option[T])(fn: (Server, T) => Server): Server => Server =
    v match {
      case None        => identity
      case Some(value) => server => fn(server, value)
    }

  private def getAllowUntrusted(): Option[Boolean] = booleanSysPropertyOrEnvVariable(
    GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER
  )

  private def getUrl(): Option[Some[URL]] = sysPropertyOrEnvVariable(GRADLE_ENTERPRISE_URL).map(u => Some(url(u)))
}
