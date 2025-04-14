package com.gradle

/**
  * An abstraction over the environment in which the build is running.
  */
abstract class Env {

  /**
    * Get the value of the environment variable `name`.
    *
    * @param name The name of environment variable whose value to extract.
    * @return An Option with the value of the environment variable `name`
    *         if it exists in this environment, `None` otherwise.
    */
  protected def fromEnvironment(name: String): Option[String]

  /**
    * Get the value of the system property `name`.
    *
    * @param name The name of system property whose value to extract.
    * @return An Option with the value of the system property `name`
    *         if it exists in this environment, `None` otherwise.
    */
  protected def fromProperties(name: String): Option[String]

  /**
    * Get the value of the system property or environment variable `name`.
    *
    * @param name The name of the system property or environment variable to extract.
    * @return An `option` with the value of the system property or environment variable,
    *         parsed as a value of type `T`, if it exists, `None` otherwise.
    */
  final def sysPropertyOrEnvVariable[T: FromString](name: String): Option[T] = sysPropertyOrEnvVariable(
    Env.Key[T](name)
  )

  /**
    * Get the value of the system property or environment variable `key`.
    *
    * @param key The key to query.
    * @return An `Option` with the value of the system property or environment variable,
    *         parsed as a value of type `T`, if it exists, `None` otherwise.
    */
  final def sysPropertyOrEnvVariable[T: FromString](key: Env.Key[T]): Option[T] =
    sysProperty(key).orElse(envVariable(key))

  /**
    * Get the value of the environment variable `key`.
    *
    * @param key The key to query.
    * @param parse The function to use to parse the value to a value of type `T`.
    * @return An `Option` with the value of the environment variable parsed as a value of type `T`
    *         if it exists, `None` otherwise.
    */
  final def envVariable[T](key: Env.Key[T])(implicit parse: FromString[T]): Option[T] =
    fromEnvironment(key.envVarName).flatMap(parse(_))

  /**
    * Get the value of the environment variable `name`.
    *
    * @param name The name of the environment variable.
    * @param parse The function to use to parse the value to a value of type `T`.
    * @return An `Option` with the value of the environment variable parsed as a value of type `T`
    *         if it exists, `None` otherwise.
    */
  final def envVariable[T](name: String)(implicit parse: FromString[T]): Option[T] =
    fromEnvironment(name).flatMap(parse(_))

  /**
    * Get the value of the system property `key`.
    *
    * @param name The key to query
    * @param parse The function to use to parse the value to a value of type `T`.
    * @return An `Option` with the value of the system property parsed as a value of type `T`
    *         if it exists, `None` otherwise.
    */
  final def sysProperty[T](key: Env.Key[T])(implicit parse: FromString[T]): Option[T] =
    fromProperties(key.name).flatMap(parse(_))

  /**
    * Get the value of the system property `name`.
    *
    * @param name The name of the system property.
    * @param parse The function to use to parse the value to a value of type `T`.
    * @return An `Option` with the value of the system property parsed as a value of type `T`
    *         if it exists, `None` otherwise.
    */
  final def sysProperty[T](name: String)(implicit parse: FromString[T]): Option[T] =
    fromProperties(name).flatMap(parse(_))

}

object Env {

  /**
    * A key used to query the environment.
    *
    * @param name The name of the system property corresponding to this key.
    */
  case class Key[T](name: String) {

    /** The name of this system property, seen as an environment variable. */
    def envVarName: String = toEnvVarName(name)
  }

  /** The default environment. Queries `sys.env` and `sys.props`. */
  object SystemEnvironment extends Env {
    override def fromEnvironment(name: String): Option[String] = sys.env.get(name)
    override def fromProperties(name: String): Option[String] = sys.props.get(name)
  }

  private def toEnvVarName(name: String): String = name.toUpperCase.replace('.', '_')
}
