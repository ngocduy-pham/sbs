/*
 * ProfBenchmark
 * 
 * Version
 * 
 * Created on October 29th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package profiling

import java.lang.reflect.Method
import java.net.URL

import scala.tools.nsc.io.Path
import scala.tools.sbs.benchmark.BenchmarkBase
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.ObjectHarness
import scala.tools.sbs.common.Reflection
import scala.tools.sbs.common.RunOnlyHarness
import scala.tools.sbs.io.Log

trait ProfBenchmark extends BenchmarkBase {

  import ProfBenchmark.Benchmark

  type subBenchmark     <: Benchmark
  type subSnippet       <: subBenchmark
  type subInitializable <: subBenchmark

  class Snippet(name: String,
                arguments: List[String],
                classpathURLs: List[URL],
                src: Path,
                timeout: Int,
                val classes: List[String],
                val exclude: List[String],
                val methodName: String,
                val fieldName: String,
                method: Method,
                context: ClassLoader,
                config: Config)
    extends super.Snippet(name, arguments, classpathURLs, src, timeout, context, method, config)
    with Benchmark {

    val howToLaunch = Right(this)

  }

  class Initializable(name: String,
                      classpathURLs: List[URL],
                      src: Path,
                      benchmarkObject: ProfTemplate,
                      context: ClassLoader,
                      config: Config)
    extends super.Initializable(name, classpathURLs, src, context, benchmarkObject, config)
    with Benchmark {

    val classes     = benchmarkObject.classes
    val exclude     = benchmarkObject.exclude
    val methodName  = benchmarkObject.methodName
    val fieldName   = benchmarkObject.fieldName
    val howToLaunch = Left(RunOnlyHarness)

  }

  val classesOpt    = "profile-class"
  val excludeOpt    = "exclude"
  val methodNameOpt = "methodname"
  val fieldNameOpt  = "fieldname"

}

object ProfBenchmark extends ProfBenchmark {

  type subBenchmark     = Benchmark
  type subSnippet       = Snippet
  type subInitializable = Initializable
  type subFactory       = Factory

  trait Benchmark extends BenchmarkBase.Benchmark {

    /** Names of the classes to be profiled the loading.
      */
    def classes: List[String]

    /** Names of the classes to be ignored from profiling.
      */
    def exclude: List[String]

    /** Name of the method to be profiled the invocations.
      */
    def methodName: String

    /** Name of the field to be profiled the accessing and modifying.
      */
    def fieldName: String

    /** How java runs this benchmark independently to sbs.
      * Snippet benchmarks can run on themselves with the method main.
      * Initializable benchmarks are loaded and run by an ObjectHarness. 
      */
    def howToLaunch: Either[ObjectHarness, Benchmark]

  }

  def factory(_log: Log, _config: Config) = new Factory with Configured {

    val log: Log       = _log
    val config: Config = _config

    def createFrom(info: BenchmarkInfo): Benchmark = {
      val argMap = BenchmarkInfo.readInfo(
        info.src,
        List(classesOpt, excludeOpt, methodNameOpt, fieldNameOpt) map toOption)
      load(
        info,
        (method: Method, context: ClassLoader) => new Snippet(
          info.name,
          info.arguments,
          info.classpathURLs,
          info.src,
          info.timeout,
          getIntoOrElse(argMap get classesOpt, stringToList, config.classes),
          getIntoOrElse(argMap get excludeOpt, stringToList, config.exclude),
          argMap getOrElse (methodNameOpt, config.methodName),
          argMap getOrElse (fieldNameOpt, config.fieldName),
          method,
          context,
          config),
        (context: ClassLoader) => new Initializable(
          info.name,
          info.classpathURLs,
          info.src,
          Reflection(config, log).getObject[ProfTemplate](info.name, config.classpathURLs ++ info.classpathURLs),
          context,
          config),
        classOf[ProfTemplate].getName)
    }

  }

}
