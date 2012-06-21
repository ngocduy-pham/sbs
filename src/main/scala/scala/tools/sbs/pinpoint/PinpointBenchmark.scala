/*
 * PinpointingBenchmark
 * 
 * Version
 * 
 * Created on October 29th, 2011
 * 
 * Created by ND P
 */

package scala.tools.sbs
package pinpoint

import java.lang.reflect.Method
import java.net.URL

import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.Directory
import scala.tools.nsc.io.Path
import scala.tools.sbs.benchmark.BenchmarkInfo
import scala.tools.sbs.common.Reflection
import scala.tools.sbs.common.RunOnlyHarness
import scala.tools.sbs.io.Log
import scala.tools.sbs.performance.PerfBenchmark
import scala.tools.sbs.profiling.ProfBenchmark

trait PinpointBenchmark extends PerfBenchmark with ProfBenchmark {

  import PinpointBenchmark.Benchmark

  type subBenchmark     <: Benchmark
  type subSnippet       <: subBenchmark
  type subInitializable <: subBenchmark

  class Snippet(info: BenchmarkInfo,
                multiplier: Int,
                measurement: Int,
                sampleNumber: Int,
                val className: String,
                val methodName: String,
                val exclude: List[String],
                val previous: Directory,
                val depth: Int,
                method: Method,
                context: ClassLoader,
                config: Config)
    extends super[PerfBenchmark].Snippet(
      info,
      method,
      context,
      config,
      multiplier,
      measurement,
      sampleNumber) with Benchmark {

    val fieldName   = ""
    val howToLaunch = Right(this)

  }

  class Initializable(info: BenchmarkInfo,
                      benchmarkObject: PinpointTemplate,
                      context: ClassLoader,
                      config: Config)
    extends super[PerfBenchmark].Initializable(
      info,
      benchmarkObject,
      context,
      config) with Benchmark {

    val className   = benchmarkObject.className
    val methodName  = benchmarkObject.methodName
    val fieldName   = ""
    val exclude     = benchmarkObject.exclude
    val previous    = benchmarkObject.previous
    val depth       = benchmarkObject.depth
    val howToLaunch = Left(RunOnlyHarness)

  }

  val classNameOpt = "classname"
  val previousOpt  = "previous"
  val depthOpt     = "depth"

}

object PinpointBenchmark extends PinpointBenchmark {

  type subBenchmark     = Benchmark
  type subSnippet       = Snippet
  type subInitializable = Initializable
  type subFactory       = Factory

  trait Benchmark extends PerfBenchmark.Benchmark with ProfBenchmark.Benchmark {

    def classes: List[String] = Nil
    def className: String

    /** Location of the old class files to be used during pinpointing regression detection.
      * Should not be included in `Config.classpathURLs` and `Benchmark.classpathURLs`.
      */
    def previous: Directory

    /** Maximum recursion depth in the process of finding bottleneck.
      * Value of -1 is stand for unlimited depth.
      */
    def depth: Int

  }

  private var factory: Option[Factory] = None

  def factory(_log: Log, _config: Config) = factory getOrElse {

    val newFactory = new Factory with Configured {
      val log: Log = _log
      val config: Config = _config

      def createFrom(info: BenchmarkInfo): Benchmark = {
        val argMap = BenchmarkInfo.readInfo(argFromSrc(info.src), List(
          classNameOpt,
          methodNameOpt,
          fieldNameOpt,
          excludeOpt,
          multiplierOpt,
          measurementOpt,
          sampleOpt,
          previousOpt,
          depthOpt))
        load(
          info,
          (method: Method, context: ClassLoader) => new Snippet(
            info,
            getIntoOrElse(argMap get multiplierOpt, stringToInt, config.multiplier),
            getIntoOrElse(argMap get measurementOpt, stringToInt, config.measurement),
            getIntoOrElse(argMap get sampleOpt, stringToInt, 0),
            argMap getOrElse (classNameOpt, config.className),
            argMap getOrElse (methodNameOpt, config.methodName),
            getIntoOrElse(argMap get excludeOpt, stringToList, config.exclude),
            getIntoOrElse(argMap get previousOpt, s => Directory(s), config.previous),
            getIntoOrElse(argMap get depthOpt, stringToInt, 1),
            method,
            context,
            config),
          (context: ClassLoader) => new Initializable(
            info,
            Reflection(config, log).getObject[PinpointTemplate](info.name, config.classpathURLs ++ info.classpathURLs),
            context,
            config),
          classOf[PinpointTemplate].getName)
      }

    }

    factory = Some(newFactory)
    newFactory
  }

}
