package com.gradle.internal

import scala.util.Try
import java.net.URL

trait FromString[T] {
  def apply(v: String): Option[T]
}

object FromString {
  implicit val url: FromString[URL] = v => Try(sbt.url(v)).toOption
  implicit val bool: FromString[Boolean] = v => Try(v.toBoolean).toOption
  implicit val string: FromString[String] = Some(_)
  implicit val unit: FromString[Unit] = _ => Some(())
}
