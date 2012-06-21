/*
 * JVMInvoker
 * 
 * Version
 * 
 * Created on September 24th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package common

import java.net.URL

import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.sys.process.Process
import scala.sys.process.ProcessIO
import scala.tools.nsc.util.ClassPath
import scala.tools.sbs.io.Log

import benchmark.BenchmarkBase.Benchmark

/** Trait used to invoke a new separated JVM.
  */
trait JVMInvoker {

  import JVMInvoker.Invoker

  /** An implement of {@link JVMInvoker}.
    */
  class ScalaInvoker(log: Log, config: Config) extends Invoker {

    /** `java` or `./jre/bin/java`, etc...
      */
    private val java = Seq(config.javacmd, "-server")

    /** `-cp <scala-library.jar, scala-compiler.jar> -Dscala=<scala-home> scala.tools.nsc.MainGenericRunner`
      */
    private def asScala(classpathURLs: List[URL]) =
      Seq(
        "-cp",
        ClassPath.fromURLs(classpathURLs ++ List(config.scalaLibraryJar.toURL, config.scalaCompilerJar.toURL): _*)) ++
        Seq(config.javaProp, "scala.tools.nsc.MainGenericRunner")

    /** `-cp <classpath from config; classpath from benchmark>`
      */
    private def asScalaClasspath(classpathURLs: List[URL]) =
      Seq("-cp", ClassPath.fromURLs(classpathURLs: _*))

    /** `-cp <classpath from config; classpath from benchmark> Runner`
      */
    private def asHarness(harness: ObjectHarness, benchmark: Benchmark, classpathURLs: List[URL]) =
      asScalaClasspath(classpathURLs) ++ Seq(harness.getClass.getName replace ("$", ""))

    /** `-cp <classpath from config; classpath from benchmark> Benchmark`
      */
    private def asBenchmark(benchmark: Benchmark, classpathURLs: List[URL]) =
      asScalaClasspath(classpathURLs) ++ Seq(benchmark.info.name)

    /** `-cp <scala-library.jar, scala-compiler.jar> -Dscala.home=<scala-home> scala.tools.nsc.MainGenericRunner
      * -cp <classpath from config; classpath from benchmark> Benchmark benchmark.arguments`
      */
    def asJavaArgument(benchmark: Benchmark, classpathURLs: List[URL]) =
      asScala(classpathURLs) ++ asBenchmark(benchmark, classpathURLs) ++ benchmark.info.arguments

    /** `-cp <scala-library.jar, scala-compiler.jar> -Dscala.home=<scala-home> scala.tools.nsc.MainGenericRunner
      * -cp <classpath from config; classpath from benchmark> Runner benchmark.toXML config.args`
      * Result must be a string on one line and starts with `<`.
      */
    def asJavaArgument(harness: ObjectHarness, benchmark: Benchmark, classpathURLs: List[URL]) =
      asScala(classpathURLs) ++
        asHarness(harness, benchmark, classpathURLs) ++
        Seq(scala.xml.Utility.trim(benchmark.info.toXML).toString) ++
        config.args

    def command(harness: ObjectHarness, benchmark: Benchmark, classpathURLs: List[URL]) =
      java ++ asJavaArgument(harness, benchmark, classpathURLs)

    def command(benchmark: Benchmark, classpathURLs: List[URL]) =
      java ++ asJavaArgument(benchmark, classpathURLs)

    def invoke[R, E](command: Seq[String],
                     stdout: String => R,
                     stderr: String => E,
                     timeout: Int): (ArrayBuffer[R], ArrayBuffer[E]) = {

      log.debug("Invoked command: " + (command mkString " "))

      val result         = ArrayBuffer[R]()
      val error          = ArrayBuffer[E]()
      val processBuilder = Process(command)

      val processIO = new ProcessIO(
        _ => (),
        Source.fromInputStream(_).getLines foreach (result append stdout(_)),
        Source.fromInputStream(_).getLines foreach (error append stderr(_)))

      val processStarter = new Thread {

        override def run = {
          var process: Process = null
          try {
            process = processBuilder.run(processIO)
            val success = process.exitValue
            log.debug("Sub-process exit value: " + success)
          }
          catch {
            case e: InterruptedException =>
              log.error("Timeout")
              process.destroy
          }
        }

      }
      val timer = new Thread {

        override def run = try {
          Thread sleep (timeout + 3000)
          processStarter.interrupt()
        }
        catch {
          case e: InterruptedException =>
            log.debug("Not timeout")
        }

      }

      processStarter.start()
      if (timeout > 0) timer.start()
      processStarter.join
      timer.interrupt()

      (result, error)
    }

  }

}

/** Factory object of {@link JVMInvoker}.
  */
object JVMInvoker extends JVMInvoker {

  trait Invoker {

    /** Invokes a new JVM which uses a typical {@link scala.tools.sbs.Runner}
      * to run a typical {@link scala.tools.sbs.benchmark.Benchmark}.
      *
      * @param command	OS command to invoke the wanted jvm under the form of a `Seq[String]`.
      * @param stdout 	function converts a `String` which is a line from the jvm's standard output to `R`.
      * @param stderr 	function converts a `String` which is a line from the jvm's standard error to `E`.
      * @param timeout	maximum time for the jvm to run.
      *
      * @return
      * <ul>
      * <li>A `ArrayBuffer[R]` array of values each had been created from one line of the jvm's standard output.
      * <li>A `ArrayBuffer[E]` array of values each had been created from one line of the jvm's standard error.
      * </ul>
      */
    def invoke[R, E](command: Seq[String],
                     stdout: String => R,
                     stderr: String => E,
                     timeout: Int): (ArrayBuffer[R], ArrayBuffer[E])

    /** OS command to invoke an new JVM which has `harness` as the main scala class
      * and `benchmark` as an argument.
      */
    def command(harness: ObjectHarness, benchmark: Benchmark, classpathURLs: List[URL]): Seq[String]

    /** OS command to invoke an new JVM which has `benchmark` as the main scala class.
      */
    def command(benchmark: Benchmark, classpathURLs: List[URL]): Seq[String]

    /** OS command argument to run with java.
      * Ex: `Seq("-cp", ".", "scala.tools.nsc.MainGenericRunner", "-version")`.
      */
    def asJavaArgument(benchmark: Benchmark, classpathURLs: List[URL]): Seq[String]

    def asJavaArgument(harness: ObjectHarness, benchmark: Benchmark, classpathURLs: List[URL]): Seq[String]

  }

  def apply(log: Log, config: Config): Invoker = new ScalaInvoker(log, config)

}
