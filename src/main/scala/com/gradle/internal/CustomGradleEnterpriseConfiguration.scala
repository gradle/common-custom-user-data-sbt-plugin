package com.gradle.internal

import com.gradle.enterprise.sbt.GradleEnterprisePlugin.autoImport.Server
import sbt.{URL, url}

import scala.collection.mutable

class CustomBuildScanConfig {
  val tags: mutable.Set[String] = mutable.Set.empty
  val links: mutable.Map[String, URL] = mutable.Map.empty
  val values: mutable.Map[String, String] = mutable.Map.empty

  def tag(s: String): Unit = {
    tags += s
  }

  def tags(newTags: Set[String]): Unit = {
    tags ++= newTags
  }

  def link(key: String, link: String): Unit = {
    links += key -> url(link)
  }

  def links(newLinks: Map[String, String]): Unit = {
    links ++= newLinks.mapValues(url)
  }

  def addValue(key: String, value: String): Unit = {
    values += key -> value
  }

  def values(newValues: Map[String, String]): Unit = {
    values ++= newValues
  }
}

class CustomServerConfig {
  private var _url: Option[URL] = None
  private var _allowUntrusted: Option[Boolean] = None

  def url: Option[URL] = _url

  def allowUntrusted: Option[Boolean] = _allowUntrusted

  def url_=(newValue: String): Unit = {
    if (newValue != null) _url = Some(new URL(newValue))
  }

  def allowUntrusted_=(newValue: Boolean): Unit = {
    _allowUntrusted = Some(newValue)
  }

  def fromServer(server: Server): CustomServerConfig = {
    _url = server.url
    _allowUntrusted = Some(server.allowUntrusted)
     return this
  }
}





