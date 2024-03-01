package com.gradle.internal

import com.gradle.internal.Utils.{booleanSysPropertyOrEnvVariable, sysPropertyOrEnvVariable}

object Overrides {

  // system properties to override Gradle Enterprise configuration
  private val GRADLE_ENTERPRISE_URL = "gradle.enterprise.url"
  private val GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER = "gradle.enterprise.allowUntrustedServer"

  def apply(serverConfig: CustomServerConfig): Unit = {
    sysPropertyOrEnvVariable(GRADLE_ENTERPRISE_URL).foreach(serverConfig.url)
    booleanSysPropertyOrEnvVariable(GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER).foreach(serverConfig.allowUntrusted)
  }
}
