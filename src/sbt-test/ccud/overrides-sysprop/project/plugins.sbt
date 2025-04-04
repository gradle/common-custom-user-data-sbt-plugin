sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("com.gradle" % "sbt-develocity-common-custom-user-data" % x)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}

// `val _ = ` is a hack to set sys prop from a .sbt file
val _ = {
  sys.props("develocity.url") = "https://develocity.company.com"
  sys.props("develocity.allowUntrustedServer") = "true"
}
