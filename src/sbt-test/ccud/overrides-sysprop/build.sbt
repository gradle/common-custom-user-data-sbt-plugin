lazy val checkOverrides = taskKey[Unit]("...")

val assertEquals = (a: Any, b: Any) => assert(a == b, s"Expected: $a but got $b")

checkOverrides := {
  val configuration = (ThisBuild / develocityConfiguration).value
  assertEquals(Some(url("https://develocity.company.com")), configuration.server.url)
  assertEquals(true, configuration.server.allowUntrusted)

}
