package com.gradle.internal

import java.io.{FileInputStream, IOException, UnsupportedEncodingException}
import java.net.{URI, URISyntaxException, URLEncoder}
import java.nio.charset.StandardCharsets
import java.util.Properties
import scala.concurrent.{Await, Future, TimeoutException, blocking}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.sys.process.{ProcessLogger, stringToProcess}

object Utils {

  private val GIT_REPO_URI_REGEX =
    "^(?:(?:https://|git://)|(?:ssh)?.*?@)(.*?(?:github|gitlab).*?)(?:/|:[0-9]*?/|:)(.*?)(?:\\.git)?$".r

  abstract class Env {
    protected def fromEnvironment(name: String): Option[String]
    protected def fromProperties(name: String): Option[String]

    final def sysPropertyOrEnvVariable[T: FromString](name: String): Option[T] = sysPropertyOrEnvVariable(
      Env.Key[T](name)
    )
    final def sysPropertyOrEnvVariable[T: FromString](key: Env.Key[T]): Option[T] =
      sysProperty(key).orElse(envVariable(key))

    final def envVariable[T](key: Env.Key[T])(implicit parse: FromString[T]): Option[T] =
      fromEnvironment(key.envVarName).flatMap(parse(_))
    final def envVariable[T](name: String)(implicit parse: FromString[T]): Option[T] =
      fromEnvironment(name).flatMap(parse(_))

    final def sysProperty[T](key: Env.Key[T])(implicit parse: FromString[T]): Option[T] =
      fromProperties(key.name).flatMap(parse(_))
    final def sysProperty[T](name: String)(implicit parse: FromString[T]): Option[T] =
      fromProperties(name).flatMap(parse(_))

  }

  object Env {
    case class Key[T](name: String) {
      def envVarName: String = toEnvVarName(name)
    }

    object SystemEnvironment extends Env {
      override def fromEnvironment(name: String): Option[String] = sys.env.get(name)
      override def fromProperties(name: String): Option[String] = sys.props.get(name)
    }

    private def toEnvVarName(name: String): String = name.toUpperCase.replace('.', '_')
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
      Await.result(f, Duration(10, "sec")) == 0
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
      if (Await.result(f, Duration(10, "sec")) != 0) None
      else Some(trimAtEnd(output.result())).filter(_.nonEmpty)
    } catch {
      case _: TimeoutException =>
        p.destroy()
        p.exitValue()
        None
      case _ @(_: IOException | _: InterruptedException) => None
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

  private[gradle] def getProperty(properties: Properties, propertyName: String): Option[String] =
    Option(properties.getProperty(propertyName)).filter(_.nonEmpty)
}
