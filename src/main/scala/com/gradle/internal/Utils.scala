package com.gradle.internal

import java.io.{IOException, UnsupportedEncodingException}
import java.net.{URI, URISyntaxException, URLEncoder}
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.sys.process._

object Utils {

  private val GIT_REPO_URI_PATTERN = Pattern.compile("^(?:(?:https://|git://)|(?:ssh)?.*?@)(.*?(?:github|gitlab).*?)(?:/|:[0-9]*?/|:)(.*?)(?:\\.git)?$")

  private[gradle] def sysPropertyOrEnvVariable(sysPropertyName: String): Option[String] = sysPropertyOrEnvVariable(sysPropertyName, toEnvVarName(sysPropertyName))

  private[gradle] def booleanSysPropertyOrEnvVariable(sysPropertyName: String): Option[Boolean] = booleanSysPropertyOrEnvVariable(sysPropertyName, toEnvVarName(sysPropertyName))

  private def toEnvVarName(sysPropertyName: String) = sysPropertyName.toUpperCase.replace('.', '_')

  private[gradle] def sysPropertyOrEnvVariable(sysPropertyName: String, envVarName: String): Option[String] = {
    val prop = sysProperty(sysPropertyName)
    if (prop.isDefined) prop else envVariable(envVarName)
  }

  private[gradle] def booleanSysPropertyOrEnvVariable(sysPropertyName: String, envVarName: String): Option[Boolean] = {
    val prop = booleanSysProperty(sysPropertyName)
    if (prop.isDefined) prop else booleanEnvVariable(envVarName)
  }

  private[gradle] def envVariable(name: String): Option[String] = sys.env.get(name)

  private[gradle] def booleanEnvVariable(name: String): Option[Boolean] = envVariable(name).map(_.toBoolean)

  private[gradle] def sysProperty(name: String): Option[String] = sys.props.get(name)

  private[gradle] def booleanSysProperty(name: String): Option[Boolean] = sysProperty(name).map(_.toBoolean)

  private[gradle] def isNotEmpty(value: Option[String]): Boolean = {
    value.isDefined && value.get.nonEmpty
  }

  private[gradle] def appendIfMissing(string: String, suffix: Char): String = {
    if (string.nonEmpty && string.charAt(string.length - 1) == suffix) string
    else string + suffix
  }

  private def trimAtEnd(str: String) = ('x' + str).trim.substring(1)

  private[gradle] def urlEncode(str: String) = {
    try {
      val encodedUrl = URLEncoder.encode(str, StandardCharsets.UTF_8.name)
      Some(encodedUrl)
    } catch {
      case _: UnsupportedEncodingException => None
    }
  }

  /**
   * Construct a repo URI from a git URL in the format of
   * <code>git://github.com/acme-inc/my-project.git</code>. If the URL cannot be parsed, None is
   * returned.
   * <p>
   * The scheme can be any of <code>git://</code>, <code>https://</code>, or <code>ssh</code>.
   */
  private[gradle] def toWebRepoUri(gitRepoUri: String): Option[URI] = {
    val matcher = GIT_REPO_URI_PATTERN.matcher(gitRepoUri)
    if (matcher.matches) {
      val scheme = "https"
      val host = matcher.group(1)
      val path = if (matcher.group(2).startsWith("/")) matcher.group(2)
      else "/" + matcher.group(2)
      toUri(scheme, host, path)
    }
    else None
  }

  private def toUri(scheme: String, host: String, path: String): Option[URI] = {
    try {
      Some(new URI(scheme, host, path, null))
    } catch {
      case _: URISyntaxException => None
    }
  }

  private[gradle] def redactUserInfo(url: String) = try {
    val userInfo = new URI(url).getUserInfo
    if (userInfo == null) url
    else url.replace(userInfo + '@', "******@")
  } catch {
    case _: URISyntaxException => url
  }

  private[gradle] def execAndCheckSuccess(args: String*): Boolean = {
    val cmd = args.mkString(" ")
    val p = cmd.run() // start asynchronously
    val f = Future(blocking(p.exitValue())) // wrap in Future
    try {
      Await.result(f, duration.Duration(10, "sec")) == 0
    } catch {
      case _: TimeoutException =>
        p.destroy()
        p.exitValue()
        false
    }
  }

  private[gradle] def execAndGetStdOut(args: String*): Option[String] = {
    val cmd = args.mkString(" ")

    val output = StringBuilder.newBuilder
    val logger = new ProcessLogger {
      def out(s: => String): Unit = {
        output.append(s).append("\n")
      }

      def err(s: => String): Unit = {} // ignore

      def buffer[T](f: => T): T = f
    }
    val p = cmd.run(logger) // start asynchronously
    val f = Future(blocking(p.exitValue())) // wrap in Future
    try {
      if (Await.result(f, duration.Duration(10, "sec")) != 0) None
      else Some(trimAtEnd(output.result()))
    } catch {
      case _: TimeoutException =>
        p.destroy()
        p.exitValue()
        None
      case _@(_: IOException | _: InterruptedException) => None
    }
  }
}
