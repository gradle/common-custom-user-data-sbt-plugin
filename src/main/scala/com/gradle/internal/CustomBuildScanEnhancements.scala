package com.gradle.internal

import com.gradle.internal.CiUtils.isCi
import com.gradle.internal.Utils.sysProperty

/**
 * Adds a standard set of useful tags, links and custom values to all build scans published.
 */
object CustomBuildScanEnhancements {

  private val SYSTEM_PROP_IDEA_VENDOR_NAME = "idea.vendor.name"
  private val SYSTEM_PROP_IDEA_VERSION = "idea.version"
  private val SYSTEM_PROP_ECLIPSE_BUILD_ID = "eclipse.buildId"
  private val SYSTEM_PROP_IDEA_SYNC_ACTIVE = "idea.sync.active"

  private val buildScan = CustomBuildScanConfig
  def apply(): Unit = {
    captureOs()
    captureIde()
  }

  private def captureOs(): Unit = {
    sysProperty("os.name").foreach(buildScan.tag)
  }

  private def captureIde(): Unit = {
    if (!isCi()) {
      if (sysProperty(SYSTEM_PROP_IDEA_VENDOR_NAME).isDefined) {
        val ideaVendorNameValue = sysProperty(SYSTEM_PROP_IDEA_VENDOR_NAME).get
        if (ideaVendorNameValue == "JetBrains") tagIde("IntelliJ IDEA", getOrEmpty(sysProperty(SYSTEM_PROP_IDEA_VERSION)))
      }
      else if (sysProperty(SYSTEM_PROP_IDEA_VERSION).isDefined) {
        // this case should be handled by the ideaVendorName condition but keeping it for compatibility reason (ideaVendorName started with 2020.1)
        tagIde("IntelliJ IDEA", sysProperty(SYSTEM_PROP_IDEA_VERSION).get)
      }
      else if (sysProperty(SYSTEM_PROP_ECLIPSE_BUILD_ID).isDefined) tagIde("Eclipse", sysProperty(SYSTEM_PROP_ECLIPSE_BUILD_ID).get)
      else buildScan.tag("Cmd Line")

      if (sysProperty(SYSTEM_PROP_IDEA_SYNC_ACTIVE).isDefined) buildScan.tag("IDE sync")
    }
  }

  private def getOrEmpty(p: Option[String]): String = p.getOrElse("")

  private def tagIde(ideLabel: String, version: String): Unit = {
    buildScan.tag(ideLabel)
    if (version.nonEmpty) buildScan.addValue(ideLabel + " version", version)
  }
}
