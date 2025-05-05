package com.gradle

import scala.concurrent.{Await, Future, blocking}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.sys.process.{BasicIO, Process, ProcessIO, ProcessLogger}
import java.util.concurrent.{TimeoutException, TimeUnit}

object ProcessUtils {

  /**
    * Executes `cmd` in a new process, blocking until it returns or at most until
    * `timeout` expires. Returns the exit code of the command.
    *
    * @param cmd The command to run.
    * @param timeout The maximum duration to wait for the command to return.
    * @param io IO configuration.
    * @return The exit code of the command.
    */
  def exec(cmd: String*)(
      timeout: Duration = Duration(10, TimeUnit.SECONDS),
      io: ProcessIO = BasicIO.standard(connectInput = false)
  ): Int = {
    val process = Process(cmd).run(io)
    val future = Future(blocking(process.exitValue()))
    try Await.result(future, timeout)
    catch {
      case _: TimeoutException =>
        process.destroy()
        process.exitValue()
    }
  }

  /**
    * Executes `cmd` and verifies the exit code is 0.
    *
    * @param cmd The command to run.
    * @return True if the exit code was 0, false otherwise.
    */
  def execAndCheckSuccess(cmd: String*): Boolean = {
    exec(cmd: _*)() == 0
  }

  /**
    * Executes `cmd` and return what was printed to stdout if the command succeeded,
    * `None` otherwise.
    *
    * @param cmd The command to run.
    * @return An `Option` with the content that was printed to stdout if the command succeeded,
    *         `None` otherwise.
    */
  def execAndGetStdOut(args: String*): Option[String] = {
    val output = new StringBuffer()
    val logger = new ProcessLogger {
      override def buffer[T](f: => T): T = f
      override def err(s: => String): Unit = () // ignored
      override def out(s: => String): Unit = output.append(s).append(System.lineSeparator())
    }
    val io = BasicIO(false, logger)
    exec(args: _*)(io = io) match {
      case 0 => Some(trimAtEnd(output.toString)).filter(_.nonEmpty)
      case _ => None
    }
  }

  private def trimAtEnd(str: String) = ('x' + str).trim.substring(1)

}
