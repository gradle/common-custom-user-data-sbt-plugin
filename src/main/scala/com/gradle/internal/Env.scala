package com.gradle.internal

import Env.Key

object Env {
  case class Key[T](name: String) {
    def envVarName: String = toEnvVarName(name)
  }

  private def toEnvVarName(name: String): String = name.toUpperCase.replace('.', '_')
}

abstract class Env {
  protected def _env(name: String): Option[String]
  protected def _prop(name: String): Option[String]

  final def env[T](key: Key[T])(implicit parse: FromString[T]): Option[T] = _env(key.envVarName).flatMap(parse(_))
  final def env[T](name: String)(implicit parse: FromString[T]): Option[T] = _env(name).flatMap(parse(_))

  final def prop[T](key: Key[T])(implicit parse: FromString[T]): Option[T] = _prop(key.name).flatMap(parse(_))
  final def prop[T](name: String)(implicit parse: FromString[T]): Option[T] = _prop(name).flatMap(parse(_))

  final def propOrEnv[T: FromString](name: String): Option[T] = propOrEnv(Env.Key[T](name))
  final def propOrEnv[T: FromString](key: Key[T]): Option[T] = prop(key).orElse(env(key))

}

object SystemEnvironment extends Env {
  override def _env(name: String): Option[String] = sys.env.get(name)
  override def _prop(name: String): Option[String] = sys.props.get(name)
}
