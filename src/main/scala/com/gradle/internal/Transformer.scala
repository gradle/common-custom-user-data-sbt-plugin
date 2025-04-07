package com.gradle.internal

import com.gradle.develocity.agent.sbt.api.configuration.DevelocityConfiguration

abstract class Transformer[C] { self =>
  def transform(in: C)(implicit env: Env): C

  protected def ifDefined[T](v: Option[T])(op: (C, T) => C): C => C = v match {
    case None        => identity
    case Some(value) => op(_, value)
  }

  def andThen(next: Transformer[C]): Transformer[C] = new Transformer[C] {
    override def transform(in: C)(implicit env: Env): C =
      next.transform(self.transform(in))
  }

  def lift(
      extract: DevelocityConfiguration => C,
      inject: (DevelocityConfiguration, C) => DevelocityConfiguration
  ): Transformer[DevelocityConfiguration] = new Transformer[DevelocityConfiguration] {
    override def transform(in: DevelocityConfiguration)(implicit env: Env): DevelocityConfiguration =
      inject(in, self.transform(extract(in)))
  }
}
