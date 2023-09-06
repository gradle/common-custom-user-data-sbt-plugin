package com.gradle.internal

import com.gradle.internal.Utils.{booleanSysPropertyOrEnvVariable, sysPropertyOrEnvVariable}

class Overrides(serverConfig: CustomServerConfig) {

  // system properties to override Gradle Enterprise configuration
  private val GRADLE_ENTERPRISE_URL = "gradle.enterprise.url"
  private val GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER = "gradle.enterprise.allowUntrustedServer"

  def apply(): Unit = {
    sysPropertyOrEnvVariable(GRADLE_ENTERPRISE_URL).foreach(url => serverConfig.url = url)
    booleanSysPropertyOrEnvVariable(GRADLE_ENTERPRISE_ALLOW_UNTRUSTED_SERVER).foreach(allowUntrusted => serverConfig.allowUntrusted = allowUntrusted)
  }
}
