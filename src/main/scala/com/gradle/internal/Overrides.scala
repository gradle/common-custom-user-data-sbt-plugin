package com.gradle.internal

import com.gradle.develocity.agent.sbt.api.configuration.Server
import java.net.URL

object Overrides extends Transformer[Server] {

  // System properties to override Develocity configuration
  private val serverUrl = Env.Key[URL]("develocity.url")
  private val allowUntrustedServer = Env.Key[Boolean]("develocity.allowUntrustedServer")

  override def transform(serverConfig: Server)(implicit env: Env): Server = {
    val ops = Seq(
      ifDefined(env.sysPropertyOrEnvVariable(allowUntrustedServer))(_.withAllowUntrusted(_)),
      ifDefined(env.sysPropertyOrEnvVariable(serverUrl))((bs, url) => bs.withUrl(Some(url)))
    )
    Function.chain(ops)(serverConfig)
  }
}
