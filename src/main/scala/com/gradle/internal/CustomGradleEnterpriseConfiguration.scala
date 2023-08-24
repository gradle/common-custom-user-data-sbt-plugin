package com.gradle.internal

import sbt.URL

import scala.collection.mutable

object CustomBuildScanConfig {
  val tags: mutable.Set[String] = mutable.Set.empty
  val links: mutable.Map[String, URL] = mutable.Map.empty
  val values: mutable.Map[String, String] = mutable.Map.empty

  def tag(s: String): Unit = {
    tags += s
  }

  def tags(newTags: Set[String]): Unit = {
    tags ++= newTags
  }

  def link(key: String, link: URL): Unit = {
    links += key -> link
  }

  def links(newLinks: Map[String, URL]): Unit = {
    links ++= newLinks
  }

  def addValue(key: String, value: String): Unit = {
    values += key -> value
  }

  def values(newValues: Map[String, String]): Unit = {
    values ++= newValues
  }
}

object CustomServerConfig {
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
}





