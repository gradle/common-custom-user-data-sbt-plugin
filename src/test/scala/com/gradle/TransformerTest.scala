package com.gradle

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class TransformerTest extends AnyFlatSpec with Matchers {

  private implicit val env: Env = Env.SystemEnvironment

  private object Add2 extends Transformer[Int] {
    override def transform(in: Int)(implicit env: Env): Int = in + 2
  }

  private object Times3 extends Transformer[Int] {
    override def transform(in: Int)(implicit env: Env): Int = in * 3
  }

  "Transformer.andThen" should "chain transformers" in {
    Add2.andThen(Times3).transform(1) shouldBe 9
    Times3.andThen(Add2).transform(1) shouldBe 5
  }
}
