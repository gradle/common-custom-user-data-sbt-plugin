package com.gradle

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import sbt.url

class FromStringTest extends AnyFlatSpec with Matchers {
  "FromString" should "parse boolean" in {
    FromString.bool("true") shouldBe Some(true)
    FromString.bool("TRUE") shouldBe Some(true)
    FromString.bool("trUE") shouldBe Some(true)
    FromString.bool("false") shouldBe Some(false)
    FromString.bool("hello") shouldBe None
  }

  it should "forward strings" in {
    FromString.string("hello") shouldBe Some("hello")
    FromString.string("") shouldBe Some("")
    FromString.string(null) shouldBe Some(null)
  }

  it should "parse URL" in {
    FromString.url("http://google.com") shouldBe Some(url("http://google.com"))
    FromString.url(
      "https://user:password@sample-site.com:8080/store/product%20details?item=Z123&region=CH#reviews"
    ) shouldBe Some(
      url("https://user:password@sample-site.com:8080/store/product%20details?item=Z123&region=CH#reviews")
    )
    FromString.url("foobar") shouldBe None
  }

  it should "parse unit from anything" in {
    FromString.unit("") shouldBe Some(())
    FromString.unit("x") shouldBe Some(())
    FromString.unit(null) shouldBe Some(())
  }
}
