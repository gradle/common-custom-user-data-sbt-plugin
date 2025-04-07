package com.gradle.internal

abstract class Env {
  protected def fromEnvironment(name: String): Option[String]
  protected def fromProperties(name: String): Option[String]

  final def sysPropertyOrEnvVariable[T: FromString](name: String): Option[T] = sysPropertyOrEnvVariable(
    Env.Key[T](name)
  )
  final def sysPropertyOrEnvVariable[T: FromString](key: Env.Key[T]): Option[T] =
    sysProperty(key).orElse(envVariable(key))

  final def envVariable[T](key: Env.Key[T])(implicit parse: FromString[T]): Option[T] =
    fromEnvironment(key.envVarName).flatMap(parse(_))
  final def envVariable[T](name: String)(implicit parse: FromString[T]): Option[T] =
    fromEnvironment(name).flatMap(parse(_))

  final def sysProperty[T](key: Env.Key[T])(implicit parse: FromString[T]): Option[T] =
    fromProperties(key.name).flatMap(parse(_))
  final def sysProperty[T](name: String)(implicit parse: FromString[T]): Option[T] =
    fromProperties(name).flatMap(parse(_))

}

object Env {
  case class Key[T](name: String) {
    def envVarName: String = toEnvVarName(name)
  }

  object SystemEnvironment extends Env {
    override def fromEnvironment(name: String): Option[String] = sys.env.get(name)
    override def fromProperties(name: String): Option[String] = sys.props.get(name)
  }

  private def toEnvVarName(name: String): String = name.toUpperCase.replace('.', '_')
}
