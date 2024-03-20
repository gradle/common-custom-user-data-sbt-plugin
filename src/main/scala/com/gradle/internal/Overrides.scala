package com.gradle.internal

import com.gradle.develocity.agent.sbt.api.configuration.Server
import com.gradle.internal.Utils.Env
import java.net.URL

class Overrides(implicit env: Env) extends Transformer[Server] {

  // System properties to override Develocity configuration
  private val serverUrl = Env.Key[URL]("gradle.enterprise.url")
  private val allowUntrustedServer = Env.Key[Boolean]("develocity.allowUntrustedServer")

  override def transform(serverConfig: Server): Server = {
    val ops = Seq(
      ifDefined(env.sysPropertyOrEnvVariable(allowUntrustedServer))(_.withAllowUntrusted(_)),
      ifDefined(env.sysPropertyOrEnvVariable(serverUrl))((bs, url) => bs.withUrl(Some(url)))
    )
    Function.chain(ops)(serverConfig)
  }
}
