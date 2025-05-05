package com.gradle.internal

import java.io.{File, UnsupportedEncodingException}
import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import java.util.Properties
import scala.util.Try
import sbt.io.Using

object Utils {

  private val GIT_REPO_URI_REGEX =
    "^(?:(?:https://|git://)(?:.+:.+@)?|(?:ssh)?.*?@)(.*?(?:github|gitlab).*?)(?:/|:[0-9]*?/|:)(.*?)(?:\\.git)?$".r

  private[gradle] def appendIfMissing(string: String, suffix: Char): String = {
    if (string.nonEmpty && string.charAt(string.length - 1) == suffix) string
    else string + suffix
  }

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

  private[gradle] def readPropertiesFile(filename: String): Properties = {
    val properties = new Properties
    Using.fileInputStream(new File(filename))(properties.load)
    properties
  }

  private[gradle] def getProperty(properties: Properties, propertyName: String): Option[String] =
    Option(properties.getProperty(propertyName)).filter(_.nonEmpty)
}
