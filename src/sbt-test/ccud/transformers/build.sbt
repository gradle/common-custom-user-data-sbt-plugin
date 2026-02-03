// The key is private in sbt-develocity, we redefine it to be able to access its value.
lazy val internalDevelocityConfigurationTransformers = settingKey[Seq[DevelocityConfiguration => DevelocityConfiguration]]("internal")

lazy val fullDevelocityConfiguration = taskKey[DevelocityConfiguration]("full configuration")

fullDevelocityConfiguration := {
  val baseConfig = develocityConfiguration.value
  val transform = Function.chain(internalDevelocityConfigurationTransformers.value)
  transform(baseConfig)
}

lazy val checkCustomValues = taskKey[Unit]("...")
lazy val checkTags = taskKey[Unit]("...")
lazy val checkLinks = taskKey[Unit]("...")

val assertEquals = (a: Any, b: Any) => assert(a == b, s"Expected: $a but got $b")

val ownCustomValues = Map("hello" -> "world")
val ownTags = Set("example", "tag")
val ownLinks = Map("Gradle" -> url("https://gradle.com"))

ThisBuild / develocityConfiguration ~= { prev =>
  prev.withBuildScan(
    prev.buildScan
      .withValues(prev.buildScan.values ++ ownCustomValues)
      .withTags(prev.buildScan.tags ++ ownTags)
      .withLinks(prev.buildScan.links ++ ownLinks)
  )
}

checkCustomValues := {
  val customValues = fullDevelocityConfiguration.value.buildScan.values
  val expectedCustomValues = ownCustomValues

  expectedCustomValues.foreach { case (key, value) =>
    assertEquals(key -> Some(value), key -> customValues.get(key))
  }
}

checkTags := {
  val tags = fullDevelocityConfiguration.value.buildScan.tags

  assert(ownTags.forall(tags.contains), s"$tags does not contain all of $ownTags")
  assert(tags.size > ownTags.size, s"$tags should contain more tags than $ownTags")
}

checkLinks := {
  val links = develocityConfiguration.value.buildScan.links
  val expectedLinks = ownLinks

  expectedLinks.foreach { case (label, url) =>
    assertEquals(label -> Some(url), label -> links.get(label))
  }
}
