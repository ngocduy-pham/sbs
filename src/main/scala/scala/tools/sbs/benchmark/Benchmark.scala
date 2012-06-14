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

import scala.tools.nsc.io.Path
import scala.tools.nsc.util.ClassPath
import scala.tools.sbs.common.Reflection
import scala.tools.sbs.io.Log
import scala.tools.sbs.io.LogFactory
import scala.tools.sbs.util.Constant
import scala.xml.Elem

import BenchmarkBase.Benchmark

trait BenchmarkBase {

  import BenchmarkBase.Benchmark

  type subBenchmark     <: Benchmark
  type subSnippet       <: subBenchmark
  type subInitializable <: subBenchmark
  type subFactory       <: Factory

  /** An implement of {@link Benchmark} trait.
    * `method` is the `main(args: Array[String])` method of the benchmark `object`.
    */
  class Snippet(val name: String,
                val arguments: List[String],
                val classpathURLs: List[URL],
                val src: Path,
                val timeout: Int,
                val context: ClassLoader,
                method: Method,
                config: Config) extends Benchmark {

    /** Benchmark process.
      */
    private var process: ProcessBuilder = _

    /** Current class loader context.
      */
    private val oldContext = Thread.currentThread.getContextClassLoader

    def init()  = Thread.currentThread.setContextClassLoader(context)
    def run()   = method.invoke(null, Array(arguments.toArray: AnyRef): _*)
    def reset() = Thread.currentThread.setContextClassLoader(oldContext)

    def createLog(mode: Mode) = LogFactory(name, mode, config)

    def toXML =
      <SnippetBenchmark>
        <name>{ name }</name>
        <arguments>{ for (arg <- arguments) yield <arg>{ arg }</arg> }</arguments>
        <classpath>{ for (cp <- classpathURLs) yield <cp> { cp.getPath } </cp> }</classpath>
        <src>{ src.path }</src>
      </SnippetBenchmark>

  }

  /** Represents benchmarks that have to be initialized before performance check.
    * `benchmarkObject`: The actual benchmark loaded using reflection.
    */
  class Initializable(val name: String,
                      val classpathURLs: List[URL],
                      val src: Path,
                      val context: ClassLoader,
                      benchmarkObject: Template,
                      config: Config) extends Benchmark {

    val arguments = List[String]()
    val timeout   = benchmarkObject.timeout

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

    def createLog(mode: Mode) = LogFactory(name, mode, config)

    def toXML =
      <InitializableBenchmark>
        <name>{ name }</name>
        <classpath>{ for (cp <- classpathURLs) yield <cp> { cp.getPath } </cp> }</classpath>
        <src>{ src.path }</src>
      </InitializableBenchmark>

  }

  /** Factory object used to create a benchmark entity.
    */
  trait Factory {
    self: Configured =>

    def createFrom(info: BenchmarkInfo): subBenchmark

    /** Creates a `Benchmark` from the given arguments.
      */
    protected def load(info: BenchmarkInfo,
                       newSnippet: (Method, ClassLoader) => subSnippet,
                       newInitializable: ClassLoader => subInitializable,
                       templateName: String): subBenchmark = {
      val classpathURLs = config.classpathURLs ++ info.classpathURLs
      try {
        val clazz = Reflection(config, log).getClass(info.name, classpathURLs)
        try {
          val method = clazz.getMethod("main", classOf[Array[String]])
          if (!Modifier.isStatic(method.getModifiers)) {
            throw new NoSuchMethodException(info.name + ".main is not static")
          }
          log.debug("Snippet benchmark: " + info.name)
          newSnippet(method, clazz.getClassLoader)
        }
        catch {
          case _: NoSuchMethodException => try {
            log.debug("Initializable benchmark: " + info.name)
            newInitializable(clazz.getClassLoader)
          }
          catch {
            case _: ClassCastException => throw new ClassCastException(
              info.name + " should implement " + templateName)
            case _: ClassNotFoundException => throw new ClassNotFoundException(
              info.name + " should be an object or a class (not trait nor abstract)")
          }
        }
      }
      catch {
        case _: ClassNotFoundException =>
          throw new ClassNotFoundException(
            info.name + " classpath = " + ClassPath.fromURLs(classpathURLs: _*))
      }
    }

    /** Creates a `Benchmark` from a xml element representing it.
      */
    def createFrom(xml: Elem): subBenchmark =
      try createFrom(
        new BenchmarkInfo(
          (xml \\ "name").text,
          Path(xml \\ "src" text),
          (xml \\ "arg") map (_.text) toList,
          (xml \\ "cp") map (cp => Path(cp.text).toURL) toList,
          0,
          false))
      catch {
        case c: ClassCastException => {
          log.error(c.toString)
          throw c
        }
        case c: ClassNotFoundException => {
          log.error(c.toString)
          throw c
        }
        case e => {
          log.error(e.toString)
          throw new Exception("Getting benchmark from super process failed")
        }
      }

    def getIntoOrElse[R](arg: Option[String], convert: String => R, default: => R): R = arg match {
      case Some(str) => convert(str)
      case _         => default
    }

    val stringToInt            = (str: String) => str.toInt
    val stringToList           = (str: String) => str split Constant.COLON toList
    def toOption(name: String) = Constant.ARG + name

  }

  /** The concrete factory for each type of benchmarks.
    */
  def factory(log: Log, config: Config): subFactory

}

object BenchmarkBase extends BenchmarkBase {

  type subBenchmark     = Benchmark
  type subSnippet       = Snippet
  type subInitializable = Initializable
  type subFactory       = Factory

  trait Benchmark {

    def name: String

    /** Arguments of the benchmark.
      */
    def arguments: List[String]

    /** Classpath necessary to run the benchmark.
      */
    def classpathURLs: List[URL]

    /** Maximum time for each benchmarking, default to 15 seconds.
      */
    def timeout: Int

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

    /** Produces a XML element representing this benchmark.
      */
    def toXML: scala.xml.Elem

  }

  def factory(_log: Log, _config: Config) = new Factory with Configured {

    val log: Log = _log
    val config: Config = _config

    def createFrom(info: BenchmarkInfo): Benchmark = load(
      info,
      (method: Method, context: ClassLoader) => new Snippet(
        info.name,
        info.arguments,
        info.classpathURLs,
        info.src,
        info.timeout,
        context,
        method,
        config
      ),
      (context: ClassLoader) => new Initializable(
        info.name,
        info.classpathURLs,
        info.src,
        context,
        Reflection(config, log).getObject[Template](info.name, config.classpathURLs ++ info.classpathURLs),
        config
      ),
      classOf[Template].getName)

  }

}
