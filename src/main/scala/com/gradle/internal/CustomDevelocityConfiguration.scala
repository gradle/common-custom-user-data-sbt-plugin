package com.gradle.internal

import com.gradle.develocity.agent.sbt.DevelocityPlugin.autoImport.Server
import sbt.{URL, url}

import scala.collection.mutable

class CustomBuildScanConfig {
  private val _tags: mutable.Set[String] = mutable.Set.empty
  private val _links: mutable.Map[String, URL] = mutable.Map.empty
  private val _values: mutable.Map[String, String] = mutable.Map.empty

  def tags(): Set[String] = _tags.toSet
  def links(): Map[String, URL] = _links.toMap
  def values(): Map[String, String] = _values.toMap

  def tag(s: String): Unit = {
    _tags += s
  }

  def tags(newTags: Set[String]): Unit = {
    _tags ++= newTags
  }

  def link(key: String, link: String): Unit = {
      try {
          _links += key -> url(link)
      } catch {
          case _: java.lang.IllegalArgumentException => {} // Ignore
      }
  }

  def links(newLinks: Map[String, String]): Unit = {
      newLinks.foreach( pair => link(pair._1, pair._2) )
  }

  def addValue(key: String, value: String): Unit = {
    _values += key -> value
  }

  def values(newValues: Map[String, String]): Unit = {
    _values ++= newValues
  }
}

class CustomServerConfig (
  private var _url: Option[URL] = None,
  private var _allowUntrusted: Option[Boolean] = None
) {

  def url(): Option[URL] = _url

  def allowUntrusted(): Option[Boolean] = _allowUntrusted

  def url(newValue: String): Unit = {
    if (newValue != null) _url = Some(new URL(newValue))
  }

  def allowUntrusted(newValue: Boolean): Unit = {
    _allowUntrusted = Some(newValue)
  }
}

object CustomServerConfig {
  def fromServer(server: Server): CustomServerConfig =
    new CustomServerConfig(server.url, Some(server.allowUntrusted))
}





