package com.gradle.internal

import java.io.{FileInputStream, IOException, InputStream, UnsupportedEncodingException}
import java.net.{URI, URISyntaxException, URLEncoder}
import java.nio.charset.StandardCharsets
import java.util.Properties
import java.util.regex.Pattern
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.sys.process._

object Utils {

  private val GIT_REPO_URI_REGEX = "^(?:(?:https://|git://)|(?:ssh)?.*?@)(.*?(?:github|gitlab).*?)(?:/|:[0-9]*?/|:)(.*?)(?:\\.git)?$".r

  private[gradle] def sysPropertyOrEnvVariable(sysPropertyName: String): Option[String] = sysPropertyOrEnvVariable(sysPropertyName, toEnvVarName(sysPropertyName))

  private[gradle] def booleanSysPropertyOrEnvVariable(sysPropertyName: String): Option[Boolean] = booleanSysPropertyOrEnvVariable(sysPropertyName, toEnvVarName(sysPropertyName))

  private def toEnvVarName(sysPropertyName: String) = sysPropertyName.toUpperCase.replace('.', '_')

  private[gradle] def sysPropertyOrEnvVariable(sysPropertyName: String, envVarName: String): Option[String] = {
    sysProperty(sysPropertyName).orElse(envVariable(envVarName))
  }

  private[gradle] def booleanSysPropertyOrEnvVariable(sysPropertyName: String, envVarName: String): Option[Boolean] = {
    booleanSysProperty(sysPropertyName).orElse(booleanEnvVariable(envVarName))
  }

  private[gradle] def envVariable(name: String): Option[String] = sys.env.get(name)

  private[gradle] def booleanEnvVariable(name: String): Option[Boolean] = envVariable(name).map(parseBoolean)

  private[gradle] def sysProperty(name: String): Option[String] = sys.props.get(name)

  private[gradle] def booleanSysProperty(name: String): Option[Boolean] = sysProperty(name).map(parseBoolean)

  private[gradle] def isNotEmpty(value: Option[String]): Boolean = value.exists(_.nonEmpty)

  private[gradle] def appendIfMissing(string: String, suffix: Char): String = {
    if (string.nonEmpty && string.charAt(string.length - 1) == suffix) string
    else string + suffix
  }

  private def parseBoolean(string: String): Boolean = try string.toBoolean catch {
    case scala.util.control.NonFatal(_) => false
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
    gitRepoUri match {
      case GIT_REPO_URI_REGEX(host, rawPath) =>
        val path = if (rawPath.startsWith("/")) rawPath else "/" + rawPath
        toUri("https", host, path)
      case _ =>
        None
    }
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

  private[gradle] def readPropertiesFile(name: String) = {
      val input = new FileInputStream(name)
      try {
          val properties = new Properties
          properties.load(input)
          properties
      } catch {
          case e: IOException =>
              throw new RuntimeException(e)
      } finally if (input != null) input.close()
  }
}
