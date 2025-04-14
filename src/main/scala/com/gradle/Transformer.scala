package com.gradle

import com.gradle.develocity.agent.sbt.api.configuration.DevelocityConfiguration

/** A tranformation on a value of type `C`. */
abstract class Transformer[C] { self =>

  /** Apply the transformation on the value `in`. */
  def transform(in: C)(implicit env: Env): C

  /**
    * If `v` is defined, build a function `(c: C) => op(c, v)`, otherwise
    * the identity function.
    *
    * @param v The optional value to test.
    * @param op The function to apply.
    * @return If `v` is defined, a function `(c: C) => op(c, v)`, otherwise
    *         the identity function.
    */
  protected def ifDefined[T](v: Option[T])(op: (C, T) => C): C => C = v match {
    case None        => identity
    case Some(value) => op(_, value)
  }

  /**
    * Combine this transformer with `next`.
    *
    * @param next The tranformer to apply on the result of applying this transformer.
    * @return A transformer that applies this transformer followed by `next`.
    */
  def andThen(next: Transformer[C]): Transformer[C] = new Transformer[C] {
    override def transform(in: C)(implicit env: Env): C =
      next.transform(self.transform(in))
  }

  /**
    * Turn this transformer on type `C` into a `Transformer` on `DevelocityConfiguration`
    * by providing a function to extract a value of type `C` from `DevelocityConfiguration`
    * and a function to inject the result back into `DevelocityConfiguration`.
    *
    * @param extract The function to extract a value of type `C`.
    * @param inject The function to inject the result into `DevelocityConfiguration`.
    * @return A transformer that operates on `DevelocityConfiguration`.
    */
  def lift(
      extract: DevelocityConfiguration => C,
      inject: (DevelocityConfiguration, C) => DevelocityConfiguration
  ): Transformer[DevelocityConfiguration] = new Transformer[DevelocityConfiguration] {
    override def transform(in: DevelocityConfiguration)(implicit env: Env): DevelocityConfiguration =
      inject(in, self.transform(extract(in)))
  }
}
