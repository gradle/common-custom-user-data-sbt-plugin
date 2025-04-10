package com.gradle.internal

import java.io.{File, UnsupportedEncodingException}
import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import java.util.Properties
import scala.concurrent.{Await, Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.sys.process.{BasicIO, ProcessIO, ProcessLogger, stringToProcess}
import scala.util.Try
import sbt.io.Using
import java.util.concurrent.TimeUnit

object Utils {

  private val GIT_REPO_URI_REGEX =
    "^(?:(?:https://|git://)(?:.+:.+@)?|(?:ssh)?.*?@)(.*?(?:github|gitlab).*?)(?:/|:[0-9]*?/|:)(.*?)(?:\\.git)?$".r

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
    Try(new URI(scheme, host, path, null)).toOption
  }

  private[gradle] def redactUserInfo(url: String): String = {
    Try(new URI(url)).toOption.flatMap(uri => Option(uri.getUserInfo)) match {
      case None       => url
      case Some(info) => url.replace(info + '@', "******@")
    }
  }

  private[gradle] def exec(cmd: String*)(
      timeout: Duration = Duration(10, TimeUnit.SECONDS),
      io: ProcessIO = BasicIO.standard(connectInput = false)
  ): Int = {
    val process = cmd.mkString(" ").run(io)
    val future = Future(blocking(process.exitValue()))
    try Await.result(future, timeout)
    catch {
      case _: java.util.concurrent.TimeoutException =>
        process.destroy()
        process.exitValue()
    }
  }

  private[gradle] def execAndCheckSuccess(args: String*): Boolean = {
    exec(args: _*)() == 0
  }

  private[gradle] def execAndGetStdOut(args: String*): Option[String] = {
    val output = new StringBuffer()
    val logger = new ProcessLogger {
      override def buffer[T](f: => T): T = f
      override def err(s: => String): Unit = () // ignored
      override def out(s: => String): Unit = output.append(s).append(System.lineSeparator())
    }
    val io = BasicIO(false, logger)
    exec(args: _*)(io = io) match {
      case 0 => Some(trimAtEnd(output.toString)).filter(_.nonEmpty)
      case _ => None
    }
  }

  private[gradle] def readPropertiesFile(filename: String): Properties = {
    val properties = new Properties
    Using.fileInputStream(new File(filename))(properties.load)
    properties
  }

  private[gradle] def getProperty(properties: Properties, propertyName: String): Option[String] =
    Option(properties.getProperty(propertyName)).filter(_.nonEmpty)
}
