package com.gradle

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.concurrent.duration.Duration

class ProcessUtilsTest extends AnyFlatSpec with Matchers {

  "ProcessUtilsTest.exec" should "return exit code" in {
    ProcessUtils.exec("sh", "-c", "exit 123")() shouldBe 123
  }

  it should "respect timeout" in {
    ProcessUtils.exec("sleep", "1")(timeout = Duration(10, "milliseconds")) should be > 0
    ProcessUtils.exec("sleep", "1")(timeout = Duration(2, "seconds")) shouldBe 0
  }

  "ProcessUtils.execAndCheckSuccess" should "return true iff the command has exit code 0" in {
    ProcessUtils.execAndCheckSuccess("sh", "-c", "exit 123") shouldBe false
    ProcessUtils.execAndCheckSuccess("sh", "-c", "exit 0") shouldBe true
  }

  "ProcessUtils.execAndGetStdOut" should "return content of stdout on success only" in {
    ProcessUtils.execAndGetStdOut("sh", "-c", "echo hello") shouldBe Some("hello")
    ProcessUtils.execAndGetStdOut("sh", "-c", "exit hello && exit 1") shouldBe None
    ProcessUtils.execAndGetStdOut("sh", "-c", "echo foo && echo >&2 echo stderr") shouldBe Some("foo")
    ProcessUtils.execAndGetStdOut("sh", "-c", "echo >&2 nothing on stdout") shouldBe None
  }

}
