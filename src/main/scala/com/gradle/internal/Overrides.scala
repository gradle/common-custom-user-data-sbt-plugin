package com.gradle.internal

import com.gradle.internal.Utils.{booleanSysPropertyOrEnvVariable, sysPropertyOrEnvVariable}

object Overrides {

  // System properties to override Develocity configuration
  private val GRADLE_ENTERPRISE_URL = "gradle.enterprise.url"
  private val GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER = "gradle.enterprise.allowUntrustedServer"

  def applyTo(serverConfig: ServerConfigTemp): Unit = {
    sysPropertyOrEnvVariable(GRADLE_ENTERPRISE_URL).foreach(serverConfig.url)
    booleanSysPropertyOrEnvVariable(GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER).foreach(serverConfig.allowUntrusted)
  }
}
