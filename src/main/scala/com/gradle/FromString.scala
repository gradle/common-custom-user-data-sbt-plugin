package com.gradle

import scala.util.Try
import java.net.URL

/**
  * Denotes types that can be constructed from a `String` representation.
  */
trait FromString[T] {

  /** Build a value of type `T` from its `String` representation. */
  def apply(v: String): Option[T]
}

object FromString {
  implicit val url: FromString[URL] = v => Try(sbt.url(v)).toOption
  implicit val bool: FromString[Boolean] = v => Try(v.toBoolean).toOption
  implicit val string: FromString[String] = Some(_)
  implicit val unit: FromString[Unit] = _ => Some(())
}
