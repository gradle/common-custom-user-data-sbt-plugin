package com.gradle.internal

abstract class Transformer[C] { self =>
  def transform(in: C): C

  protected def ifDefined[T](v: Option[T])(op: (C, T) => C): C => C = v match {
    case None        => identity
    case Some(value) => op(_, value)
  }
}
