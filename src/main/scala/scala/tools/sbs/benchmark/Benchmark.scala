/*
 * Benchmark
 * 
 * Version
 * 
 * Created on May 29th, 2012
 * 
 * Created by ND P
 */

package scala.tools.sbs
package benchmark

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URL

import scala.tools.nsc.util.ClassPath
import scala.tools.sbs.common.Reflection
import scala.tools.sbs.io.Log
import scala.tools.sbs.io.LogFactory
import scala.xml.Elem

trait BenchmarkBase {
  self: Configured =>

  // format: OFF
  type BenchmarkType    <: Benchmark
  type subSnippet       <: BenchmarkType
  type subInitializable <: BenchmarkType
  type subFactory       <: Factory
  // format: ON

  trait Benchmark {

    /** Information about the benchmark.
      */
    val info: BenchmarkInfo

    /** Creates the logging object for each benchmark.
      */
    def createLog(mode: Mode): Log

    /** Sets the running context and load benchmark classes.
      */
    def init(): Unit

    /** Runs the benchmark object and throws Exceptions (if any).
      */
    def run(): Unit

    /** Resets the context.
      */
    def reset(): Unit

    /** Class loader
      */
    def context: ClassLoader

  }

  /** An implement of {@link Benchmark} trait.
    * `method` is the `main(args: Array[String])` method of the benchmark `object`.
    */
  class Snippet(val info: BenchmarkInfo,
                val context: ClassLoader,
                method: Method,
                config: Config) extends Benchmark {

    /** Benchmark process.
      */
    private var process: ProcessBuilder = _

    /** Current class loader context.
      */
    private val oldContext = Thread.currentThread.getContextClassLoader

    def init() = Thread.currentThread.setContextClassLoader(context)
    def run() = method.invoke(null, Array(info.arguments.toArray: AnyRef): _*)
    def reset() = Thread.currentThread.setContextClassLoader(oldContext)

    def createLog(mode: Mode) = LogFactory(info.name, mode, config)

  }

  /** Represents benchmarks that have to be initialized before performance check.
    * `benchmarkObject`: The actual benchmark loaded using reflection.
    */
  class Initializable(val info: BenchmarkInfo,
                      val context: ClassLoader,
                      benchmarkObject: Template,
                      config: Config) extends Benchmark {

    /** Current class loader context.
      */
    private val oldContext = Thread.currentThread().getContextClassLoader()

    def init() = {
      Thread.currentThread.setContextClassLoader(context)
      benchmarkObject.init
    }

    def run() = benchmarkObject.run

    def reset() = {
      Thread.currentThread.setContextClassLoader(oldContext)
      benchmarkObject.reset
    }

    def createLog(mode: Mode) = LogFactory(info.name, mode, config)

  }

  /** Factory object used to create a benchmark entity.
    */
  trait Factory {

    def expand(info: BenchmarkInfo): Option[BenchmarkType]

    /** Creates a `Benchmark` from the given arguments.
      */
    def load(info: BenchmarkInfo,
             newSnippet: (Method, ClassLoader) => subSnippet,
             newInitializable: ClassLoader => subInitializable,
             templateName: String): Option[BenchmarkType] = {
      val classpathURLs = config.classpathURLs ++ info.classpathURLs
      try {
        val clazz = Reflection(config, log).getClass(info.name, classpathURLs)
        val method = clazz.getMethod("main", classOf[Array[String]])
        if (!Modifier.isStatic(method.getModifiers)) {
          // throw new NoSuchMethodException(info.name + ".main is not static")
          log.debug("Initializable benchmark: " + info.name)
          try { Some(newInitializable(clazz.getClassLoader)) }
          catch {
            case _: ClassCastException =>
              log.error(info.name + " should extend " + templateName)
              None
          }
        }
        log.debug("Snippet benchmark: " + info.name)
        Some(newSnippet(method, clazz.getClassLoader))
      }
      catch {
        case _: ClassNotFoundException =>
          log.error("class not found: " + info.name + " classpath = " + ClassPath.fromURLs(classpathURLs: _*))
          None
      }
    }

    /** Creates a `Benchmark` from a xml element representing it.
      */
    def expand(xml: Elem): Option[BenchmarkType] =
      try expand(new BenchmarkInfo(xml))
      catch {
        case c: ClassCastException => {
          log.error(c.toString)
          None
        }
        case c: ClassNotFoundException => {
          log.error(c.toString)
          None
        }
        case e => {
          log.error(e.toString)
          log.debug("Getting benchmark from super process failed")
          None
        }
      }

  }

  /** The concrete factory for each type of benchmarks.
    */
  val factory: subFactory

}
